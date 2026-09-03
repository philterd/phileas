/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services.split;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Config;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.config.Splitting;
import ai.philterd.phileas.policy.filters.Date;
import ai.philterd.phileas.policy.filters.EmailAddress;
import ai.philterd.phileas.policy.filters.Ssn;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.rules.DateFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.EmailAddressFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

/** The line-width splitter wraps on spaces, so a date containing spaces can be cut at a seam. */
class SplittingOverlapTest {

    private static final String SEAM_INPUT = "the date was May 22, 1999 in the record and more text here";

    private static final String MIXED_INPUT =
            "ssn 123-45-6789 and mail ada@example.com and the date was May 22, 1999 in the record here";

    private Policy policy(final int overlap) {

        final Date date = new Date();
        date.setDateFilterStrategies(List.of(new DateFilterStrategy()));

        final Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(List.of(new SsnFilterStrategy()));

        final EmailAddress emailAddress = new EmailAddress();
        emailAddress.setEmailAddressFilterStrategies(List.of(new EmailAddressFilterStrategy()));

        final Identifiers identifiers = new Identifiers();
        identifiers.setDate(date);
        identifiers.setSsn(ssn);
        identifiers.setEmailAddress(emailAddress);

        final Splitting splitting = new Splitting();
        splitting.setEnabled(true);
        splitting.setMethod("width");
        splitting.setThreshold(20);
        splitting.setOverlap(overlap);

        final Config config = new Config();
        config.setSplitting(splitting);

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);
        policy.setConfig(config);

