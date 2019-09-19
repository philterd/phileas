package com.mtnfog.phileas.model.metadata.zipcode;

import com.mtnfog.phileas.model.metadata.MetadataService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;

public class ZipCodeMetadataService implements MetadataService<ZipCodeMetadataRequest, ZipCodeMetadataResponse> {

    private HashMap<String, Integer> zipCodes2010Census;

    public ZipCodeMetadataService() throws IOException {
        zipCodes2010Census = loadZipCodes2010Census();
    }

    @Override
    public ZipCodeMetadataResponse getMetadata(ZipCodeMetadataRequest request) {

        final int population = zipCodes2010Census.get(request.getZipCode());

        return new ZipCodeMetadataResponse(population);

    }

    private HashMap<String, Integer> loadZipCodes2010Census() throws IOException {

        final HashMap<String, Integer> zipcodes = new HashMap<>();

        final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("2010+Census+Population+By+Zipcode.csv");

        final Reader reader = new InputStreamReader(inputStream);
        final CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);

        for (final CSVRecord csvRecord : csvParser) {

            final String zipCode = csvRecord.get(0);
            final int population = Integer.parseInt(csvRecord.get(1));

            zipcodes.put(zipCode, population);

        }

        reader.close();

        return zipcodes;

    }

}
