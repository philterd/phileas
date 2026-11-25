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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

import static ai.philterd.phileas.services.ooxml.TestPackage.A;
import static ai.philterd.phileas.services.ooxml.WordDocs.drawingParagraphXml;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A DrawingML line break ({@code <a:br>}) in chart, SmartArt and text box text separates the words on
 * either side in the text the filters see, and stays where it was when the text is rewritten. Ported from
 * phileas-dotnet's DrawingTextBreakRedactionTests.
 */
public class DrawingTextBreakRedactionTest {

    private static final List<String[]> GRID = List.of(new String[]{"Name"}, new String[]{"Value"});

    private static TextFilter names() {
        return TestFilters.forPolicy(TestFilters.NAME_POLICY);
    }

    private static TextFilter emails() {
        return TestFilters.email();
    }

    // A text body holding the paragraph; returns the parsed a:p element.
    private static Element paragraph(final String paragraphXml) {
        final Document d = WordDocs.parse("<a:txBody xmlns:a=\"" + A + "\"><a:bodyPr/>" + paragraphXml + "</a:txBody>");
        return (Element) d.getElementsByTagNameNS(A, "p").item(0);
    }

    private static Element paragraph(final String... parts) {
        return paragraph(drawingParagraphXml(parts));
    }

    private static Element root(final Element paragraph) {
        return paragraph.getOwnerDocument().getDocumentElement();
    }

    private record Redacted(String rendered, List<String> seen) {
    }

    private static Redacted redactDirectly(final TextFilter filter, final Element paragraph) {
        final List<String> seen = new ArrayList<>();
        ChartRedactor.redactDrawingText(root(paragraph), TestFilters.recording(filter, seen), true, new ArrayList<>(),
                new OfficeSpanMath.Order());
        return new Redacted(WordDocs.renderDrawingParagraph(paragraph), seen);
    }

    // --- The method directly ---

    @Test
    public void textPassedToTheFilterHasALineFeedForEachBreak() {
        final Redacted r = redactDirectly(names(), paragraph("Patient George", "\n", "Washington was seen"));

        assertEquals(List.of("Patient George\nWashington was seen"), r.seen());
    }

    @Test
    public void nameSplitByBreakIsRedactedAndTheBreakStaysBetweenThem() {
        final Redacted r = redactDirectly(names(), paragraph("Patient George", "\n", "Washington was seen"));

        assertEquals("Patient {{{REDACTED-first-name}}}|\n|{{{REDACTED-first-name}}} was seen", r.rendered());
    }

