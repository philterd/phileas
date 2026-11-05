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
import ai.philterd.phileas.model.filtering.Span;
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

    private EinFilter filter(final boolean onlyValidPrefixes) {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new EinFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        return new EinFilter(filterConfiguration, onlyValidPrefixes);

    }

    @Test
    public void filterEinEveryHyphenSubstitute() throws Exception {

        // The soft hyphen, U+2010 through U+2015, the minus sign, and the small and fullwidth hyphen-minus.
        for (final char hyphen : new char[]{'\u00AD', '\u2010', '\u2011', '\u2012', '\u2013', '\u2014', '\u2015',
                '\u2212', '\uFE58', '\uFE63', '\uFF0D'}) {

            final String ein = "12" + hyphen + "3456789";
            final Filtered filtered = filter(false).filter(contextService, getPolicy(), "context", PIECE, "the ein is " + ein + ".");

            Assertions.assertEquals(1, filtered.getSpans().size(), String.format("U+%04X", (int) hyphen));
            Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 21, FilterType.EIN), String.format("U+%04X", (int) hyphen));
            Assertions.assertEquals(ein, filtered.getSpans().get(0).getText());

        }

    }

    @Test
    public void filterEinWrappedAfterTheHyphen() throws Exception {

        // Wrapped onto the next line, and onto an indented continuation line.
        for (final String ein : List.of("12-\n3456789", "12-\r\n3456789", "12-\n    3456789", "12\u2011\n\t3456789")) {

            final String input = "the ein is " + ein + ".";
            final Filtered filtered = filter(false).filter(contextService, getPolicy(), "context", PIECE, input);

            Assertions.assertEquals(1, filtered.getSpans().size(), ein);
            final Span span = filtered.getSpans().get(0);
            Assertions.assertEquals(FilterType.EIN, span.getFilterType());
            Assertions.assertEquals(ein, span.getText());

            // The span indexes into the original text and covers the whole EIN, line break included.
            Assertions.assertEquals(11, span.getCharacterStart());
            Assertions.assertEquals(11 + ein.length(), span.getCharacterEnd());
            Assertions.assertEquals(ein, input.substring(span.getCharacterStart(), span.getCharacterEnd()));

        }

    }

    @Test
    public void filterEinNotWrappedWithoutAHyphen() throws Exception {

        // A line break is a separator only after a hyphen.
        Assertions.assertEquals(0, filter(false).filter(contextService, getPolicy(), "context", PIECE, "12\n3456789").getSpans().size());
        Assertions.assertEquals(0, filter(false).filter(contextService, getPolicy(), "context", PIECE, "12-3456\n789").getSpans().size());

    }

    @Test
    public void filterEinNotInsideALongerHyphenatedToken() throws Exception {

        // https://github.com/philterd/phileas/issues/343: "45-6789123" is not an EIN inside two adjacent SSNs.
        for (final String input : List.of("123-45-6789123-45-6789", "case 12-1234567-8", "case 12\u20111234567\u20118",
                "case 12\u20121234567\u20128", "case 12\u22121234567\u22128", "x-12-3456789")) {
            Assertions.assertEquals(0, filter(false).filter(contextService, getPolicy(), "context", PIECE, input).getSpans().size(), input);
        }

    }

    @Test
    public void filterEinNextToPunctuationDashes() throws Exception {

        // An en dash, em dash, horizontal bar, or small em dash next to an EIN is punctuation, not part of
        // a longer token.
        for (final char dash : new char[]{'\u2013', '\u2014', '\u2015', '\uFE58'}) {

            final String label = String.format("U+%04X", (int) dash);

            final Filtered after = filter(false).filter(contextService, getPolicy(), "context", PIECE, "EIN 12-3456789" + dash + "see below.");
            Assertions.assertEquals(1, after.getSpans().size(), label);
            Assertions.assertEquals("12-3456789", after.getSpans().get(0).getText(), label);

            final Filtered range = filter(false).filter(contextService, getPolicy(), "context", PIECE, "EINs 12-3456789" + dash + "98-7654321.");
            Assertions.assertEquals(2, range.getSpans().size(), label);

        }

    }

    @Test
    public void filterEinOnlyValidPrefixesOnAWrappedOrUnicodeEin() throws Exception {

        for (final String ein : List.of("12\u20113456789", "12-\n    3456789")) {
            Assertions.assertEquals(1, filter(true).filter(contextService, getPolicy(), "context", PIECE, "the ein is " + ein + ".").getSpans().size(), ein);
        }

        // 07 is not an IRS-issued prefix.
        for (final String ein : List.of("07\u20111234567", "07-\n    1234567")) {
            Assertions.assertEquals(0, filter(true).filter(contextService, getPolicy(), "context", PIECE, "the ein is " + ein + ".").getSpans().size(), ein);
            Assertions.assertEquals(1, filter(false).filter(contextService, getPolicy(), "context", PIECE, "the ein is " + ein + ".").getSpans().size(), ein);
        }

    }

}
