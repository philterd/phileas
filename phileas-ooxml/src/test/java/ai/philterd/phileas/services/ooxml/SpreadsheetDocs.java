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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static ai.philterd.phileas.services.ooxml.TestPackage.PKG_RELS;
import static ai.philterd.phileas.services.ooxml.TestPackage.REL;
import static ai.philterd.phileas.services.ooxml.TestPackage.S;
import static ai.philterd.phileas.services.ooxml.TestPackage.escape;
import static ai.philterd.phileas.services.ooxml.TestPackage.escapeAttribute;

/**
 * Excel fixtures for tests, ported from phileas-dotnet's {@code SpreadsheetTestHelper}. Workbooks are
 * returned as bytes.
 */
final class SpreadsheetDocs {

    static final String CT_WORKSHEET = "application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml";
    static final String CT_SHARED_STRINGS = "application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml";
    static final String CT_COMMENTS = "application/vnd.openxmlformats-officedocument.spreadsheetml.comments+xml";
    static final String CT_THREADED_COMMENTS = "application/vnd.ms-excel.threadedcomments+xml";
    static final String CT_PERSON = "application/vnd.ms-excel.person+xml";
    static final String CT_DRAWING = "application/vnd.openxmlformats-officedocument.drawing+xml";
    static final String CT_CHART = "application/vnd.openxmlformats-officedocument.drawingml.chart+xml";
    static final String CT_PIVOT_CACHE_DEFINITION = "application/vnd.openxmlformats-officedocument.spreadsheetml.pivotCacheDefinition+xml";
    static final String CT_PIVOT_CACHE_RECORDS = "application/vnd.openxmlformats-officedocument.spreadsheetml.pivotCacheRecords+xml";
    static final String CT_PIVOT_TABLE = "application/vnd.openxmlformats-officedocument.spreadsheetml.pivotTable+xml";
    static final String CT_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    static final String THREADED = "http://schemas.microsoft.com/office/spreadsheetml/2018/threadedcomments";
    static final String REL_THREADED_COMMENT = "http://schemas.microsoft.com/office/2017/10/relationships/threadedComment";
    static final String REL_PERSON = "http://schemas.microsoft.com/office/2017/10/relationships/person";

    private static final String XDR = "http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing";
    private static final String PERSON_ID = "{11111111-1111-1111-1111-111111111111}";

    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?");

    /**
     * A workbook under construction: one sheet of cells (numbers as number cells, other text as shared
     * strings), to which a test may add parts and relationships before {@link #toBytes()}.
     */
    static final class Workbook {
        final TestPackage pkg = new TestPackage();
        final String workbookPart = "/xl/workbook.xml";
        final String sheetPart = "/xl/worksheets/sheet1.xml";
        final String sheetName;
        final List<String[]> rows;
        /** Extra worksheet children inserted after sheetData (for example headerFooter, drawing, legacyDrawing). */
        final StringBuilder afterSheetData = new StringBuilder();
        /** Extra workbook children inserted before sheets (for example fileSharing). */
        final StringBuilder beforeSheets = new StringBuilder();
        /** Extra workbook children inserted after sheets (for example pivotCaches). */
        final StringBuilder afterSheets = new StringBuilder();
        /** When set, the first sheet's sheetData element is this XML instead of being built from {@link #rows}. */
        String sheetDataXml;
        /** Whether the workbook has a shared-string table. */
        boolean sharedStrings = true;
        /** Further sheets as {name, sheetData XML}. */
        final List<String[]> extraSheets = new ArrayList<>();
        String sheetRelId;

        Workbook(final List<String[]> rows, final String sheetName) {
            this.rows = rows;
            this.sheetName = sheetName;
        }

        /** Adds a sheet whose text values are inline strings, as phileas-dotnet's AppendSheet does. */
        Workbook addSheet(final List<String[]> sheetRows, final String name) {
            final StringBuilder sheetData = new StringBuilder("<sheetData>");
            for (int r = 0; r < sheetRows.size(); r++) {
                sheetData.append("<row r=\"").append(r + 1).append("\">");
                final String[] row = sheetRows.get(r);
                for (int c = 0; c < row.length; c++) {
                    if (row[c] != null) {
                        sheetData.append(inlineCell(SpreadsheetColumn.indexToLetter(c + 1) + (r + 1), row[c], false));
                    }
                }
                sheetData.append("</row>");
            }
            sheetData.append("</sheetData>");
            extraSheets.add(new String[]{name, sheetData.toString()});
            return this;
        }

