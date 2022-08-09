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

import org.entur.balhut.addresses.kartverket.KartverketAddress;
import org.entur.balhut.addresses.kartverket.KartverketAddressReader;
import org.entur.balhut.peliasDocument.model.PeliasDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressStreamToElasticSearchCommands {

    private final AddressToPeliasMapper addressMapper;

    private final AddressToStreetMapper addressToStreetMapper;

    @Autowired
    public AddressStreamToElasticSearchCommands(AddressToPeliasMapper addressMapper, AddressToStreetMapper addressToStreetMapper) {
        this.addressMapper = addressMapper;
        this.addressToStreetMapper = addressToStreetMapper;
    }

    public Collection<PeliasDocument> transform(InputStream addressStream) {
        Collection<KartverketAddress> addresses = new KartverketAddressReader().read(addressStream);
        // Create documents for all individual addresses
        List<PeliasDocument> peliasDocuments = addresses.stream().map(addressMapper::toPeliasDocument)
                .sorted(Comparator.comparing(PeliasDocument::defaultName)).collect(Collectors.toList());

        // Create separate document per unique street
        peliasDocuments.addAll(addressToStreetMapper.createStreetPeliasDocumentsFromAddresses(peliasDocuments));

        return peliasDocuments;
    }
}
