package org.entur.balhut.csv;

import com.opencsv.CSVWriter;
import org.entur.balhut.peliasDocument.model.Parent;
import org.entur.balhut.peliasDocument.model.PeliasDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.entur.balhut.csv.CSVHeader.*;
import static org.entur.balhut.peliasDocument.model.PeliasDocument.DEFAULT_INDEX;
import static org.entur.balhut.peliasDocument.model.PeliasDocument.DEFAULT_SOURCE;

public final class PeliasDocumentToCSV {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeliasDocumentToCSV.class);

    public InputStream makeCSVFromPeliasDocument(List<PeliasDocument> peliasDocuments) {

        LOGGER.debug("Creating CSV file for " + peliasDocuments.size() + " pelias documents");

        var csvDocumentsAsStringArrays = peliasDocuments.parallelStream()
                .map(PeliasDocumentToCSV::createCSVDocument)
                .map(PeliasDocumentToCSV::createStringArray)
                .toList();

        return writeStringArraysToCSVFile(csvDocumentsAsStringArrays);
    }

    private static String[] createStringArray(HashMap<CSVHeader, CSVValue> csvDocument) {
        return Stream.of(CSVHeader.values())
                .map(header -> csvDocument.computeIfAbsent(header, h -> CSVValue("")))
                .map(CSVValue::toString)
                .toArray(String[]::new);
    }

    private static FileInputStream writeStringArraysToCSVFile(List<String[]> stringArrays) {
        LOGGER.debug("Writing CSV data to output stream");
        try {
            File file = File.createTempFile("temp", "csv");
            try (FileOutputStream fis = new FileOutputStream(file)) {
                try (var writer = new CSVWriter(new OutputStreamWriter(fis))) {
                    writer.writeNext(Stream.of(CSVHeader.values()).map(CSVHeader::columnName).toArray(String[]::new));
                    for (String[] array : stringArrays) {
                        writer.writeNext(array);
                    }
                }
            }
            return new FileInputStream(file);
        } catch (Exception exception) {
            throw new RuntimeException("Fail to create csv.", exception);
        }
    }

    private static HashMap<CSVHeader, CSVValue> createCSVDocument(PeliasDocument peliasDocument) {

        var map = new HashMap<CSVHeader, CSVValue>();
        map.put(ID, CSVValue(peliasDocument.sourceId()));
        map.put(INDEX, CSVValue(DEFAULT_INDEX));
        map.put(TYPE, CSVValue(peliasDocument.layer()));
        map.put(SOURCE, CSVValue(DEFAULT_SOURCE));
        map.put(SOURCE_ID, CSVValue(peliasDocument.sourceId()));
        map.put(LAYER, CSVValue(peliasDocument.layer()));
        map.put(POPULARITY, CSVValue(peliasDocument.popularity()));
        map.put(CATEGORY, CSVJsonValue(List.of(peliasDocument.category())));
        if (peliasDocument.parent() != null) {
            map.put(PARENT, CSVJsonValue(wrapValidParentFieldsInLists(peliasDocument.parent().getParentFields())));
        }

        map.put(NAME, CSVValue(peliasDocument.defaultName()));
        if (peliasDocument.centerPoint() != null) {
            map.put(LATITUDE, CSVValue(peliasDocument.centerPoint().lat()));
            map.put(LONGITUDE, CSVValue(peliasDocument.centerPoint().lon()));
        }
        if (peliasDocument.addressParts() != null) {
            map.put(ADDRESS_STREET, CSVValue(peliasDocument.addressParts().getStreet()));
            map.put(ADDRESS_NUMBER, CSVValue(peliasDocument.addressParts().getNumber()));
            map.put(ADDRESS_ZIP, CSVValue(peliasDocument.addressParts().getZip()));
            // TODO: Test, if address name is required, Name is not supported by pelias csv-importer
        }

        return map;
    }


    /**
     * See the following comment to learn why we need to do this.
     * https://github.com/pelias/csv-importer/pull/97#issuecomment-1203920795
     */
    private static Map<Parent.FieldName, List<Parent.Field>> wrapValidParentFieldsInLists(Map<Parent.FieldName, Parent.Field> parentFields) {

        List<Map.Entry<Parent.FieldName, Parent.Field>> collect = parentFields.entrySet()
                .stream()
                .filter(entry -> !entry.getValue().isValid()).toList();

        return parentFields.entrySet()
                .stream()
                .filter(entry -> entry.getValue().isValid())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> List.of(entry.getValue())));
    }

    private static CSVValue CSVValue(Object value) {
        return new CSVValue(value, false);
    }

    private static CSVValue CSVJsonValue(Object value) {
        return new CSVValue(value, true);
    }
}
