/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 */

package org.entur.balhut.addresses.kartverket;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class KartverketAddressReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(KartverketAddressReader.class);

    public static Stream<KartverketAddress> read(Path csvFilePath) {
        LOGGER.debug("Reading Kartverket addresses from " + csvFilePath);
        try {
            // Intentionally not closing the reader, since we are using the stream further in the process.
            Reader reader = Files.newBufferedReader(csvFilePath);
            CsvToBean<KartverketAddress> cb = new CsvToBeanBuilder<KartverketAddress>(reader)
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
