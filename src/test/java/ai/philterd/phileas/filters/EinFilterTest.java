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
import ai.philterd.phileas.services.filters.regex.EinFilter;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.EinFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class EinFilterTest extends AbstractFilterTest {

    @Test
    public void filterEin1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new EinFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final EinFilter filter = new EinFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ein is 12-3456789.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 21, FilterType.EIN));
        Assertions.assertEquals("12-3456789", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterEinBareNineDigitsNotClaimed() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new EinFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final EinFilter filter = new EinFilter(filterConfiguration);

        // A bare nine-digit run is ambiguous with SSN and is deliberately not claimed as an EIN.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the number is 123456789.");
        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterEinSsnFormNotClaimed() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new EinFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final EinFilter filter = new EinFilter(filterConfiguration);

        // SSN hyphenation (3-2-4) is not an EIN; the hyphen position distinguishes them.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ssn is 123-45-6789.");
        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterEinOnlyValidPrefixesOff() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new EinFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        // Default (off): a format-valid EIN with a prefix the IRS does not issue (07) is still detected.
        final EinFilter filter = new EinFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ein is 07-1234567.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("07-1234567", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterEinOnlyValidPrefixesOn() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new EinFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final EinFilter filter = new EinFilter(filterConfiguration, true);

        // 07 is not an IRS-issued prefix, so it is dropped when onlyValidPrefixes is on.
        final Filtered invalid = filter.filter(contextService, getPolicy(), "context", PIECE, "the ein is 07-1234567.");
        Assertions.assertEquals(0, invalid.getSpans().size());

        // 12 is a valid IRS prefix, so it is kept.
        final Filtered valid = filter.filter(contextService, getPolicy(), "context", PIECE, "the ein is 12-3456789.");
        Assertions.assertEquals(1, valid.getSpans().size());
        Assertions.assertEquals("12-3456789", valid.getSpans().get(0).getText());

    }

    @Test
    public void filterEinContext() throws Exception {

        final EinFilterStrategy einFilterStrategy = new EinFilterStrategy();
        einFilterStrategy.setStrategy(RANDOM_REPLACE);
        einFilterStrategy.setReplacementScope(AbstractFilterStrategy.REPLACEMENT_SCOPE_CONTEXT);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(einFilterStrategy))
                .withWindowSize(windowSize)
                .build();

        final EinFilter filter = new EinFilter(filterConfiguration);

        // The same EIN in the same context resolves to the same replacement.
        final Filtered filtered1 = filter.filter(contextService, getPolicy(), "context", PIECE, "the ein is 12-3456789.");
        Assertions.assertEquals(1, filtered1.getSpans().size());
        final String replacement1 = filtered1.getSpans().get(0).getReplacement();

        final Filtered filtered2 = filter.filter(contextService, getPolicy(), "context", PIECE, "the ein is 12-3456789.");
        Assertions.assertEquals(1, filtered2.getSpans().size());
        final String replacement2 = filtered2.getSpans().get(0).getReplacement();

        Assertions.assertEquals(replacement1, replacement2);

        // A different EIN in a different context resolves to a different replacement.
        final FilterConfiguration filterConfiguration2 = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(einFilterStrategy))
                .withWindowSize(windowSize)
                .build();

        final EinFilter filter2 = new EinFilter(filterConfiguration2);

        final Filtered filtered3 = filter2.filter(contextService, getPolicy(), "anothercontext", PIECE, "the ein is 98-7654321.");
        Assertions.assertEquals(1, filtered3.getSpans().size());
        final String replacement3 = filtered3.getSpans().get(0).getReplacement();

        Assertions.assertNotEquals(replacement1, replacement3);

    }

    @Test
    public void filterEinStrategyApplied() throws Exception {

        final EinFilterStrategy strategy = new EinFilterStrategy();
        strategy.setStrategy("MASK");

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(strategy))
                .withWindowSize(windowSize)
                .build();

        final EinFilter filter = new EinFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ein is 12-3456789.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        // MASK replaces each character with the mask character.
        Assertions.assertEquals("**********", filtered.getSpans().get(0).getReplacement());

    }

}
