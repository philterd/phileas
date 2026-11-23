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
import org.w3c.dom.Element;

import java.util.List;

import static ai.philterd.phileas.services.ooxml.TestPackage.W;
import static ai.philterd.phileas.services.ooxml.WordDocs.t;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Non-breaking hyphens, soft hyphens and symbol characters in a Word paragraph are read the way the
 * filters need them and survive redaction outside a redacted value. Ported from phileas-dotnet's
 * WordRunCharacterRedactionTests.
 */
public class WordRunCharacterRedactionTest {

    private static final String NBH = "<w:noBreakHyphen/>";
    private static final String SHY = "<w:softHyphen/>";
    private static final String BR = "<w:br/>";
    private static final String PHONE = "<w:sym w:font=\"Wingdings\" w:char=\"F028\"/>";

    private static final String DATE_POLICY = "{\"identifiers\":{\"date\":{}}}";
    private static final String FIRST_NAME_POLICY = "{\"identifiers\":{\"firstName\":{}}}";

    private static OoxmlRedactionOptions defaults() {
        return OoxmlRedactionOptions.defaults();
    }

    private static byte[] docx(final String... runChildren) {
        return WordDocs.createRaw("<w:p><w:r>" + String.join("", runChildren) + "</w:r></w:p>");
    }

    private static byte[] redact(final byte[] input, final TextFilter filter) throws Exception {
        return WordDocumentRedactor.redact(input, filter, defaults()).document();
    }

    // The first body paragraph with each run-level character shown as a marker.
    private static String render(final byte[] docx) {
        final Element paragraph = WordBreakRedactionTest.firstParagraph(docx);
        final StringBuilder sb = new StringBuilder();
        for (final Element e : OoxmlPackage.allDescendants(paragraph)) {
            if (!W.equals(e.getNamespaceURI())) {
                continue;
            }
            switch (e.getLocalName()) {
                case "t" -> sb.append(e.getTextContent());
                case "noBreakHyphen" -> sb.append("[nbh]");
                case "softHyphen" -> sb.append("[shy]");
                case "sym" -> sb.append("[sym ").append(e.getAttributeNS(W, "font")).append(' ')
                        .append(e.getAttributeNS(W, "char")).append(']');
                case "br" -> sb.append("[br]");
                default -> {
                }
            }
        }
        return sb.toString();
    }

    @Test
    public void filterTextHasAHyphenForANonBreakingHyphenASpaceForASymbolAndNothingForASoftHyphen() throws Exception {
        final byte[] input = docx(t("SSN 123"), NBH, t("45"), NBH, t("6789, call"), PHONE, t("Wash"), SHY, t("ington"));

        assertEquals(List.of("SSN 123-45-6789, call Washington"), WordDocumentRedactor.readParagraphs(input));
    }

    @Test
    public void dateWithNonBreakingHyphensIsDetectedAndRedacted() throws Exception {
        final byte[] input = docx(t("Seen 2026"), NBH, t("03"), NBH, t("15 in clinic"));
        final TextFilter filter = TestFilters.forPolicy(DATE_POLICY);

        final List<OfficeRedactionSpan> detected = WordDocumentRedactor.detect(input, filter, defaults());
        final byte[] output = redact(input, filter);

        assertEquals(1, detected.size());
        assertEquals("2026-03-15", detected.get(0).getText());
        assertEquals("Seen {{{REDACTED-date}}} in clinic", render(output));
    }

    @Test
    public void customIdentifierWithNonBreakingHyphensIsDetectedAndRedacted() throws Exception {
        final byte[] input = docx(t("Chart MRN"), NBH, t("4821"), NBH, t("0937 reviewed"));

        final byte[] output = redact(input, TestFilters.customIdentifier("\\bMRN-\\d{4}-\\d{4}\\b", "mrn"));

        // Java Phileas labels a custom identifier "id" (.NET: "identifier").
        assertEquals("Chart {{{REDACTED-id}}} reviewed", render(output));
    }