        byte[] toBytes() {
            final Map<String, Integer> shared = new LinkedHashMap<>();
            String sheetData = sheetDataXml;
            if (sheetData == null) {
                final StringBuilder sb = new StringBuilder("<sheetData>");
                for (int r = 0; r < rows.size(); r++) {
                    sb.append("<row r=\"").append(r + 1).append("\">");
                    final String[] row = rows.get(r);
                    for (int c = 0; c < row.length; c++) {
                        final String value = row[c];
                        if (value == null) {
                            continue;
                        }
                        final String ref = SpreadsheetColumn.indexToLetter(c + 1) + (r + 1);
                        if (NUMBER.matcher(value).matches()) {
                            sb.append("<c r=\"").append(ref).append("\"><v>").append(value).append("</v></c>");
                        } else {
                            final int index = shared.computeIfAbsent(value, k -> shared.size());
                            sb.append("<c r=\"").append(ref).append("\" t=\"s\"><v>").append(index).append("</v></c>");
                        }
                    }
                    sb.append("</row>");
                }
                sb.append("</sheetData>");
                sheetData = sb.toString();
            }

            final StringBuilder sst = new StringBuilder("<sst xmlns=\"" + S + "\">");
            for (final String value : shared.keySet()) {
                sst.append("<si><t>").append(escape(value)).append("</t></si>");
            }
            sst.append("</sst>");

            pkg.rel("/", "rIdWb", TestPackage.REL_OFFICE_DOCUMENT, workbookPart);
            sheetRelId = sheetRelId == null ? "rIdSheet1" : sheetRelId;
            pkg.rel(workbookPart, sheetRelId, REL + "worksheet", sheetPart);
            final StringBuilder sheets = new StringBuilder("<sheet name=\"" + escapeAttribute(sheetName)
                    + "\" sheetId=\"1\" r:id=\"" + sheetRelId + "\"/>");
            for (int i = 0; i < extraSheets.size(); i++) {
                final String part = "/xl/worksheets/sheet" + (i + 2) + ".xml";
                final String id = "rIdSheet" + (i + 2);
                pkg.rel(workbookPart, id, REL + "worksheet", part);
                sheets.append("<sheet name=\"").append(escapeAttribute(extraSheets.get(i)[0])).append("\" sheetId=\"")
                        .append(i + 2).append("\" r:id=\"").append(id).append("\"/>");
                pkg.part(part, CT_WORKSHEET, "<worksheet xmlns=\"" + S + "\" xmlns:r=\"" + TestPackage.R + "\">"
                        + extraSheets.get(i)[1] + "</worksheet>");
            }
            if (sharedStrings) {
                pkg.rel(workbookPart, "rIdSst", REL + "sharedStrings", "/xl/sharedStrings.xml");
            }
            pkg.part(workbookPart, TestPackage.CT_XLSX_MAIN,
                    "<workbook xmlns=\"" + S + "\" xmlns:r=\"" + TestPackage.R + "\">" + beforeSheets + "<sheets>" + sheets
                            + "</sheets>" + afterSheets + "</workbook>");
            pkg.part(sheetPart, CT_WORKSHEET,
                    "<worksheet xmlns=\"" + S + "\" xmlns:r=\"" + TestPackage.R + "\">" + sheetData + afterSheetData + "</worksheet>");
            if (sharedStrings) {
                pkg.part("/xl/sharedStrings.xml", CT_SHARED_STRINGS, sst.toString());
            }
            return pkg.toBytes();
        }
    }

    /** A single-sheet workbook; each row is an array of cell values (null for an empty cell). */
    static byte[] createXlsx(final List<String[]> rows) {
        return createXlsx(rows, "Sheet1");
    }

    static byte[] createXlsx(final List<String[]> rows, final String sheetName) {
        return new Workbook(rows, sheetName).toBytes();
    }

    /** A workbook with a first sheet of shared strings and a second sheet of inline strings. */
    static byte[] createXlsxWithSecondSheet(final List<String[]> rows, final String sheetName,
                                           final List<String[]> secondRows, final String secondSheetName) {
        return new Workbook(rows, sheetName).addSheet(secondRows, secondSheetName).toBytes();
    }

    // --- headers, footers, comments ------------------------------------------------------------

    /** A single-sheet workbook with a print header and footer (which may contain Excel field codes). */
    static byte[] createXlsxWithHeaderFooter(final List<String[]> rows, final String oddHeader, final String oddFooter) {
        final Workbook wb = new Workbook(rows, "Sheet1");
        wb.afterSheetData.append("<headerFooter><oddHeader>").append(escape(oddHeader)).append("</oddHeader><oddFooter>")
                .append(escape(oddFooter)).append("</oddFooter></headerFooter>");
        return wb.toBytes();
    }

    /** A single-sheet workbook with a legacy cell comment (xl/comments1.xml). */
    static byte[] createXlsxWithLegacyComment(final List<String[]> rows, final String cellRef, final String commentText) {
        final Workbook wb = new Workbook(rows, "Sheet1");
        addLegacyComment(wb, cellRef, commentText, "Reviewer");
        return wb.toBytes();
    }

    /** A single-sheet workbook with a threaded cell comment (xl/threadedComments and xl/persons). */
    static byte[] createXlsxWithThreadedComment(final List<String[]> rows, final String cellRef, final String commentText,
                                                final String authorDisplayName) {
        final Workbook wb = new Workbook(rows, "Sheet1");
        addThreadedComment(wb, cellRef, commentText, authorDisplayName);
        return wb.toBytes();
    }

