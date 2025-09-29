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

import ai.philterd.phileas.filters.rules.dictionary.FuzzyDictionaryFilter;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.enums.SensitivityLevel;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.services.defaults.DefaultContextService;
import ai.philterd.phileas.services.anonymization.StateAnonymizationService;
import ai.philterd.phileas.services.strategies.dynamic.StateFilterStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class StateFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(StateFilterTest.class);

    @Test
    public void filterStatesLow() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StateFilterStrategy()))
                .withAnonymizationService(new StateAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_STATE, filterConfiguration, SensitivityLevel.LOW, true);

        FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE,"Lived in Washington", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals("Washington", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterStatesMedium() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StateFilterStrategy()))
                .withAnonymizationService(new StateAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_STATE, filterConfiguration, SensitivityLevel.MEDIUM, true);

        FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "Lived in Wshington", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterStatesHigh() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StateFilterStrategy()))
                .withAnonymizationService(new StateAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.LOCATION_STATE, filterConfiguration, SensitivityLevel.HIGH, true);

        FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "Lived in Wasinton", attributes);
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

}