        return policy;

    }

    private Policy unsplitPolicy() {
        final Policy policy = policy(0);
        policy.getConfig().getSplitting().setEnabled(false);
        return policy;
    }

    private TextFilterResult filter(final Policy policy, final String input) throws Exception {
        return new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null)
                .filter(policy, "context", input);
    }

    @Test
    void withoutOverlapASeamDateIsOnlyPartlyRedacted() throws Exception {

        final TextFilterResult result = filter(policy(0), SEAM_INPUT);

        // The year survives in the clear: each piece saw only part of the date.
        Assertions.assertEquals("the date was {{{REDACTED-date}}}, 1999 in the record and more text here",
                result.getFilteredText());

    }

    @Test
    void withOverlapASeamDateIsRedactedWhole() throws Exception {

        final TextFilterResult result = filter(policy(20), SEAM_INPUT);

        Assertions.assertEquals("the date was {{{REDACTED-date}}} in the record and more text here",
                result.getFilteredText());

    }

    @Test
    void aSeamDateIsRedactedOnceAndKeepsItsOffsets() throws Exception {

        final TextFilterResult result = filter(policy(20), SEAM_INPUT);

        final List<Span> applied = result.getExplanation().appliedSpans();
        Assertions.assertEquals(1, applied.size(), "the date must be reported once, not once per piece");

        // The offsets are absolute in the input, not relative to a piece.
        Assertions.assertEquals(SEAM_INPUT.indexOf("May 22, 1999"), applied.get(0).getCharacterStart());
        Assertions.assertEquals(SEAM_INPUT.indexOf("May 22, 1999") + "May 22, 1999".length(),
                applied.get(0).getCharacterEnd());
        Assertions.assertEquals("May 22, 1999", applied.get(0).getText());

    }

    @Test
    void overlapDoesNotChangeAnEntityAwayFromASeam() throws Exception {

        final String input = "one two three four five six seven eight nine ssn 123-45-6789 ten";

        Assertions.assertEquals(filter(policy(0), input).getFilteredText(),
                filter(policy(25), input).getFilteredText());

    }

    @Test
    void overlapMatchesFilteringWithoutSplittingAtAll() throws Exception {

        final String input = MIXED_INPUT;

        final TextFilterResult unsplit = filter(unsplitPolicy(), input);
        final TextFilterResult overlapped = filter(policy(30), input);

        Assertions.assertEquals(unsplit.getFilteredText(), overlapped.getFilteredText());
        Assertions.assertEquals(unsplit.getExplanation().appliedSpans().size(),
                overlapped.getExplanation().appliedSpans().size());

    }

    @Test
    void overlapDoesNotDoubleCountTokens() throws Exception {

        Assertions.assertEquals(filter(unsplitPolicy(), MIXED_INPUT).getTokens(),
                filter(policy(30), MIXED_INPUT).getTokens());

    }

    @Test
    void contiguousSplittingLeaksASeamValueThatOverlapCatches() throws Exception {

        final TextFilterResult contiguous = filter(policy(0), MIXED_INPUT);
        final TextFilterResult overlapped = filter(policy(30), MIXED_INPUT);

        // The date is cut at a seam, so contiguous pieces find one fewer entity and leave the year.
        Assertions.assertTrue(contiguous.getFilteredText().contains("May 22, 1999"));
        Assertions.assertFalse(overlapped.getFilteredText().contains("1999"));
        Assertions.assertEquals(contiguous.getExplanation().appliedSpans().size() + 1,
                overlapped.getExplanation().appliedSpans().size());

    }

    @Test
    void everySpanOffsetPointsAtItsOwnTextInTheInput() throws Exception {

        final TextFilterResult result = filter(policy(30), MIXED_INPUT);

        for(final Span span : result.getExplanation().appliedSpans()) {
            Assertions.assertEquals(span.getText(),
                    MIXED_INPUT.substring(span.getCharacterStart(), span.getCharacterEnd()),
                    "span offsets do not match its text: " + span);
        }

    }

    @Test
    void noValueIsReportedTwiceWithAnOverlap() throws Exception {

        final TextFilterResult result = filter(policy(30), MIXED_INPUT);
        final List<Span> spans = result.getExplanation().appliedSpans();

        for(int i = 0; i < spans.size(); i++) {
            for(int j = i + 1; j < spans.size(); j++) {
                Assertions.assertFalse(
                        spans.get(i).getCharacterStart() == spans.get(j).getCharacterStart()
                                && spans.get(i).getCharacterEnd() == spans.get(j).getCharacterEnd(),
                        "the same value was reported twice: " + spans.get(i));
            }
        }

    }

    @Test
    void twoSeamsInOneDocumentAreBothCaught() throws Exception {

        final String input = "the first date was May 22, 1999 and the second date was June 30, 2001 in the record";

        final TextFilterResult result = filter(policy(30), input);

        Assertions.assertFalse(result.getFilteredText().contains("1999"));
        Assertions.assertFalse(result.getFilteredText().contains("2001"));
        Assertions.assertEquals(2, result.getExplanation().appliedSpans().size());

    }

    @Test
    void parityHoldsForARangeOfOverlaps() throws Exception {

        final String expected = filter(unsplitPolicy(), MIXED_INPUT).getFilteredText();

        for(final int overlap : new int[]{10, 30, 100, 1000}) {
            Assertions.assertEquals(expected, filter(policy(overlap), MIXED_INPUT).getFilteredText(),
                    "overlap of " + overlap + " changed the result");
        }

    }

    @Test
    void overlapIsIgnoredWhenSplittingIsDisabled() throws Exception {

        final Policy policy = policy(30);
        policy.getConfig().getSplitting().setEnabled(false);

        Assertions.assertEquals(filter(unsplitPolicy(), MIXED_INPUT).getFilteredText(),
                filter(policy, MIXED_INPUT).getFilteredText());

    }

    @Test
    void overlapIsIgnoredBelowTheThreshold() throws Exception {

        final String input = "ssn 123-45-6789";

        final Policy policy = policy(30);
        policy.getConfig().getSplitting().setThreshold(5000);

        Assertions.assertEquals("ssn {{{REDACTED-ssn}}}", filter(policy, input).getFilteredText());

    }

    @Test
    void anUnlocatablePieceFallsBackToFilteringWithoutOverlap() throws Exception {

        // The sentence splitter joins sentences with a space, so its pieces are not verbatim in this
        // input and an overlap cannot be applied. Filtering must still be correct.
        final String input = "Ada lives here.\nThe ssn is 123-45-6789. Ada lives here. The ssn is 123-45-6789.";

        final Policy withOverlap = policy(5);
        withOverlap.getConfig().getSplitting().setMethod("characters");
        withOverlap.getConfig().getSplitting().setThreshold(45);

        final Policy without = policy(0);
        without.getConfig().getSplitting().setMethod("characters");
        without.getConfig().getSplitting().setThreshold(45);

        final String filtered = filter(withOverlap, input).getFilteredText();

        Assertions.assertEquals(filter(without, input).getFilteredText(), filtered);
        Assertions.assertFalse(filtered.contains("123-45-6789"), "the SSNs must still be redacted");

    }

    @Test
    void anOverlapLargerThanTheDocumentStillFiltersOnce() throws Exception {

        final TextFilterResult result = filter(policy(5000), SEAM_INPUT);

        Assertions.assertEquals("the date was {{{REDACTED-date}}} in the record and more text here",
                result.getFilteredText());
        Assertions.assertEquals(1, result.getExplanation().appliedSpans().size());

    }

}
