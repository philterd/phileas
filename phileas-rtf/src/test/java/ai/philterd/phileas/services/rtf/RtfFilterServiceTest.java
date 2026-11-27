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
package ai.philterd.phileas.services.rtf;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.RtfFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static ai.philterd.phileas.services.rtf.RtfDocs.AUTHOR;
import static ai.philterd.phileas.services.rtf.RtfDocs.COMPANY;
import static ai.philterd.phileas.services.rtf.RtfDocs.EDITOR;
import static ai.philterd.phileas.services.rtf.RtfDocs.EMAIL;
import static ai.philterd.phileas.services.rtf.RtfDocs.OTHER_EMAIL;
import static ai.philterd.phileas.services.rtf.RtfDocs.SSN;
import static ai.philterd.phileas.services.rtf.RtfDocs.bytes;
import static ai.philterd.phileas.services.rtf.RtfDocs.doc;
import static ai.philterd.phileas.services.rtf.RtfDocs.text;

public class RtfFilterServiceTest {

    private static final String EMAIL_REDACTED = "{{{REDACTED-email-address}}}";
    private static final String SSN_REDACTED = "{{{REDACTED-ssn}}}";
    // A replacement as RTF writes it.
    private static final String EMAIL_RTF = RtfEdits.escape(EMAIL_REDACTED);

    private static final RtfFilterService SERVICE = new RtfFilterService(new PhileasConfiguration(new Properties()),
            new DefaultContextService(), new InMemoryVectorService(), null);

