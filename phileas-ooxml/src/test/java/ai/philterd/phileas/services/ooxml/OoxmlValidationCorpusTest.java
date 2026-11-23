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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redacts a corpus of documents covering each structure the redactors handle and writes each input and
 * output to {@code target/ooxml-validation/}. CI then validates the outputs with the Open XML SDK
 * ({@code src/test/validator}), failing on any error an output has that its input does not.
 */
public class OoxmlValidationCorpusTest {

    private static final String EMAIL = "jane.doe@example.com";
    private static final String SSN = "123-45-6789";
    private static final String POLICY = "{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}}}";

    static Map<String, byte[]> corpus() throws Exception {
        final Map<String, byte[]> corpus = new LinkedHashMap<>();
        corpus.put("word-saved.docx", WordSavedDocumentTest.wordSaved());
        corpus.put("body.docx", OoxmlTestFixtures.docx("Patient SSN " + SSN + ".", "Email " + EMAIL + "."));
        corpus.put("header.docx", OoxmlTestFixtures.docxWithHeader("Header " + EMAIL, "Body SSN " + SSN + "."));
        corpus.put("hyperlink.docx", OoxmlTestFixtures.docxWithHyperlink("write to us", "mailto:" + EMAIL));
        corpus.put("embedded-workbook.docx", OoxmlTestFixtures.docxWithEmbeddedXlsx("Body.",
                SpreadsheetDocs.createXlsx(List.of(new String[]{"SSN"}, new String[]{SSN}))));
        corpus.put("header-footer.docx", WordDocs.createWithHeaderFooter("Header " + EMAIL, "Footer SSN " + SSN, "Body."));
        corpus.put("hyperlink-header.docx", WordDocs.createWithHyperlinkInHeader("email me", "mailto:" + EMAIL, "Body."));
        corpus.put("simple-field.docx", WordDocs.createWithSimpleField("HYPERLINK \"mailto:" + EMAIL + "\"", "Contact us", "Body."));
        corpus.put("complex-field.docx", WordDocs.createWithComplexField(
                new String[]{"HYPERLINK \"mailto:jane.doe@", "example.com\""}, "Contact", "Body."));
        corpus.put("tracked-deletion.docx", WordDocs.createWithTrackedDeletion("deleted SSN " + SSN, "Visible."));
        corpus.put("comment.docx", WordDocs.createWithComment("Note SSN " + SSN, "Body."));
        corpus.put("threaded-comments.docx", WordDocs.createWithThreadedThread(new String[]{"First " + EMAIL, "Reply " + SSN}, "Body."));
        corpus.put("notes.docx", WordDocs.createWithNotes("Footnote " + SSN, "Endnote " + EMAIL, "Body."));
        corpus.put("chart.docx", WordDocs.createWithChartData("Chart for " + EMAIL, "Series " + SSN, "Category", "Body."));
        corpus.put("chart-embedded-workbook.docx", WordDocs.createWithChartEmbeddedWorkbook(
                SpreadsheetDocs.createXlsx(List.of(new String[]{"Name", "SSN"}, new String[]{"Jane", SSN})), "Series " + EMAIL, "Category"));
        corpus.put("smartart.docx", WordDocs.createWithSmartArt(WordDocs.aParagraph("Node ", EMAIL), "Body."));
        corpus.put("text-boxes.docx", WordDocs.createRaw(
                WordDocs.textBoxParaXml("Box " + SSN, "Before " + EMAIL + " ", " after")
                        + WordDocs.vmlTextBoxParaXml("VML box " + EMAIL)));
        corpus.put("runs.docx", WordDocs.createRaw(
                "<w:p><w:r><w:rPr><w:b/></w:rPr><w:t xml:space=\"preserve\">SSN 123-</w:t></w:r>"
                        + "<w:r><w:rPr><w:i/></w:rPr><w:t>45-6789</w:t></w:r><w:r><w:t xml:space=\"preserve\"> end.</w:t></w:r></w:p>"
                        + "<w:p><w:r><w:t xml:space=\"preserve\">Line one</w:t><w:br/><w:t>" + EMAIL + "</w:t><w:tab/><w:t>tabbed</w:t></w:r></w:p>"
                        + "<w:tbl><w:tr><w:tc><w:p><w:r><w:t>SSN</w:t></w:r></w:p></w:tc><w:tc><w:p><w:r><w:t>" + SSN
                        + "</w:t></w:r></w:p></w:tc></w:tr></w:tbl><w:p/>"));
        corpus.put("authorship.docx", AuthorshipClearingTest.word());
        corpus.put("embedded-ole.docx", WordDocs.createWithEmbeddedOleObject(new byte[]{1, 2, 3, 4}, "Body."));

        final List<String[]> rows = List.of(
                new String[]{"Name", "SSN", "Email"},
                new String[]{"Jane", SSN, EMAIL},
                new String[]{"Shared", EMAIL, "No PII"});
        corpus.put("cells.xlsx", SpreadsheetDocs.createXlsx(rows));
        corpus.put("header-footer.xlsx", SpreadsheetDocs.createXlsxWithHeaderFooter(rows, "&LHeader " + EMAIL, "&CFooter " + SSN));
        corpus.put("comments.xlsx", SpreadsheetDocs.createXlsxWithLegacyAndThreadedComments(rows, "A1", "Legacy " + SSN,
                "B1", "Threaded " + EMAIL, "Reviewer"));
        corpus.put("chart.xlsx", SpreadsheetDocs.createXlsxWithChart(rows, "Chart " + EMAIL, "Series " + SSN, "Category"));
        corpus.put("text-box.xlsx", SpreadsheetDocs.createXlsxWithTextBox(rows, "Box ", SSN));
        corpus.put("pivot.xlsx", SpreadsheetDocs.createXlsxWithPivotCache(rows, new String[]{"Jane", SSN, EMAIL}, EMAIL));
        corpus.put("typed.xlsx", SpreadsheetDocs.createTyped(List.of(
                new SpreadsheetDocs.CellSpec[]{SpreadsheetDocs.CellSpec.of("SSN"), SpreadsheetDocs.CellSpec.of("Copy")},
                new SpreadsheetDocs.CellSpec[]{SpreadsheetDocs.CellSpec.of(SSN), SpreadsheetDocs.CellSpec.formula(SSN, "A2")})));
        corpus.put("authorship.xlsx", AuthorshipClearingTest.workbook("Sheet1"));
        corpus.put("embedded-document.xlsx", SpreadsheetDocs.createXlsxWithEmbeddedPackage(rows,
                OoxmlTestFixtures.docx("Embedded " + EMAIL), TestPackage.CT_DOCX));
        return corpus;
    }

    @Test
    public void writesRedactedCorpusForValidation() throws Exception {
        final Path root = Path.of("target", "ooxml-validation");
        Files.createDirectories(root.resolve("input"));
        Files.createDirectories(root.resolve("output"));
        final TextFilter filter = TestFilters.forPolicy(POLICY);

        for (final Map.Entry<String, byte[]> entry : corpus().entrySet()) {
            final String name = entry.getKey();
            final byte[] input = entry.getValue();
            final OoxmlRedactionResult result = name.endsWith(".docx")
                    ? WordDocumentRedactor.redact(input, filter, OoxmlRedactionOptions.defaults())
                    : XlsxRedactor.redact(input, filter, OoxmlRedactionOptions.defaults());
            Files.write(root.resolve("input").resolve(name), input);
            Files.write(root.resolve("output").resolve(name), result.document());

            final String xml = TestPackage.allXml(result.document());
            Assertions.assertFalse(xml.contains(EMAIL), name);
            Assertions.assertFalse(xml.contains(SSN), name);
        }
    }

}
