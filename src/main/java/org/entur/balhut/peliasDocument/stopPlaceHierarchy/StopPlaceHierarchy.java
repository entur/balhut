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

package org.entur.balhut.peliasDocument.stopPlaceHierarchy;

import org.rutebanken.netex.model.StopPlace;

import java.util.*;

public class StopPlaceHierarchy {

    private final StopPlaceHierarchy parent;
    private final StopPlace place;
    private Collection<StopPlaceHierarchy> children;


    public StopPlaceHierarchy(StopPlace place, StopPlaceHierarchy parent) {
        this.place = place;
        this.parent = parent;
    }

    public StopPlaceHierarchy(StopPlace place) {
        this(place, null);
    }


    public StopPlace getPlace() {
        return place;
    }

    public Collection<StopPlaceHierarchy> getChildren() {
        return children;
    }

    public void setChildren(Collection<StopPlaceHierarchy> children) {
        this.children = children;
    }

    public StopPlaceHierarchy getParent() {
        return parent;
    }
}
