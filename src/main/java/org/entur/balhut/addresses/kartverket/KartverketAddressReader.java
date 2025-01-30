package org.entur.balhut.addresses.kartverket;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KartverketAddressReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    KartverketAddressReader.class
  );

  public static Stream<KartverketAddress> read(Path csvFilePath) {
    LOGGER.debug("Reading Kartverket addresses from " + csvFilePath);
    try {
      // Intentionally not closing the reader, since we are using the stream further in the process.
      Reader reader = Files.newBufferedReader(csvFilePath);
      CsvToBean<KartverketAddress> cb = new CsvToBeanBuilder<KartverketAddress>(
        reader
      )
        .withType(KartverketAddress.class)
        .withSeparator(';')
        .withSkipLines(1)
        .build();
      return cb.stream();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
