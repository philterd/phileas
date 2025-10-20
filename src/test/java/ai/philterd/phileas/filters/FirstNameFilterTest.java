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
import ai.philterd.phileas.services.anonymization.PersonsAnonymizationService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.strategies.dynamic.FirstNameFilterStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class FirstNameFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(FirstNameFilterTest.class);

    @Test
    public void filterLow() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"John");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(50, filtered.getSpans().size());

    }

    @Test
    public void filterMedium1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.MEDIUM, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "Michel had eye cancer");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(20, filtered.getSpans().size());

    }

    @Test
    public void filterMedium2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "Jennifer had eye cancer");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(4, filtered.getSpans().size());

    }

    @Test
    public void filterHigh() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.HIGH, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "Sandra in Washington");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(2, filtered.getSpans().size());

    }

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.MEDIUM, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "Melissa");

        showSpans(filtered.getSpans());
        Assertions.assertEquals(33, filtered.getSpans().size());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"thomas");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());

    }

    @Test
    public void filter3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"dat");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());

    }

    @Test
    public void filter4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"joie");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());

    }

    @Test
    public void filter5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"John");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(50, filtered.getSpans().size());

    }

    @Test
    public void filter6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"Smith,Melissa A,MD");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(3, filtered.getSpans().size());

    }

}