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
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.List;
import java.util.Map;

/**
 * Author names and document properties are cleared whatever the policy, including one with no filters.
 */
public class AuthorshipClearingTest {

    private static final String AUTHOR = "Jordan Example";
    private static final String EDITOR = "Casey Sample";
    private static final String USER_ID = "jordan.example@example.com";
    private static final String COMPANY = "Example Holdings";
    private static final String MANAGER = "Morgan Placeholder";
    private static final String SHARER = "Riley Tester";
    private static final String PROFILE = "jexample";
    private static final String TEMPLATE = "file:///C:\\Users\\" + PROFILE + "\\AppData\\Roaming\\Microsoft\\Templates\\Normal.dotm";

    private static final TextFilter NO_FILTERS = TestFilters.forPolicy("{}");

    private static final String REL_CORE = "http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties";
    private static final String REL_APP = TestPackage.REL + "extended-properties";
    private static final String CT_CORE = "application/vnd.openxmlformats-package.core-properties+xml";
    private static final String CT_APP = "application/vnd.openxmlformats-officedocument.extended-properties+xml";
    private static final String CORE_NS = "http://schemas.openxmlformats.org/package/2006/metadata/core-properties";
    private static final String DC = "http://purl.org/dc/elements/1.1/";
    private static final String APP_NS = "http://schemas.openxmlformats.org/officeDocument/2006/extended-properties";
    private static final String CT_SETTINGS = "application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml";
    private static final String CT_REVISION_HEADERS = "application/vnd.openxmlformats-officedocument.spreadsheetml.revisionHeaders+xml";
    private static final String CT_REVISION_LOG = "application/vnd.openxmlformats-officedocument.spreadsheetml.revisionLog+xml";
    private static final String CT_USER_NAMES = "application/vnd.openxmlformats-officedocument.spreadsheetml.userNames+xml";
    private static final String GUID = "{33333333-3333-3333-3333-333333333333}";

    private static void addDocumentProperties(final TestPackage pkg) {
        pkg.rel("/", REL_CORE, "/docProps/core.xml");
        pkg.part("/docProps/core.xml", CT_CORE, "<cp:coreProperties xmlns:cp=\"" + CORE_NS + "\" xmlns:dc=\"" + DC + "\">"
                + "<dc:title>Quarterly notes</dc:title><dc:creator>" + AUTHOR + "</dc:creator>"
                + "<cp:lastModifiedBy>" + EDITOR + "</cp:lastModifiedBy><cp:revision>3</cp:revision></cp:coreProperties>");
        pkg.rel("/", REL_APP, "/docProps/app.xml");
        pkg.part("/docProps/app.xml", CT_APP, "<Properties xmlns=\"" + APP_NS + "\"><Application>Microsoft Office Word</Application>"
                + "<Manager>" + MANAGER + "</Manager><Company>" + COMPANY + "</Company></Properties>");
    }