    @Test
    public void nonBreakingHyphenOutsideTheValueIsKept() throws Exception {
        final byte[] input = docx(t("Ref A"), NBH, t("17 sent to a@example.com"));

        final byte[] output = redact(input, TestFilters.email());

        assertEquals("Ref A[nbh]17 sent to {{{REDACTED-email-address}}}", render(output));
    }

    @Test
    public void softHyphenOutsideTheValueIsKept() throws Exception {
        final byte[] input = docx(t("Contact Wash"), SHY, t("ington at a@example.com"));

        final byte[] output = redact(input, TestFilters.email());

        assertEquals("Contact Wash[shy]ington at {{{REDACTED-email-address}}}", render(output));
    }

    @Test
    public void symbolOutsideTheValueIsKeptWithItsFontAndCharacter() throws Exception {
        final byte[] input = docx(t("Call"), PHONE, t("a@example.com"));

        final byte[] output = redact(input, TestFilters.email());

        assertEquals("Call[sym Wingdings F028]{{{REDACTED-email-address}}}", render(output));
    }

    @Test
    public void wordWithASoftHyphenIsMatchedWholeAndTheSoftHyphenGoesWithIt() throws Exception {
        final byte[] input = docx(t("Dear Wash"), SHY, t("ington,"));

        final byte[] output = redact(input, TestFilters.forPolicy(FIRST_NAME_POLICY));

        assertEquals("Dear {{{REDACTED-first-name}}},", render(output));
    }

    // A soft hyphen joins the text around it, so the one before the value follows a space.
    @Test
    public void softHyphensAtTheStartAndRightBeforeARedactedValueAreKept() throws Exception {
        final byte[] input = docx(SHY, t("Mail "), SHY, t("a@example.com"));

        final byte[] output = redact(input, TestFilters.email());

        assertEquals("[shy]Mail [shy]{{{REDACTED-email-address}}}", render(output));
    }

    // The span covers "A-17 B" and the break after it.
    @Test
    public void eachCharacterInsideARedactedValueGoesWithItWhileABreakIsKept() throws Exception {
        final byte[] input = docx(t("Ref A"), NBH, t("1"), SHY, t("7"), PHONE, t("B"), BR, t("end"));
        final String paragraph = WordDocumentRedactor.readParagraphs(input).get(0);
        assertEquals("Ref A-17 B\nend", paragraph);
        final OfficeRedactionSpan span = new OfficeRedactionSpan(0, 0, 4, 11, "A-17 B\n", "X", null);

        final byte[] output = WordDocumentRedactor.applySpans(input, List.of(span), defaults(), null);

        assertEquals("Ref X[br]end", render(output));
    }

    @Test
    public void spansFromRedactReapplyThroughApplySpans() throws Exception {
        final byte[] input = docx(t("Ref A"), NBH, t("17,"), PHONE, t("seen 2026"), NBH,
                t("03"), NBH, t("15, Wash"), SHY, t("ington"));
        final TextFilter filter = TestFilters.forPolicy(DATE_POLICY);

        final OoxmlRedactionResult redacted = WordDocumentRedactor.redact(input, filter, defaults());
        final byte[] reapplied = WordDocumentRedactor.applySpans(input, redacted.spans(), defaults(), null);

        final String paragraph = WordDocumentRedactor.readParagraphs(input).get(0);
        assertEquals(1, redacted.spans().size());
        final OfficeRedactionSpan date = redacted.spans().get(0);
        assertEquals("2026-03-15", paragraph.substring(date.getCharacterStart(), date.getCharacterEnd()));
        assertEquals("Ref A[nbh]17,[sym Wingdings F028]seen {{{REDACTED-date}}}, Wash[shy]ington", render(reapplied));
        assertEquals(render(redacted.document()), render(reapplied));
    }

}
