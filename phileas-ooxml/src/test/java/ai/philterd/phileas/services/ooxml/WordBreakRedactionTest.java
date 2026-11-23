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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

import static ai.philterd.phileas.services.ooxml.TestPackage.W;
import static ai.philterd.phileas.services.ooxml.WordDocs.t;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Soft breaks, carriage returns and tabs in a Word paragraph separate the words on either side in the
 * text the filters see, and survive redaction. Ported from phileas-dotnet's WordBreakRedactionTests.
 */
public class WordBreakRedactionTest {

    private static final String BR = "<w:br/>";
    private static final String CR = "<w:cr/>";
    private static final String TAB = "<w:tab/>";

    private static final String NAME_OR_EMAIL_POLICY = "{\"identifiers\":{\"firstName\":{},\"emailAddress\":{}}}";

    private static TextFilter names() {
        return TestFilters.forPolicy(TestFilters.NAME_POLICY);
    }

    private static TextFilter emails() {
        return TestFilters.email();
    }

    private static OoxmlRedactionOptions defaults() {
        return OoxmlRedactionOptions.defaults();
    }

    // One body paragraph holding one run built from the given children.
    private static byte[] docx(final String... runChildren) {
        return WordDocs.createRaw("<w:p><w:r>" + String.join("", runChildren) + "</w:r></w:p>");
    }

    private static byte[] redact(final byte[] input, final TextFilter filter) throws Exception {
        return WordDocumentRedactor.redact(input, filter, defaults()).document();
    }

    // The first body paragraph as text, writing each break and carriage return as \n and each tab as \t.
    static String render(final byte[] docx) {
        final Element paragraph = firstParagraph(docx);
        final StringBuilder sb = new StringBuilder();
        for (final Element e : OoxmlPackage.allDescendants(paragraph)) {
            if (!W.equals(e.getNamespaceURI())) {
                continue;
            }
            switch (e.getLocalName()) {
                case "t" -> sb.append(e.getTextContent());
                case "br", "cr" -> sb.append('\n');
                case "tab" -> {
                    if (isInRun(e)) {
                        sb.append('\t');
                    }
                }
                default -> {
                }
            }
        }
        return sb.toString();
    }

    private static boolean isInRun(final Element e) {
        final Node parent = e.getParentNode();
        return parent instanceof Element p && W.equals(p.getNamespaceURI()) && "r".equals(p.getLocalName());
    }