    // Comments with initials, people.xml, tracked changes of several kinds in the body and a header, and properties.
    static byte[] word() {
        final String body = "<w:p><w:ins w:id=\"1\" w:author=\"" + AUTHOR + "\" w:date=\"2026-01-02T00:00:00Z\">"
                + WordDocs.run("inserted") + "</w:ins>"
                + "<w:del w:id=\"2\" w:author=\"" + EDITOR + "\"><w:r><w:delText>removed</w:delText></w:r></w:del>"
                + "<w:r><w:rPr><w:b/><w:rPrChange w:id=\"3\" w:author=\"" + EDITOR + "\"><w:rPr/></w:rPrChange></w:rPr>"
                + WordDocs.t("bold") + "</w:r></w:p>"
                + "<w:p><w:pPr><w:pPrChange w:id=\"4\" w:author=\"" + AUTHOR + "\"><w:pPr/></w:pPrChange></w:pPr>"
                + WordDocs.run("changed") + "</w:p>"
                + "<w:p><w:moveFromRangeStart w:id=\"6\" w:name=\"move1\" w:date=\"2026-01-02T00:00:00Z\" w:author=\"" + EDITOR + "\"/>"
                + "<w:moveFrom w:id=\"7\" w:author=\"" + EDITOR + "\">" + WordDocs.run("moved") + "</w:moveFrom>"
                + "<w:moveFromRangeEnd w:id=\"6\"/></w:p>"
                + "<w:p><w:moveToRangeStart w:id=\"8\" w:name=\"move1\" w:date=\"2026-01-02T00:00:00Z\" w:author=\"" + EDITOR + "\"/>"
                + "<w:moveTo w:id=\"9\" w:author=\"" + EDITOR + "\">" + WordDocs.run("moved") + "</w:moveTo>"
                + "<w:moveToRangeEnd w:id=\"8\"/></w:p>";
        final TestPackage pkg = WordDocs.docx(body);
        final String headerId = pkg.rel(WordDocs.DOCUMENT, TestPackage.REL + "header", "/word/header1.xml");
        pkg.part("/word/header1.xml", WordDocs.CT_HEADER, "<w:hdr " + WordDocs.DOC_NAMESPACES + "><w:p><w:ins w:id=\"5\" w:author=\""
                + AUTHOR + "\">" + WordDocs.run("header") + "</w:ins></w:p></w:hdr>");
        pkg.part(WordDocs.DOCUMENT, TestPackage.CT_DOCX_MAIN, WordDocs.documentXml(body
                + "<w:sectPr><w:headerReference w:type=\"default\" r:id=\"" + headerId + "\"/></w:sectPr>"));
        pkg.rel(WordDocs.DOCUMENT, TestPackage.REL + "comments", "/word/comments.xml");
        pkg.part("/word/comments.xml", WordDocs.CT_COMMENTS, "<w:comments " + WordDocs.DOC_NAMESPACES + ">"
                + "<w:comment w:id=\"1\" w:author=\"" + AUTHOR + "\" w:initials=\"JE\">" + WordDocs.paraXml("Looks fine.")
                + "</w:comment></w:comments>");
        pkg.rel(WordDocs.DOCUMENT, WordDocs.REL_PEOPLE, "/word/people.xml");
        pkg.part("/word/people.xml", WordDocs.CT_PEOPLE, "<w15:people xmlns:w15=\"" + WordDocs.W15 + "\">"
                + "<w15:person w15:author=\"" + AUTHOR + "\"><w15:presenceInfo w15:providerId=\"AD\" w15:userId=\"" + USER_ID
                + "\"/></w15:person></w15:people>");
        pkg.rel(WordDocs.DOCUMENT, TestPackage.REL + "settings", "/word/settings.xml");
        final String templateId = pkg.externalRel("/word/settings.xml", AuthorshipCleaner.REL_ATTACHED_TEMPLATE, TEMPLATE);
        pkg.part("/word/settings.xml", CT_SETTINGS, "<w:settings " + WordDocs.DOC_NAMESPACES + "><w:zoom w:percent=\"100\"/>"
                + "<w:attachedTemplate r:id=\"" + templateId + "\"/><w:defaultTabStop w:val=\"720\"/></w:settings>");
        addDocumentProperties(pkg);
        return pkg.toBytes();
    }

    // A note as Excel writes it, a threaded comment, a write-reservation user, shared-workbook revision
    // users, and properties.
    static byte[] workbook(final String sheetName) {
        final SpreadsheetDocs.Workbook wb = new SpreadsheetDocs.Workbook(
                List.of(new String[]{"Name"}, new String[]{"Alice"}), sheetName);
        SpreadsheetDocs.addNote(wb, "A1", AUTHOR, "Checked.");
        SpreadsheetDocs.addThreadedComment(wb, "A2", "Agreed.", EDITOR);
        wb.beforeSheets.append("<fileSharing readOnlyRecommended=\"1\" userName=\"" + SHARER + "\"/>");
        wb.pkg.rel(wb.workbookPart, TestPackage.REL + "revisionHeaders", "/xl/revisions/revisionHeaders.xml");
        final String logId = wb.pkg.rel("/xl/revisions/revisionHeaders.xml", TestPackage.REL + "revisionLog",
                "/xl/revisions/revisionLog1.xml");
        wb.pkg.part("/xl/revisions/revisionHeaders.xml", CT_REVISION_HEADERS, "<headers xmlns=\"" + TestPackage.S
                + "\" xmlns:r=\"" + TestPackage.R + "\" guid=\"" + GUID + "\" lastGuid=\"" + GUID + "\" shared=\"1\""
                + " diskRevisions=\"1\" revisionId=\"1\" version=\"2\"><header guid=\"" + GUID + "\""
                + " dateTime=\"2026-01-02T10:00:00\" maxSheetId=\"2\" userName=\"" + SHARER + "\" r:id=\"" + logId + "\">"
                + "<sheetIdMap count=\"1\"><sheetId val=\"1\"/></sheetIdMap></header></headers>");
        wb.pkg.part("/xl/revisions/revisionLog1.xml", CT_REVISION_LOG, "<revisions xmlns=\"" + TestPackage.S
                + "\" xmlns:r=\"" + TestPackage.R + "\"><rcmt sheetId=\"1\" cell=\"A1\" guid=\"" + GUID + "\""
                + " action=\"add\" author=\"" + SHARER + "\"/></revisions>");
        wb.pkg.rel(wb.workbookPart, TestPackage.REL + "usernames", "/xl/revisions/userNames.xml");
        wb.pkg.part("/xl/revisions/userNames.xml", CT_USER_NAMES, "<users xmlns=\"" + TestPackage.S + "\" count=\"1\">"
                + "<userInfo guid=\"" + GUID + "\" name=\"" + SHARER + "\" id=\"1\" dateTime=\"2026-01-02T10:00:00\"/></users>");
        addDocumentProperties(wb.pkg);
        return wb.toBytes();
    }

