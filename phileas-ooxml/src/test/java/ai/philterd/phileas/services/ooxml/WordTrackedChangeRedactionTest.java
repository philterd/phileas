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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tracked-change deleted text is never reintroduced as normal text when a paragraph is rebuilt, and its
 * PII is still redacted in place. Ported from phileas-dotnet's WordTrackedChangeRedactionTests.
 */
public class WordTrackedChangeRedactionTest {

    private static final TextFilter FILTER = TestFilters.email();

    // Normal text, a tracked deletion, then normal text containing an email.
    private static final String PARAGRAPH_WITH_DELETION =
            "<w:p>"
                    + "<w:r><w:t xml:space=\"preserve\">Email </w:t></w:r>"
                    + "<w:del w:id=\"1\" w:author=\"Reviewer\"><w:r><w:delText xml:space=\"preserve\">DELETEDSECRET </w:delText></w:r></w:del>"
                    + "<w:r><w:t xml:space=\"preserve\">to a@example.com today</w:t></w:r>"
                    + "</w:p>";

    private static OoxmlRedactionOptions defaults() {
        return OoxmlRedactionOptions.defaults();
    }

    @Test
    public void redactRebuiltParagraphDoesNotReintroduceDeletedText() throws Exception {
        final byte[] input = WordDocs.createRaw(PARAGRAPH_WITH_DELETION);
        assertTrue(WordDocs.anyPartContains(input, "DELETEDSECRET"));

        final byte[] output = WordDocumentRedactor.redact(input, FILTER, defaults()).document();

        assertTrue(WordDocs.bodyParagraphs(output).stream().noneMatch(p -> p.contains("DELETEDSECRET")));
        assertFalse(WordDocs.anyPartContains(output, "DELETEDSECRET"));
    }

    @Test
    public void redactStillRedactsVisibleTextAroundTheDeletion() throws Exception {
        final byte[] input = WordDocs.createRaw(PARAGRAPH_WITH_DELETION);

        final byte[] output = WordDocumentRedactor.redact(input, FILTER, defaults()).document();

        assertFalse(WordDocs.anyPartContains(output, "@example.com"));
        assertTrue(WordDocs.bodyParagraphs(output).stream()
                .anyMatch(p -> p.contains("Email") && p.contains("REDACTED") && p.contains("today")));
    }

    @Test
    public void readParagraphsExcludesDeletedText() throws Exception {
        final byte[] input = WordDocs.createRaw(PARAGRAPH_WITH_DELETION);

        final List<String> paragraphs = WordDocumentRedactor.readParagraphs(input);

        assertTrue(paragraphs.stream()
                .anyMatch(p -> p.contains("Email") && p.contains("a@example.com") && !p.contains("DELETEDSECRET")));
    }

    // A deletion holding an email would ship verbatim, so detection must flag it.
    @Test
    public void detectFlagsPiiInDeletedText() throws Exception {
        final byte[] input = WordDocs.createRaw(
                "<w:p><w:r><w:t xml:space=\"preserve\">keep </w:t></w:r>"
                        + "<w:del w:id=\"1\" w:author=\"R\"><w:r><w:delText xml:space=\"preserve\">deleted@example.com</w:delText></w:r></w:del>"
                        + "<w:r><w:t xml:space=\"preserve\"> end</w:t></w:r></w:p>");

        final List<OfficeRedactionSpan> spans = WordDocumentRedactor.detect(input, FILTER, defaults());

        assertTrue(spans.stream().anyMatch(s -> s.getText().contains("@example.com")));
    }

    // A paragraph that is only a tracked deletion with no PII is left exactly as it was.
    @Test
    public void redactUnchangedParagraphWithDeletionPreservesItAsTrackedDeletion() throws Exception {
        final byte[] input = WordDocs.createRaw(
                "<w:p><w:del w:id=\"1\" w:author=\"R\"><w:r><w:delText xml:space=\"preserve\">DELONLY</w:delText></w:r></w:del></w:p>"
                        + "<w:p><w:r><w:t xml:space=\"preserve\">body a@example.com</w:t></w:r></w:p>");

        final byte[] output = WordDocumentRedactor.redact(input, FILTER, defaults()).document();

        final String xml = WordDocs.documentXml(output);
        assertTrue(xml.contains("delText"));
        assertTrue(xml.contains("DELONLY"));
        assertFalse(WordDocs.anyPartContains(output, "@example.com"));
        assertNotNull(WordDocs.body(output));
    }

}