    private static Policy policy(final String word) {
        return new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}}"
                + (word == null ? "" : ",\"config\":{\"word\":" + word + "}") + "}", Policy.class);
    }

    private static BinaryDocumentFilterResult redact(final String word, final String rtf) throws Exception {
        return SERVICE.filter(policy(word), "ctx", bytes(rtf), MimeType.APPLICATION_RTF);
    }

    private static String out(final String word, final String rtf) throws Exception {
        return text(redact(word, rtf).getDocument());
    }

    private static String visible(final String rtf) {
        return RtfTextExtractor.extract(rtf);
    }

    // --- text ----------------------------------------------------------------------------------------

    @Test
    public void redactsBodyTextInPlaceKeepingItsFormatting() throws Exception {

        final String input = doc("Email {\\b " + EMAIL + "} today.\\par ");
        final String output = out(null, input);

        Assertions.assertEquals(input.replace(EMAIL, EMAIL_RTF), output);
        Assertions.assertEquals("Email " + EMAIL_REDACTED + " today.", visible(output));

    }

    @Test
    public void writesWhatItDoesNotChangeByteForByte() throws Exception {
        // A control word right before text without a space delimiter is left as it was written.
        final String input = doc("{\\fs20x\\b\\'41\\u8217?s} Email " + EMAIL + ".\\par ");
        Assertions.assertEquals(input.replace(EMAIL, EMAIL_RTF), out(null, input));
    }

    @Test
    public void redactsAValueSplitAcrossRunsAndEscapes() throws Exception {

        // The email is split across a bold run and written with a hex escape for the @; the SSN uses Unicode
        // escapes for its hyphens.
        final String input = doc("Mail jane.doe{\\b \\'40exam}ple.com and 123\\u45?45\\u45?6789 now.\\par ");
        final String output = out(null, input);

        Assertions.assertEquals("Mail " + EMAIL_REDACTED + " and " + SSN_REDACTED + " now.", visible(output));
        Assertions.assertTrue(output.contains("{\\b }"), "the bold run is kept");
        Assertions.assertFalse(output.contains("jane") || output.contains("6789"), output);

    }

    @Test
    public void redactsTablesFootnotesAndEndnotes() throws Exception {

        final String input = doc("\\trowd\\cellx2000\\cellx4000\\intbl Email\\cell " + EMAIL + "\\cell\\row\\pard "
                + "See note{\\footnote\\pard Contact " + OTHER_EMAIL + ".}{\\footnote\\ftnalt\\pard SSN " + SSN + ".}.\\par ");
        final String output = out(null, input);

        Assertions.assertFalse(output.contains(EMAIL) || output.contains(OTHER_EMAIL) || output.contains(SSN), output);
        Assertions.assertEquals("Email\t" + EMAIL_REDACTED + "\nSee note.\nContact " + EMAIL_REDACTED + ".\nSSN "
                + SSN_REDACTED + ".", visible(output));
        Assertions.assertTrue(output.contains("\\ftnalt"));

    }

    @Test
    public void redactsFieldInstructionsAndKeepsTheField() throws Exception {
        final String input = doc("{\\field{\\*\\fldinst HYPERLINK \"mailto:" + EMAIL + "\"}{\\fldrslt write}}\\par ");
        final String output = out(null, input);
        Assertions.assertFalse(output.contains(EMAIL), output);
        Assertions.assertTrue(output.contains("{\\*\\fldinst HYPERLINK \"mailto:" + EMAIL_RTF + "\"}"), output);
    }

    @Test
    public void aReplacementAfterAControlWordWithoutADelimiterStaysText() throws Exception {

        final Policy policy = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{\"emailAddressFilterStrategies\":"
                + "[{\"strategy\":\"STATIC_REPLACE\",\"staticReplacement\":\"hidden\"}]}}}", Policy.class);
        final String input = doc("Email \\b\\'6aane.doe@example.com\\b0  done.\\par ");

        final String output = text(SERVICE.filter(policy, "ctx", bytes(input), MimeType.APPLICATION_RTF).getDocument());

        Assertions.assertEquals("Email hidden done.", visible(output));
        Assertions.assertTrue(output.contains("\\b hidden"), output);

    }

    // --- config.word ---------------------------------------------------------------------------------

    @Test
    public void headersFooters() throws Exception {
        final String input = doc("{\\header\\pard Header " + EMAIL + "\\par}{\\footer\\pard Footer\\par}Body.\\par ");
        Assertions.assertFalse(out("{\"headersFooters\":\"redact\"}", input).contains(EMAIL));
        final String kept = out("{\"headersFooters\":\"keep\"}", input);
        Assertions.assertTrue(kept.contains("{\\header\\pard Header " + EMAIL + "\\par}"), kept);
        Assertions.assertTrue(kept.contains("{\\footer\\pard Footer\\par}"));
    }

    @Test
    public void commentsRemoveLeavesNoCommentTextAuthorOrMarks() throws Exception {
        final String output = out("{\"comments\":\"remove\"}", RtfDocs.commented());
        for (final String gone : List.of("annotation", "atnauthor", "atnid", "atrfstart", "atrfend", "atnref", "chatn",
                "Ask", AUTHOR, EMAIL)) {
            Assertions.assertFalse(output.contains(gone), gone + " in " + output);
        }
        Assertions.assertEquals("Body text.", visible(output));
    }

    @Test
    public void commentsRedactKeepsThemAndClearsTheirAuthors() throws Exception {
        final String output = out("{\"comments\":\"redact\"}", RtfDocs.commented());
        Assertions.assertTrue(output.contains("Ask " + EMAIL_RTF), output);
        Assertions.assertTrue(output.contains("{\\*\\atnauthor " + RtfRedactor.NEUTRAL_AUTHOR + "}"), output);
        Assertions.assertTrue(output.contains("{\\*\\atnid }"), output);
        Assertions.assertFalse(output.contains(AUTHOR));
    }

    @Test
    public void trackedChangesAcceptRemovesDeletionsAndRevisionMarksAndRedactsTheInsertion() throws Exception {
        final String output = out("{\"trackedChanges\":\"accept\"}", RtfDocs.tracked());
        for (final String gone : List.of("\\deleted", "\\revised", "\\revauth", "revtbl", "Gone", OTHER_EMAIL, AUTHOR)) {
            Assertions.assertFalse(output.contains(gone), gone + " in " + output);
        }
        Assertions.assertEquals("Kept Added " + EMAIL_REDACTED + ".", visible(output));
    }

    @Test
    public void trackedChangesRedactKeepsTheRevisionsAndRedactsDeletedText() throws Exception {
        final String output = out("{\"trackedChanges\":\"redact\"}", RtfDocs.tracked());
        Assertions.assertTrue(output.contains("{\\deleted\\revauthdel1 Gone " + EMAIL_RTF + " }"), output);
        Assertions.assertTrue(output.contains("{\\revised\\revauth1 Added " + EMAIL_RTF + "}"), output);
        Assertions.assertTrue(output.contains("{\\*\\revtbl {" + RtfRedactor.NEUTRAL_AUTHOR + ";}{" + RtfRedactor.NEUTRAL_AUTHOR + ";}}"), output);
        Assertions.assertFalse(output.contains(AUTHOR));
    }

    @Test
    public void acceptingADeletedParagraphMarkJoinsTheParagraphs() throws Exception {
        final String output = out(null, doc("First{\\deleted\\par} second.\\par "));
        Assertions.assertEquals("First second.", visible(output));
    }

    @Test
    public void hiddenText() throws Exception {
        final String input = doc("Visible {\\v Secret " + EMAIL + "} shown.\\par ");
        final String removed = out("{\"hiddenText\":\"remove\"}", input);
        Assertions.assertFalse(removed.contains("Secret") || removed.contains(EMAIL), removed);
        final String kept = out("{\"hiddenText\":\"redact\"}", input);
        Assertions.assertTrue(kept.contains("{\\v Secret " + EMAIL_RTF + "}"), kept);
    }

    @Test
    public void uninspectableObjects() throws Exception {
        final String input = doc("Before {\\object\\objemb{\\*\\objclass Package}{\\*\\objdata 0102}"
                + "{\\result {\\pict\\wmetafile8 0102}}} after.\\par ");
        Assertions.assertFalse(out("{\"uninspectableObjects\":\"remove\"}", input).contains("\\object"));
        Assertions.assertTrue(out("{\"uninspectableObjects\":\"keep\"}", input).contains("\\object\\objemb"));
    }

    @Test
    public void removeDocumentProperties() throws Exception {
        final String input = RtfDocs.withInfo("Body.\\par ");

        final String removed = out("{\"removeDocumentProperties\":true}", input);
        for (final String gone : List.of("Quarterly notes", "Planning", "budget", "Draft for review", "Finance",
                "userprops", "Example Client")) {
            Assertions.assertFalse(removed.contains(gone), gone + " in " + removed);
        }
        Assertions.assertTrue(removed.contains("{\\title }"), removed);

        final String kept = out("{\"removeDocumentProperties\":false}", input);
        Assertions.assertTrue(kept.contains("{\\title Quarterly notes}") && kept.contains("Example Client"), kept);
    }

    @Test
    public void authorshipIsClearedWithEverySetting() throws Exception {
        final String document = RtfDocs.everything();
        Assertions.assertTrue(document.contains(AUTHOR) && document.contains(EDITOR) && document.contains(COMPANY));
        for (final String comments : List.of("remove", "redact")) {
            for (final String tracked : List.of("accept", "redact")) {
                for (final String hidden : List.of("remove", "redact")) {
                    for (final boolean properties : new boolean[]{true, false}) {
                        final String word = "{\"comments\":\"" + comments + "\",\"trackedChanges\":\"" + tracked
                                + "\",\"hiddenText\":\"" + hidden + "\",\"removeDocumentProperties\":" + properties + "}";
                        final String output = out(word, document);
                        for (final String name : List.of(AUTHOR, EDITOR, COMPANY)) {
                            Assertions.assertFalse(output.contains(name), name + " with " + word + ": " + output);
                        }
                        Assertions.assertTrue(output.contains("{\\author }") && output.contains("{\\operator }"), output);
                    }
                }
            }
        }
    }

    @Test
    public void theDefaultsRemoveCommentsAcceptChangesAndRemoveHiddenText() throws Exception {
        Assertions.assertFalse(out(null, RtfDocs.commented()).contains("annotation"));
        Assertions.assertFalse(out(null, RtfDocs.tracked()).contains("\\deleted"));
        Assertions.assertFalse(out(null, doc("A {\\v hidden} b.\\par ")).contains("hidden"));
    }

    @Test
    public void oneServiceAppliesEachCallsPolicy() throws Exception {
        Assertions.assertFalse(out("{\"comments\":\"remove\"}", RtfDocs.commented()).contains("annotation"));
        Assertions.assertTrue(out("{\"comments\":\"redact\"}", RtfDocs.commented()).contains("annotation"));
    }

    // --- spans -----------------------------------------------------------------------------------------

    @Test
    public void spansHaveTheirRunAndOffsets() throws Exception {
        final List<Span> spans = redact(null, doc("First.\\par Email " + EMAIL + ".\\par ")).getExplanation().appliedSpans();
        Assertions.assertEquals(1, spans.size());
        Assertions.assertEquals(1, spans.get(0).getParagraphNumber());
        Assertions.assertEquals("Email ".length(), spans.get(0).getCharacterStart());
        Assertions.assertEquals(EMAIL, spans.get(0).getText());
    }

    @Test
    public void applyingEveryDetectedSpanGivesTheSameDocument() throws Exception {
        final Policy policy = policy("{\"comments\":\"redact\",\"trackedChanges\":\"redact\",\"hiddenText\":\"redact\"}");
        final String input = RtfDocs.everything().replace("Body text", "Mail jane.doe{\\b \\'40exam}ple.com, SSN " + SSN
                + "{\\footnote\\pard " + OTHER_EMAIL + "}");
        final BinaryDocumentFilterResult result = SERVICE.filter(policy, "ctx", bytes(input), MimeType.APPLICATION_RTF);
        final byte[] applied = SERVICE.apply(policy, bytes(input), result.getExplanation().appliedSpans(), MimeType.APPLICATION_RTF);
        Assertions.assertEquals(text(result.getDocument()), text(applied));
    }

    // --- safety ------------------------------------------------------------------------------------------

    @Test
    public void refusesMalformedInput() {
        Assertions.assertThrows(RtfFormatException.class, () -> redact(null, "{\\rtf1 unclosed"));
        Assertions.assertThrows(RtfFormatException.class, () -> redact(null, "{\\rtf1 " + "{".repeat(100_000)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SERVICE.filter(policy(null), "ctx",
                bytes(doc("x\\par ")), MimeType.TEXT_PLAIN));
    }

    @Test
    public void everyOutputIsWellFormedRtf() throws Exception {
        for (final String input : List.of(RtfDocs.commented(), RtfDocs.tracked(), RtfDocs.withInfo("x\\par "),
                doc("{\\v " + EMAIL + "}\\par "))) {
            for (final String word : List.of("{}", "{\"comments\":\"redact\",\"trackedChanges\":\"redact\",\"hiddenText\":\"redact\"}")) {
                final String output = out(word, input);
                Assertions.assertFalse(RtfTokenizer.tokenize(output).isEmpty(), output);
            }
        }
    }

}
