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

package org.entur.balhut.peliasDocument.model;

import java.util.*;

public class PeliasDocument {

    public static final String DEFAULT_INDEX = "pelias";
    public static final String DEFAULT_SOURCE = "kartverket";

    private final String layer;
    private final String sourceId;
    private String defaultName;
    private GeoPoint centerPoint;
    private AddressParts addressParts;
    private Parent parent;
    private long popularity = 1L;
    private String category;

    public PeliasDocument(String layer, String sourceId) {
        this.layer = Objects.requireNonNull(layer);
        this.sourceId = Objects.requireNonNull(sourceId);
    }

    public String layer() {
        return layer;
    }

    public String defaultName() {
        return defaultName;
    }

    public void addDefaultName(String defaultName) {
        this.defaultName = defaultName;
    }

    public AddressParts addressParts() {
        return addressParts;
    }

    public void setAddressParts(AddressParts addressParts) {
        this.addressParts = addressParts;
    }

    public Parent parent() {
        return parent;
    }

    public void setParent(Parent parent) {
        this.parent = parent;
    }

    public Long popularity() {
        return popularity;
    }

    public void setPopularity(Long popularity) {
        this.popularity = popularity;
    }

    public String sourceId() {
        return sourceId;
    }

    public String category() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public GeoPoint centerPoint() {
        return centerPoint;
    }

    public void setCenterPoint(GeoPoint centerPoint) {
        this.centerPoint = centerPoint;
    }
}