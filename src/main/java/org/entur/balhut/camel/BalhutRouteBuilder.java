package org.entur.balhut.camel;

import org.apache.camel.Exchange;
import org.entur.balhut.addresses.AddressToPeliasMapper;
import org.entur.balhut.addresses.AddressToStreetMapper;
import org.entur.balhut.addresses.kartverket.KartverketAddress;
import org.entur.balhut.addresses.kartverket.KartverketAddressReader;
import org.entur.balhut.blobStore.BalhutBlobStoreService;
import org.entur.balhut.blobStore.KakkaBlobStoreService;
import org.entur.geocoder.Utilities;
import org.entur.geocoder.ZipUtilities;
import org.entur.geocoder.camel.ErrorHandlerRouteBuilder;
import org.entur.geocoder.csv.CSVCreator;
import org.entur.geocoder.model.PeliasDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Component
public class BalhutRouteBuilder extends ErrorHandlerRouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(BalhutRouteBuilder.class);

    private static final String OUTPUT_FILENAME_HEADER = "balhutOutputFilename";

    @Value("${blobstore.gcs.kakka.kartverket.addresses.folder:kartverket/addresses}")
    private String kartverketAddressesFolder;

    @Value("${balhut.workdir:/tmp/balhut/geocoder}")
    private String balhutWorkDir;

    private final KakkaBlobStoreService kakkaBlobStoreService;
    private final BalhutBlobStoreService balhutBlobStoreService;
    private final AddressToPeliasMapper addressMapper;
    private final AddressToStreetMapper addressToStreetMapper;

    public BalhutRouteBuilder(
            KakkaBlobStoreService kakkaBlobStoreService,
            BalhutBlobStoreService balhutBlobStoreService,
            AddressToPeliasMapper addressMapper,
            AddressToStreetMapper addressToStreetMapper,
            @Value("${balhut.camel.redelivery.max:3}") int maxRedelivery,
            @Value("${balhut.camel.redelivery.delay:5000}") int redeliveryDelay,
            @Value("${balhut.camel.redelivery.backoff.multiplier:3}") int backOffMultiplier) {

        super(maxRedelivery, redeliveryDelay, backOffMultiplier);
        this.kakkaBlobStoreService = kakkaBlobStoreService;
        this.balhutBlobStoreService = balhutBlobStoreService;
        this.addressMapper = addressMapper;
        this.addressToStreetMapper = addressToStreetMapper;
    }

    @Override
    public void configure() {

        from("direct:makeCSV")
                .process(this::loadAddressesFile)
                .process(this::unzipAddressesFileToWorkingDirectory)
                .process(this::readAddressesCSVFile)
                .process(this::createPeliasDocumentStreamForAllIndividualAddresses)
                .process(this::addPeliasDocumentStreamForStreets)
                .process(this::createCSVFile)
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
        logger.debug("Read addresses CSV file");
        try (Stream<Path> paths = Files.walk(Paths.get(balhutWorkDir + "/addresses"))) {
            paths.filter(Utilities::isValidFile).findFirst().ifPresent(path -> {
                exchange.getIn().setBody(KartverketAddressReader.read(path));
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createPeliasDocumentStreamForAllIndividualAddresses(Exchange exchange) {
        logger.debug("Converting stream of kartverket addresses to stream of pelias documents.");

        @SuppressWarnings("unchecked")
        Stream<KartverketAddress> addresses = exchange.getIn().getBody(Stream.class);
        // Create documents for all individual addresses
        List<PeliasDocument> peliasDocuments = addresses.parallel()
                .map(addressMapper::toPeliasDocument).toList();

        exchange.getIn().setBody(peliasDocuments);
    }

    private void addPeliasDocumentStreamForStreets(Exchange exchange) {
        logger.debug("Adding peliasDocuments stream for unique streets");

        @SuppressWarnings("unchecked")
        List<PeliasDocument> peliasDocumentsForIndividualAddresses = exchange.getIn().getBody(List.class);

        // Create separate document per unique street
        exchange.getIn().setBody(
                Stream.concat(
                        peliasDocumentsForIndividualAddresses.stream(),
                        addressToStreetMapper.createStreetPeliasDocumentsFromAddresses(peliasDocumentsForIndividualAddresses))
        );
    }

    private void createCSVFile(Exchange exchange) {
        logger.debug("Creating CSV file form PeliasDocuments stream");

        @SuppressWarnings("unchecked")
        Stream<PeliasDocument> peliasDocuments = exchange.getIn().getBody(Stream.class);
        exchange.getIn().setBody(CSVCreator.create(peliasDocuments));
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
}