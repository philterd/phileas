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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.filters.rules.dictionary.CityCommonWords;
import ai.philterd.phileas.filters.rules.dictionary.FuzzyDictionaryFilter;
import ai.philterd.phileas.filters.rules.dictionary.SetDictionaryFilter;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.SensitivityLevel;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.services.strategies.dynamic.CityFilterStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class CityFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(CityFilterTest.class);

    @Test
    public void filterCitiesExactMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CityFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, SensitivityLevel.MEDIUM, true);

        Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Lived in Washington.");

        showSpans(Span.dropOverlappingSpans(filtered.getSpans()));

        // At MEDIUM sensitivity (Levenshtein distance up to 1) only the exact city "Washington" matches.
        Assertions.assertEquals(1, Span.dropOverlappingSpans(filtered.getSpans()).size());
        Assertions.assertTrue(checkSpanInSpans(Span.dropOverlappingSpans(filtered.getSpans()), 9, 19, FilterType.LOCATION_CITY, "Washington", "{{{REDACTED-city}}}"));

    }

    @Test
    public void filterCitiesExactMatch2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CityFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, SensitivityLevel.HIGH, true);

        Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Lived in New York.");

        showSpans(Span.dropOverlappingSpans(filtered.getSpans()));

        Assertions.assertEquals(1, Span.dropOverlappingSpans(filtered.getSpans()).size());
        Assertions.assertTrue(checkSpan(Span.dropOverlappingSpans(filtered.getSpans()).get(0), 9, 17, FilterType.LOCATION_CITY));
        Assertions.assertEquals("New York", Span.dropOverlappingSpans(filtered.getSpans()).get(0).getText());

    }

    @Test
    public void filterCitiesLow() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CityFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, SensitivityLevel.LOW, true);

        Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE,"Lived in Wshington");

        showSpans(Span.dropOverlappingSpans(filtered.getSpans()));

        Assertions.assertEquals(2, Span.dropOverlappingSpans(filtered.getSpans()).size());
        Assertions.assertTrue(checkSpanInSpans(Span.dropOverlappingSpans(filtered.getSpans()), 9, 18, FilterType.LOCATION_CITY, "Wshington", "{{{REDACTED-city}}}"));

    }

    @Test
    public void filterCitiesMedium() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CityFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, SensitivityLevel.MEDIUM, true);

        Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Lived in Wshington");

        showSpans(Span.dropOverlappingSpans(filtered.getSpans()));

        // At MEDIUM sensitivity (Levenshtein distance up to 1) only "Wshington" (distance 1 from
        // "Washington") matches. At LOW sensitivity (distance up to 2) an additional looser match is
        // found, which is what distinguishes MEDIUM from LOW (see filterCitiesLow).
        Assertions.assertEquals(1, Span.dropOverlappingSpans(filtered.getSpans()).size());
        Assertions.assertTrue(checkSpanInSpans(Span.dropOverlappingSpans(filtered.getSpans()), 9, 18, FilterType.LOCATION_CITY, "Wshington", "{{{REDACTED-city}}}"));

    }

    @Test
    public void filterCitiesHigh() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CityFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, SensitivityLevel.HIGH, true);

        Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE,"Lived in Wasinton");

        showSpans(Span.dropOverlappingSpans(filtered.getSpans()));

        Assertions.assertEquals(0, Span.dropOverlappingSpans(filtered.getSpans()).size());

    }

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("candidate1", "candidate2");

        final CityFilterStrategy cityFilterStrategy = new CityFilterStrategy();
        cityFilterStrategy.setStrategy(RANDOM_REPLACE);
        cityFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(cityFilterStrategy))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Lived in Washington.");
        showSpans(Span.dropOverlappingSpans(filtered.getSpans()));
        Assertions.assertEquals(2, Span.dropOverlappingSpans(filtered.getSpans()).size());
        Assertions.assertTrue(candidates.contains(Span.dropOverlappingSpans(filtered.getSpans()).get(0).getReplacement()));

    }

    /** The city filter as a policy builds it: exact matching by default, or fuzzy at the default sensitivity. */
    private List<Filter> cityFilters() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CityFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        return List.of(new SetDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration),
                new FuzzyDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, SensitivityLevel.AUTO, false));

    }

    private List<String> cities(final Filter filter, final String text) throws Exception {
        return Span.dropOverlappingSpans(filter.filter(contextService, getPolicy(), "context", PIECE, text).getSpans())
                .stream().map(Span::getText).toList();
    }

    @Test
    public void filterCitiesCommonWordsAreNotCities() throws Exception {

        for (final Filter filter : cityFilters()) {

            final String name = filter.getClass().getSimpleName();

            Assertions.assertEquals(List.of(), cities(filter, "Call the clinic with questions."), name);
            Assertions.assertEquals(List.of(), cities(filter, "Driver license on file."), name);
            Assertions.assertEquals(List.of("Austin"), cities(filter, "She lives on Oak Street in Austin."), name);
            Assertions.assertEquals(List.of(), cities(filter, "We will call the driver on the street near the oak tree."), name);
            Assertions.assertEquals(List.of(), cities(filter, "The patient has hope and is in good spirits at the center."), name);
            Assertions.assertEquals(List.of(), cities(filter, "Phoenix is hot this time of year."), name);
            Assertions.assertEquals(List.of(), cities(filter, "She moved in hope of a job."), name);

        }

    }

    @Test
    public void filterCitiesCommonWordsInALocationContextAreCities() throws Exception {

        for (final Filter filter : cityFilters()) {

            final String name = filter.getClass().getSimpleName();

            // Fuzzy matching can also report other capitalized words close to a city name, so only the city is checked.
            for (final String[] c : new String[][] {{"She lives in Call, Texas.", "Call"}, {"Born in Phoenix.", "Phoenix"},
                    {"Moved from Hope last year.", "Hope"}, {"Drove to Mobile on Monday.", "Mobile"}, {"A farm near Bend.", "Bend"},
                    {"Just outside Reading.", "Reading"}, {"Heading toward Buffalo.", "Buffalo"}, {"Flew via Phoenix.", "Phoenix"},
                    {"Seen at the clinic (in Phoenix).", "Phoenix"}, {"LIVES IN PHOENIX.", "PHOENIX"}}) {
                Assertions.assertTrue(cities(filter, c[0]).contains(c[1]), name + ": " + c[0]);
            }

            Assertions.assertTrue(cities(filter, "Mobile, Alabama is on the coast.").contains("Mobile"), name);
            Assertions.assertTrue(cities(filter, "Patient from Reading, PA.").contains("Reading"), name);
            Assertions.assertEquals(2, cities(filter, "Hope, AR and Hope, Arkansas.").stream().filter("Hope"::equals).count(), name);
            Assertions.assertTrue(cities(filter, "Buffalo, NY.").contains("Buffalo"), name);
            Assertions.assertTrue(cities(filter, "PHOENIX, AZ").contains("PHOENIX"), name);

            // A lowercase common word is not a city even after a location word, and a comma alone is not a state.
            Assertions.assertEquals(List.of(), cities(filter, "lives in phoenix"), name);
            Assertions.assertEquals(List.of(), cities(filter, "Hope, faith, and charity."), name);
            Assertions.assertEquals(List.of(), cities(filter, "Call, PAID in full."), name);

        }

    }

    @Test
    public void filterCitiesOtherNamesNeedNoContext() throws Exception {

        for (final Filter filter : cityFilters()) {
            final String name = filter.getClass().getSimpleName();
            Assertions.assertEquals(List.of("Austin"), cities(filter, "Austin is growing."), name);
            Assertions.assertEquals(List.of("austin"), cities(filter, "drives to work in austin"), name);
            Assertions.assertEquals(List.of("New Hope"), cities(filter, "Lived on a farm by New Hope."), name);
        }

    }

    @Test
    public void filterCitiesGivenTermsNeedNoContext() throws Exception {

        // Terms a policy gives replace the bundled list, and are matched as given.
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CityFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> terms = new LinkedHashSet<>(List.of("Call", "Hope"));

        for (final Filter filter : List.<Filter>of(new SetDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, terms, null),
                new FuzzyDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, SensitivityLevel.AUTO, terms, false))) {
            Assertions.assertEquals(List.of("Call"), cities(filter, "Call the clinic with questions."), filter.getClass().getSimpleName());
        }

    }

    @Test
    public void cityCommonWordsAreSingleLowercaseBundledCities() throws Exception {

        final Set<String> cities;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("cities.txt"), StandardCharsets.UTF_8))) {
            cities = reader.lines().map(String::toLowerCase).collect(Collectors.toSet());
        }

        final List<String> words;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("city-common-words.txt"), StandardCharsets.UTF_8))) {
            words = reader.lines().filter(line -> !line.startsWith("#")).toList();
        }

        Assertions.assertFalse(words.isEmpty());

        for (final String word : words) {
            Assertions.assertTrue(word.matches("[a-z]+"), word);
            Assertions.assertTrue(cities.contains(word), word);
            Assertions.assertTrue(CityCommonWords.isCommonWord(word), word);
        }

        Assertions.assertFalse(CityCommonWords.isCommonWord("Austin"));

    }

}
