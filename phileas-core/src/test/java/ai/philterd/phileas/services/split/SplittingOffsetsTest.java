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
import ai.philterd.phileas.policy.filters.PhoneNumber;
import ai.philterd.phileas.policy.filters.Ssn;
import ai.philterd.phileas.policy.filters.ZipCode;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.DateFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.EmailAddressFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.PhoneNumberFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.ZipCodeFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Properties;

/** Span offsets must index into the input, not into the filtered output, however it was split. */
class SplittingOffsetsTest {

    // Blank lines and runs of spaces, which a trim-and-rejoin splitter cannot reproduce.
    private static final String INPUT = """
            Intake   record for the clinic

            The ssn is 123-45-6789 and the second ssn is 987-65-4321.

            Mail    ada@example.com or grace@example.com for a copy.
            Call 090-342-3423 during business hours.

            The date was May 22, 1999 when the record was opened, and the
            date was June 30, 2001 when it was closed.
            """;

    private Policy policy(final String method, final int overlap, final String staticReplacement) {

        final Date date = new Date();
        date.setDateFilterStrategies(List.of(strategy(new DateFilterStrategy(), staticReplacement)));

        final Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(List.of(strategy(new SsnFilterStrategy(), staticReplacement)));

        final EmailAddress emailAddress = new EmailAddress();
        emailAddress.setEmailAddressFilterStrategies(
                List.of(strategy(new EmailAddressFilterStrategy(), staticReplacement)));

        final PhoneNumber phoneNumber = new PhoneNumber();
        phoneNumber.setPhoneNumberFilterStrategies(
                List.of(strategy(new PhoneNumberFilterStrategy(), staticReplacement)));

        final Identifiers identifiers = new Identifiers();
        identifiers.setDate(date);
        identifiers.setSsn(ssn);
        identifiers.setEmailAddress(emailAddress);
        identifiers.setPhoneNumber(phoneNumber);

        final Splitting splitting = new Splitting();
        splitting.setEnabled(true);
        splitting.setMethod(method);
        splitting.setThreshold(64);
        splitting.setOverlap(overlap);

        final Config config = new Config();
        config.setSplitting(splitting);

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);
        policy.setConfig(config);