    private static void assertNoAuthorship(final byte[] output) {
        final String xml = TestPackage.allXml(output);
        for (final String value : new String[]{AUTHOR, EDITOR, USER_ID, COMPANY, MANAGER, "JE", SHARER, PROFILE}) {
            Assertions.assertFalse(xml.contains(value), value);
        }
    }

    private static void assertPropertiesCleared(final byte[] output) {
        Assertions.assertEquals(List.of(""), TestPackage.texts(output, "docProps/core.xml", DC, "creator"));
        Assertions.assertEquals(List.of(""), TestPackage.texts(output, "docProps/core.xml", CORE_NS, "lastModifiedBy"));
        Assertions.assertEquals(List.of("Quarterly notes"), TestPackage.texts(output, "docProps/core.xml", DC, "title"));
        Assertions.assertEquals(List.of(""), TestPackage.texts(output, "docProps/app.xml", APP_NS, "Company"));
        Assertions.assertEquals(List.of(""), TestPackage.texts(output, "docProps/app.xml", APP_NS, "Manager"));
        Assertions.assertEquals(List.of("Microsoft Office Word"), TestPackage.texts(output, "docProps/app.xml", APP_NS, "Application"));
    }

    private static void assertWordCleared(final byte[] output) {
        assertNoAuthorship(output);
        assertPropertiesCleared(output);

        int authors = 0;
        for (final String part : List.of("word/document.xml", "word/header1.xml", "word/comments.xml")) {
            final NodeList all = TestPackage.dom(output, part).getElementsByTagNameNS(TestPackage.W, "*");
            for (int i = 0; i < all.getLength(); i++) {
                final Element e = (Element) all.item(i);
                if (e.hasAttributeNS(TestPackage.W, "author")) {
                    Assertions.assertEquals(AuthorshipCleaner.NEUTRAL_AUTHOR, e.getAttributeNS(TestPackage.W, "author"));
                    authors++;
                }
                Assertions.assertFalse(e.hasAttributeNS(TestPackage.W, "initials"));
            }
        }
        Assertions.assertEquals(10, authors);

        Assertions.assertTrue(TestPackage.elements(output, "word/settings.xml", TestPackage.W, "attachedTemplate").isEmpty());
        Assertions.assertEquals(2, TestPackage.dom(output, "word/settings.xml").getDocumentElement().getChildNodes().getLength());
        Assertions.assertFalse(TestPackage.allXml(output).contains("attachedTemplate"));

        final Map<String, byte[]> entries = TestPackage.entries(output);
        Assertions.assertFalse(entries.containsKey("word/people.xml"));
        Assertions.assertFalse(TestPackage.xml(output, "word/_rels/document.xml.rels").contains("people"));
        Assertions.assertFalse(TestPackage.xml(output, "[Content_Types].xml").contains("people"));
    }

    @Test
    public void wordAuthorsPeopleAndPropertiesAreClearedWithAPolicyWithNoFilters() throws Exception {
        assertWordCleared(WordDocumentRedactor.redact(word(), NO_FILTERS, OoxmlRedactionOptions.defaults()).document());
    }

    @Test
    public void wordApplySpansClearsAuthorship() throws Exception {
        assertWordCleared(WordDocumentRedactor.applySpans(word(), List.of(), OoxmlRedactionOptions.defaults(), null));
    }

    @Test
    public void excelCommentAuthorsPersonsAndPropertiesAreClearedWithAPolicyWithNoFilters() throws Exception {
        final byte[] output = XlsxRedactor.redact(workbook("Sheet1"), NO_FILTERS, OoxmlRedactionOptions.defaults()).document();
        assertExcelCleared(output);
    }