    /** A workbook with both a legacy and a threaded comment, on separate cells. */
    static byte[] createXlsxWithLegacyAndThreadedComments(final List<String[]> rows, final String legacyCellRef,
                                                          final String legacyText, final String threadedCellRef,
                                                          final String threadedText, final String threadedAuthorDisplayName) {
        final Workbook wb = new Workbook(rows, "Sheet1");
        addLegacyComment(wb, legacyCellRef, legacyText, "Reviewer");
        addThreadedComment(wb, threadedCellRef, threadedText, threadedAuthorDisplayName);
        return wb.toBytes();
    }

    static void addLegacyComment(final Workbook wb, final String cellRef, final String text, final String author) {
        wb.pkg.rel(wb.sheetPart, REL + "comments", "/xl/comments1.xml");
        wb.pkg.part("/xl/comments1.xml", CT_COMMENTS, "<comments xmlns=\"" + S + "\"><authors><author>" + escape(author)
                + "</author></authors><commentList><comment ref=\"" + cellRef + "\" authorId=\"0\"><text><r>"
                + "<t xml:space=\"preserve\">" + escape(text) + "</t></r></text></comment></commentList></comments>");
    }

    /** A legacy comment shaped as Excel writes a note: the author's name and a colon in bold, then the text. */
    static void addNote(final Workbook wb, final String cellRef, final String author, final String text) {
        wb.pkg.rel(wb.sheetPart, REL + "comments", "/xl/comments1.xml");
        wb.pkg.part("/xl/comments1.xml", CT_COMMENTS, "<comments xmlns=\"" + S + "\"><authors><author>" + escape(author)
                + "</author></authors><commentList><comment ref=\"" + cellRef + "\" authorId=\"0\"><text>"
                + "<r><rPr><b/><sz val=\"9\"/><rFont val=\"Tahoma\"/></rPr><t>" + escape(author) + ":</t></r>"
                + "<r><rPr><sz val=\"9\"/><rFont val=\"Tahoma\"/></rPr><t xml:space=\"preserve\">\n" + escape(text)
                + "</t></r></text></comment></commentList></comments>");
    }

    static void addThreadedComment(final Workbook wb, final String cellRef, final String text, final String author) {
        wb.pkg.rel(wb.workbookPart, REL_PERSON, "/xl/persons/person.xml");
        wb.pkg.part("/xl/persons/person.xml", CT_PERSON, "<personList xmlns=\"" + THREADED + "\" xmlns:x=\"" + S + "\">"
                + "<person displayName=\"" + escapeAttribute(author) + "\" id=\"" + PERSON_ID
                + "\" userId=\"S::demo::abc\" providerId=\"None\"/></personList>");
        wb.pkg.rel(wb.sheetPart, REL_THREADED_COMMENT, "/xl/threadedComments/threadedComment1.xml");
        wb.pkg.part("/xl/threadedComments/threadedComment1.xml", CT_THREADED_COMMENTS,
                "<ThreadedComments xmlns=\"" + THREADED + "\" xmlns:x=\"" + S + "\"><threadedComment ref=\"" + cellRef
                        + "\" dT=\"2024-01-01T00:00:00.00\" personId=\"" + PERSON_ID
                        + "\" id=\"{22222222-2222-2222-2222-222222222222}\"><text>" + escape(text)
                        + "</text></threadedComment></ThreadedComments>");
    }

    /** All comment text and author/person names in the workbook (legacy and threaded), joined with newlines. */
    static String allCommentText(final byte[] xlsx) {
        final List<String> out = new ArrayList<>();
        for (final String name : TestPackage.entries(xlsx).keySet()) {
            if (isLegacyComments(name) || isThreadedComments(name)) {
                out.add(TestPackage.dom(xlsx, name).getDocumentElement().getTextContent());
            }
        }
        for (final String name : TestPackage.entries(xlsx).keySet()) {
            if (isPersons(name)) {
                for (final Element person : TestPackage.elements(xlsx, name, THREADED, "person")) {
                    out.add(person.getAttribute("displayName"));
                }
            }
        }
        return String.join("\n", out);
    }

