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


import java.util.HashMap;
import java.util.Map;

/**
 * KOORDINATSYSTEMKODE;       Sosikoden for koordinatsystemet. Verdiene er:
 * 1   NGO1948 Gauss-K. Akse 1
 * 2   NGO1948 Gauss-K. Akse 2
 * 3   NGO1948 Gauss-K. Akse 3
 * 4   NGO1948 Gauss-K. Akse 4
 * 5   NGO1948 Gauss-K. Akse 5
 * 6   NGO1948 Gauss-K. Akse 6
 * 7   NGO1948 Gauss-K. Akse 7
 * 8   NGO1948 Gauss-K. Akse 8
 * 9   NGO1948 Geografisk
 * 21   EUREF89 UTM Sone 31
 * 22   EUREF89 UTM Sone 32
 * 23   EUREF89 UTM Sone 33
 * 24   EUREF89 UTM Sone 34
 * 25   EUREF89 UTM Sone 35
 * 26   EUREF89 UTM Sone 36
 * 31   ED50 UTM Sone 31
 * 32   ED50 UTM Sone 32
 * 33   ED50 UTM Sone 33
 * 34   ED50 UTM Sone 34
 * 35   ED50 UTM Sone 35
 * 36   ED50 UTM Sone 36
 * 50   ED50 Geografisk
 * 53   Møre A
 * 54   Møre B
 * 84   EUREF89 Geografisk
 * 51   NGO 56A (Møre)
 * 52   NGO 56B (Møre)
 */
public class KartverketCoordinateSystemMapper {

    private static final Map<String, String> COORDINATE_SYSTEM_MAPPING = new HashMap<>() {{

        // TODO: Trenger vi de gamle koder?
        put("21", "31");
        put("22", "32");
        put("23", "33");
        put("24", "34");
        put("25", "35");
        put("26", "36");

        // EPSG to utm mapping since new dataset form kartverket uses EPSG codes
        // https://register.geonorge.no/epsg-koder?register=SOSI+kodelister&text=

        put("25831", "31");
        put("25832", "32");
        put("25833", "33");
        put("25834", "34");
        put("25835", "35");
        put("25836", "36");
    }};

    public static String toUTMZone(String kartverketCoordinateSystemCode) {
        return COORDINATE_SYSTEM_MAPPING.get(kartverketCoordinateSystemCode);
    }
}
