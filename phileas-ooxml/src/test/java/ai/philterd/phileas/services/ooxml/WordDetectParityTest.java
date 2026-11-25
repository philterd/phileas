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

import static ai.philterd.phileas.services.ooxml.TestPackage.REL;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Detect reports what Redact redacts, including shape/SmartArt text and hyperlink targets, without
 * modifying the document. Ported from phileas-dotnet's WordDetectParityTests.
 */
public class WordDetectParityTest {

    private static final String EMAIL_AND_URL_POLICY = "{\"identifiers\":{\"emailAddress\":{},\"url\":{}}}";

    // Comments and tracked changes are kept, so detection is compared in them too.
    private static OoxmlRedactionOptions defaults() {
        return OoxmlTestFixtures.keepingCommentsAndRevisions();
    }

    private static String describe(final OfficeRedactionSpan s) {
        return s.getOrder() + "|" + s.getParagraphIndex() + "|" + s.getCharacterStart() + "-" + s.getCharacterEnd() + "|"
                + s.getText() + "|" + s.getReplacement() + "|" + s.getClassification();
    }

    // PII in body text, a SmartArt node, a chart title, a hyperlink target and a tracked deletion.
    private static byte[] createDocumentWithEveryLocation() {
        final TestPackage pkg = new TestPackage();
        pkg.rel("/", TestPackage.REL_OFFICE_DOCUMENT, WordDocs.DOCUMENT);
        final String linkId = pkg.externalRel(WordDocs.DOCUMENT, REL + "hyperlink", "mailto:jane@example.com");
        WordDocs.addSmartArt(pkg, "<a:p><a:r><a:t>Node smart@example.com</a:t></a:r></a:p>");
        pkg.rel(WordDocs.DOCUMENT, REL + "chart", "/word/charts/chart1.xml");
        pkg.part("/word/charts/chart1.xml", WordDocs.CT_CHART,
                "<c:chartSpace xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" "
                        + "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">"
                        + "<c:chart><c:title><c:tx><c:rich><a:bodyPr/><a:lstStyle/>"
                        + "<a:p><a:r><a:t>Chart chart@example.com</a:t></a:r></a:p></c:rich></c:tx></c:title>"
                        + "<c:plotArea><c:layout/></c:plotArea></c:chart></c:chartSpace>");
        pkg.part(WordDocs.DOCUMENT, TestPackage.CT_DOCX_MAIN, WordDocs.documentXml(
                "<w:p><w:hyperlink r:id=\"" + linkId + "\">" + WordDocs.run("Write to us") + "</w:hyperlink></w:p>"
                        + WordDocs.paraXml("Body mail body@example.com here.")
                        + "<w:p><w:del w:author=\"Reviewer\" w:id=\"1\"><w:r><w:delText>gone@example.com</w:delText></w:r></w:del></w:p>"));
        return pkg.toBytes();
    }

    @Test
    public void detectReportsSmartArtTextWithoutModifyingTheDocument() throws Exception {
        final byte[] input = WordDocs.createWithSmartArt(WordDocs.aParagraph("Mail john@example.com"), "Body.");
        final byte[] before = input.clone();

        final List<OfficeRedactionSpan> spans = WordDocumentRedactor.detect(input, TestFilters.email(), defaults());

        assertEquals(1, spans.size());
        assertEquals("john@example.com", spans.get(0).getText());
        assertEquals(-1, spans.get(0).getParagraphIndex());
        assertArrayEquals(before, input);
        assertTrue(WordDocs.allDrawingText(input).contains("john@example.com"));
    }

    @Test
    public void detectReportsHyperlinkTargetsWithoutModifyingTheDocument() throws Exception {
        final byte[] input = WordDocs.createWithHyperlink("Write to us", "mailto:jane@example.com", "Body.");
        final byte[] before = input.clone();

        final List<OfficeRedactionSpan> spans = WordDocumentRedactor.detect(input, TestFilters.email(), defaults());

        assertEquals(1, spans.size());
        assertEquals("mailto:jane@example.com", spans.get(0).getText());
        assertEquals(-1, spans.get(0).getParagraphIndex());
        assertArrayEquals(before, input);
        assertEquals(List.of("mailto:jane@example.com"), WordDocs.hyperlinkTargets(input));
    }

    @Test
    public void detectReportsHyperlinkTargetsInAHeader() throws Exception {
        final byte[] input = WordDocs.createWithHyperlinkInHeader("Write to us", "mailto:jane@example.com", "Body.");

        final List<OfficeRedactionSpan> spans = WordDocumentRedactor.detect(input, TestFilters.email(), defaults());

        assertEquals(1, spans.size());
        assertEquals("mailto:jane@example.com", spans.get(0).getText());
    }

    @Test
    public void detectReturnsTheSameSpansAsRedact() throws Exception {
        final byte[] input = createDocumentWithEveryLocation();

        final List<OfficeRedactionSpan> detected = WordDocumentRedactor.detect(input, TestFilters.email(), defaults());
        final List<OfficeRedactionSpan> redacted = WordDocumentRedactor.redact(input, TestFilters.email(), defaults()).spans();

        assertEquals(redacted.stream().map(WordDetectParityTest::describe).toList(),
                detected.stream().map(WordDetectParityTest::describe).toList());
        assertEquals(List.of("body@example.com", "smart@example.com", "mailto:jane@example.com", "gone@example.com", "chart@example.com"),
                detected.stream().map(OfficeRedactionSpan::getText).toList());
    }

    // The redacted hyperlink target is a URL; reporting it would make verification say PII was left.
    @Test
    public void detectOnARedactedFileReportsNothingEvenWithAUrlFilter() throws Exception {
        final byte[] input = createDocumentWithEveryLocation();
        final TextFilter filter = TestFilters.forPolicy(EMAIL_AND_URL_POLICY);

        final byte[] output = WordDocumentRedactor.redact(input, filter, defaults()).document();

        assertEquals(List.of(WordDocumentRedactor.REDACTED_HYPERLINK_TARGET), WordDocs.hyperlinkTargets(output));
        assertEquals(List.of(), WordDocumentRedactor.detect(output, filter, defaults()));
    }

    @Test
    public void redactingARedactedFileAgainDoesNotReportTheHyperlinkPlaceholder() throws Exception {
        final byte[] input = WordDocs.createWithHyperlink("Write to us", "mailto:jane@example.com", "Body.");
        final TextFilter filter = TestFilters.forPolicy(EMAIL_AND_URL_POLICY);

        final byte[] once = WordDocumentRedactor.redact(input, filter, defaults()).document();
        final OoxmlRedactionResult twice = WordDocumentRedactor.redact(once, filter, defaults());

        assertEquals(List.of(), twice.spans());
        assertTrue(WordDocs.hyperlinkIdsAllResolve(twice.document()));
    }

}