    @Test
    public void excelApplySpansClearsAuthorship() throws Exception {
        assertExcelCleared(XlsxRedactor.applySpans(workbook("Sheet1"), List.of(), OoxmlRedactionOptions.defaults(), null));
    }

    @Test
    public void excelAuthorsAreClearedOnSheetsTheWorksheetOptionLeavesOut() throws Exception {
        final OoxmlRedactionOptions options = OoxmlRedactionOptions.defaults().setWorksheet("Other");
        assertExcelCleared(XlsxRedactor.redact(workbook("Sheet1"), NO_FILTERS, options).document());
    }

    private static void assertExcelCleared(final byte[] output) {
        assertNoAuthorship(output);
        assertPropertiesCleared(output);
        Assertions.assertEquals(List.of(AuthorshipCleaner.NEUTRAL_AUTHOR),
                TestPackage.texts(output, "xl/comments1.xml", TestPackage.S, "author"));
        final List<Element> persons = TestPackage.elements(output, "xl/persons/person.xml", SpreadsheetDocs.THREADED, "person");
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(AuthorshipCleaner.NEUTRAL_AUTHOR, persons.get(0).getAttribute("displayName"));
        Assertions.assertFalse(persons.get(0).hasAttribute("userId"));
        Assertions.assertFalse(persons.get(0).hasAttribute("providerId"));
        Assertions.assertFalse(persons.get(0).getAttribute("id").isEmpty());
        // The note keeps its text: only the author's name is replaced.
        Assertions.assertEquals(List.of(AuthorshipCleaner.NEUTRAL_AUTHOR + ":", "\nChecked."),
                TestPackage.texts(output, "xl/comments1.xml", TestPackage.S, "t"));

        final String neutral = AuthorshipCleaner.NEUTRAL_AUTHOR;
        Assertions.assertEquals(neutral, TestPackage.elements(output, "xl/workbook.xml", TestPackage.S, "fileSharing")
                .get(0).getAttribute("userName"));
        Assertions.assertEquals(neutral, TestPackage.elements(output, "xl/revisions/revisionHeaders.xml", TestPackage.S, "header")
                .get(0).getAttribute("userName"));
        Assertions.assertEquals(neutral, TestPackage.elements(output, "xl/revisions/revisionLog1.xml", TestPackage.S, "rcmt")
                .get(0).getAttribute("author"));
        Assertions.assertEquals(neutral, TestPackage.elements(output, "xl/revisions/userNames.xml", TestPackage.S, "userInfo")
                .get(0).getAttribute("name"));
    }

    private static byte[] note(final String commentXml) {
        final SpreadsheetDocs.Workbook wb = new SpreadsheetDocs.Workbook(List.<String[]>of(new String[]{"Name"}), "Sheet1");
        wb.pkg.rel(wb.sheetPart, TestPackage.REL + "comments", "/xl/comments1.xml");
        wb.pkg.part("/xl/comments1.xml", SpreadsheetDocs.CT_COMMENTS, "<comments xmlns=\"" + TestPackage.S + "\"><authors>"
                + "<author>" + EDITOR + "</author><author>" + AUTHOR + "</author></authors><commentList>"
                + commentXml + "</commentList></comments>");
        return wb.toBytes();
    }

    private static List<String> noteTexts(final String commentXml) throws Exception {
        final byte[] output = XlsxRedactor.redact(note(commentXml), NO_FILTERS, OoxmlRedactionOptions.defaults()).document();
        return TestPackage.texts(output, "xl/comments1.xml", TestPackage.S, "t");
    }

    @Test
    public void aNoteAuthorNameSplitAcrossRunsIsReplaced() throws Exception {
        Assertions.assertEquals(List.of("Redacted:", "", " Checked."), noteTexts("<comment ref=\"A1\" authorId=\"1\"><text>"
                + "<r><t xml:space=\"preserve\">Jordan </t></r><r><t>Example</t></r><r><t xml:space=\"preserve\">: Checked.</t></r>"
                + "</text></comment>"));
    }

    @Test
    public void aNoteWithoutRunsHasItsAuthorNameReplaced() throws Exception {
        Assertions.assertEquals(List.of("Redacted: Checked."), noteTexts("<comment ref=\"A1\" authorId=\"1\"><text>"
                + "<t>" + AUTHOR + ": Checked.</t></text></comment>"));
    }

