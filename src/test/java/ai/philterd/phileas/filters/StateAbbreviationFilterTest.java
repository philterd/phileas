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

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.services.anonymization.StateAbbreviationAnonymizationService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.filters.regex.StateAbbreviationFilter;
import ai.philterd.phileas.services.strategies.rules.StateAbbreviationFilterStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class StateAbbreviationFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(StateAbbreviationFilterTest.class);

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StateAbbreviationFilterStrategy()))
                .withAnonymizationService(new StateAbbreviationAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StateAbbreviationFilter filter = new StateAbbreviationFilter(filterConfiguration);

        final String input = "The patient is from WV.";
        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, input);

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(20, filtered.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(22, filtered.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals(FilterType.STATE_ABBREVIATION, filtered.getSpans().get(0).getFilterType());
        Assertions.assertEquals("WV", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StateAbbreviationFilterStrategy()))
                .withAnonymizationService(new StateAbbreviationAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StateAbbreviationFilter filter = new StateAbbreviationFilter(filterConfiguration);

        final String input = "The patient is from wv.";
        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, input);

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(20, filtered.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(22, filtered.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals(FilterType.STATE_ABBREVIATION, filtered.getSpans().get(0).getFilterType());

    }

    @Test
    public void filter3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StateAbbreviationFilterStrategy()))
                .withAnonymizationService(new StateAbbreviationAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StateAbbreviationFilter filter = new StateAbbreviationFilter(filterConfiguration);

        final String input = "Patients from WV and MD.";
        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, input);

        showSpans(filtered.getSpans());

        Assertions.assertEquals(2, filtered.getSpans().size());

        Assertions.assertEquals(21, filtered.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(23, filtered.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals(FilterType.STATE_ABBREVIATION, filtered.getSpans().get(0).getFilterType());
        Assertions.assertEquals("MD", filtered.getSpans().get(0).getText());

        Assertions.assertEquals(14, filtered.getSpans().get(1).getCharacterStart());
        Assertions.assertEquals(16, filtered.getSpans().get(1).getCharacterEnd());
        Assertions.assertEquals(FilterType.STATE_ABBREVIATION, filtered.getSpans().get(1).getFilterType());
        Assertions.assertEquals("WV", filtered.getSpans().get(1).getText());

    }

}
