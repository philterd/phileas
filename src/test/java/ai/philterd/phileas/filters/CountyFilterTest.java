/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License", attributes);
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

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.enums.SensitivityLevel;
import ai.philterd.phileas.filters.rules.dictionary.FuzzyDictionaryFilter;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.services.defaults.DefaultContextService;
import ai.philterd.phileas.services.strategies.dynamic.CountyFilterStrategy;
import ai.philterd.phileas.services.anonymization.CountyAnonymizationService;
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

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE,"Lived in Fyette", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(3, filterResult.getSpans().size());

    }

    @Test
    public void filterCountiesMedium() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CountyFilterStrategy()))
                .withAnonymizationService(new CountyAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_COUNTY, filterConfiguration, SensitivityLevel.MEDIUM, true);

        FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "He lived in Fyette", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(2, filterResult.getSpans().size());
        Assertions.assertEquals("Payette", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("Fayette", filterResult.getSpans().get(1).getText());

    }

    @Test
    public void filterCountiesHigh() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CountyFilterStrategy()))
                .withAnonymizationService(new CountyAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_COUNTY, filterConfiguration, SensitivityLevel.HIGH, true);

        FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "Lived in Fyette", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterCountiesOffWithExactMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CountyFilterStrategy()))
                .withAnonymizationService(new CountyAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_COUNTY, filterConfiguration, SensitivityLevel.OFF, true);

        FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "Lived in Fayette", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 16, FilterType.LOCATION_COUNTY));
        Assertions.assertEquals("Fayette", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterCountiesOffNoExactMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CountyFilterStrategy()))
                .withAnonymizationService(new CountyAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_COUNTY, filterConfiguration, SensitivityLevel.OFF, true);

        FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "Lived in Fyette", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

}
