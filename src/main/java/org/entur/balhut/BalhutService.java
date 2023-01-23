package org.entur.balhut;

import org.entur.balhut.addresses.PeliasDocumentAddressMapper;
import org.entur.balhut.addresses.PeliasDocumentStreetMapper;
import org.entur.balhut.addresses.kartverket.KartverketAddress;
import org.entur.balhut.addresses.kartverket.KartverketAddressReader;
import org.entur.balhut.blobStore.BalhutBlobStoreService;
import org.entur.balhut.blobStore.KakkaBlobStoreService;
import org.entur.geocoder.Utilities;
import org.entur.geocoder.ZipUtilities;
import org.entur.geocoder.csv.CSVCreator;
import org.entur.geocoder.model.PeliasDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Service
public class BalhutService {

    private static final Logger logger = LoggerFactory.getLogger(BalhutService.class);

    @Value("${blobstore.gcs.kakka.kartverket.addresses.folder:kartverket/addresses}")
    private String kartverketAddressesFolder;

    @Value("${balhut.workdir:/tmp/balhut/geocoder}")
    private String balhutWorkDir;

    private final KakkaBlobStoreService kakkaBlobStoreService;
    private final BalhutBlobStoreService balhutBlobStoreService;
    private final PeliasDocumentAddressMapper peliasDocumentAddressMapper;
    private final PeliasDocumentStreetMapper peliasDocumentStreetMapper;

    public BalhutService(KakkaBlobStoreService kakkaBlobStoreService,
                         BalhutBlobStoreService balhutBlobStoreService,
                         PeliasDocumentAddressMapper peliasDocumentAddressMapper,
                         PeliasDocumentStreetMapper peliasDocumentStreetMapper) {
        this.kakkaBlobStoreService = kakkaBlobStoreService;
        this.balhutBlobStoreService = balhutBlobStoreService;
        this.peliasDocumentAddressMapper = peliasDocumentAddressMapper;
        this.peliasDocumentStreetMapper = peliasDocumentStreetMapper;
    }

    @Retryable(
            value = Exception.class,
            maxAttemptsExpression = "${balhut.retry.maxAttempts:3}",
            backoff = @Backoff(
                    delayExpression = "${balhut.retry.maxDelay:5000}",
                    multiplierExpression = "${balhut.retry.backoff.multiplier:3}"))
    protected InputStream loadAddressesFile() {
        logger.info("Loading addresses file");
        return kakkaBlobStoreService.findLatestBlob(kartverketAddressesFolder);
    }

    protected Path unzipAddressesFileToWorkingDirectory(InputStream inputStream) {
        logger.info("Unzipping addresses file");
        ZipUtilities.unzipFile(inputStream, balhutWorkDir + "/addresses");
        try (Stream<Path> paths = Files.walk(Paths.get(balhutWorkDir + "/addresses"))) {
            return paths
                    .filter(Utilities::isValidFile)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Unzipped file not found."));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected Stream<KartverketAddress> readKartverketAddressesFromFile(Path path) {
        logger.info("Read kartverket addresses file");
        return KartverketAddressReader.read(path);
    }

    protected List<PeliasDocument> createPeliasDocumentsForAllIndividualAddresses(Stream<KartverketAddress> kartverketAddresses) {
        logger.info("Converting stream of kartverket addresses to pelias documents");

        // Create documents for all individual addresses
        return kartverketAddresses
                .parallel()
                .map(peliasDocumentAddressMapper::toPeliasDocument)
                .toList();
    }

    protected Stream<PeliasDocument> addPeliasDocumentStreamForStreets(List<PeliasDocument> individualAddressDocuments) {
        logger.info("Adding peliasDocuments stream for unique streets");

        // Create separate document per unique street
        return Stream.concat(
                individualAddressDocuments.stream(),
                peliasDocumentStreetMapper.createStreetPeliasDocumentsFromAddresses(individualAddressDocuments));
    }

    protected InputStream createCSVFile(Stream<PeliasDocument> peliasDocuments) {
        logger.info("Creating CSV file form PeliasDocuments stream");
        return CSVCreator.create(peliasDocuments);
    }

    protected String getOutputFilename() {
        return "balhut_export_geocoder_" + System.currentTimeMillis();
    }

    protected InputStream zipCSVFile(InputStream inputStream, String filename) {
        logger.info("Zipping the created csv file");
        return ZipUtilities.zipFile(inputStream, filename + ".csv");
    }

    @Retryable(
            value = Exception.class,
            maxAttemptsExpression = "${balhut.retry.maxAttempts:3}",
            backoff = @Backoff(
                    delayExpression = "${balhut.retry.maxDelay:5000}",
                    multiplierExpression = "${balhut.retry.backoff.multiplier:3}"))
    protected void uploadCSVFile(InputStream csvZipFile, String filename) {
        logger.info("Uploading the zipped CSV file to balhut");
        balhutBlobStoreService.uploadBlob(filename + ".zip", csvZipFile);
    }

    @Retryable(
            value = Exception.class,
            maxAttemptsExpression = "${balhut.retry.maxAttempts:3}",
            backoff = @Backoff(
                    delayExpression = "${balhut.retry.maxDelay:5000}",
                    multiplierExpression = "${balhut.retry.backoff.multiplier:3}"))
    protected void copyCSVFileAsLatestToConfiguredBucket(String filename) {
        logger.info("Copying latest file to haya");
        balhutBlobStoreService.copyBlobAsLatestToTargetBucket(filename + ".zip");
    }
}