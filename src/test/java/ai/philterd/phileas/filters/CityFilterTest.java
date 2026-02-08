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

import ai.philterd.phileas.filters.rules.dictionary.FuzzyDictionaryFilter;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.SensitivityLevel;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.strategies.dynamic.CityFilterStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class CityFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(CityFilterTest.class);

    @Test
    public void filterCitiesExactMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CityFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, SensitivityLevel.MEDIUM, true);

        Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "Lived in Washington.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 9, 19, FilterType.LOCATION_CITY));

    }

    @Test
    public void filterCitiesExactMatch2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CityFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, SensitivityLevel.HIGH, true);

        Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "Lived in New York.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 9, 17, FilterType.LOCATION_CITY));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 13, 17, FilterType.LOCATION_CITY));
        Assertions.assertEquals("new york", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterCitiesLow() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CityFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, SensitivityLevel.LOW, true);

        Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"Lived in Wshington");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());

    }

    @Test
    public void filterCitiesMedium() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CityFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, SensitivityLevel.MEDIUM, true);

        Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "Lived in Wshington");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 9, 18, FilterType.LOCATION_CITY));

    }

    @Test
    public void filterCitiesHigh() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CityFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, SensitivityLevel.HIGH, true);

        Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"Lived in Wasinton");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("candidate1", "candidate2");

        final CityFilterStrategy cityFilterStrategy = new CityFilterStrategy();
        cityFilterStrategy.setStrategy(RANDOM_REPLACE);
        cityFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(cityFilterStrategy))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "Lived in Washington.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

}
