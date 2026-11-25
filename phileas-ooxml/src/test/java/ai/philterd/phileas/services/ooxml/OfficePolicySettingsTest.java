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

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.OoxmlFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Each {@code config.word} and {@code config.excel} setting, with both of its values, applied from the policy. */
public class OfficePolicySettingsTest {

    private static final String EMAIL = "jane.doe@example.com";
    private static final String OTHER_EMAIL = "john.roe@example.com";
    private static final String SSN = "123-45-6789";
    private static final String REDACTED = "{{{REDACTED-email-address}}}";

    private static final String W = TestPackage.W;
    private static final String DC = "http://purl.org/dc/elements/1.1/";
    private static final String CORE_NS = "http://schemas.openxmlformats.org/package/2006/metadata/core-properties";
    private static final String CUSTOM_NS = "http://schemas.openxmlformats.org/officeDocument/2006/custom-properties";

    private static final OoxmlFilterService SERVICE = new OoxmlFilterService(new PhileasConfiguration(new Properties()),
            new DefaultContextService(), new InMemoryVectorService(), null);

    private static Policy policy(final String config) {
        return new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}},\"config\":" + config + "}", Policy.class);
    }

    private static byte[] word(final String settings, final byte[] input) throws Exception {
        return SERVICE.filter(policy("{\"word\":" + settings + "}"), "ctx", input, MimeType.APPLICATION_DOCX).getDocument();
    }

    private static byte[] excel(final String settings, final byte[] input) throws Exception {
        return SERVICE.filter(policy("{\"excel\":" + settings + "}"), "ctx", input, MimeType.APPLICATION_XLSX).getDocument();
    }

    private static int count(final byte[] pkg, final String entry, final String local) {
        return TestPackage.elements(pkg, entry, W, local).size();
    }

    // --- fixtures -----------------------------------------------------------------------------

    // A comment anchored in the body with its range and reference, mirrored as a threaded comment.
    static byte[] commented() {
        final TestPackage pkg = WordDocs.docx("<w:p><w:commentRangeStart w:id=\"1\"/>" + WordDocs.run("Body text.")
                + "<w:commentRangeEnd w:id=\"1\"/><w:r><w:rPr><w:rStyle w:val=\"CommentReference\"/></w:rPr>"
                + "<w:commentReference w:id=\"1\"/></w:r></w:p>");
        pkg.rel(WordDocs.DOCUMENT, TestPackage.REL + "comments", "/word/comments.xml");
        pkg.part("/word/comments.xml", WordDocs.CT_COMMENTS, "<w:comments " + WordDocs.DOC_NAMESPACES + ">"
                + "<w:comment w:id=\"1\" w:author=\"Reviewer\" w:initials=\"R\">" + WordDocs.paraXml("Ask " + EMAIL)
                + "</w:comment></w:comments>");
        pkg.rel(WordDocs.DOCUMENT, "http://schemas.microsoft.com/office/2011/relationships/commentsExtended",
                "/word/commentsExtended.xml");
        pkg.part("/word/commentsExtended.xml", "application/vnd.openxmlformats-officedocument.wordprocessingml.commentsExtended+xml",
                "<w15:commentsEx xmlns:w15=\"http://schemas.microsoft.com/office/word/2012/wordml\">"
                        + "<w15:commentEx w15:paraId=\"00000001\" w15:done=\"0\"/></w15:commentsEx>");
        return pkg.toBytes();
    }

    // An insertion, a deletion, a deleted table row, and a formatting change.
    static byte[] tracked() {
        return WordDocs.createRaw("<w:p><w:ins w:id=\"1\" w:author=\"A\">" + WordDocs.run("Added " + EMAIL) + "</w:ins>"
                + "<w:del w:id=\"2\" w:author=\"A\"><w:r><w:delText>Gone " + OTHER_EMAIL + "</w:delText></w:r></w:del>"
                + "<w:r><w:rPr><w:b/><w:rPrChange w:id=\"3\" w:author=\"A\"><w:rPr/></w:rPrChange></w:rPr><w:t>Kept</w:t></w:r></w:p>"
                + "<w:tbl><w:tblPr/><w:tblGrid><w:gridCol/></w:tblGrid>"
                + "<w:tr><w:tc><w:p>" + WordDocs.run("Row one") + "</w:p></w:tc></w:tr>"
                + "<w:tr><w:trPr><w:del w:id=\"4\" w:author=\"A\"/></w:trPr><w:tc><w:p><w:del w:id=\"5\" w:author=\"A\">"
                + "<w:r><w:delText>Row two</w:delText></w:r></w:del></w:p></w:tc></w:tr></w:tbl><w:p/>");
    }

    // A deleted paragraph mark, a move, an inserted row, and a table whose only row is deleted.
    static byte[] trackedStructure() {
        return WordDocs.createRaw("<w:p><w:pPr><w:rPr><w:del w:id=\"1\" w:author=\"A\"/></w:rPr></w:pPr>" + WordDocs.run("First")
                + "</w:p><w:p><w:moveFromRangeStart w:id=\"2\" w:name=\"m\"/><w:moveFrom w:id=\"3\" w:author=\"A\">"
                + WordDocs.run("Moved " + EMAIL) + "</w:moveFrom><w:moveFromRangeEnd w:id=\"2\"/></w:p>"
                + "<w:p><w:moveToRangeStart w:id=\"4\" w:name=\"m\"/><w:moveTo w:id=\"5\" w:author=\"A\">"
                + WordDocs.run("Moved " + EMAIL) + "</w:moveTo><w:moveToRangeEnd w:id=\"4\"/></w:p>"
                + "<w:tbl><w:tblPr/><w:tblGrid><w:gridCol/></w:tblGrid><w:tr><w:trPr><w:ins w:id=\"6\" w:author=\"A\"/></w:trPr>"
                + "<w:tc><w:p>" + WordDocs.run("New row") + "</w:p></w:tc></w:tr></w:tbl>"
                + "<w:tbl><w:tblPr/><w:tblGrid><w:gridCol/></w:tblGrid><w:tr><w:trPr><w:del w:id=\"7\" w:author=\"A\"/></w:trPr>"
                + "<w:tc><w:p>" + WordDocs.run("Only row") + "</w:p></w:tc></w:tr></w:tbl><w:p/>");
    }

    // An index entry field whose markers are hidden but whose end is not, around visible text.
    static byte[] hiddenField() {
        return WordDocs.createRaw("<w:p>" + WordDocs.run("Before ")
                + "<w:r><w:rPr><w:vanish/></w:rPr><w:fldChar w:fldCharType=\"begin\"/></w:r>"
                + "<w:r><w:rPr><w:vanish/></w:rPr><w:instrText xml:space=\"preserve\"> XE \"Example\" </w:instrText></w:r>"
                + "<w:r><w:fldChar w:fldCharType=\"end\"/></w:r>" + WordDocs.run(" after") + "</w:p>");
    }

    @Test
    public void acceptingTrackedChangesHandlesMovesMarksAndRows() throws Exception {

        final byte[] output = word("{}", trackedStructure());

        for (final String local : List.of("ins", "del", "moveFrom", "moveTo", "moveFromRangeStart", "moveToRangeEnd")) {
            Assertions.assertEquals(0, count(output, "word/document.xml", local), local);
        }
        final String text = String.join("\n", WordDocumentRedactor.readParagraphs(output));
        Assertions.assertEquals(1, TestPackage.occurrences(text, "Moved "), text);
        Assertions.assertFalse(text.contains("Only row"), text);
        Assertions.assertTrue(text.contains("New row"), text);
        Assertions.assertEquals(1, count(output, "word/document.xml", "tbl"));

    }

    @Test
    public void removingHiddenTextKeepsAFieldWellFormed() throws Exception {
        final byte[] output = word("{}", hiddenField());
        Assertions.assertEquals(2, count(output, "word/document.xml", "fldChar"));
        Assertions.assertEquals(List.of("Before  after"), WordDocumentRedactor.readParagraphs(output));
    }

    // A hidden run, and a run whose hidden mark is turned off.
    static byte[] hidden() {
        return WordDocs.createRaw("<w:p>" + WordDocs.run("Visible. ")
                + "<w:r><w:rPr><w:vanish/></w:rPr><w:t xml:space=\"preserve\">Secret " + EMAIL + " </w:t></w:r>"
                + "<w:r><w:rPr><w:vanish w:val=\"0\"/></w:rPr><w:t>Shown</w:t></w:r></w:p>");
    }

    // Title, subject, keywords, category, description, and creator, and a custom property.
    static void addProperties(final TestPackage pkg) {
        pkg.rel("/", "http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties", "/docProps/core.xml");
        pkg.part("/docProps/core.xml", "application/vnd.openxmlformats-package.core-properties+xml",
                "<cp:coreProperties xmlns:cp=\"" + CORE_NS + "\" xmlns:dc=\"" + DC + "\">"
                        + "<dc:title>Quarterly notes</dc:title><dc:subject>Planning</dc:subject><cp:keywords>budget</cp:keywords>"
                        + "<cp:category>Finance</cp:category><dc:description>Draft for review</dc:description>"
                        + "<dc:creator>Example Author</dc:creator></cp:coreProperties>");
        pkg.rel("/", TestPackage.REL + "custom-properties", "/docProps/custom.xml");
        pkg.part("/docProps/custom.xml", "application/vnd.openxmlformats-officedocument.custom-properties+xml",
                "<Properties xmlns=\"" + CUSTOM_NS + "\" xmlns:vt=\"http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes\">"
                        + "<property fmtid=\"{D5CDD505-2E9C-101B-9397-08002B2CF9AE}\" pid=\"2\" name=\"Client\">"
                        + "<vt:lpwstr>Example Client</vt:lpwstr></property></Properties>");
    }

    static byte[] wordWithProperties() {
        final TestPackage pkg = WordDocs.docx(WordDocs.paras("Body."));
        addProperties(pkg);
        return pkg.toBytes();
    }

    static byte[] workbookWithProperties() {
        final SpreadsheetDocs.Workbook wb = new SpreadsheetDocs.Workbook(List.<String[]>of(new String[]{"Name"}), "Sheet1");
        addProperties(wb.pkg);
        return wb.toBytes();
    }

    private static void assertPropertiesCleared(final byte[] output, final boolean cleared) {
        final Map<String, String> core = Map.of("title", "Quarterly notes", "subject", "Planning",
                "description", "Draft for review");
        for (final Map.Entry<String, String> e : core.entrySet()) {
            Assertions.assertEquals(List.of(cleared ? "" : e.getValue()), TestPackage.texts(output, "docProps/core.xml", DC, e.getKey()));
        }
        Assertions.assertEquals(List.of(cleared ? "" : "budget"), TestPackage.texts(output, "docProps/core.xml", CORE_NS, "keywords"));
        Assertions.assertEquals(List.of(cleared ? "" : "Finance"), TestPackage.texts(output, "docProps/core.xml", CORE_NS, "category"));
        Assertions.assertEquals(cleared ? 0 : 1, TestPackage.elements(output, "docProps/custom.xml", CUSTOM_NS, "property").size());
        // Authorship is cleared either way.
        Assertions.assertEquals(List.of(""), TestPackage.texts(output, "docProps/core.xml", DC, "creator"));
    }

    // --- Word ---------------------------------------------------------------------------------

    @Test
    public void wordHeadersFooters() throws Exception {
        final byte[] input = WordDocs.createWithHeaderFooter("Header " + EMAIL, "Footer", "Body.");
        Assertions.assertFalse(WordDocs.headersText(word("{\"headersFooters\":\"redact\"}", input)).contains(EMAIL));
        Assertions.assertTrue(WordDocs.headersText(word("{\"headersFooters\":\"keep\"}", input)).contains(EMAIL));
    }

    @Test
    public void wordCommentsRemoveLeavesNoCommentTextAuthorOrCopy() throws Exception {

        final byte[] output = word("{\"comments\":\"remove\"}", commented());

        final Map<String, byte[]> entries = TestPackage.entries(output);
        Assertions.assertTrue(entries.keySet().stream().noneMatch(n -> n.toLowerCase().contains("comments")), entries.keySet().toString());
        for (final String local : List.of("commentRangeStart", "commentRangeEnd", "commentReference")) {
            Assertions.assertEquals(0, count(output, "word/document.xml", local), local);
        }
        final String xml = TestPackage.allXml(output);
        Assertions.assertFalse(xml.contains("Ask "), "comment text");
        Assertions.assertFalse(xml.contains("Reviewer"), "comment author");
        Assertions.assertFalse(xml.toLowerCase().contains("comments"), "comment relationship or content type");
        Assertions.assertEquals(List.of("Body text."), WordDocs.bodyParagraphs(output));

    }

    @Test
    public void wordCommentsRedactKeepsAndRedactsThem() throws Exception {
        final byte[] output = word("{\"comments\":\"redact\"}", commented());
        Assertions.assertEquals(List.of("Ask " + REDACTED), WordDocs.commentTexts(output));
        Assertions.assertEquals(1, count(output, "word/document.xml", "commentReference"));
    }

    @Test
    public void wordTrackedChangesAcceptLeavesNoRevisionsAndRedactsTheAcceptedText() throws Exception {

        final byte[] output = word("{\"trackedChanges\":\"accept\"}", tracked());

        for (final String local : List.of("ins", "del", "delText", "rPrChange")) {
            Assertions.assertEquals(0, count(output, "word/document.xml", local), local);
        }
        final String body = WordDocs.allBodyText(output);
        Assertions.assertTrue(body.contains("Added " + REDACTED), body);
        Assertions.assertFalse(TestPackage.allXml(output).contains("Gone"));
        Assertions.assertFalse(TestPackage.allXml(output).contains("Row two"));
        Assertions.assertEquals(1, count(output, "word/document.xml", "tr"));

    }

    @Test
    public void wordTrackedChangesRedactKeepsAndRedactsThem() throws Exception {

        final byte[] output = word("{\"trackedChanges\":\"redact\"}", tracked());

        Assertions.assertEquals(1, count(output, "word/document.xml", "ins"));
        Assertions.assertTrue(WordDocs.deletedTexts(output).contains("Gone " + REDACTED), WordDocs.deletedTexts(output).toString());
        Assertions.assertEquals(2, count(output, "word/document.xml", "tr"));

    }

    @Test
    public void wordHiddenTextRemoveLeavesNoHiddenRun() throws Exception {

        final byte[] output = word("{\"hiddenText\":\"remove\"}", hidden());

        for (final Element vanish : TestPackage.elements(output, "word/document.xml", W, "vanish")) {
            Assertions.assertEquals("0", vanish.getAttributeNS(W, "val"), "only a hidden mark that is turned off remains");
        }
        Assertions.assertFalse(TestPackage.allXml(output).contains("Secret"));
        Assertions.assertEquals(List.of("Visible. Shown"), WordDocs.bodyParagraphs(output));

    }

    @Test
    public void wordHiddenTextRedactKeepsAndRedactsIt() throws Exception {
        final byte[] output = word("{\"hiddenText\":\"redact\"}", hidden());
        Assertions.assertEquals(2, count(output, "word/document.xml", "vanish"));
        Assertions.assertEquals(List.of("Visible. Secret " + REDACTED + " Shown"), WordDocs.bodyParagraphs(output));
        // The replacement stays in the hidden run.
        final Element hiddenRun = TestPackage.elements(output, "word/document.xml", W, "vanish").get(0);
        Assertions.assertTrue(hiddenRun.getParentNode().getParentNode().getTextContent().contains(REDACTED));
    }

    @Test
    public void wordCharts() throws Exception {
        final byte[] input = WordDocs.createWithChartData("Chart for " + EMAIL, "Series", "Category", "Body.");
        Assertions.assertFalse(TestPackage.allXml(word("{\"charts\":\"redact\"}", input)).contains(EMAIL));
        Assertions.assertTrue(TestPackage.allXml(word("{\"charts\":\"keep\"}", input)).contains(EMAIL));
    }

    @Test
    public void wordUninspectableObjects() throws Exception {
        final byte[] input = WordDocs.createWithEmbeddedOleObject(new byte[]{1, 2, 3, 4}, "Body.");
        Assertions.assertTrue(TestPackage.entries(word("{\"uninspectableObjects\":\"remove\"}", input)).keySet().stream()
                .noneMatch(n -> n.startsWith("word/embeddings/")));
        Assertions.assertTrue(TestPackage.entries(word("{\"uninspectableObjects\":\"keep\"}", input)).keySet().stream()
                .anyMatch(n -> n.startsWith("word/embeddings/")));
    }

    @Test
    public void wordRemoveDocumentProperties() throws Exception {
        assertPropertiesCleared(word("{\"removeDocumentProperties\":true}", wordWithProperties()), true);
        assertPropertiesCleared(word("{\"removeDocumentProperties\":false}", wordWithProperties()), false);
    }

    @Test
    public void wordHighlight() throws Exception {
        final byte[] input = WordDocs.create("Email " + EMAIL + ".");
        Assertions.assertEquals(1, count(word("{\"highlight\":true}", input), "word/document.xml", "highlight"));
        Assertions.assertEquals(0, count(word("{\"highlight\":false}", input), "word/document.xml", "highlight"));
    }

    @Test
    public void theDefaultsRemoveCommentsAcceptChangesRemoveHiddenTextAndClearProperties() throws Exception {
        Assertions.assertTrue(TestPackage.entries(word("{}", commented())).keySet().stream().noneMatch(n -> n.contains("comments")));
        Assertions.assertEquals(0, count(word("{}", tracked()), "word/document.xml", "del"));
        Assertions.assertFalse(TestPackage.allXml(word("{}", hidden())).contains("Secret"));
        assertPropertiesCleared(word("{}", wordWithProperties()), true);
        assertPropertiesCleared(excel("{}", workbookWithProperties()), true);
    }

    @Test
    public void oneServiceAppliesEachCallsPolicy() throws Exception {
        final byte[] input = commented();
        Assertions.assertTrue(WordDocs.commentTexts(word("{\"comments\":\"remove\"}", input)).isEmpty());
        Assertions.assertEquals(List.of("Ask " + REDACTED), WordDocs.commentTexts(word("{\"comments\":\"redact\"}", input)));
        Assertions.assertTrue(WordDocs.commentTexts(word("{\"comments\":\"remove\"}", input)).isEmpty());
    }

    @Test
    public void detectAndApplyNumberTheParagraphsAsRedactionDoes() throws Exception {

        // A comment, a hidden run, and a deletion come before the body text, so removing them must not shift
        // the paragraph and offsets that applying the spans uses.
        final byte[] input = WordDocs.createRaw("<w:p><w:commentRangeStart w:id=\"1\"/><w:commentRangeEnd w:id=\"1\"/>"
                + "<w:r><w:commentReference w:id=\"1\"/></w:r>"
                + "<w:r><w:rPr><w:vanish/></w:rPr><w:t xml:space=\"preserve\">Hidden </w:t></w:r>"
                + "<w:del w:id=\"2\" w:author=\"A\"><w:r><w:delText>Old </w:delText></w:r></w:del>"
                + WordDocs.run("Email " + EMAIL) + "</w:p>");
        final Policy policy = policy("{}");

        final var result = SERVICE.filter(policy, "ctx", input, MimeType.APPLICATION_DOCX);
        final byte[] applied = SERVICE.apply(policy, input, result.getExplanation().appliedSpans(), MimeType.APPLICATION_DOCX);

        Assertions.assertEquals(WordDocs.bodyParagraphs(result.getDocument()), WordDocs.bodyParagraphs(applied));
        Assertions.assertEquals(List.of("Email " + REDACTED), WordDocs.bodyParagraphs(applied));

    }

    // --- Excel --------------------------------------------------------------------------------

    private static final List<String[]> ROWS = List.of(new String[]{"Name", "Email"}, new String[]{"Jane", EMAIL});

    @Test
    public void excelPrintHeadersFooters() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithHeaderFooter(ROWS, "&LHeader " + OTHER_EMAIL, "&CFooter");
        Assertions.assertFalse(SpreadsheetDocs.headerFooterText(excel("{\"printHeadersFooters\":\"redact\"}", input)).contains(OTHER_EMAIL));
        Assertions.assertTrue(SpreadsheetDocs.headerFooterText(excel("{\"printHeadersFooters\":\"keep\"}", input)).contains(OTHER_EMAIL));
    }

    @Test
    public void excelCharts() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithChart(ROWS, "Chart for " + OTHER_EMAIL, "Series", "Category");
        Assertions.assertFalse(SpreadsheetDocs.allChartXml(excel("{\"charts\":\"redact\"}", input)).contains(OTHER_EMAIL));
        Assertions.assertTrue(SpreadsheetDocs.allChartXml(excel("{\"charts\":\"keep\"}", input)).contains(OTHER_EMAIL));
    }

    @Test
    public void excelFormulaCaches() throws Exception {
        final byte[] input = SpreadsheetDocs.createTyped(List.of(
                new SpreadsheetDocs.CellSpec[]{SpreadsheetDocs.CellSpec.of("SSN"), SpreadsheetDocs.CellSpec.of("Copy")},
                new SpreadsheetDocs.CellSpec[]{SpreadsheetDocs.CellSpec.of(SSN), SpreadsheetDocs.CellSpec.formula(SSN, "A2")}));
        Assertions.assertFalse(TestPackage.allXml(excel("{\"formulaCaches\":\"redact\"}", input)).contains(SSN));
        final byte[] kept = excel("{\"formulaCaches\":\"keep\"}", input);
        Assertions.assertTrue(SpreadsheetDocs.isFormulaCell(kept, "B2"));
        Assertions.assertEquals(SSN, SpreadsheetDocs.cachedValue(kept, "B2"));
    }

    @Test
    public void excelPivotCaches() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithPivotCache(ROWS, new String[]{"Jane", OTHER_EMAIL}, OTHER_EMAIL);
        Assertions.assertFalse(SpreadsheetDocs.allPivotXml(excel("{\"pivotCaches\":\"redact\"}", input)).contains(OTHER_EMAIL));
        Assertions.assertTrue(SpreadsheetDocs.allPivotXml(excel("{\"pivotCaches\":\"keep\"}", input)).contains(OTHER_EMAIL));
    }

    @Test
    public void excelUninspectableObjects() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithEmbeddedOleObject(ROWS, new byte[]{1, 2, 3, 4});
        Assertions.assertFalse(SpreadsheetDocs.xlsxHasAnyEmbeddedObject(excel("{\"uninspectableObjects\":\"remove\"}", input)));
        Assertions.assertTrue(SpreadsheetDocs.xlsxHasAnyEmbeddedObject(excel("{\"uninspectableObjects\":\"keep\"}", input)));
    }

    @Test
    public void excelRemoveDocumentProperties() throws Exception {
        assertPropertiesCleared(excel("{\"removeDocumentProperties\":true}", workbookWithProperties()), true);
        assertPropertiesCleared(excel("{\"removeDocumentProperties\":false}", workbookWithProperties()), false);
    }

    // --- embedded documents -------------------------------------------------------------------------

    @Test
    public void aWordDocumentEmbeddedInAWorkbookFollowsTheWordSettings() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithEmbeddedPackage(ROWS, commented(), TestPackage.CT_DOCX);
        final String policy = "{\"word\":{\"comments\":\"redact\"}}";
        final byte[] output = SERVICE.filter(policy(policy), "ctx", input, MimeType.APPLICATION_XLSX).getDocument();
        Assertions.assertEquals(List.of("Ask " + REDACTED),
                WordDocs.commentTexts(TestPackage.entries(output).get("xl/embeddings/package1.docx")));
    }

}