        return policy;

    }

    // A replacement shorter than the value drifts the filtered text left of the input.
    private <T extends AbstractFilterStrategy> T strategy(final T strategy, final String staticReplacement) {

        if(staticReplacement != null) {
            strategy.setStrategy(AbstractFilterStrategy.STATIC_REPLACE);
            strategy.setStaticReplacement(staticReplacement);
        }

        return strategy;

    }

    private TextFilterResult filter(final Policy policy, final String input) throws Exception {
        return new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null)
                .filter(policy, "context", input);
    }

    private void assertOffsetsIndexIntoTheInput(final TextFilterResult result, final String input) {

        Assertions.assertFalse(result.getExplanation().identifiedSpans().isEmpty(),
                "nothing was found, so the offsets were not exercised");

        for(final Span span : result.getExplanation().identifiedSpans()) {
            Assertions.assertEquals(span.getText(),
                    input.substring(span.getCharacterStart(), span.getCharacterEnd()),
                    "identified span offsets do not index into the input: " + span);
        }

        for(final Span span : result.getExplanation().appliedSpans()) {
            Assertions.assertEquals(span.getText(),
                    input.substring(span.getCharacterStart(), span.getCharacterEnd()),
                    "applied span offsets do not index into the input: " + span);
        }

    }

    @ParameterizedTest
    @CsvSource({"width,0", "width,32", "newline,0", "newline,32"})
    void spanOffsetsIndexIntoTheInput(final String method, final int overlap) throws Exception {
        assertOffsetsIndexIntoTheInput(filter(policy(method, overlap, null), INPUT), INPUT);
    }

    @ParameterizedTest
    @CsvSource({"width,0", "width,32", "newline,0", "newline,32"})
    void spanOffsetsIndexIntoTheInputWhenReplacementsChangeTheLength(final String method, final int overlap)
            throws Exception {

        // "*" is much shorter than any value it replaces, so every span after the first is drifted.
        final TextFilterResult result = filter(policy(method, overlap, "*"), INPUT);

        Assertions.assertTrue(result.getFilteredText().length() < INPUT.length(),
                "the replacements did not change the length, so drift was not exercised");

        assertOffsetsIndexIntoTheInput(result, INPUT);

    }

    @ParameterizedTest
    @ValueSource(strings = {"width", "newline"})
    void whitespaceIsPreservedWhenSplitting(final String method) throws Exception {

        final String filtered = filter(policy(method, 0, null), INPUT).getFilteredText();

        Assertions.assertTrue(filtered.startsWith("Intake   record for the clinic\n\n"),
                "the blank line and the run of spaces were lost: " + filtered);
        Assertions.assertTrue(filtered.contains("Mail    "), "the run of spaces was lost: " + filtered);
        Assertions.assertEquals(INPUT.chars().filter(c -> c == '\n').count(),
                filtered.chars().filter(c -> c == '\n').count(), "newlines were lost: " + filtered);

    }

    @ParameterizedTest
    @ValueSource(strings = {"width", "newline"})
    void theDocumentIsActuallySplitIntoSeveralPieces(final String method) {

        // Guards the tests above, which prove nothing if the input filters as one piece.
        Assertions.assertTrue(SplitFactory.getSplitService(method, 64).split(INPUT).size() > 1,
                "the input did not split into several pieces");

    }

    @ParameterizedTest
    @ValueSource(strings = {"width", "newline"})
    void splittingWithoutAnOverlapDoesNotRedactLessThanWithOne(final String method) throws Exception {

        // An overlap can only find more, never fewer.
        Assertions.assertTrue(
                filter(policy(method, 0, null), INPUT).getExplanation().appliedSpans().size()
                        <= filter(policy(method, 32, null), INPUT).getExplanation().appliedSpans().size());

    }

    @ParameterizedTest
    @ValueSource(strings = {"width", "newline"})
    void awkwardLeadingCharactersDoNotCostTheOffsets(final String method) throws Exception {

        // A control character is trimmed but is not isWhitespace; U+2028 and U+3000 are the
        // reverse. Testing the wrong set either way drops the document onto the fallback.
        for(final char awkward : new char[]{'\u0001', '\u001F', '\u2028', '\u2029', '\u3000', '\u00A0'}) {

            final String input = awkward + INPUT;

            assertOffsetsIndexIntoTheInput(filter(policy(method, 0, null), input), input);

        }

    }

    @Test
    void everyValueIsStillRedactedWithoutAnOverlap() throws Exception {

        final String filtered = filter(policy("newline", 0, null), INPUT).getFilteredText();

        Assertions.assertFalse(filtered.contains("123-45-6789"));
        Assertions.assertFalse(filtered.contains("987-65-4321"));
        Assertions.assertFalse(filtered.contains("ada@example.com"));
        Assertions.assertFalse(filtered.contains("grace@example.com"));

    }

    @ParameterizedTest
    @CsvSource({"width,0", "width,32", "newline,0", "newline,32"})
    void anIdentifiedButUnappliedSpanAlsoIndexesIntoTheInput(final String method, final int overlap)
            throws Exception {

        // A validated zip code filter marks an unreal zip unapplied. Such a span never reaches
        // the text, so its offsets are all a caller gets.
        final String input = INPUT + "\nThe zip code is 00000 and that is not a real one.\n";

        final ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(List.of(new ZipCodeFilterStrategy()));
        zipCode.setValidate(true);

        final Policy policy = policy(method, overlap, null);
        policy.getIdentifiers().setZipCode(zipCode);

        final TextFilterResult result = filter(policy, input);

        final List<Span> unapplied = result.getExplanation().identifiedSpans().stream()
                .filter(span -> !span.isApplied()).toList();

        Assertions.assertEquals(1, unapplied.size(), "the invalid zip code was not identified unapplied");
        Assertions.assertEquals("00000", unapplied.get(0).getText());
        Assertions.assertTrue(result.getFilteredText().contains("00000"), "an unapplied span was redacted");

        assertOffsetsIndexIntoTheInput(result, input);

    }

    @Test
    void theCharactersMethodIndexesIntoTheInputWhenItsPiecesAreVerbatim() throws Exception {

        // The sentence splitter rejoins with one space, so its pieces are verbatim when that is
        // what separated them.
        final String input = "The ssn is 123-45-6789. Mail ada@example.com for a copy. "
                + "The second ssn is 987-65-4321. Mail grace@example.com instead. "
                + "The date was May 22, 1999 when it opened.";

        final Policy policy = policy("characters", 0, null);
        policy.getConfig().getSplitting().setThreshold(60);

        final TextFilterResult result = filter(policy, input);

        assertOffsetsIndexIntoTheInput(result, input);

    }

    @Test
    void theFallbackShiftsTheOffsetsAndNormalizesWhitespace() throws Exception {

        // Pins the documented cost of the fallback. Failing here means the fallback was fixed,
        // and the documentation needs updating with it, not that something broke.
        final String input = "Ada lives here.\nThe ssn is 123-45-6789. Ada lives here. The ssn is 123-45-6789.";

        final Policy policy = policy("characters", 0, null);
        policy.getConfig().getSplitting().setThreshold(45);

        final TextFilterResult result = filter(policy, input);

        Assertions.assertFalse(result.getFilteredText().contains("123-45-6789"),
                "the fallback must still redact");
        Assertions.assertFalse(result.getFilteredText().contains("\n"),
                "the fallback is expected to normalize whitespace");

        // A fallback offset can run past the end of the input, not merely point at wrong text.
        final boolean everyOffsetIsGood = result.getExplanation().appliedSpans().stream().allMatch(
                span -> span.getCharacterEnd() <= input.length()
                        && span.getText().equals(
                                input.substring(span.getCharacterStart(), span.getCharacterEnd())));

        Assertions.assertFalse(everyOffsetIsGood, "the fallback is expected to shift the offsets");

    }

    @ParameterizedTest
    @ValueSource(strings = {"width", "newline", "characters"})
    void aDocumentWithNothingToRedactIsReturnedExactlyAsItWas(final String method) throws Exception {

        final String input = "Intake   record for the clinic\n\nNothing here is sensitive at all.\n"
                + "   Not one    value in this    document.\n\nNot a single one of them.\n";

        final TextFilterResult result = filter(policy(method, 0, null), input);

        Assertions.assertEquals(input, result.getFilteredText());
        Assertions.assertTrue(result.getExplanation().identifiedSpans().isEmpty());

    }

    @ParameterizedTest
    @ValueSource(strings = {"width", "newline"})
    void whitespaceOnlyInputIsReturnedUnchanged(final String method) throws Exception {

        // Every piece is dropped as empty. A whitespace document must not come back empty.
        final String input = " ".repeat(40) + "\n\n" + " ".repeat(40);

        final Policy policy = policy(method, 0, null);
        policy.getConfig().getSplitting().setThreshold(16);

        Assertions.assertEquals(input, filter(policy, input).getFilteredText());

    }

    @ParameterizedTest
    @ValueSource(strings = {"width", "newline"})
    void aDocumentBelowTheThresholdIsNotSplitAndKeepsItsOffsets(final String method) throws Exception {

        final String input = "The ssn is 123-45-6789.";

        final Policy policy = policy(method, 0, null);
        policy.getConfig().getSplitting().setThreshold(input.length() + 1);

        assertOffsetsIndexIntoTheInput(filter(policy, input), input);

    }

    @ParameterizedTest
    @ValueSource(strings = {"width", "newline"})
    void aDocumentExactlyAtTheThresholdIsSplitAndKeepsItsOffsets(final String method) throws Exception {

        // The guard is >=, so a document of exactly the threshold length is split.
        final String input = "The ssn is 123-45-6789 and the mail is ada@example.com here.";

        final Policy policy = policy(method, 0, null);
        policy.getConfig().getSplitting().setThreshold(input.length());

        assertOffsetsIndexIntoTheInput(filter(policy, input), input);

    }

    @ParameterizedTest
    @ValueSource(strings = {"width", "newline"})
    void noValueIsReportedTwiceWithoutAnOverlap(final String method) throws Exception {

        final List<Span> spans = filter(policy(method, 0, null), INPUT).getExplanation().appliedSpans();

        for(int i = 0; i < spans.size(); i++) {
            for(int j = i + 1; j < spans.size(); j++) {
                Assertions.assertFalse(
                        spans.get(i).getCharacterStart() == spans.get(j).getCharacterStart()
                                && spans.get(i).getCharacterEnd() == spans.get(j).getCharacterEnd(),
                        "the same value was reported twice: " + spans.get(i));
            }
        }

    }

}
