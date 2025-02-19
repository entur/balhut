package org.entur.balhut;

import java.io.InputStream;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class BalhutApplication implements ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(
    BalhutApplication.class
  );

  private final BalhutService bs;

  public BalhutApplication(BalhutService bs) {
    this.bs = bs;
  }

  public static void main(String[] args) {
    SpringApplication.run(BalhutApplication.class, args);
  }

  @Override
  public void run(ApplicationArguments args) {
    Stream
      .of(bs.loadAddressesFile())
      .map(bs::unzipAddressesFileToWorkingDirectory)
      .map(bs::readKartverketAddressesFromFile)
      .map(bs::createPeliasDocumentsForAllIndividualAddresses)
      .map(bs::addPeliasDocumentStreamForStreets)
      .map(bs::createCSVFile)
      .findFirst()
      .ifPresentOrElse(
        this::zipAndUploadCSVFile,
        () -> logger.info("No or empty addresses file found.")
      );
  }

  private void zipAndUploadCSVFile(InputStream inputStream) {
    String outputFilename = bs.getOutputFilename();
    InputStream csvZipFile = bs.zipCSVFile(inputStream, outputFilename);
    bs.uploadCSVFile(csvZipFile, outputFilename);
    bs.copyCSVFileAsLatestToConfiguredBucket(outputFilename);
    logger.info("Uploaded zipped csv files to balhut and haya");
  }
}
