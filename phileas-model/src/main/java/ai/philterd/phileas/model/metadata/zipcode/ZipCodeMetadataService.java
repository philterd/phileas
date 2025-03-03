/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class ZipCodeMetadataService implements MetadataService<ZipCodeMetadataRequest, ZipCodeMetadataResponse> {

    private final HashMap<String, Integer> zipCodes2010Census;

    public ZipCodeMetadataService() throws IOException {
        zipCodes2010Census = loadZipCodes2010Census();
    }

    @Override
    public ZipCodeMetadataResponse getMetadata(final ZipCodeMetadataRequest request) {

        final int population = zipCodes2010Census.getOrDefault(request.getZipCode(), -1);

        if(population == -1) {
            // The zip code was not found.
            return new ZipCodeMetadataResponse(-1, false);
        } else {
            return new ZipCodeMetadataResponse(population);
        }

    }

    private HashMap<String, Integer> loadZipCodes2010Census() throws IOException {

        final HashMap<String, Integer> zipcodes = new HashMap<>();

        try (InputStream inputStream =  getClass().getClassLoader().getResourceAsStream("zip-code-population.csv");
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {

            final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {

                if(!line.startsWith("#")) {
                    final String[] zipCodePopulation = line.split(",");

                    final String zipCode = zipCodePopulation[0];
                    final int population = Integer.parseInt(zipCodePopulation[1]);

                    zipcodes.put(zipCode, population);
                }

            }

        }

        return zipcodes;

    }

}