    @Test
    public void noteTextNotStartingWithItsAuthorsNameIsUnchanged() throws Exception {
        // The text starts with the other author's name, and with this author's name but no colon.
        final String other = "<comment ref=\"A1\" authorId=\"1\"><text><r><t>" + EDITOR + ":</t></r></text></comment>";
        final String noColon = "<comment ref=\"A2\" authorId=\"1\"><text><r><t>" + AUTHOR + " checked.</t></r></text></comment>";
        Assertions.assertEquals(List.of(EDITOR + ":", AUTHOR + " checked."), noteTexts(other + noColon));
    }

    @Test
    public void aNoteAuthorNameIsReplacedBeforeThePolicyRedactsIt() throws Exception {
        final byte[] output = XlsxRedactor.redact(workbook("Sheet1"), TestFilters.forPolicy(TestFilters.NAME_POLICY),
                OoxmlRedactionOptions.defaults()).document();
        Assertions.assertEquals(AuthorshipCleaner.NEUTRAL_AUTHOR + ":",
                TestPackage.texts(output, "xl/comments1.xml", TestPackage.S, "t").get(0));
    }

    @Test
    public void excelAuthorsAreNoLongerReportedAsDetections() throws Exception {
        final List<OfficeRedactionSpan> spans = XlsxRedactor.detect(workbook("Sheet1"),
                TestFilters.forPolicy(TestFilters.NAME_POLICY), OoxmlRedactionOptions.defaults());
        Assertions.assertTrue(spans.stream().noneMatch(s -> s.getText().contains("Jordan") || s.getText().contains("Casey")));
    }

    @Test
    public void embeddedDocumentsAreCleared() throws Exception {
        final byte[] docx = WordDocumentRedactor.redact(OoxmlTestFixtures.docxWithEmbeddedXlsx("Body.", workbook("Sheet1")),
                NO_FILTERS, OoxmlRedactionOptions.defaults()).document();
        assertExcelCleared(TestPackage.entries(docx).get("word/embeddings/Microsoft_Excel_Worksheet.xlsx"));

        final byte[] xlsx = XlsxRedactor.redact(SpreadsheetDocs.createXlsxWithEmbeddedPackage(
                        List.<String[]>of(new String[]{"Name"}), word(), TestPackage.CT_DOCX),
                NO_FILTERS, OoxmlRedactionOptions.defaults()).document();
        assertWordCleared(TestPackage.entries(xlsx).get("xl/embeddings/package1.docx"));
    }

    @Test
    public void redactingAgainChangesNoPart() throws Exception {
        final byte[] once = XlsxRedactor.redact(workbook("Sheet1"), NO_FILTERS, OoxmlRedactionOptions.defaults()).document();
        final byte[] twice = XlsxRedactor.redact(once, NO_FILTERS, OoxmlRedactionOptions.defaults()).document();
        for (final Map.Entry<String, byte[]> entry : TestPackage.entries(once).entrySet()) {
            Assertions.assertArrayEquals(entry.getValue(), TestPackage.entries(twice).get(entry.getKey()), entry.getKey());
        }
    }

    @Test
    public void partsWithNothingToClearAreUnchanged() throws Exception {
        assertUnchanged(OoxmlTestFixtures.docx("Nothing to see."),
                WordDocumentRedactor.redact(OoxmlTestFixtures.docx("Nothing to see."), NO_FILTERS, OoxmlRedactionOptions.defaults()).document());
        // A workbook with a note not starting with its author's name, and fileSharing without a userName.
        final SpreadsheetDocs.Workbook wb = new SpreadsheetDocs.Workbook(List.<String[]>of(new String[]{"Name"}), "Sheet1");
        SpreadsheetDocs.addLegacyComment(wb, "A1", "Checked.", AuthorshipCleaner.NEUTRAL_AUTHOR);
        wb.beforeSheets.append("<fileSharing readOnlyRecommended=\"1\"/>");
        final byte[] input = wb.toBytes();
        assertUnchanged(input, XlsxRedactor.redact(input, NO_FILTERS, OoxmlRedactionOptions.defaults()).document());
    }

    private static void assertUnchanged(final byte[] input, final byte[] output) {
        Assertions.assertEquals(TestPackage.entries(input).keySet(), TestPackage.entries(output).keySet());
        for (final Map.Entry<String, byte[]> entry : TestPackage.entries(input).entrySet()) {
            Assertions.assertArrayEquals(entry.getValue(), TestPackage.entries(output).get(entry.getKey()), entry.getKey());
        }
    }

}