    @Test
    public void nameAfterBreakIsRedactedInFull() {
        final Redacted r = redactDirectly(names(), paragraph("Signed,", "\n", "George Washington"));

        assertEquals("Signed,|\n|{{{REDACTED-first-name}}} {{{REDACTED-first-name}}}", r.rendered());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Write to john@example.com;tomorrow please;Write to {{{REDACTED-email-address}}}#tomorrow please",
            "Write to contact;john@example.com today;Write to contact#{{{REDACTED-email-address}}} today"
    })
    public void emailNextToBreakRemovesOnlyTheEmail(final String row) {
        final String[] values = row.split(";");
        final Redacted r = redactDirectly(emails(), paragraph(values[0], "\n", values[1]));

        // '#' stands for the "|\n|" a break renders as.
        assertEquals(values[2].replace("#", "|\n|"), r.rendered());
    }

    @Test
    public void severalBreaksEachStayBetweenTheSameText() {
        final Redacted r = redactDirectly(emails(), paragraph("Line one", "\n", "a@example.com", "\n", "\n", "Line four"));

        assertEquals("Line one|\n|{{{REDACTED-email-address}}}|\n|\n|Line four", r.rendered());
    }

    @Test
    public void runFormattingIsKept() {
        final Element paragraph = paragraph("<a:p><a:r><a:rPr b=\"1\"/><a:t>Signed,</a:t></a:r><a:br/>"
                + "<a:r><a:t>George Washington</a:t></a:r></a:p>");

        redactDirectly(names(), paragraph);

        final Element first = OoxmlPackage.children(paragraph, A, "r").get(0);
        final Element rPr = OoxmlPackage.firstChild(first, A, "rPr");
        assertEquals("1", rPr == null ? null : rPr.getAttribute("b"));
        assertEquals("Signed,", OoxmlPackage.firstChild(first, A, "t").getTextContent());
    }

    // A model detecting "George\nWashington" as one name returns one span across the break.
    @Test
    public void spanCoveringABreakIsWrittenWhereItStartsAndTheBreakKept() {
        final Element paragraph = paragraph("Patient George", "\n", "Washington was seen");
        final TextFilter filter = text -> TestFilters.result("Patient {{{REDACTED-ph-eye}}} was seen",
                List.of(TestFilters.span(text, 8, 25, "{{{REDACTED-ph-eye}}}")));
        final List<OfficeRedactionSpan> captured = new ArrayList<>();

        ChartRedactor.redactDrawingText(root(paragraph), filter, true, captured, new OfficeSpanMath.Order());

        assertEquals("Patient {{{REDACTED-ph-eye}}}|\n| was seen", WordDocs.renderDrawingParagraph(paragraph));
        assertEquals(1, captured.size());
        assertEquals("George\nWashington", captured.get(0).getText());
    }

    // Filtered text without spans that produce it is written whole rather than nothing being redacted.
    @Test
    public void filterWhoseSpansDoNotMatchItsTextStillHasItsTextWritten() {
        final Element paragraph = paragraph("Patient George", "\n", "Washington was seen");
        final TextFilter filter = text -> TestFilters.result("Patient [name]\n[name] was seen", List.of());

        ChartRedactor.redactDrawingText(root(paragraph), filter, true, new ArrayList<>(), new OfficeSpanMath.Order());

        assertEquals("Patient [name] [name] was seen|\n|", WordDocs.renderDrawingParagraph(paragraph));
    }

    @Test
    public void spanAcrossRunsIsStillRedacted() {
        final Redacted r = redactDirectly(emails(), paragraph("Mail george@", "example.com now"));

        assertEquals("Mail {{{REDACTED-email-address}}}| now", r.rendered());
    }

    @Test
    public void detectOnlyLeavesTheParagraphUnchanged() {
        final Element paragraph = paragraph("Write to john@example.com", "\n", "tomorrow");
        final List<OfficeRedactionSpan> captured = new ArrayList<>();

        ChartRedactor.redactDrawingText(root(paragraph), emails(), false, captured, new OfficeSpanMath.Order());

        assertEquals("Write to john@example.com|\n|tomorrow", WordDocs.renderDrawingParagraph(paragraph));
        assertEquals(1, captured.size());
        assertEquals("john@example.com", captured.get(0).getText());
    }

    // --- Through whole documents ---

    @Test
    public void docxChartTitle() throws Exception {
        final byte[] input = WordDocs.createWithChart(drawingParagraphXml("Signed,", "\n", "George Washington"), "Body.");

        final byte[] output = WordDocumentRedactor.redact(input, names(), OoxmlRedactionOptions.defaults()).document();

        assertTrue(WordDocs.drawingParagraphs(output).contains("Signed,|\n|{{{REDACTED-first-name}}} {{{REDACTED-first-name}}}"));
    }

    @Test
    public void docxSmartArtNode() throws Exception {
        final byte[] input = WordDocs.createWithSmartArt(drawingParagraphXml("Write to john@example.com", "\n", "tomorrow please"), "Body.");

        final byte[] output = WordDocumentRedactor.redact(input, emails(), OoxmlRedactionOptions.defaults()).document();

        assertTrue(WordDocs.drawingParagraphs(output).contains("Write to {{{REDACTED-email-address}}}|\n|tomorrow please"));
    }

    @Test
    public void docxReviewLinesShowTheBreak() throws Exception {
        final byte[] input = WordDocs.createWithSmartArt(drawingParagraphXml("Line one", "\n", "Line two"), "Body.");

        assertTrue(WordDocumentRedactor.readReviewLines(input).contains("Line one\nLine two"));
    }

    @Test
    public void xlsxChartTitle() throws Exception {
        final byte[] input = WordDocs.createXlsxWithChartXml(GRID, SpreadsheetDocs.chartSpaceXmlWithTitleParagraph(
                drawingParagraphXml("Patient George", "\n", "Washington"), "Series", "Category"));

        final byte[] output = XlsxRedactor.redact(input, names(), OoxmlRedactionOptions.defaults()).document();

        assertTrue(WordDocs.drawingParagraphs(output).contains("Patient {{{REDACTED-first-name}}}|\n|{{{REDACTED-first-name}}}"));
    }

    @Test
    public void xlsxTextBox() throws Exception {
        final byte[] input = WordDocs.createXlsxWithTextBoxParagraph(GRID,
                drawingParagraphXml("Write to contact", "\n", "john@example.com today"));

        final byte[] output = XlsxRedactor.redact(input, emails(), OoxmlRedactionOptions.defaults()).document();

        assertTrue(WordDocs.drawingParagraphs(output).contains("Write to contact|\n|{{{REDACTED-email-address}}} today"));
    }

}
