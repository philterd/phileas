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

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ssn is 11-1234567.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 21, FilterType.SSN));

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
        // A TIN inside a longer hyphenated token is not a TIN.

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

        // A TIN takes the same hyphens and the same wrapping.

        final SsnFilter filter = new SsnFilter(getSsnFilterConfiguration());

        final Filtered filtered1 = filter.filter(contextService, getPolicy(), "context", PIECE, "the tin is 11‑1234567.");
        showSpans(filtered1.getSpans());
        Assertions.assertEquals(1, filtered1.getSpans().size());
        Assertions.assertEquals("11‑1234567", filtered1.getSpans().get(0).getText());

        final Filtered filtered2 = filter.filter(contextService, getPolicy(), "context", PIECE, "the tin is 11-\n1234567.");
        showSpans(filtered2.getSpans());
        Assertions.assertEquals(1, filtered2.getSpans().size());
        Assertions.assertEquals("11-\n1234567", filtered2.getSpans().get(0).getText());

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

}
