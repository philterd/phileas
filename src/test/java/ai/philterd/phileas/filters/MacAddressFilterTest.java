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
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.filters.regex.MacAddressFilter;
import ai.philterd.phileas.services.strategies.rules.MacAddressFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class MacAddressFilterTest extends AbstractFilterTest {

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new MacAddressFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final MacAddressFilter filter = new MacAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the mac is 00-14-22-04-25-37.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 28, FilterType.MAC_ADDRESS));
        Assertions.assertEquals("00-14-22-04-25-37", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new MacAddressFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final MacAddressFilter filter = new MacAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the mac is 00:14:22:04:25:37.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 28, FilterType.MAC_ADDRESS));

    }

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("candidate1", "candidate2");

        final MacAddressFilterStrategy macAddressFilterStrategy = new MacAddressFilterStrategy();
        macAddressFilterStrategy.setStrategy(RANDOM_REPLACE);
        macAddressFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(macAddressFilterStrategy))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final MacAddressFilter filter = new MacAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the mac is 00-14-22-04-25-37.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

}
