package org.entur.balhut.addresses;

import org.entur.geocoder.model.AddressParts;
import org.entur.geocoder.model.ParentType;
import org.entur.geocoder.model.PeliasDocument;
import org.entur.geocoder.model.PeliasId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
public class PeliasDocumentStreetMapper {

    private static final String DEFAULT_SOURCE = "KVE";
    private static final String STREET_LAYER = "Street";

    private final long popularity;

    public PeliasDocumentStreetMapper(@Value("${pelias.address.street.boost:2}") long popularity) {
        this.popularity = popularity;
    }

    public Stream<PeliasDocument> createStreetPeliasDocumentsFromAddresses(List<PeliasDocument> peliasDocuments) {
        Collection<ArrayList<PeliasDocument>> addressesPerStreet = peliasDocuments.stream()
                .filter(PeliasDocumentStreetMapper::hasValidAddress)
                .collect(Collectors.groupingBy(
                        UniqueStreetKey::new,
                        Collectors.mapping(Function.identity(), Collectors.toCollection(ArrayList::new)))
                ).values();

        return addressesPerStreet.stream().map(this::createPeliasStreetDocFromAddresses);
    }

    private static boolean hasValidAddress(PeliasDocument peliasDocument) {
        return peliasDocument.getAddressParts() != null && !ObjectUtils.isEmpty(peliasDocument.getAddressParts().street());
    }

    private PeliasDocument createPeliasStreetDocFromAddresses(ArrayList<PeliasDocument> addressesOnStreet) {
        PeliasDocument templateAddress = getAddressRepresentingStreet(addressesOnStreet);

        String streetName = templateAddress.getAddressParts().street();
        String uniqueId = templateAddress.getParents().idFor(ParentType.LOCALITY) + "-" + streetName;
        PeliasDocument streetDocument = new PeliasDocument(new PeliasId(DEFAULT_SOURCE, STREET_LAYER, uniqueId));

        streetDocument.setDefaultName(streetName);

        streetDocument.getParents().addOrReplaceParents(templateAddress.getParents().parents());

        streetDocument.setCenterPoint(templateAddress.getCenterPoint());
        streetDocument.setAddressParts(new AddressParts(streetName));

        streetDocument.addCategory("street");
        streetDocument.addCategory("address");
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

    public record UniqueStreetKey(String streetName, PeliasId localityId) {

        public UniqueStreetKey(PeliasDocument peliasDocument) {
            this(peliasDocument.getAddressParts().street(),
                    peliasDocument.getParents().idFor(ParentType.LOCALITY)
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
