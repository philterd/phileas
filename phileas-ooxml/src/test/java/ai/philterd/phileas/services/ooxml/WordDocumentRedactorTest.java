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
package ai.philterd.phileas.services.ooxml;

import ai.philterd.phileas.policy.config.Word;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Word (.docx) redaction. Ported from phileas-dotnet's WordDocumentRedactorTests. */
public class WordDocumentRedactorTest {

    // Emails redact to {{{REDACTED-email-address}}}.
    private static final TextFilter FILTER = TestFilters.email();

    private static OoxmlRedactionOptions defaults() {
        return OoxmlRedactionOptions.defaults();
    }

    // Emptying a changed paragraph must not delete the paragraph itself.
    @Test
    public void redactReplacesChangedParagraphsWithoutDeletingThem() throws Exception {
        final byte[] input = WordDocs.create("keep one", "email aaa@example.com here", "email bbb@example.com too", "keep four");
        final int originalCount = WordDocs.bodyParagraphs(input).size();

        final byte[] output = WordDocumentRedactor.redact(input, FILTER, defaults()).document();

        final List<String> result = WordDocs.bodyParagraphs(output);
        assertEquals(originalCount, result.size());
        assertTrue(result.contains("keep one"));
        assertTrue(result.contains("keep four"));
        assertTrue(result.contains("email {{{REDACTED-email-address}}} here"));
        assertTrue(result.stream().noneMatch(p -> p.contains("@example.com")));
    }

    @Test
    public void readParagraphsAndDetectGiveParagraphIndexedSpans() throws Exception {
        final byte[] input = WordDocs.create("keep one", "email aaa@example.com here", "keep three");
        final byte[] copy = input.clone();

        assertEquals(List.of("keep one", "email aaa@example.com here", "keep three"), WordDocumentRedactor.readParagraphs(input));

        final List<OfficeRedactionSpan> spans = WordDocumentRedactor.detect(input, FILTER, defaults());
        assertEquals(1, spans.size());
        assertEquals(1, spans.get(0).getParagraphIndex());
        assertEquals("aaa@example.com", spans.get(0).getText());
        // Detection leaves the input untouched.
        assertArrayEquals(copy, input);
        assertTrue(WordDocs.bodyParagraphs(input).stream().anyMatch(p -> p.contains("@example.com")));
    }

    @Test
    public void redactUnchangedTextLeavesParagraphsIdentical() throws Exception {
        final byte[] input = WordDocs.create("alpha", "beta", "gamma");

        final byte[] output = WordDocumentRedactor.redact(input, FILTER, defaults()).document();

        assertEquals(WordDocs.bodyParagraphs(input), WordDocs.bodyParagraphs(output));
    }

    @Test
    public void redactRedactsHeadersAndFooters() throws Exception {
        final byte[] input = WordDocs.createWithHeaderFooter("header hh@example.com", "footer ff@example.com", "body bb@example.com");

        final byte[] output = WordDocumentRedactor.redact(input, FILTER, defaults()).document();

        final String body = WordDocs.allBodyText(output);
        final String header = WordDocs.headersText(output);
        final String footer = WordDocs.footersText(output);
        assertFalse((body + header + footer).contains("@example.com"));
        assertTrue(header.contains("REDACTED"));
        assertTrue(footer.contains("REDACTED"));
        assertTrue(body.contains("REDACTED"));
    }

    @Test
    public void redactLeavesInputUnchanged() throws Exception {
        final byte[] input = WordDocs.create("contact data@example.com");
        final byte[] copy = input.clone();

        WordDocumentRedactor.redact(input, FILTER, defaults());

        assertArrayEquals(copy, input);
        assertTrue(WordDocs.bodyParagraphs(input).stream().anyMatch(p -> p.contains("@example.com")));
    }

    // A failure mid-redaction propagates; no partial document is returned.
    @Test
    public void redactFailureMidwayThrows() {
        final byte[] input = WordDocs.create("keep one", "boom secret@example.com here");
        final TextFilter throwing = t -> {
            if (t.contains("boom")) {
                throw new IllegalStateException("simulated redaction failure");
            }
            return FILTER.apply(t);
        };

        assertThrows(IllegalStateException.class, () -> WordDocumentRedactor.redact(input, throwing, defaults()));
    }

    @Test
    public void redactWithHighlightHighlightsTheReplacement() throws Exception {
        final byte[] input = WordDocs.create("email zzz@example.com end");

        final byte[] output = WordDocumentRedactor.redact(input, FILTER, defaults().setWord(new Word().setHighlight(true))).document();

        assertTrue(WordDocs.bodyParagraphs(output).contains("email {{{REDACTED-email-address}}} end"));
        final String xml = WordDocs.documentXml(output);
        assertTrue(xml.contains("highlight"));
        assertTrue(xml.contains("yellow"));
    }

    // --- Tracked deletions (w:delText) ---

    @Test
    public void redactRedactsDeletedTrackedTextKeepsTheDeletion() throws Exception {
        final byte[] input = WordDocs.createWithTrackedDeletion("removed contact aaa@example.com", "Visible body text.");

        final byte[] output = WordDocumentRedactor.redact(input, FILTER, OoxmlTestFixtures.keepingCommentsAndRevisions()).document();

        final List<String> deleted = WordDocs.deletedTexts(output);
        assertFalse(deleted.isEmpty());
        final String joined = String.join("", deleted);
        assertFalse(joined.contains("aaa@example.com"));
        assertTrue(joined.contains("REDACTED"));
        assertFalse(WordDocs.anyPartContains(output, "aaa@example.com"));
    }

    @Test
    public void redactReportsDeletedTextRedactionAsSpan() throws Exception {
        final byte[] input = WordDocs.createWithTrackedDeletion("old email aaa@example.com", "Body.");

        final List<OfficeRedactionSpan> spans = WordDocumentRedactor.redact(input, FILTER, OoxmlTestFixtures.keepingCommentsAndRevisions()).spans();

        assertTrue(spans.stream().anyMatch(s -> s.getText().contains("aaa@example.com")));
    }

    @Test
    public void detectFindsPiiInDeletedText() throws Exception {
        final byte[] input = WordDocs.createWithTrackedDeletion("hidden aaa@example.com", "Body.");

        final List<OfficeRedactionSpan> residuals = WordDocumentRedactor.detect(input, FILTER, OoxmlTestFixtures.keepingCommentsAndRevisions());

        assertTrue(residuals.stream().anyMatch(s -> s.getText().contains("aaa@example.com")));
    }

    // applySpans with a policy filter re-redacts the non-positional content, including deleted text.
    @Test
    public void applySpansWithFilterRedactsDeletedTextLikeModify() throws Exception {
        final byte[] input = WordDocs.createWithTrackedDeletion("old aaa@example.com", "Body.");

        final byte[] output = WordDocumentRedactor.applySpans(input, new ArrayList<>(), defaults(), FILTER);

        assertFalse(WordDocs.anyPartContains(output, "aaa@example.com"));
    }

}
