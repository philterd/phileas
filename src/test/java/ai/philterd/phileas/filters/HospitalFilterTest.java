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
import ai.philterd.phileas.services.strategies.dynamic.HospitalFilterStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class HospitalFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(HospitalFilterTest.class);

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new HospitalFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.HOSPITAL, filterConfiguration, SensitivityLevel.LOW, true);

        Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"Wyoming Medical Center");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("Wyoming Medical Center", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("candidate1", "candidate2");

        final HospitalFilterStrategy hospitalFilterStrategy = new HospitalFilterStrategy();
        hospitalFilterStrategy.setStrategy(RANDOM_REPLACE);
        hospitalFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(hospitalFilterStrategy))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.HOSPITAL, filterConfiguration, SensitivityLevel.LOW, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "Wyoming Medical Center");
        showSpans(filtered.getSpans());
        Assertions.assertTrue(filtered.getSpans().size() >= 1);
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

}
