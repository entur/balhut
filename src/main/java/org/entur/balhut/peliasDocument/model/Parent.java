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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.*;

import static org.entur.balhut.peliasDocument.model.PeliasDocument.DEFAULT_SOURCE;

public class Parent {

    private final Map<FieldName, Field> fields = new HashMap<>();

    public Parent (FieldName fieldName, Field field) {
        addOrReplaceParentField(fieldName, field);
    }

    public void addOrReplaceParentField(FieldName fieldName, Field field) {
        fields.compute(fieldName, (fldName, fld) -> field);
    }

    public void setNameFor(FieldName fieldName, String name) {
        fields.computeIfPresent(fieldName, (fldName, field) -> new Field(field.id(), name, field.abbr()));
    }

    public Optional<String> idFor(Parent.FieldName fieldName) {
        return Optional.ofNullable(fields.get(fieldName)).map(Parent.Field::id);
    }

    public Optional<String> nameFor(Parent.FieldName fieldName) {
        return Optional.ofNullable(fields.get(fieldName)).map(Parent.Field::name);
    }

    public Map<FieldName, Field> getParentFields() {
        return fields;
    }

    public record Field(String id, String name, String abbr, String source) {

        public Field(String id, String name) {
            this(id, name, null, DEFAULT_SOURCE);
        }

        public Field(String id, String name, String abbr) {
            this(id, name, abbr, DEFAULT_SOURCE);
        }

        @JsonIgnore
        public boolean isValid() {
            return this.id != null && !this.id.isBlank() && this.name != null && !this.name.isBlank();
        }
    }

    public enum FieldName {
        COUNTRY("country"),
        COUNTY("county"),
        BOROUGH("borough"),
        POSTAL_CODE("postalcode"),
        LOCALITY("locality");

        private final String value;

        FieldName(String value) {
            this.value = value;
        }

        @JsonValue
        public String value() {
            return value;
        }
    }
}