    static Element firstParagraph(final byte[] docx) {
        final Element body = WordDocs.body(docx);
        for (Node n = body.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && W.equals(e.getNamespaceURI()) && "p".equals(e.getLocalName())) {
                return e;
            }
        }
        throw new IllegalStateException("no paragraph");
    }

    private static List<Element> elements(final byte[] docx, final String local) {
        final List<Element> out = new ArrayList<>();
        for (final Element e : OoxmlPackage.descendants(WordDocs.body(docx), W, local)) {
            if (!"tab".equals(local) || isInRun(e)) {
                out.add(e);
            }
        }
        return out;
    }

    @Test
    public void breaksAndTabsGiveTheFiltersAndReadParagraphsOneCharacterEach() throws Exception {
        final byte[] input = docx(t("A"), BR, t("B"), CR, t("C"), TAB, t("D"));
        final List<String> seen = new ArrayList<>();

        WordDocumentRedactor.redact(input, TestFilters.recording(names(), seen), defaults());

        assertEquals(List.of("A\nB\nC\tD"), seen);
        assertEquals(List.of("A\nB\nC\tD"), WordDocumentRedactor.readParagraphs(input));
    }

    @Test
    public void nameSplitBySoftBreakIsRedactedAndTheBreakKept() throws Exception {
        final byte[] input = docx(t("Patient George"), BR, t("Washington was seen today."));

        final byte[] output = redact(input, names());

        final String text = render(output);
        assertFalse(text.contains("George"));
        assertFalse(text.contains("Washington"));
        assertEquals("Patient {{{REDACTED-first-name}}}\n{{{REDACTED-first-name}}} was seen today.", text);
        assertEquals(1, elements(output, "br").size());
    }

    @Test
    public void nameSplitByCarriageReturnIsRedactedAndTheCarriageReturnKept() throws Exception {
        final byte[] input = docx(t("Patient George"), CR, t("Washington was seen."));

        final byte[] output = redact(input, names());

        final String text = render(output);
        assertFalse(text.contains("George"));
        assertFalse(text.contains("Washington"));
        assertEquals(1, elements(output, "cr").size());
    }

    @Test
    public void nameAfterSoftBreakIsRedactedInFull() throws Exception {
        final byte[] input = docx(t("Signed,"), BR, t("George Washington"));

        final byte[] output = redact(input, names());

        assertEquals("Signed,\n{{{REDACTED-first-name}}} {{{REDACTED-first-name}}}", render(output));
    }

    @Test
    public void nameAfterTabIsRedactedInFullAndTheTabKept() throws Exception {
        final byte[] input = docx(t("Name:"), TAB, t("George Washington"));

        final byte[] output = redact(input, names());

        assertEquals("Name:\t{{{REDACTED-first-name}}} {{{REDACTED-first-name}}}", render(output));
        assertEquals(1, elements(output, "tab").size());
    }

    @Test
    public void positionalTabIsATabAndKeepsItsSettings() throws Exception {
        final String ptab = "<w:ptab w:alignment=\"right\" w:relativeTo=\"margin\" w:leader=\"dot\"/>";
        final byte[] input = docx(t("Name:"), ptab, t("George Washington"));

        assertEquals(List.of("Name:\tGeorge Washington"), WordDocumentRedactor.readParagraphs(input));

        final byte[] output = redact(input, names());

        final List<Element> kept = elements(output, "ptab");
        assertEquals(1, kept.size());
        assertEquals("right", kept.get(0).getAttributeNS(W, "alignment"));
        assertEquals("dot", kept.get(0).getAttributeNS(W, "leader"));
        assertEquals(List.of("Name:\t{{{REDACTED-first-name}}} {{{REDACTED-first-name}}}"), WordDocumentRedactor.readParagraphs(output));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Write to contact|john@example.com today.|Write to contact\\n{{{REDACTED-email-address}}} today.",
            "Write to john@example.com|tomorrow please.|Write to {{{REDACTED-email-address}}}\\ntomorrow please."
    })
    public void emailNextToSoftBreakRemovesOnlyTheEmail(final String before, final String after, final String expected) throws Exception {
        final byte[] input = docx(t(before), BR, t(after));

        final byte[] output = redact(input, emails());

        assertEquals(expected.replace("\\n", "\n"), render(output));
        assertEquals(1, elements(output, "br").size());
    }

    @Test
    public void pageBreakKeepsItsType() throws Exception {
        final byte[] input = docx(t("Email john@example.com"), "<w:br w:type=\"page\"/>", t("Next page"));

        final byte[] output = redact(input, emails());

        final List<Element> kept = elements(output, "br");
        assertEquals(1, kept.size());
        assertEquals("page", kept.get(0).getAttributeNS(W, "type"));
        assertEquals("Email {{{REDACTED-email-address}}}\nNext page", render(output));
    }

    @Test
    public void spansFromRedactIndexTheTextWithBreaksAndReapplyThroughApplySpans() throws Exception {
        final byte[] input = docx(t("Signed,"), BR, t("George"), TAB, t("john@example.com"));
        final TextFilter filter = TestFilters.forPolicy(NAME_OR_EMAIL_POLICY);

        final OoxmlRedactionResult redacted = WordDocumentRedactor.redact(input, filter, defaults());
        final List<OfficeRedactionSpan> spans = redacted.spans();
        final byte[] reapplied = WordDocumentRedactor.applySpans(input, spans, defaults(), null);

        final String paragraph = WordDocumentRedactor.readParagraphs(input).get(0);
        assertEquals("Signed,\nGeorge\tjohn@example.com", paragraph);
        for (final OfficeRedactionSpan s : spans) {
            assertEquals(s.getText(), paragraph.substring(s.getCharacterStart(), s.getCharacterEnd()));
        }
        assertTrue(spans.stream().anyMatch(s -> s.getText().equals("George")));
        assertTrue(spans.stream().anyMatch(s -> s.getText().equals("john@example.com")));
        assertEquals(render(redacted.document()), render(reapplied));
        assertEquals("Signed,\n{{{REDACTED-first-name}}}\t{{{REDACTED-email-address}}}", render(reapplied));
    }

    // A span covering the break itself is kept after the replacement rather than dropped.
    @Test
    public void breakInsideARedactedSpanFollowsTheReplacement() throws Exception {
        final byte[] input = docx(t("Patient George"), BR, t("Washington was seen."));
        final OfficeRedactionSpan span = new OfficeRedactionSpan(0, 0, 8, 25, "George\nWashington", "{{{REDACTED-ph-eye}}}", null);

        final byte[] output = WordDocumentRedactor.applySpans(input, List.of(span), defaults(), null);

        assertEquals("Patient {{{REDACTED-ph-eye}}}\n was seen.", render(output));
    }

    @Test
    public void breakInATrackedDeletionIsNotPartOfTheText() throws Exception {
        final byte[] input = WordDocs.createRaw("<w:p><w:r>" + t("George") + "</w:r>"
                + "<w:del w:author=\"Reviewer\" w:id=\"1\"><w:r><w:br/><w:delText>x</w:delText></w:r></w:del>"
                + "<w:r>" + t(" Washington") + "</w:r></w:p>");

        assertEquals(List.of("George Washington"), WordDocumentRedactor.readParagraphs(input));
    }

    // A picture makes the paragraph complex; a run holding only a break must not be emitted twice.
    @Test
    public void breakOnlyRunInADrawingParagraphIsNotDuplicated() throws Exception {
        final byte[] input = WordDocs.createRaw("<w:p><w:r><w:pict/></w:r>"
                + "<w:r>" + t("Email john@example.com") + "</w:r>"
                + "<w:r><w:br/></w:r>"
                + "<w:r>" + t("Next line") + "</w:r></w:p>");

        final byte[] output = redact(input, emails());

        assertEquals("Email {{{REDACTED-email-address}}}\nNext line", render(output));
        assertEquals(1, elements(output, "br").size());
        assertEquals(1, elements(output, "pict").size());
    }

    @Test
    public void paragraphWithBreaksAndNoPiiIsLeftUntouched() throws Exception {
        final byte[] input = docx(t("Line one"), BR, t("Line two"), TAB, t("end"));

        final byte[] output = redact(input, emails());

        assertEquals(render(input), render(output));
        assertEquals(1, elements(output, "br").size());
        assertEquals(1, elements(output, "tab").size());
    }

}
