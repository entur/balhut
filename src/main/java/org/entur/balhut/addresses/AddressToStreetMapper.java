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

package org.entur.balhut.addresses;

import org.entur.geocoder.model.AddressParts;
import org.entur.geocoder.model.ParentType;
import org.entur.geocoder.model.PeliasDocument;
import org.entur.geocoder.model.PeliasDocumentList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.entur.balhut.addresses.AddressToPeliasMapper.DEFAULT_LAYER;
import static org.entur.balhut.addresses.AddressToPeliasMapper.DEFAULT_SOURCE;

/**
 * Create "street" documents for Pelias from addresses.
 * <p>
 * Streets are assumed to be contained fully in a single locality (kommune) and the names for streets are assumed to be unique within a single locality.
 * <p>
 * Centerpoint and parent info for street is fetched from the median address (ordered by number + alpha) in the street.
 * <p>
 * NB! Streets are stored in the "address" layer in pelias, as this is prioritized
 */
@Service
public class AddressToStreetMapper {

    private static final String STREET_CATEGORY = "street";

    private final long popularity;

    public AddressToStreetMapper(@Value("${pelias.address.street.boost:2}") long popularity) {
        this.popularity = popularity;
    }

    public PeliasDocumentList createStreetPeliasDocumentsFromAddresses(PeliasDocumentList addresses) {
        Collection<PeliasDocumentList> addressesPerStreet =
                addresses.stream()
                        .filter(address ->
                                address.getAddressParts() != null && !ObjectUtils.isEmpty(address.getAddressParts().street())
                        ).collect(Collectors.groupingBy(
                                UniqueStreetKey::new,
                                Collectors.mapping(Function.identity(), Collectors.toCollection(PeliasDocumentList::new)))
                        ).values();

        return addressesPerStreet.stream()
                .map(this::createPeliasStreetDocFromAddresses).collect(Collectors.toCollection(PeliasDocumentList::new));
    }

    private PeliasDocument createPeliasStreetDocFromAddresses(PeliasDocumentList addressesOnStreet) {
        PeliasDocument templateAddress = getAddressRepresentingStreet(addressesOnStreet);

        String streetName = templateAddress.getAddressParts().street();
        String uniqueId = templateAddress.getParents().idFor(ParentType.LOCALITY) + "-" + streetName;
        PeliasDocument streetDocument = new PeliasDocument(DEFAULT_LAYER, DEFAULT_SOURCE, uniqueId);

        streetDocument.setDefaultName(streetName);
        streetDocument.setParents(templateAddress.getParents());

        streetDocument.setCenterPoint(templateAddress.getCenterPoint());
        streetDocument.setAddressParts(new AddressParts(streetName));

        streetDocument.addCategory(STREET_CATEGORY);
        streetDocument.setPopularity(popularity);

        return streetDocument;
    }

    /**
     * Use median address in street (ordered by number + alpha) as representative of the street.
     */
    private static PeliasDocument getAddressRepresentingStreet(List<PeliasDocument> addressesOnStreet) {
        addressesOnStreet.sort(Comparator.comparing(o -> o.getAddressParts().number()));
        return addressesOnStreet.get(addressesOnStreet.size() / 2);
    }

    public record UniqueStreetKey(String streetName, String localityId) {

        public UniqueStreetKey(PeliasDocument peliasDocument) {
            this(peliasDocument.getAddressParts().street(),
                    peliasDocument.getParents().idFor(ParentType.LOCALITY).orElse(null)
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UniqueStreetKey that = (UniqueStreetKey) o;

            if (!Objects.equals(streetName, that.streetName)) return false;
            return Objects.equals(localityId, that.localityId);
        }

        @Override
        public int hashCode() {
            int result = streetName != null ? streetName.hashCode() : 0;
            result = 31 * result + (localityId != null ? localityId.hashCode() : 0);
            return result;
        }
    }
}
