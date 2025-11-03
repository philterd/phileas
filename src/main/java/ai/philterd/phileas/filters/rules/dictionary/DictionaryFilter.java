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
package ai.philterd.phileas.filters.rules.dictionary;

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.rules.RulesFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A filter that operates on a preset list of dictionary words.
 */
public abstract class DictionaryFilter extends RulesFilter {

    /**
     * Creates a new dictionary-based filter.
     * @param filterType The {@link FilterType type} of the filter.
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     */
    public DictionaryFilter(FilterType filterType, FilterConfiguration filterConfiguration) {
        super(filterType, filterConfiguration);
    }

    public Map<String, Pattern> loadData(final FilterType filterType) throws IOException {

        final Map<String, Pattern> dictionary = new HashMap<>();

        final String fileName;

        if(filterType == FilterType.LOCATION_CITY) {
            fileName = "cities";
        } else if(filterType == FilterType.LOCATION_COUNTY) {
            fileName = "counties";
        } else if(filterType == FilterType.LOCATION_STATE) {
            fileName = "states";
        } else if(filterType == FilterType.HOSPITAL) {
            fileName = "hospitals";
        } else if(filterType == FilterType.HOSPITAL_ABBREVIATION) {
            fileName = "hospital-abbreviations";
        } else if(filterType == FilterType.FIRST_NAME) {
            fileName = "names";
        } else if(filterType == FilterType.SURNAME) {
            fileName = "surnames";
        } else {
            throw new IllegalArgumentException("Invalid filter type.");
        }

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
             final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {

                final Pattern pattern = Pattern.compile("\\b" + line + "\\b", Pattern.CASE_INSENSITIVE);
                dictionary.put(line, pattern);

            }

        }

        return dictionary;

    }

}
