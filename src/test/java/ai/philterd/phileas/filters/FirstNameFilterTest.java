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
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.SensitivityLevel;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.services.strategies.dynamic.FirstNameFilterStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class FirstNameFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(FirstNameFilterTest.class);

    @Test
    void filterLow() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"John");
        showSpans(Span.dropOverlappingSpans(filtered.getSpans()));
        Assertions.assertEquals(1, Span.dropOverlappingSpans(filtered.getSpans()).size());

    }

    @Test
    void filterMedium1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.MEDIUM, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "Michel had eye cancer");
        showSpans(Span.dropOverlappingSpans(filtered.getSpans()));
        Assertions.assertEquals(1, Span.dropOverlappingSpans(filtered.getSpans()).size());

    }

    @Test
    void filterMedium2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "Jennifer had eye cancer");
        showSpans(Span.dropOverlappingSpans(filtered.getSpans()));
        Assertions.assertEquals(1, Span.dropOverlappingSpans(filtered.getSpans()).size());

    }

    @Test
    void filterHigh() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.HIGH, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "Sandra in Washington");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(2, filtered.getSpans().size());

    }

    @Test
    void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.MEDIUM, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "Melissa");

        showSpans(Span.dropOverlappingSpans(filtered.getSpans()));
        Assertions.assertEquals(1, Span.dropOverlappingSpans(filtered.getSpans()).size());

    }

    @Test
    void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"Thomas");
        showSpans(Span.dropOverlappingSpans(filtered.getSpans()));
        Assertions.assertEquals(1, Span.dropOverlappingSpans(filtered.getSpans()).size());

        final Filtered filtered2 = filter.filter(getPolicy(), "context", PIECE,"thomas");
        showSpans(filtered2.getSpans());
        Assertions.assertEquals(0, filtered2.getSpans().size());

    }

    @Test
    void filter3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW, false);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"dat");
        showSpans(Span.dropOverlappingSpans(filtered.getSpans()));
        Assertions.assertEquals(1, Span.dropOverlappingSpans(filtered.getSpans()).size());

    }

    @Test
    void filter4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW, false);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"joie");
        showSpans(Span.dropOverlappingSpans(filtered.getSpans()));
        Assertions.assertEquals(1, Span.dropOverlappingSpans(filtered.getSpans()).size());

    }

    @Test
    void filter5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"John");
        showSpans(Span.dropOverlappingSpans(filtered.getSpans()));
        Assertions.assertEquals(1, Span.dropOverlappingSpans(filtered.getSpans()).size());

    }

    @Test
    void filter6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"Smith,Melissa A,MD");
        showSpans(Span.dropOverlappingSpans(filtered.getSpans()));
        Assertions.assertEquals(3, Span.dropOverlappingSpans(filtered.getSpans()).size());

    }

    @Test
    void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("John", "Melissa", "James");

        final FirstNameFilterStrategy firstNameFilterStrategy = new FirstNameFilterStrategy();
        firstNameFilterStrategy.setStrategy(RANDOM_REPLACE);
        firstNameFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(firstNameFilterStrategy))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"Timothy");
        showSpans(Span.dropOverlappingSpans(filtered.getSpans()));
        Assertions.assertEquals(1, Span.dropOverlappingSpans(filtered.getSpans()).size());
        Assertions.assertTrue(candidates.contains(Span.dropOverlappingSpans(filtered.getSpans()).get(0).getReplacement()));

    }

    @Test
    void filterWithCandidates2() throws Exception {

        final List<String> candidates = List.of("John");

        final FirstNameFilterStrategy firstNameFilterStrategy = new FirstNameFilterStrategy();
        firstNameFilterStrategy.setStrategy(RANDOM_REPLACE);
        firstNameFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(firstNameFilterStrategy))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"Timothy");
        showSpans(Span.dropOverlappingSpans(filtered.getSpans()));
        Assertions.assertEquals(1, Span.dropOverlappingSpans(filtered.getSpans()).size());
        Assertions.assertTrue(candidates.contains(Span.dropOverlappingSpans(filtered.getSpans()).get(0).getReplacement()));

    }

    @Test
    void filterWithCandidates3() throws Exception {

        final List<String> candidates = Collections.emptyList();

        final FirstNameFilterStrategy firstNameFilterStrategy = new FirstNameFilterStrategy();
        firstNameFilterStrategy.setStrategy(RANDOM_REPLACE);
        firstNameFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(firstNameFilterStrategy))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"Timothy");
        showSpans(Span.dropOverlappingSpans(filtered.getSpans()));
        Assertions.assertEquals(1, Span.dropOverlappingSpans(filtered.getSpans()).size());
        Assertions.assertFalse(candidates.contains(Span.dropOverlappingSpans(filtered.getSpans()).get(0).getReplacement()));

    }

}