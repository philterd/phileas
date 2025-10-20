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
import ai.philterd.phileas.services.anonymization.CountyAnonymizationService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.strategies.dynamic.CountyFilterStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CountyFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(CountyFilterTest.class);

    @Test
    public void filterCountiesLow() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CountyFilterStrategy()))
                .withAnonymizationService(new CountyAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_COUNTY, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"Lived in Fyette");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(3, filtered.getSpans().size());

    }

    @Test
    public void filterCountiesMedium() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CountyFilterStrategy()))
                .withAnonymizationService(new CountyAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_COUNTY, filterConfiguration, SensitivityLevel.MEDIUM, true);

        Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "He lived in Fyette");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertEquals("Payette", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("Fayette", filtered.getSpans().get(1).getText());

    }

    @Test
    public void filterCountiesHigh() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CountyFilterStrategy()))
                .withAnonymizationService(new CountyAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_COUNTY, filterConfiguration, SensitivityLevel.HIGH, true);

        Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "Lived in Fyette");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterCountiesOffWithExactMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CountyFilterStrategy()))
                .withAnonymizationService(new CountyAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_COUNTY, filterConfiguration, SensitivityLevel.OFF, true);

        Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "Lived in Fayette");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 9, 16, FilterType.LOCATION_COUNTY));
        Assertions.assertEquals("Fayette", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterCountiesOffNoExactMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CountyFilterStrategy()))
                .withAnonymizationService(new CountyAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_COUNTY, filterConfiguration, SensitivityLevel.OFF, true);

        Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "Lived in Fyette");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

}
