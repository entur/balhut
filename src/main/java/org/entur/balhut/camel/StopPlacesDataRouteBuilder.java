package org.entur.balhut.camel;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.entur.balhut.addresses.AddressStreamToElasticSearchCommands;
import org.entur.balhut.adminUnitsCache.AdminUnitsCache;
import org.entur.balhut.csv.PeliasDocumentToCSV;
import org.entur.balhut.peliasDocument.stopPlacestoPeliasDocument.StopPlacesToPeliasDocument;
import org.entur.balhut.services.BalhutBlobStoreService;
import org.entur.balhut.services.KakkaBlobStoreService;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Component
public class StopPlacesDataRouteBuilder extends RouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(StopPlacesDataRouteBuilder.class);

    private static final String OUTPUT_FILENAME_HEADER = "balhutOutputFilename";
    private static final String ADMIN_UNITS_CACHE_PROPERTY = "AdminUnitsCache";

    @Value("${balhut.camel.redelivery.max:3}")
    private int maxRedelivery;

    @Value("${balhut.camel.redelivery.delay:5000}")
    private int redeliveryDelay;

    @Value("${balhut.camel.redelivery.backoff.multiplier:3}")
    private int backOffMultiplier;

    @Value("${balhut.update.cron.schedule:0+0/3+*+1/1+*+?+*}")
    private String cronSchedule;

    @Value("${blobstore.gcs.kakka.tiamat.geocoder.file:kartverket/addresses}")
    private String kartverketAddressesFolder;

    @Value("${balhut.workdir:/tmp/balhut/geocoder}")
    private String balhutWorkDir;

    @Value("${admin.units.cache.max.size:30000}")
    private Integer cacheMaxSize;

    private final AddressStreamToElasticSearchCommands addressStreamToElasticSearchCommands;

    private final KakkaBlobStoreService kakkaBlobStoreService;
    private final BalhutBlobStoreService balhutBlobStoreService;
    private final StopPlacesToPeliasDocument stopPlacesToPeliasDocument;

    public StopPlacesDataRouteBuilder(
            AddressStreamToElasticSearchCommands addressStreamToElasticSearchCommands, KakkaBlobStoreService kakkaBlobStoreService,
            BalhutBlobStoreService balhutBlobStoreService,
            StopPlacesToPeliasDocument stopPlacesToPeliasDocument) {
        this.addressStreamToElasticSearchCommands = addressStreamToElasticSearchCommands;
        this.kakkaBlobStoreService = kakkaBlobStoreService;
        this.balhutBlobStoreService = balhutBlobStoreService;
        this.stopPlacesToPeliasDocument = stopPlacesToPeliasDocument;
    }

    @Override
    public void configure() {

        errorHandler(defaultErrorHandler()
                .redeliveryDelay(redeliveryDelay)
                .maximumRedeliveries(maxRedelivery)
                .onRedelivery(StopPlacesDataRouteBuilder::logRedelivery)
                .useExponentialBackOff()
                .backOffMultiplier(backOffMultiplier)
                .logExhausted(true)
                .logRetryStackTrace(true));

        from("quartz://balhut/makeCSV?cron=" + cronSchedule + "&trigger.timeZone=Europe/Oslo")
                .to("direct:makeCSV");

        from("direct:makeCSV")
                .process(this::loadAddressesFile)
                .process(this::unzipAddressesFileToWorkingDirectory)
                .process(this::parseNetexFile)
                .process(this::buildAdminUnitCache) // TODO: Trenger vi det ????, Vi enricher ikke vel addresses data ??
                .process(this::netexEntitiesIndexToPeliasDocuments)
                .bean(PeliasDocumentToCSV::new)
                .process(this::setOutputFilenameHeader)
                .process(this::zipCSVFile)
                .process(this::uploadCSVFile)
                .process(this::updateCurrentFile);
    }

    private void loadAddressesFile(Exchange exchange) {
        logger.debug("Loading addresses file");
        exchange.getIn().setBody(
                kakkaBlobStoreService.findLatestBlob(kartverketAddressesFolder),
                InputStream.class
        );
    }

    private void unzipAddressesFileToWorkingDirectory(Exchange exchange) {
        logger.debug("Unzipping addresses file");
        ZipUtilities.unzipFile(
                exchange.getIn().getBody(InputStream.class),
                balhutWorkDir
        );
    }

    private void parseNetexFile(Exchange exchange) {
        logger.debug("Parsing the Netex file.");
        try (Stream<Path> paths = Files.walk(Paths.get(balhutWorkDir))) {
            paths.filter(Files::isRegularFile).findFirst().ifPresent(path -> {
                try (InputStream inputStream = new FileInputStream(path.toFile())) {

                    addressStreamToElasticSearchCommands.transform(inputStream);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void buildAdminUnitCache(Exchange exchange) {
        logger.debug("Building admin units cache.");
        var netexEntitiesIndex = exchange.getIn().getBody(NetexEntitiesIndex.class);
        var adminUnitsCache = AdminUnitsCache.buildNewCache(netexEntitiesIndex, cacheMaxSize);
        exchange.setProperty(ADMIN_UNITS_CACHE_PROPERTY, adminUnitsCache);
    }

    private void netexEntitiesIndexToPeliasDocuments(Exchange exchange) {
        logger.debug("Converting netexEntitiesIndex to PeliasDocuments");
        var netexEntitiesIndex = exchange.getIn().getBody(NetexEntitiesIndex.class);
        AdminUnitsCache adminUnitCache = exchange.getProperty(ADMIN_UNITS_CACHE_PROPERTY, AdminUnitsCache.class);
        exchange.getIn().setBody(stopPlacesToPeliasDocument.toPeliasDocuments(netexEntitiesIndex, adminUnitCache));
    }

    private void setOutputFilenameHeader(Exchange exchange) {
        exchange.getIn().setHeader(
                OUTPUT_FILENAME_HEADER,
                "balhut_export_geocoder_" + System.currentTimeMillis()
        );
    }

    private void zipCSVFile(Exchange exchange) {
        logger.debug("Zipping the created csv file");
        ByteArrayInputStream zipFile = ZipUtilities.zipFile(
                exchange.getIn().getBody(InputStream.class),
                exchange.getIn().getHeader(OUTPUT_FILENAME_HEADER, String.class) + ".csv"
        );
        exchange.getIn().setBody(zipFile);
    }

    private void uploadCSVFile(Exchange exchange) {
        logger.debug("Uploading the CSV file");
        balhutBlobStoreService.uploadBlob(
                exchange.getIn().getHeader(OUTPUT_FILENAME_HEADER, String.class) + ".zip",
                exchange.getIn().getBody(InputStream.class)
        );
    }

    private void updateCurrentFile(Exchange exchange) {
        logger.debug("Updating the current file");
        String currentCSVFileName = exchange.getIn().getHeader(OUTPUT_FILENAME_HEADER, String.class) + ".zip";
        balhutBlobStoreService.uploadBlob(
                "current",
                new ByteArrayInputStream(currentCSVFileName.getBytes())
        );
    }

    private static void logRedelivery(Exchange exchange) {
        var redeliveryCounter = exchange.getIn().getHeader("CamelRedeliveryCounter", Integer.class);
        var redeliveryMaxCounter = exchange.getIn().getHeader("CamelRedeliveryMaxCounter", Integer.class);
        var camelCaughtThrowable = exchange.getProperty("CamelExceptionCaught", Throwable.class);

        logger.warn("Exchange failed, redelivering the message locally, attempt {}/{}...",
                redeliveryCounter, redeliveryMaxCounter, camelCaughtThrowable);
    }
}