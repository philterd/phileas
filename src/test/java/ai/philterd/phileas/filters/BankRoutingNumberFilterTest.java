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
import ai.philterd.phileas.services.filters.regex.BankRoutingNumberFilter;
import ai.philterd.phileas.services.strategies.rules.BankRoutingNumberFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class BankRoutingNumberFilterTest extends AbstractFilterTest {

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new BankRoutingNumberFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final BankRoutingNumberFilter filter = new BankRoutingNumberFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the routing number is 111000025 patient is 3.5years old.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 31, FilterType.BANK_ROUTING_NUMBER));
        Assertions.assertNotEquals(filtered.getSpans().get(0).getText(), filtered.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new BankRoutingNumberFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final BankRoutingNumberFilter filter = new BankRoutingNumberFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the routing number is 111007025 patient is 3.5years old.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("111000025", "111000026");

        final BankRoutingNumberFilterStrategy bankRoutingNumberFilterStrategy = new BankRoutingNumberFilterStrategy();
        bankRoutingNumberFilterStrategy.setStrategy(RANDOM_REPLACE);
        bankRoutingNumberFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(bankRoutingNumberFilterStrategy))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final BankRoutingNumberFilter filter = new BankRoutingNumberFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the routing number is 111000025");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

}
