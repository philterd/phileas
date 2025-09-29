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
import ai.philterd.phileas.services.strategies.dynamic.SurnameFilterStrategy;
import ai.philterd.phileas.services.anonymization.SurnameAnonymizationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class SurnameFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(SurnameFilterTest.class);

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SurnameFilterStrategy()))
                .withAnonymizationService(new SurnameAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.SURNAME, filterConfiguration, SensitivityLevel.LOW, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "Lived in Wshington", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(44, filterResult.getSpans().size());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SurnameFilterStrategy()))
                .withAnonymizationService(new SurnameAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.SURNAME, filterConfiguration, SensitivityLevel.MEDIUM, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Lived in Wshington", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(44, filterResult.getSpans().size());

    }

    @Test
    public void filter3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SurnameFilterStrategy()))
                .withAnonymizationService(new SurnameAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.SURNAME, filterConfiguration, SensitivityLevel.HIGH, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "Jones", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filter4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SurnameFilterStrategy()))
                .withAnonymizationService(new SurnameAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.SURNAME, filterConfiguration, SensitivityLevel.LOW, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "date", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filter5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SurnameFilterStrategy()))
                .withAnonymizationService(new SurnameAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.SURNAME, filterConfiguration, SensitivityLevel.LOW, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "Jones", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(349, filterResult.getSpans().size());

    }

    @Test
    public void filter6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SurnameFilterStrategy()))
                .withAnonymizationService(new SurnameAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.SURNAME, filterConfiguration, SensitivityLevel.LOW, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "from", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

}
