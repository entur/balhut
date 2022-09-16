package org.entur.balhut.camel;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.entur.balhut.addresses.AddressToPeliasMapper;
import org.entur.balhut.addresses.AddressToStreetMapper;
import org.entur.balhut.addresses.kartverket.KartverketAddress;
import org.entur.balhut.addresses.kartverket.KartverketAddressReader;
import org.entur.balhut.csv.PeliasDocumentToCSV;
import org.entur.balhut.peliasDocument.model.AddressParts;
import org.entur.balhut.peliasDocument.model.Parent;
import org.entur.balhut.peliasDocument.model.PeliasDocument;
import org.entur.balhut.services.BalhutBlobStoreService;
import org.entur.balhut.services.KakkaBlobStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
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

    @Value("${blobstore.gcs.kakka.tiamat.geocoder.file:tiamat/geocoder/tiamat_export_geocoder_latest.zip}")
    private String tiamatGeocoderFile;

    @Value("${blobstore.gcs.kakka.kartverket.addresses.folder:kartverket/addresses}")
    private String kartverketAddressesFolder;

    @Value("${balhut.workdir:/tmp/balhut/geocoder}")
    private String balhutWorkDir;

    private final KakkaBlobStoreService kakkaBlobStoreService;
    private final BalhutBlobStoreService balhutBlobStoreService;
    private final AddressToPeliasMapper addressMapper;
    private final AddressToStreetMapper addressToStreetMapper;

    public StopPlacesDataRouteBuilder(
            KakkaBlobStoreService kakkaBlobStoreService,
            BalhutBlobStoreService balhutBlobStoreService,
            AddressToPeliasMapper addressMapper,
            AddressToStreetMapper addressToStreetMapper) {
        this.kakkaBlobStoreService = kakkaBlobStoreService;
        this.balhutBlobStoreService = balhutBlobStoreService;
        this.addressMapper = addressMapper;
        this.addressToStreetMapper = addressToStreetMapper;
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

        from("direct:makeCSV")
                .filter(method(StopPlacesDataRouteBuilder.class, "readyToProcess"))
                .process(this::loadAddressesFile)
                .process(this::unzipAddressesFileToWorkingDirectory)
                .process(this::readAddressesCSVFile)
                .process(this::createPeliasDocumentsForAllIndividualAddresses)
                .process(this::addPeliasDocumentForStreets)
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
                balhutWorkDir + "/addresses"
        );
    }

    private void readAddressesCSVFile(Exchange exchange) {
        logger.debug("Read addresses CSV file.");
        try (Stream<Path> paths = Files.walk(Paths.get(balhutWorkDir + "/addresses"))) {
            paths.filter(Files::isRegularFile).findFirst().ifPresent(path -> {
                try (InputStream inputStream = new FileInputStream(path.toFile())) {
                    exchange.getIn().setBody(new KartverketAddressReader().read(inputStream));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createPeliasDocumentsForAllIndividualAddresses(Exchange exchange) {
        logger.debug("Create peliasDocuments for addresses");

        Collection<KartverketAddress> addresses = exchange.getIn().getBody(Collection.class);

        long startTime = System.nanoTime();

        // Create documents for all individual addresses
        List<PeliasDocument> peliasDocuments = addresses.parallelStream()
                .map(addressMapper::toPeliasDocument)
                .collect(Collectors.toList());

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;

        logger.debug("Create documents for all individual addresses duration(ms): " + duration);

        exchange.getIn().setBody(peliasDocuments);
    }

    private void addPeliasDocumentForStreets(Exchange exchange) {
        logger.debug("Add peliasDocuments for streets.");

        List<PeliasDocument> peliasDocuments = exchange.getIn().getBody(List.class);

        long startTime = System.nanoTime();

        // Create separate document per unique street
        peliasDocuments.addAll(addressToStreetMapper.createStreetPeliasDocumentsFromAddresses(peliasDocuments));

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;

        logger.debug("Add peliasDocuments for streets duration(ms): " + duration);

        exchange.getIn().setBody(peliasDocuments);
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