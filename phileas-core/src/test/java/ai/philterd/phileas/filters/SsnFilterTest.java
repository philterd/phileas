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
import ai.philterd.phileas.services.filters.regex.SsnFilter;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class SsnFilterTest extends AbstractFilterTest {

    private FilterConfiguration getSsnFilterConfiguration() {
        return new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withWindowSize(windowSize)
                .build();
    }

    @Test
    public void filterSsn1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ssn is 123-45-6789.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 22, FilterType.SSN));
        Assertions.assertEquals("123-45-6789", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterSsn2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ssn is 123456789.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 20, FilterType.SSN));

    }

    @Test
    public void filterSsn3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ssn is 123 45 6789.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 22, FilterType.SSN));

    }

    @Test
    public void filterSsn4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ssn is 123 45 6789.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 22, FilterType.SSN));

    }

    @Test
    public void filterSsn5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ssn is 123 454 6789.");
        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterSsn6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ssn is 123 4f 6789.");
        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterSsn7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        // NN-NNNNNNN is an EIN, which the EIN filter detects; the SSN filter does not.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ssn is 11-1234567.");
        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterSsn8() throws Exception {

        // https://github.com/philterd/phileas/issues/343
        // Two SSNs with nothing between them: a fragment straddling the two used to match instead.

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "123-45-6789123-45-6789");
        showSpans(filtered.getSpans());

        // The run is redacted as one span. Nothing separates the two values, so there is no
        // boundary between them to split on.
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 22, FilterType.SSN));
        Assertions.assertEquals("123-45-6789123-45-6789", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterSsn9() throws Exception {

        // https://github.com/philterd/phileas/issues/343
        // A space between the two SSNs still gives two spans.

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "123-45-6789 123-45-6789");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 11, FilterType.SSN));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 12, 23, FilterType.SSN));

    }

    @Test
    public void filterSsn10() throws Exception {

        // https://github.com/philterd/phileas/issues/343
        // A comma between the two SSNs still gives two spans.

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "123-45-6789, 123-45-6789");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 11, FilterType.SSN));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 13, 24, FilterType.SSN));

    }

    @Test
    public void filterSsn11() throws Exception {

        // https://github.com/philterd/phileas/issues/343
        // A match may not start or end partway through a longer run of digits.

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        // A run that does not divide evenly into SSNs matches nothing, however long it is. The
        // last nine digits of a twenty-digit account number are not an SSN.
        for (final String input : List.of("the account is 1234567891.",
                "the account is 12345678901234567890.",
                "the account is 123456789012345678901234567890123456789012345678.")) {

            final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);
            showSpans(filtered.getSpans());
            Assertions.assertEquals(0, filtered.getSpans().size(), input);

        }

    }

    @Test
    public void filterSsn12() throws Exception {

        // https://github.com/philterd/phileas/issues/343
        // Nothing inside a longer hyphenated token is an SSN.

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "case 12-1234567-8");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterSsn13() throws Exception {

        // The ASCII control for the Unicode and line-wrap cases below.

        final SsnFilter filter = new SsnFilter(getSsnFilterConfiguration());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "SSN: 078-05-1120");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 5, 16, FilterType.SSN));
        Assertions.assertEquals("078-05-1120", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterSsn14() throws Exception {

        // Each hyphen-like character an editor or a PDF extractor substitutes for an ASCII hyphen.

        final SsnFilter filter = new SsnFilter(getSsnFilterConfiguration());

        for (final char hyphen : new char[] {'­', '‐', '‑', '‒', '–',
                '—', '―', '−', '﹘', '﹣', '－'}) {

            final String input = "SSN: 078" + hyphen + "05" + hyphen + "1120";

            final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);
            Assertions.assertEquals(1, filtered.getSpans().size(), input);
            Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 5, 16, FilterType.SSN), input);
            Assertions.assertEquals("078" + hyphen + "05" + hyphen + "1120", filtered.getSpans().get(0).getText());

        }

    }

    @Test
    public void filterSsn15() throws Exception {

        // An identifier wrapped onto the next line, with and without indentation on it.

        final SsnFilter filter = new SsnFilter(getSsnFilterConfiguration());

        final Filtered filtered1 = filter.filter(contextService, getPolicy(), "context", PIECE, "SSN: 078-05-\n1120");
        showSpans(filtered1.getSpans());
        Assertions.assertEquals(1, filtered1.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered1.getSpans().get(0), 5, 17, FilterType.SSN));
        Assertions.assertEquals("078-05-\n1120", filtered1.getSpans().get(0).getText());

        final Filtered filtered2 = filter.filter(contextService, getPolicy(), "context", PIECE, "SSN: 078-05-\r\n    1120 end");
        showSpans(filtered2.getSpans());
        Assertions.assertEquals(1, filtered2.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered2.getSpans().get(0), 5, 22, FilterType.SSN));
        Assertions.assertEquals("078-05-\r\n    1120", filtered2.getSpans().get(0).getText());

        // The wrap may fall at either hyphen, and a non-breaking hyphen wraps the same way.
        final Filtered filtered3 = filter.filter(contextService, getPolicy(), "context", PIECE, "SSN: 078‑\n05-1120");
        showSpans(filtered3.getSpans());
        Assertions.assertEquals(1, filtered3.getSpans().size());
        Assertions.assertEquals("078‑\n05-1120", filtered3.getSpans().get(0).getText());

    }

    @Test
    public void filterSsn16() throws Exception {

        // A line break is only a separator when a hyphen precedes it, so digits on their own lines
        // are not joined into an SSN.

        final SsnFilter filter = new SsnFilter(getSsnFilterConfiguration());

        for (final String input : List.of("totals\n123\n45\n6789\n",
                "row 1: 123\nrow 2: 45\nrow 3: 6789",
                "the ssn is 123\n45-6789",
                "the ssn is 123-45\n6789",
                "a 123456\n789 b",
                "invoice 12345-\n678 90")) {

            final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);
            showSpans(filtered.getSpans());
            Assertions.assertEquals(0, filtered.getSpans().size(), input);

        }

    }

    @Test
    public void filterSsn17() throws Exception {

        // Repeated identifiers in different forms, surrounded by non-ASCII text.

        final SsnFilter filter = new SsnFilter(getSsnFilterConfiguration());

        final String input = "СНИЛС 078‑05‑1120, "
                + "супруга 078-05-\n1120, конец";

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);
        showSpans(filtered.getSpans());
        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertEquals("078‑05‑1120", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("078-05-\n1120", filtered.getSpans().get(1).getText());

        // The spans index into the original text.
        for (final Span span : filtered.getSpans()) {
            Assertions.assertEquals(span.getText(), input.substring(span.getCharacterStart(), span.getCharacterEnd()));
        }

    }

    @Test
    public void filterSsn18() throws Exception {

        // NN-NNNNNNN is not an SSN in any of the forms the EIN filter accepts.

        final SsnFilter filter = new SsnFilter(getSsnFilterConfiguration());

        for (final String ein : List.of("11-1234567", "11\u20111234567", "11-\n1234567", "11-\n    1234567")) {
            final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ein is " + ein + ".");
            Assertions.assertEquals(0, filtered.getSpans().size(), ein);
        }

        // A Unicode hyphen extends the token the same way an ASCII one does.
        final Filtered filtered3 = filter.filter(contextService, getPolicy(), "context", PIECE, "case 12‑1234567‑8");
        showSpans(filtered3.getSpans());
        Assertions.assertEquals(0, filtered3.getSpans().size());

    }

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("candidate1", "candidate2");

        final SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();
        ssnFilterStrategy.setStrategy(RANDOM_REPLACE);
        ssnFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(ssnFilterStrategy))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ssn is 123-45-6789.");
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
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final Filtered filtered1 = filter.filter(contextService, getPolicy(), "context", PIECE, "the ssn is 123-45-6789.");
        Assertions.assertEquals(1, filtered1.getSpans().size());
        final String replacement1 = filtered1.getSpans().get(0).getReplacement();

        final Filtered filtered2 = filter.filter(contextService, getPolicy(), "context", PIECE, "the ssn is 123-45-6789.");
        Assertions.assertEquals(1, filtered2.getSpans().size());
        final String replacement2 = filtered2.getSpans().get(0).getReplacement();

        Assertions.assertEquals(replacement1, replacement2);

        final FilterConfiguration filterConfiguration2 = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(ssnFilterStrategy))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter2 = new SsnFilter(filterConfiguration2);

        final Filtered filtered3 = filter2.filter(contextService, getPolicy(), "anothercontext", PIECE, "the ssn is 555-55-1234.");
        Assertions.assertEquals(1, filtered3.getSpans().size());
        final String replacement3 = filtered3.getSpans().get(0).getReplacement();

        Assertions.assertNotEquals(replacement1, replacement3);

    }

    @Test
    public void filterSsnSeparatorsMustMatch() throws Exception {

        // https://github.com/philterd/phileas/issues/460
        final SsnFilter filter = new SsnFilter(getSsnFilterConfiguration());

        for (final String ssn : List.of("123-45-6789", "123 45 6789", "123456789", "123\u201145\u20116789", "123-45\u20116789", "123-45-\n6789")) {
            final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ssn is " + ssn + ".");
            Assertions.assertEquals(1, filtered.getSpans().size(), ssn);
            Assertions.assertEquals(ssn, filtered.getSpans().get(0).getText());
        }

        for (final String mismatched : List.of("123-456789", "12345-6789", "123 456789", "12345 6789", "123-45 6789", "123 45-6789")) {
            Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "the value is " + mismatched + ".").getSpans().size(), mismatched);
        }

    }

    @Test
    public void filterSsnZipPlus4NotMatched() throws Exception {

        // https://github.com/philterd/phileas/issues/460
        final SsnFilter filter = new SsnFilter(getSsnFilterConfiguration());

        for (final String zip : List.of("52168-1208", "02965-1837", "12345-6789")) {
            Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "ships to " + zip + " today.").getSpans().size(), zip);
        }

    }

    @Test
    public void filterSsnRunWithMatchingSeparators() throws Exception {

        // https://github.com/philterd/phileas/issues/343 and https://github.com/philterd/phileas/issues/460
        final SsnFilter filter = new SsnFilter(getSsnFilterConfiguration());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ssns are 123-45-6789123456789.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("123-45-6789123456789", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterSsnOneDigitToABox() throws Exception {

        // The SSN fields on Form 1040 are one digit to a box, so the text of a filled form reads
        // 5 1 2 4 8 7 3 2 9. https://github.com/philterd/phileas/issues/544
        final SsnFilter filter = new SsnFilter(getSsnFilterConfiguration());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Your social security number 5 1 2 4 8 7 3 2 9 Spouse");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 28, 45, FilterType.SSN));
        Assertions.assertEquals("5 1 2 4 8 7 3 2 9", filtered.getSpans().get(0).getText());

        for (final String ssn : List.of("5\t1\t2\t4\t8\t7\t3\t2\t9", "5 1 2 4 8 7 3 2 9", "1 2 3 4 5 6 7 8 9")) {
            final Filtered spaced = filter.filter(contextService, getPolicy(), "context", PIECE, "ssn " + ssn + ".");
            Assertions.assertEquals(1, spaced.getSpans().size(), ssn);
            Assertions.assertEquals(ssn, spaced.getSpans().get(0).getText());
        }

    }

    @Test
    public void filterSsnOneDigitToABoxKeepsTheRangeChecks() throws Exception {

        // https://github.com/philterd/phileas/issues/544
        final SsnFilter filter = new SsnFilter(getSsnFilterConfiguration());

        // Area 000, area 666, group 00, serial 0000, and a value beginning with 9 (an ITIN).
        for (final String excluded : List.of("0 0 0 4 5 6 7 8 9", "6 6 6 4 5 6 7 8 9", "1 2 3 0 0 6 7 8 9", "1 2 3 4 5 0 0 0 0", "9 1 2 7 0 1 2 3 4")) {
            Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "ssn " + excluded + ".").getSpans().size(), excluded);
        }

    }

    @Test
    public void filterSsnOneDigitToABoxNotPartOfALongerRun() throws Exception {

        // https://github.com/philterd/phileas/issues/544
        final SsnFilter filter = new SsnFilter(getSsnFilterConfiguration());

        for (final String input : List.of(
                "5 1 2 4 8 7 3 2",
                "5 1 2 4 8 7 3 2 9 1",
                "1 5 1 2 4 8 7 3 2 9",
                "5 1 2 4 8 7 3 2 9 5 1 2 4 8 7 3 2 9",
                "5 1  2 4 8 7 3 2 9",
                "5 1 2 4 8 7 3 2 9a",
                "a5 1 2 4 8 7 3 2 9",
                "15 1 2 4 8 7 3 2 9")) {
            Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "the values are " + input + " here.").getSpans().size(), input);
        }

    }

    @Test
    public void filterSsnOneDigitToABoxHasALowerConfidence() throws Exception {

        // Any nine single digits written this way match, such as a row of scores, so the spaced form
        // starts at a lower confidence, which a strategy condition can tell apart from the other forms.
        // https://github.com/philterd/phileas/issues/544
        final SsnFilter filter = new SsnFilter(getSsnFilterConfiguration());

        Assertions.assertEquals(0.75, filter.filter(contextService, getPolicy(), "context", PIECE, "ssn 5 1 2 4 8 7 3 2 9").getSpans().get(0).getConfidence(), 0.0001);
        Assertions.assertEquals(0.90, filter.filter(contextService, getPolicy(), "context", PIECE, "ssn 512-48-7329").getSpans().get(0).getConfidence(), 0.0001);

        final SsnFilterStrategy strategy = new SsnFilterStrategy();
        strategy.setConditions("confidence >= 0.9");

        final SsnFilter conditional = new SsnFilter(new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(strategy))
                .withWindowSize(windowSize)
                .build());

        final List<Span> spans = conditional.filter(contextService, getPolicy(), "context", PIECE, "scores 1 2 3 4 5 6 7 8 9 and ssn 512-48-7329").getSpans();
        Assertions.assertEquals(2, spans.size());
        Assertions.assertFalse(spans.stream().filter(s -> s.getText().equals("1 2 3 4 5 6 7 8 9")).findFirst().orElseThrow().isApplied());
        Assertions.assertTrue(spans.stream().filter(s -> s.getText().equals("512-48-7329")).findFirst().orElseThrow().isApplied());

    }

}
