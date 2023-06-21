/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.model.metadata.zipcode;

import ai.philterd.phileas.model.metadata.MetadataService;
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
