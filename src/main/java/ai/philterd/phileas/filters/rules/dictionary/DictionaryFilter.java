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

import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.rules.RulesFilter;
import ai.philterd.phileas.model.filtering.FilterType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * A filter that operates on a preset list of dictionary words.
 */
public abstract class DictionaryFilter extends RulesFilter {

    // Predefined dictionaries (cities, counties, states, hospitals, first names, surnames) are loaded
    // from bundled resource files and are identical across every FilterService instance. They are
    // large (names especially), so they are loaded once per type and shared process-wide rather than
    // duplicated per instance. The cached structures are immutable, so concurrent reads are safe.
    // Custom (policy-supplied) dictionaries are not cached here; they vary per policy.
    private static final Map<FilterType, PredefinedDictionary> PREDEFINED_DICTIONARIES = new ConcurrentHashMap<>();

    /**
     * An immutable, shareable predefined dictionary: the term-to-pattern map (used by fuzzy matching),
     * the lowercased terms (used by exact set matching), and the maximum n-gram size.
     */
    record PredefinedDictionary(Map<String, Pattern> data, Set<String> lowerCaseTerms, int maxNgramSize) {
    }

    /**
     * Creates a new dictionary-based filter.
     * @param filterType The {@link FilterType type} of the filter.
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     */
    public DictionaryFilter(FilterType filterType, FilterConfiguration filterConfiguration) {
        super(filterType, filterConfiguration);
    }

    /**
     * Returns the term-to-pattern map for a predefined dictionary, shared across all instances.
     * @param filterType The predefined dictionary {@link FilterType}.
     * @return The shared, unmodifiable dictionary map.
     * @throws IOException If the dictionary file cannot be read.
     */
    public Map<String, Pattern> loadData(final FilterType filterType) throws IOException {
        return getPredefinedDictionary(filterType).data();
    }

    /**
     * Returns the shared predefined dictionary for a type, loading and caching it on first use.
     * @param filterType The predefined dictionary {@link FilterType}.
     * @return The shared {@link PredefinedDictionary}.
     * @throws IOException If the dictionary file cannot be read.
     */
    static PredefinedDictionary getPredefinedDictionary(final FilterType filterType) throws IOException {

        // computeIfAbsent loads each predefined dictionary at most once, even under concurrent first
        // use. The loader throws a checked IOException, which a mapping function cannot, so it is
        // wrapped in a CompletionException and unwrapped here.
        try {
            return PREDEFINED_DICTIONARIES.computeIfAbsent(filterType, type -> {
                try {
                    return buildPredefinedDictionary(type);
                } catch (final IOException e) {
                    throw new CompletionException(e);
                }
            });
        } catch (final CompletionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw e;
        }

    }

    private static PredefinedDictionary buildPredefinedDictionary(final FilterType filterType) throws IOException {

        final Map<String, Pattern> data = readDictionaryResource(filterType);

        // Derive the exact-match structures used by SetDictionaryFilter: the lowercased terms and the
        // maximum n-gram size (the most whitespace-separated words in any single entry).
        final Set<String> lowerCaseTerms = new HashSet<>();
        int maxNgramSize = 0;
        for (final String term : data.keySet()) {
            final String[] split = term.split("\\s");
            if (split.length > maxNgramSize) {
                maxNgramSize = split.length;
            }
            lowerCaseTerms.add(term.toLowerCase());
        }

        return new PredefinedDictionary(
                Collections.unmodifiableMap(data),
                Collections.unmodifiableSet(lowerCaseTerms),
                maxNgramSize);

    }

    private static Map<String, Pattern> readDictionaryResource(final FilterType filterType) throws IOException {

        final Map<String, Pattern> dictionary = new HashMap<>();

        final String fileName;

        if(filterType == FilterType.LOCATION_CITY) {
            fileName = "cities.txt";
        } else if(filterType == FilterType.LOCATION_COUNTY) {
            fileName = "counties.txt";
        } else if(filterType == FilterType.LOCATION_STATE) {
            fileName = "states.txt";
        } else if(filterType == FilterType.HOSPITAL) {
            fileName = "hospitals.txt";
        } else if(filterType == FilterType.FIRST_NAME) {
            fileName = "first-names.txt";
        } else if(filterType == FilterType.SURNAME) {
            fileName = "surnames.txt";
        } else {
            throw new IllegalArgumentException("Invalid filter type.");
        }

        try (InputStream inputStream = DictionaryFilter.class.getClassLoader().getResourceAsStream(fileName);
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