    /** The raw XML of every comment-related part (for strong leak checks). */
    static String allCommentXml(final byte[] xlsx) {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, byte[]> e : TestPackage.entries(xlsx).entrySet()) {
            if (isLegacyComments(e.getKey()) || isThreadedComments(e.getKey()) || isPersons(e.getKey())) {
                sb.append(new String(e.getValue(), StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }

    private static boolean isLegacyComments(final String name) {
        return name.startsWith("xl/comments") && name.endsWith(".xml");
    }

    private static boolean isThreadedComments(final String name) {
        return name.startsWith("xl/threadedComments/") && name.endsWith(".xml");
    }

    private static boolean isPersons(final String name) {
        return name.startsWith("xl/persons/") && name.endsWith(".xml");
    }

    /** The concatenated header/footer text across every worksheet, joined with newlines. */
    static String headerFooterText(final byte[] xlsx) {
        final List<String> out = new ArrayList<>();
        for (final String name : worksheetEntries(xlsx)) {
            for (final Element hf : TestPackage.elements(xlsx, name, S, "headerFooter")) {
                for (final Element child : OoxmlPackage.childElements(hf)) {
                    out.add(child.getTextContent());
                }
            }
        }
        return String.join("\n", out);
    }

    // --- charts -----------------------------------------------------------------------------

    /** A chart part (c:chartSpace) with the given title, cached series name, and cached category. */
    static String chartSpaceXml(final String title, final String seriesName, final String category) {
        return chartSpaceXmlWithTitleParagraph("<a:p><a:r><a:t>" + escape(title) + "</a:t></a:r></a:p>", seriesName, category);
    }

    /** As {@link #chartSpaceXml}, with the title given as a raw DrawingML paragraph. */
    static String chartSpaceXmlWithTitleParagraph(final String titleAParagraphXml, final String seriesName, final String category) {
        return "<c:chartSpace xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" "
                + "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<c:chart>"
                + "<c:title><c:tx><c:rich><a:bodyPr/><a:lstStyle/>" + titleAParagraphXml + "</c:rich></c:tx><c:overlay val=\"0\"/></c:title>"
                + "<c:autoTitleDeleted val=\"0\"/>"
                + "<c:plotArea><c:layout/>"
                + "<c:barChart><c:barDir val=\"col\"/><c:grouping val=\"clustered\"/>"
                + "<c:ser><c:idx val=\"0\"/><c:order val=\"0\"/>"
                + "<c:tx><c:strRef><c:f>Sheet1!$B$1</c:f><c:strCache><c:ptCount val=\"1\"/><c:pt idx=\"0\"><c:v>" + escape(seriesName) + "</c:v></c:pt></c:strCache></c:strRef></c:tx>"
                + "<c:cat><c:strRef><c:f>Sheet1!$A$2</c:f><c:strCache><c:ptCount val=\"1\"/><c:pt idx=\"0\"><c:v>" + escape(category) + "</c:v></c:pt></c:strCache></c:strRef></c:cat>"
                + "<c:val><c:numRef><c:f>Sheet1!$B$2</c:f><c:numCache><c:formatCode>General</c:formatCode><c:ptCount val=\"1\"/><c:pt idx=\"0\"><c:v>42</c:v></c:pt></c:numCache></c:numRef></c:val>"
                + "</c:ser><c:axId val=\"111111111\"/><c:axId val=\"222222222\"/></c:barChart>"
                + "<c:catAx><c:axId val=\"111111111\"/><c:scaling><c:orientation val=\"minMax\"/></c:scaling><c:delete val=\"0\"/><c:axPos val=\"b\"/><c:crossAx val=\"222222222\"/></c:catAx>"
                + "<c:valAx><c:axId val=\"222222222\"/><c:scaling><c:orientation val=\"minMax\"/></c:scaling><c:delete val=\"0\"/><c:axPos val=\"l\"/><c:crossAx val=\"111111111\"/></c:valAx>"
                + "</c:plotArea><c:plotVisOnly val=\"1\"/></c:chart></c:chartSpace>";
    }

    /** A single-sheet workbook with an embedded chart under the worksheet's drawing. */
    static byte[] createXlsxWithChart(final List<String[]> rows, final String title, final String seriesName, final String category) {
        return createXlsxWithChartXml(rows, chartSpaceXml(title, seriesName, category));
    }

    /** A single-sheet workbook with an embedded chart whose chart part is {@code chartSpaceXml}. */
    static byte[] createXlsxWithChartXml(final List<String[]> rows, final String chartSpaceXml) {
        final Workbook wb = new Workbook(rows, "Sheet1");
        final String drawing = "/xl/drawings/drawing1.xml";
        final String drawingId = wb.pkg.rel(wb.sheetPart, REL + "drawing", drawing);
        final String chartId = wb.pkg.rel(drawing, REL + "chart", "/xl/charts/chart1.xml");
        wb.pkg.part("/xl/charts/chart1.xml", CT_CHART, chartSpaceXml);
        wb.pkg.part(drawing, CT_DRAWING,
                "<xdr:wsDr xmlns:xdr=\"" + XDR + "\" xmlns:a=\"" + TestPackage.A + "\" xmlns:r=\"" + TestPackage.R
                        + "\" xmlns:c=\"" + TestPackage.C + "\">"
                        + "<xdr:twoCellAnchor>"
                        + "<xdr:from><xdr:col>3</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>1</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:from>"
                        + "<xdr:to><xdr:col>9</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>15</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:to>"
                        + "<xdr:graphicFrame macro=\"\"><xdr:nvGraphicFramePr><xdr:cNvPr id=\"2\" name=\"Chart 1\"/><xdr:cNvGraphicFramePr/></xdr:nvGraphicFramePr>"
                        + "<xdr:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/></xdr:xfrm>"
                        + "<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/chart\"><c:chart r:id=\"" + chartId + "\"/></a:graphicData></a:graphic>"
                        + "</xdr:graphicFrame><xdr:clientData/></xdr:twoCellAnchor></xdr:wsDr>");
        wb.afterSheetData.append("<drawing r:id=\"").append(drawingId).append("\"/>");
        return wb.toBytes();
    }

    // --- shapes ---------------------------------------------------------------------------------

    /** A single-sheet workbook with a text box whose one paragraph holds the given runs. */
    static byte[] createXlsxWithTextBox(final List<String[]> rows, final String... runs) {
        final StringBuilder p = new StringBuilder("<a:p>");
        for (final String run : runs) {
            p.append("<a:r><a:t>").append(escape(run)).append("</a:t></a:r>");
        }
        return createXlsxWithTextBoxParagraph(rows, p.append("</a:p>").toString());
    }

    /** As {@link #createXlsxWithTextBox}, with the paragraph given as raw a:p XML. */
    static byte[] createXlsxWithTextBoxParagraph(final List<String[]> rows, final String aParagraphXml) {
        final Workbook wb = new Workbook(rows, "Sheet1");
        final String drawing = "/xl/drawings/drawing1.xml";
        final String drawingId = wb.pkg.rel(wb.sheetPart, REL + "drawing", drawing);
        wb.pkg.part(drawing, CT_DRAWING,
                "<xdr:wsDr xmlns:xdr=\"" + XDR + "\" xmlns:a=\"" + TestPackage.A + "\">"
                        + "<xdr:twoCellAnchor>"
                        + "<xdr:from><xdr:col>1</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>1</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:from>"
                        + "<xdr:to><xdr:col>5</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>6</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:to>"
                        + "<xdr:sp macro=\"\" textlink=\"\">"
                        + "<xdr:nvSpPr><xdr:cNvPr id=\"2\" name=\"TextBox 1\"/><xdr:cNvSpPr txBox=\"1\"/></xdr:nvSpPr>"
                        + "<xdr:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"1000000\" cy=\"500000\"/></a:xfrm>"
                        + "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></xdr:spPr>"
                        + "<xdr:txBody><a:bodyPr/><a:lstStyle/>" + aParagraphXml + "</xdr:txBody>"
                        + "</xdr:sp><xdr:clientData/></xdr:twoCellAnchor></xdr:wsDr>");
        wb.afterSheetData.append("<drawing r:id=\"").append(drawingId).append("\"/>");
        return wb.toBytes();
    }

    /** The concatenated drawing XML (shapes and text boxes) of the workbook. */
    static String allDrawingXml(final byte[] xlsx) {
        return concatEntries(xlsx, "xl/drawings/");
    }

    /** The concatenated XML of every chart part in the workbook. */
    static String allChartXml(final byte[] xlsx) {
        return concatEntries(xlsx, "xl/charts/");
    }

    // --- pivot caches ---------------------------------------------------------------------------

    /**
     * A workbook with a pivot cache and a pivot table on column A of the first sheet. The cache's first field
     * (column A) has {@code sharedItems} as its items, one record per item. With {@code recordInlineString},
     * a second field (column B) holds values inline in the records, the first record's being that string.
     */
    static byte[] createXlsxWithPivotCache(final List<String[]> rows, final String[] sharedItems, final String recordInlineString) {
        final Workbook wb = new Workbook(rows, "Sheet1");
        final String definition = "/xl/pivotCache/pivotCacheDefinition1.xml";
        final String records = "/xl/pivotCache/pivotCacheRecords1.xml";
        final String table = "/xl/pivotTables/pivotTable1.xml";
        final boolean inlineField = recordInlineString != null;
        final String[] header = rows.isEmpty() ? new String[0] : rows.get(0);
        final String firstName = header.length > 0 ? header[0] : "Name";
        final String secondName = header.length > 1 ? header[1] : "Value";
        final int lastRow = Math.max(rows.size(), 2);

        final StringBuilder shared = new StringBuilder();
        for (final String s : sharedItems) {
            shared.append("<s v=\"").append(escapeAttribute(s)).append("\"/>");
        }
        final StringBuilder recordXml = new StringBuilder();
        for (int i = 0; i < sharedItems.length; i++) {
            recordXml.append("<r><x v=\"").append(i).append("\"/>");
            if (inlineField) {
                recordXml.append("<s v=\"").append(escapeAttribute(i == 0 ? recordInlineString : "Other")).append("\"/>");
            }
            recordXml.append("</r>");
        }

        final String recordsRelId = wb.pkg.rel(definition, REL + "pivotCacheRecords", records);
        wb.pkg.part(definition, CT_PIVOT_CACHE_DEFINITION,
                "<pivotCacheDefinition xmlns=\"" + S + "\" xmlns:r=\"" + TestPackage.R + "\" r:id=\"" + recordsRelId
                        + "\" recordCount=\"" + sharedItems.length + "\" createdVersion=\"6\" refreshedVersion=\"6\" minRefreshableVersion=\"3\">"
                        + "<cacheSource type=\"worksheet\"><worksheetSource ref=\"A1:" + (inlineField ? "B" : "A") + lastRow + "\" sheet=\"Sheet1\"/></cacheSource>"
                        + "<cacheFields count=\"" + (inlineField ? 2 : 1) + "\">"
                        + "<cacheField name=\"" + escapeAttribute(firstName) + "\" numFmtId=\"0\"><sharedItems count=\"" + sharedItems.length + "\">" + shared + "</sharedItems></cacheField>"
                        + (inlineField ? "<cacheField name=\"" + escapeAttribute(secondName) + "\" numFmtId=\"0\"><sharedItems/></cacheField>" : "")
                        + "</cacheFields></pivotCacheDefinition>");
        wb.pkg.part(records, CT_PIVOT_CACHE_RECORDS,
                "<pivotCacheRecords xmlns=\"" + S + "\" xmlns:r=\"" + TestPackage.R + "\" count=\"" + sharedItems.length + "\">"
                        + recordXml + "</pivotCacheRecords>");

        // A row field listing every item, then a grand total, placed to the right of the data.
        final StringBuilder items = new StringBuilder();
        final StringBuilder rowItems = new StringBuilder();
        for (int i = 0; i < sharedItems.length; i++) {
            items.append("<item x=\"").append(i).append("\"/>");
            rowItems.append(i == 0 ? "<i><x/></i>" : "<i><x v=\"" + i + "\"/></i>");
        }
        wb.pkg.rel(wb.sheetPart, REL + "pivotTable", table);
        wb.pkg.rel(table, REL + "pivotCacheDefinition", definition);
        wb.pkg.part(table, CT_PIVOT_TABLE,
                "<pivotTableDefinition xmlns=\"" + S + "\" name=\"PivotTable1\" cacheId=\"1\" dataCaption=\"Values\""
                        + " createdVersion=\"6\" updatedVersion=\"6\" minRefreshableVersion=\"3\" outline=\"1\" outlineData=\"1\">"
                        + "<location ref=\"H1:H" + (sharedItems.length + 2) + "\" firstHeaderRow=\"1\" firstDataRow=\"1\" firstDataCol=\"0\"/>"
                        + "<pivotFields count=\"" + (inlineField ? 2 : 1) + "\">"
                        + "<pivotField axis=\"axisRow\" showAll=\"0\"><items count=\"" + (sharedItems.length + 1) + "\">" + items + "<item t=\"default\"/></items></pivotField>"
                        + (inlineField ? "<pivotField showAll=\"0\"/>" : "")
                        + "</pivotFields>"
                        + "<rowFields count=\"1\"><field x=\"0\"/></rowFields>"
                        + "<rowItems count=\"" + (sharedItems.length + 1) + "\">" + rowItems + "<i t=\"grand\"><x/></i></rowItems>"
                        + "<colItems count=\"1\"><i/></colItems>"
                        + "</pivotTableDefinition>");
        final String cacheRelId = wb.pkg.rel(wb.workbookPart, REL + "pivotCacheDefinition", definition);
        wb.afterSheets.append("<pivotCaches><pivotCache cacheId=\"1\" r:id=\"").append(cacheRelId).append("\"/></pivotCaches>");
        return wb.toBytes();
    }

    /** The concatenated XML of every pivot cache and pivot table part. */
    static String allPivotXml(final byte[] xlsx) {
        return concatEntries(xlsx, "xl/pivotCache/") + concatEntries(xlsx, "xl/pivotTables/");
    }

    /** True if any pivot cache definition is set to refresh on load. */
    static boolean anyPivotRefreshOnLoad(final byte[] xlsx) {
        for (final String name : TestPackage.entries(xlsx).keySet()) {
            if (name.startsWith("xl/pivotCache/pivotCacheDefinition") && name.endsWith(".xml")) {
                if (isTrue(TestPackage.dom(xlsx, name).getDocumentElement().getAttribute("refreshOnLoad"))) {
                    return true;
                }
            }
        }
        return false;
    }

    // --- embedded objects -------------------------------------------------------------------------

    /** A workbook with an embedded package (an Office document) on the first worksheet. */
    static byte[] createXlsxWithEmbeddedPackage(final List<String[]> rows, final byte[] pkgBytes, final String contentType) {
        final Workbook wb = new Workbook(rows, "Sheet1");
        final String ext = contentType.contains("spreadsheetml") ? "xlsx"
                : contentType.contains("wordprocessingml") ? "docx"
                : contentType.contains("presentationml") ? "pptx" : "bin";
        final String part = "/xl/embeddings/package1." + ext;
        wb.pkg.part(part, contentType, pkgBytes);
        wb.pkg.rel(wb.sheetPart, "rIdEmb", REL + "package", part);
        return wb.toBytes();
    }

    /** A workbook with an embedded object that cannot be inspected (a PowerPoint package, treated as opaque). */
    static byte[] createXlsxWithEmbeddedOleObject(final List<String[]> rows, final byte[] bytes) {
        return createXlsxWithEmbeddedPackage(rows, bytes, CT_PPTX);
    }

    /** True if any worksheet still carries an embedded package or OLE object. */
    static boolean xlsxHasAnyEmbeddedObject(final byte[] xlsx) {
        return !embeddedTargets(xlsx).isEmpty();
    }

    /** All cell text of the first embedded package on any worksheet, read as an .xlsx (empty if none). */
    static String xlsxEmbeddedPackageAllText(final byte[] xlsx) {
        for (final String target : embeddedTargets(xlsx)) {
            return allText(TestPackage.entries(xlsx).get(target));
        }
        return "";
    }

    // Entry names of embedded package or OLE object parts related from a worksheet.
    private static List<String> embeddedTargets(final byte[] xlsx) {
        final List<String> out = new ArrayList<>();
        final Map<String, byte[]> entries = TestPackage.entries(xlsx);
        for (final String name : worksheetEntries(xlsx)) {
            final int slash = name.lastIndexOf('/');
            final String relsName = name.substring(0, slash + 1) + "_rels/" + name.substring(slash + 1) + ".rels";
            out.addAll(relTargets(xlsx, name, relsName, REL + "package", REL + "oleObject").stream()
                    .filter(entries::containsKey).toList());
        }
        return out;
    }

    /** The resolved entry names targeted by relationships of the given types in a .rels entry. */
    static List<String> relTargets(final byte[] pkg, final String sourceEntry, final String relsEntry, final String... types) {
        final List<String> out = new ArrayList<>();
        for (final Element rel : TestPackage.elements(pkg, relsEntry, PKG_RELS, "Relationship")) {
            if ("External".equals(rel.getAttribute("TargetMode"))) {
                continue;
            }
            for (final String type : types) {
                if (type.equals(rel.getAttribute("Type"))) {
                    final URI resolved = URI.create("/" + sourceEntry).resolve(rel.getAttribute("Target"));
                    out.add(resolved.getPath().substring(1));
                }
            }
        }
        return out;
    }

    // --- typed cells ------------------------------------------------------------------------------

    enum CellKind { AUTO, NUMBER, TEXT, BOOLEAN, DATE, FORMULA, ERROR }

    /** A cell for {@link #createTyped}: a value, its kind, and a formula for {@link CellKind#FORMULA}. */
    record CellSpec(String value, CellKind kind, String formula) {
        static CellSpec of(final String value) {
            return new CellSpec(value, CellKind.AUTO, null);
        }

        static CellSpec formula(final String cachedValue, final String formula) {
            return new CellSpec(cachedValue, CellKind.FORMULA, formula);
        }
    }

    /** A single-sheet workbook with explicitly typed cells (numbers, booleans, dates, formulas) and no shared strings. */
    static byte[] createTyped(final List<CellSpec[]> rows) {
        final StringBuilder sheetData = new StringBuilder("<sheetData>");
        for (int r = 0; r < rows.size(); r++) {
            sheetData.append("<row r=\"").append(r + 1).append("\">");
            final CellSpec[] row = rows.get(r);
            for (int c = 0; c < row.length; c++) {
                if (row[c] != null) {
                    sheetData.append(buildCell(SpreadsheetColumn.indexToLetter(c + 1) + (r + 1), row[c]));
                }
            }
            sheetData.append("</row>");
        }
        sheetData.append("</sheetData>");
        final Workbook wb = new Workbook(List.of(), "Sheet1");
        wb.sheetDataXml = sheetData.toString();
        wb.sharedStrings = false;
        return wb.toBytes();
    }

    private static String buildCell(final String ref, final CellSpec spec) {
        CellKind kind = spec.kind();
        if (kind == CellKind.AUTO) {
            kind = NUMBER.matcher(spec.value()).matches() ? CellKind.NUMBER : CellKind.TEXT;
        }
        final String v = "<v>" + escape(spec.value()) + "</v>";
        return switch (kind) {
            case NUMBER -> "<c r=\"" + ref + "\">" + v + "</c>";
            case BOOLEAN -> "<c r=\"" + ref + "\" t=\"b\">" + v + "</c>";
            case DATE -> "<c r=\"" + ref + "\" t=\"d\">" + v + "</c>";
            case ERROR -> "<c r=\"" + ref + "\" t=\"e\">" + v + "</c>";
            // A cached result that is not a number is a string result (t="str"); Excel rejects it otherwise.
            case FORMULA -> "<c r=\"" + ref + "\"" + (NUMBER.matcher(spec.value()).matches() ? "" : " t=\"str\"") + "><f>" + escape(spec.formula() == null ? "1+1" : spec.formula()) + "</f>" + v + "</c>";
            default -> inlineCell(ref, spec.value(), true);
        };
    }

    private static String inlineCell(final String ref, final String value, final boolean preserve) {
        return "<c" + (ref == null ? "" : " r=\"" + ref + "\"") + " t=\"inlineStr\"><is><t"
                + (preserve ? " xml:space=\"preserve\"" : "") + ">" + escape(value) + "</t></is></c>";
    }

    /** A single-sheet workbook whose cells have no reference (column identity comes from document order). */
    static byte[] createXlsxWithoutCellReferences(final List<String[]> rows) {
        final StringBuilder sheetData = new StringBuilder("<sheetData>");
        for (int r = 0; r < rows.size(); r++) {
            sheetData.append("<row r=\"").append(r + 1).append("\">");
            for (final String value : rows.get(r)) {
                if (value != null) {
                    sheetData.append(inlineCell(null, value, false));
                }
            }
            sheetData.append("</row>");
        }
        sheetData.append("</sheetData>");
        final Workbook wb = new Workbook(List.of(), "Sheet1");
        wb.sheetDataXml = sheetData.toString();
        wb.sharedStrings = false;
        return wb.toBytes();
    }

    // --- reading --------------------------------------------------------------------------------

    /** Every cell's text across the workbook's sheets, joined with newlines. */
    static String allText(final byte[] xlsx) {
        final Map<String, byte[]> entries = TestPackage.entries(xlsx);
        final List<String> shared = new ArrayList<>();
        if (entries.containsKey("xl/sharedStrings.xml")) {
            final Document sst = TestPackage.dom(xlsx, "xl/sharedStrings.xml");
            for (final Element si : OoxmlPackage.children(sst.getDocumentElement(), S, "si")) {
                shared.add(si.getTextContent());
            }
        }
        final List<String> lines = new ArrayList<>();
        for (final String name : entries.keySet()) {
            if (!name.startsWith("xl/worksheets/") || !name.endsWith(".xml")) {
                continue;
            }
            for (final Element cell : TestPackage.elements(xlsx, name, S, "c")) {
                lines.add(readCell(cell, shared));
            }
        }
        return String.join("\n", lines);
    }

    static String readCell(final Element cell, final List<String> shared) {
        final String t = cell.getAttribute("t");
        final Element v = OoxmlPackage.firstChild(cell, S, "v");
        if ("s".equals(t)) {
            try {
                final int index = Integer.parseInt(v == null ? "" : v.getTextContent().trim());
                return index >= 0 && index < shared.size() ? shared.get(index) : "";
            } catch (final NumberFormatException e) {
                return "";
            }
        }
        if ("inlineStr".equals(t)) {
            final Element is = OoxmlPackage.firstChild(cell, S, "is");
            return is == null ? "" : is.getTextContent();
        }
        return v == null ? "" : v.getTextContent();
    }

    /** The text of the cell with the given reference (first match across worksheets), or null when absent. */
    static String cellText(final byte[] xlsx, final String cellReference) {
        final Element cell = findCell(xlsx, cellReference);
        if (cell == null) {
            return null;
        }
        final List<String> shared = new ArrayList<>();
        if (TestPackage.entries(xlsx).containsKey("xl/sharedStrings.xml")) {
            for (final Element si : OoxmlPackage.children(TestPackage.dom(xlsx, "xl/sharedStrings.xml").getDocumentElement(), S, "si")) {
                shared.add(si.getTextContent());
            }
        }
        return readCell(cell, shared);
    }

    /** True if the cell is a genuine number cell (no type, no formula). */
    static boolean isNumberCell(final byte[] xlsx, final String cellReference) {
        final Element cell = findCell(xlsx, cellReference);
        return cell != null && OoxmlPackage.firstChild(cell, S, "f") == null && !cell.hasAttribute("t");
    }

    /** True if the cell still holds a formula. */
    static boolean isFormulaCell(final byte[] xlsx, final String cellReference) {
        final Element cell = findCell(xlsx, cellReference);
        return cell != null && OoxmlPackage.firstChild(cell, S, "f") != null;
    }

    /** The cell's cached or stored value (its v element), or null when it has none. */
    static String cachedValue(final byte[] xlsx, final String cellReference) {
        final Element cell = findCell(xlsx, cellReference);
        final Element v = cell == null ? null : OoxmlPackage.firstChild(cell, S, "v");
        return v == null ? null : v.getTextContent();
    }

    /** True if the workbook is set to fully recalculate on open. */
    static boolean fullCalcOnLoad(final byte[] xlsx) {
        for (final Element calcPr : TestPackage.elements(xlsx, "xl/workbook.xml", S, "calcPr")) {
            if (isTrue(calcPr.getAttribute("fullCalcOnLoad"))) {
                return true;
            }
        }
        return false;
    }

    /** True if the cell is still boolean typed. */
    static boolean isBooleanCell(final byte[] xlsx, final String cellReference) {
        final Element cell = findCell(xlsx, cellReference);
        return cell != null && "b".equals(cell.getAttribute("t"));
    }

    /** True if the cell is stored as an inline string (what a redacted cell becomes). */
    static boolean isInlineStringCell(final byte[] xlsx, final String cellReference) {
        final Element cell = findCell(xlsx, cellReference);
        return cell != null && "inlineStr".equals(cell.getAttribute("t"));
    }

    /** The concatenated raw XML of every worksheet part. */
    static String worksheetXml(final byte[] xlsx) {
        return concatEntries(xlsx, "xl/worksheets/");
    }

    private static Element findCell(final byte[] xlsx, final String cellReference) {
        for (final String name : worksheetEntries(xlsx)) {
            for (final Element cell : TestPackage.elements(xlsx, name, S, "c")) {
                if (cellReference.equals(cell.getAttribute("r"))) {
                    return cell;
                }
            }
        }
        return null;
    }

    private static List<String> worksheetEntries(final byte[] xlsx) {
        final List<String> out = new ArrayList<>();
        for (final String name : TestPackage.entries(xlsx).keySet()) {
            if (name.startsWith("xl/worksheets/") && !name.contains("_rels") && name.endsWith(".xml")) {
                out.add(name);
            }
        }
        return out;
    }

    private static String concatEntries(final byte[] pkg, final String prefix) {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, byte[]> e : TestPackage.entries(pkg).entrySet()) {
            if (e.getKey().startsWith(prefix) && !e.getKey().contains("_rels") && e.getKey().endsWith(".xml")) {
                sb.append(new String(e.getValue(), StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }

    private static boolean isTrue(final String value) {
        return "1".equals(value) || "true".equals(value);
    }

    private SpreadsheetDocs() {
    }

}
