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
import ai.philterd.phileas.services.filters.regex.SsnFilter;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class SsnFilterTest extends AbstractFilterTest {

    @Test
    public void filterSsn1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the ssn is 123-45-6789.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 22, FilterType.SSN));
        Assertions.assertEquals("123-45-6789", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterSsn2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the ssn is 123456789.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 20, FilterType.SSN));

    }

    @Test
    public void filterSsn3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the ssn is 123 45 6789.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 22, FilterType.SSN));

    }

    @Test
    public void filterSsn4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the ssn is 123 45 6789.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 22, FilterType.SSN));

    }

    @Test
    public void filterSsn5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the ssn is 123 454 6789.");
        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterSsn6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the ssn is 123 4f 6789.");
        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterSsn7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the ssn is 11-1234567.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 21, FilterType.SSN));

    }

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("candidate1", "candidate2");

        final SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();
        ssnFilterStrategy.setStrategy(RANDOM_REPLACE);
        ssnFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(ssnFilterStrategy))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the ssn is 123-45-6789.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

    @Test
    public void filterSsnContext() throws Exception {

        final SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();
        ssnFilterStrategy.setStrategy(RANDOM_REPLACE);
        ssnFilterStrategy.setReplacementScope(AbstractFilterStrategy.REPLACEMENT_SCOPE_CONTEXT);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(ssnFilterStrategy))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered1 = filter.filter(getPolicy(), "context", PIECE, "the ssn is 123-45-6789.");
        Assertions.assertEquals(1, filtered1.getSpans().size());
        final String replacement1 = filtered1.getSpans().get(0).getReplacement();

        final Filtered filtered2 = filter.filter(getPolicy(), "context", PIECE, "the ssn is 123-45-6789.");
        Assertions.assertEquals(1, filtered2.getSpans().size());
        final String replacement2 = filtered2.getSpans().get(0).getReplacement();

        Assertions.assertEquals(replacement1, replacement2);

        final FilterConfiguration filterConfiguration2 = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(ssnFilterStrategy))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter2 = new SsnFilter(filterConfiguration2);

        final Filtered filtered3 = filter2.filter(getPolicy(), "anothercontext", PIECE, "the ssn is 555-55-1234.");
        Assertions.assertEquals(1, filtered3.getSpans().size());
        final String replacement3 = filtered3.getSpans().get(0).getReplacement();

        Assertions.assertNotEquals(replacement1, replacement3);

    }

}
