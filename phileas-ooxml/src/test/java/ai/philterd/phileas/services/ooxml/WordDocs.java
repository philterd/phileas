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
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ai.philterd.phileas.services.ooxml.TestPackage.A;
import static ai.philterd.phileas.services.ooxml.TestPackage.PKG_RELS;
import static ai.philterd.phileas.services.ooxml.TestPackage.R;
import static ai.philterd.phileas.services.ooxml.TestPackage.REL;
import static ai.philterd.phileas.services.ooxml.TestPackage.W;
import static ai.philterd.phileas.services.ooxml.TestPackage.escape;

/**
 * Word fixtures for tests, ported from phileas-dotnet's {@code WordDocs}. Documents are built from raw XML
 * equivalent to the Open XML SDK calls the C# helpers make, and returned as bytes.
 */
final class WordDocs {

    static final String DOCUMENT = "/word/document.xml";
    static final String DOCUMENT_ENTRY = "word/document.xml";

    static final String CT_HEADER = "application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml";
    static final String CT_FOOTER = "application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml";
    static final String CT_COMMENTS = "application/vnd.openxmlformats-officedocument.wordprocessingml.comments+xml";
    static final String CT_FOOTNOTES = "application/vnd.openxmlformats-officedocument.wordprocessingml.footnotes+xml";
    static final String CT_ENDNOTES = "application/vnd.openxmlformats-officedocument.wordprocessingml.endnotes+xml";
    static final String CT_PEOPLE = "application/vnd.openxmlformats-officedocument.wordprocessingml.people+xml";
    static final String CT_COMMENTS_EX = "application/vnd.openxmlformats-officedocument.wordprocessingml.commentsExtended+xml";
    static final String CT_THREADED = "application/vnd.openxmlformats-officedocument.wordprocessingml.threadedcomments+xml";
    static final String CT_CHART = "application/vnd.openxmlformats-officedocument.drawingml.chart+xml";
    static final String CT_DIAGRAM_DATA = "application/vnd.openxmlformats-officedocument.drawingml.diagramData+xml";
    static final String CT_DRAWING = "application/vnd.openxmlformats-officedocument.drawing+xml";
    static final String CT_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    static final String REL_THREADED = "http://schemas.microsoft.com/office/2018/08/relationships/threadedComment";
    static final String REL_PEOPLE = "http://schemas.microsoft.com/office/2011/relationships/people";
    static final String REL_COMMENTS_EX = "http://schemas.microsoft.com/office/2011/relationships/commentsExtended";
    static final String W15 = "http://schemas.microsoft.com/office/word/2012/wordml";

    /** Every namespace the raw-XML fixtures use, declared on the document root. */
    static final String DOC_NAMESPACES =
            "xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" "
                    + "xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" "
                    + "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" "
                    + "xmlns:wps=\"http://schemas.microsoft.com/office/word/2010/wordprocessingShape\" "
                    + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" "
                    + "xmlns:v=\"urn:schemas-microsoft-com:vml\" "
                    + "xmlns:w10=\"urn:schemas-microsoft-com:office:word\" "
                    + "xmlns:mc=\"http://schemas.openxmlformats.org/markup-compatibility/2006\"";

    private WordDocs() {
    }

    // --- building -----------------------------------------------------------------------

    /** A package with a main document part whose body is {@code bodyInnerXml}. */
    static TestPackage docx(final String bodyInnerXml) {
        final TestPackage pkg = new TestPackage();
        pkg.rel("/", TestPackage.REL_OFFICE_DOCUMENT, DOCUMENT);
        pkg.part(DOCUMENT, TestPackage.CT_DOCX_MAIN, documentXml(bodyInnerXml));
        return pkg;
    }

    static String documentXml(final String bodyInnerXml) {
        return "<w:document " + DOC_NAMESPACES + "><w:body>" + bodyInnerXml + "</w:body></w:document>";
    }

    /** A .docx with one body paragraph per string. */
    static byte[] create(final String... paragraphs) {
        return docx(paras(paragraphs)).toBytes();
    }

    /** A .docx whose body is exactly the supplied raw WordprocessingML. */
    static byte[] createRaw(final String bodyInnerXml) {
        return docx(bodyInnerXml).toBytes();
    }

    /** A .docx with a default header and footer plus body paragraphs. */
    static byte[] createWithHeaderFooter(final String headerText, final String footerText, final String... bodyParagraphs) {
        final TestPackage pkg = new TestPackage();
        pkg.rel("/", TestPackage.REL_OFFICE_DOCUMENT, DOCUMENT);
        final String headerId = pkg.rel(DOCUMENT, REL + "header", "/word/header1.xml");
        final String footerId = pkg.rel(DOCUMENT, REL + "footer", "/word/footer1.xml");
        pkg.part("/word/header1.xml", CT_HEADER, "<w:hdr " + DOC_NAMESPACES + ">" + paraXml(headerText) + "</w:hdr>");
        pkg.part("/word/footer1.xml", CT_FOOTER, "<w:ftr " + DOC_NAMESPACES + ">" + paraXml(footerText) + "</w:ftr>");
        pkg.part(DOCUMENT, TestPackage.CT_DOCX_MAIN, documentXml(paras(bodyParagraphs)
                + "<w:sectPr><w:headerReference w:type=\"default\" r:id=\"" + headerId + "\"/>"
                + "<w:footerReference w:type=\"default\" r:id=\"" + footerId + "\"/></w:sectPr>"));
        return pkg.toBytes();
    }

    /** A .docx with a tracked deletion carrying {@code deletedText}, after optional visible paragraphs. */
    static byte[] createWithTrackedDeletion(final String deletedText, final String... bodyParagraphs) {
        final String deletion = "<w:p><w:del w:id=\"1\" w:author=\"Reviewer\" w:date=\"2024-01-01T00:00:00Z\">"
                + "<w:r><w:delText xml:space=\"preserve\">" + escape(deletedText) + "</w:delText></w:r></w:del></w:p>";
        return createRaw(paras(bodyParagraphs) + deletion);
    }

    /** A .docx with a simple field whose instruction is {@code instruction} and cached result {@code resultText}. */
    static byte[] createWithSimpleField(final String instruction, final String resultText, final String... bodyParagraphs) {
        return createRaw("<w:p><w:fldSimple w:instr=\"" + TestPackage.escapeAttribute(instruction) + "\">"
                + run(resultText) + "</w:fldSimple></w:p>" + paras(bodyParagraphs));
    }

    /** A .docx with a complex field: begin, instrText run(s), separate, result, end. */
    static byte[] createWithComplexField(final String[] instructionRuns, final String resultText, final String... bodyParagraphs) {
        final StringBuilder p = new StringBuilder("<w:p><w:r><w:fldChar w:fldCharType=\"begin\"/></w:r>");
        for (final String t : instructionRuns) {
            p.append("<w:r><w:instrText xml:space=\"preserve\">").append(escape(t)).append("</w:instrText></w:r>");
        }
        p.append("<w:r><w:fldChar w:fldCharType=\"separate\"/></w:r>").append(run(resultText))
                .append("<w:r><w:fldChar w:fldCharType=\"end\"/></w:r></w:p>");
        return createRaw(p + paras(bodyParagraphs));
    }

    /** A .docx whose first paragraph holds a hyperlink to the external {@code targetUri}. */
    static byte[] createWithHyperlink(final String displayText, final String targetUri, final String... bodyParagraphs) {
        return hyperlinkPackage(displayText, targetUri, bodyParagraphs).toBytes();
    }

    static TestPackage hyperlinkPackage(final String displayText, final String targetUri, final String... bodyParagraphs) {
        final TestPackage pkg = new TestPackage();
        pkg.rel("/", TestPackage.REL_OFFICE_DOCUMENT, DOCUMENT);
        final String id = pkg.externalRel(DOCUMENT, REL + "hyperlink", targetUri);
        pkg.part(DOCUMENT, TestPackage.CT_DOCX_MAIN, documentXml("<w:p><w:hyperlink r:id=\"" + id + "\">"
                + run(displayText) + "</w:hyperlink></w:p>" + paras(bodyParagraphs)));
        return pkg;
    }

    /** A .docx whose header holds a hyperlink to the external {@code targetUri}. */
    static byte[] createWithHyperlinkInHeader(final String displayText, final String targetUri, final String... bodyParagraphs) {
        final TestPackage pkg = new TestPackage();
        pkg.rel("/", TestPackage.REL_OFFICE_DOCUMENT, DOCUMENT);
        final String headerId = pkg.rel(DOCUMENT, REL + "header", "/word/header1.xml");
        final String linkId = pkg.externalRel("/word/header1.xml", REL + "hyperlink", targetUri);
        pkg.part("/word/header1.xml", CT_HEADER, "<w:hdr " + DOC_NAMESPACES + "><w:p><w:hyperlink r:id=\"" + linkId + "\">"
                + run(displayText) + "</w:hyperlink></w:p></w:hdr>");
        pkg.part(DOCUMENT, TestPackage.CT_DOCX_MAIN, documentXml(paras(bodyParagraphs)
                + "<w:sectPr><w:headerReference w:type=\"default\" r:id=\"" + headerId + "\"/></w:sectPr>"));
        return pkg.toBytes();
    }

    /** A .docx with one Word comment plus the given body paragraphs. */
    static byte[] createWithComment(final String commentText, final String... bodyParagraphs) {
        final TestPackage pkg = docx(paras(bodyParagraphs));
        pkg.rel(DOCUMENT, REL + "comments", "/word/comments.xml");
        pkg.part("/word/comments.xml", CT_COMMENTS, "<w:comments " + DOC_NAMESPACES + ">"
                + comment(1, "Reviewer", "R", commentText) + "</w:comments>");
        return pkg.toBytes();
    }

    /** A .docx with comments by the given authors ({author, text} pairs) and a matching word/people.xml. */
    static byte[] createWithAuthoredComments(final String[][] comments, final String... bodyParagraphs) {
        final TestPackage pkg = docx(paras(bodyParagraphs));
        final StringBuilder xml = new StringBuilder("<w:comments " + DOC_NAMESPACES + ">");
        final Set<String> authors = new LinkedHashSet<>();
        for (int i = 0; i < comments.length; i++) {
            final String author = comments[i][0];
            authors.add(author);
            xml.append(comment(i + 1, author, author.isEmpty() ? "X" : author.substring(0, 1), comments[i][1]));
        }
        xml.append("</w:comments>");
        pkg.rel(DOCUMENT, REL + "comments", "/word/comments.xml");
        pkg.part("/word/comments.xml", CT_COMMENTS, xml.toString());
        final StringBuilder people = new StringBuilder("<w15:people xmlns:w15=\"" + W15 + "\">");
        for (final String a : authors) {
            final String e = TestPackage.escapeAttribute(a);
            people.append("<w15:person w15:author=\"").append(e).append("\"><w15:presenceInfo w15:providerId=\"None\" w15:userId=\"")
                    .append(e).append("\"/></w15:person>");
        }
        people.append("</w15:people>");
        pkg.rel(DOCUMENT, REL_PEOPLE, "/word/people.xml");
        pkg.part("/word/people.xml", CT_PEOPLE, people.toString());
        return pkg.toBytes();
    }

    /** A .docx with a classic comment and a threaded-comment duplicate in word/threadedComments.xml. */
    static byte[] createWithThreadedComment(final String threadedText, final String classicText, final String... bodyParagraphs) {
        final TestPackage pkg = docx(paras(bodyParagraphs));
        pkg.rel(DOCUMENT, REL + "comments", "/word/comments.xml");
        pkg.part("/word/comments.xml", CT_COMMENTS, "<w:comments " + DOC_NAMESPACES + ">"
                + comment(1, "Reviewer", "R", classicText) + "</w:comments>");
        pkg.rel(DOCUMENT, REL_THREADED, "/word/threadedComments.xml");
        pkg.part("/word/threadedComments.xml", CT_THREADED, "<w15:threadedComments xmlns:w15=\"" + W15 + "\">"
                + "<w15:threadedComment w15:id=\"1\" w15:paraId=\"00000001\" w15:dateUtc=\"2024-01-01T00:00:00Z\">"
                + "<w15:text>" + escape(threadedText) + "</w15:text></w15:threadedComment></w15:threadedComments>");
        return pkg.toBytes();
    }

    /** A .docx modelling a comment thread: classic comments, commentsExtended, and threaded duplicates. */
    static byte[] createWithThreadedThread(final String[] messages, final String... bodyParagraphs) {
        final TestPackage pkg = docx(paras(bodyParagraphs));
        final StringBuilder comments = new StringBuilder("<w:comments " + DOC_NAMESPACES + ">");
        final StringBuilder ex = new StringBuilder("<w15:commentsEx xmlns:w15=\"" + W15 + "\">");
        final StringBuilder threads = new StringBuilder("<w15:threadedComments xmlns:w15=\"" + W15 + "\">");
        for (int i = 0; i < messages.length; i++) {
            final String paraId = String.format("%08d", i + 1);
            comments.append(comment(i + 1, "Reviewer", "R", messages[i]));
            ex.append("<w15:commentEx w15:paraId=\"").append(paraId).append("\" w15:done=\"0\"/>");
            threads.append("<w15:threadedComment w15:id=\"").append(i + 1).append("\" w15:paraId=\"").append(paraId)
                    .append("\" w15:dateUtc=\"2024-01-01T00:00:00Z\"").append(i == 0 ? "" : " w15:parentId=\"1\"")
                    .append("><w15:text>").append(escape(messages[i])).append("</w15:text></w15:threadedComment>");
        }
        pkg.rel(DOCUMENT, REL + "comments", "/word/comments.xml");
        pkg.part("/word/comments.xml", CT_COMMENTS, comments.append("</w:comments>").toString());
        pkg.rel(DOCUMENT, REL_COMMENTS_EX, "/word/commentsExtended.xml");
        pkg.part("/word/commentsExtended.xml", CT_COMMENTS_EX, ex.append("</w15:commentsEx>").toString());
        pkg.rel(DOCUMENT, REL_THREADED, "/word/threadedComments.xml");
        pkg.part("/word/threadedComments.xml", CT_THREADED, threads.append("</w15:threadedComments>").toString());
        return pkg.toBytes();
    }

    /** A .docx with an optional footnote and an optional endnote (null for none). */
    static byte[] createWithNotes(final String footnoteText, final String endnoteText, final String... bodyParagraphs) {
        return createWithMultipleNotes(footnoteText == null ? null : new String[]{footnoteText},
                endnoteText == null ? null : new String[]{endnoteText}, bodyParagraphs);
    }

    /** A .docx with several footnotes and/or endnotes, one paragraph each. */
    static byte[] createWithMultipleNotes(final String[] footnotes, final String[] endnotes, final String... bodyParagraphs) {
        final TestPackage pkg = docx(paras(bodyParagraphs));
        if (footnotes != null && footnotes.length > 0) {
            pkg.rel(DOCUMENT, REL + "footnotes", "/word/footnotes.xml");
            pkg.part("/word/footnotes.xml", CT_FOOTNOTES, notesXml("footnote", footnotes));
        }
        if (endnotes != null && endnotes.length > 0) {
            pkg.rel(DOCUMENT, REL + "endnotes", "/word/endnotes.xml");
            pkg.part("/word/endnotes.xml", CT_ENDNOTES, notesXml("endnote", endnotes));
        }
        return pkg.toBytes();
    }

    /**
     * A .docx whose endnote is {@code endnoteText}, after text the Word settings remove or skip, each holding
     * {@code text}: a hidden-text paragraph, a tracked deletion, a header and footer, and a footnote, all numbered
     * before the endnote. It also has a comment, which is numbered after it.
     */
    static byte[] createWithEverythingBeforeAnEndnote(final String text, final String endnoteText) {
        final TestPackage pkg = new TestPackage();
        pkg.rel("/", TestPackage.REL_OFFICE_DOCUMENT, DOCUMENT);
        final String headerId = pkg.rel(DOCUMENT, REL + "header", "/word/header1.xml");
        final String footerId = pkg.rel(DOCUMENT, REL + "footer", "/word/footer1.xml");
        pkg.part("/word/header1.xml", CT_HEADER, "<w:hdr " + DOC_NAMESPACES + ">" + paraXml("Head " + text) + "</w:hdr>");
        pkg.part("/word/footer1.xml", CT_FOOTER, "<w:ftr " + DOC_NAMESPACES + ">" + paraXml("Foot " + text) + "</w:ftr>");
        pkg.rel(DOCUMENT, REL + "comments", "/word/comments.xml");
        pkg.part("/word/comments.xml", CT_COMMENTS, "<w:comments " + DOC_NAMESPACES + ">"
                + comment(1, "Reviewer", "R", "Ask " + text) + "</w:comments>");
        pkg.rel(DOCUMENT, REL + "footnotes", "/word/footnotes.xml");
        pkg.part("/word/footnotes.xml", CT_FOOTNOTES, notesXml("footnote", new String[]{"Note " + text}));
        pkg.rel(DOCUMENT, REL + "endnotes", "/word/endnotes.xml");
        pkg.part("/word/endnotes.xml", CT_ENDNOTES, notesXml("endnote", new String[]{endnoteText}));
        pkg.part(DOCUMENT, TestPackage.CT_DOCX_MAIN, documentXml(
                "<w:p><w:r><w:rPr><w:vanish/></w:rPr><w:t xml:space=\"preserve\">Hidden " + escape(text) + "</w:t></w:r></w:p>"
                + "<w:p><w:del w:id=\"2\" w:author=\"Reviewer\" w:date=\"2024-01-01T00:00:00Z\"><w:r>"
                + "<w:delText xml:space=\"preserve\">Gone " + escape(text) + "</w:delText></w:r></w:del></w:p>"
                + paraXml("Body.")
                + "<w:sectPr><w:headerReference w:type=\"default\" r:id=\"" + headerId + "\"/>"
                + "<w:footerReference w:type=\"default\" r:id=\"" + footerId + "\"/></w:sectPr>"));
        return pkg.toBytes();
    }

    /** A .docx whose footnote #1 is the supplied raw WordprocessingML. */
    static byte[] createWithRawFootnote(final String footnoteInnerXml, final String... bodyParagraphs) {
        final TestPackage pkg = docx(paras(bodyParagraphs));
        pkg.rel(DOCUMENT, REL + "footnotes", "/word/footnotes.xml");
        pkg.part("/word/footnotes.xml", CT_FOOTNOTES, "<w:footnotes " + DOC_NAMESPACES + ">"
                + "<w:footnote w:type=\"separator\" w:id=\"-1\"><w:p/></w:footnote>"
                + "<w:footnote w:id=\"1\">" + footnoteInnerXml + "</w:footnote></w:footnotes>");
        return pkg.toBytes();
    }

    private static String notesXml(final String kind, final String[] notes) {
        final StringBuilder xml = new StringBuilder("<w:" + kind + "s " + DOC_NAMESPACES + ">");
        xml.append("<w:").append(kind).append(" w:type=\"separator\" w:id=\"-1\"><w:p/></w:").append(kind).append(">");
        for (int i = 0; i < notes.length; i++) {
            xml.append("<w:").append(kind).append(" w:id=\"").append(i + 1).append("\">").append(paraXml(notes[i]))
                    .append("</w:").append(kind).append(">");
        }
        return xml.append("</w:").append(kind).append("s>").toString();
    }

    private static String comment(final int id, final String author, final String initials, final String text) {
        return "<w:comment w:id=\"" + id + "\" w:author=\"" + TestPackage.escapeAttribute(author) + "\" w:initials=\""
                + TestPackage.escapeAttribute(initials) + "\">" + paraXml(text) + "</w:comment>";
    }

    // --- DrawingML: charts, SmartArt ----------------------------------------------------------

    /** A DrawingML paragraph with one run per string. */
    static String aParagraph(final String... runs) {
        final StringBuilder sb = new StringBuilder("<a:p>");
        for (final String r : runs) {
            sb.append("<a:r><a:t xml:space=\"preserve\">").append(escape(r)).append("</a:t></a:r>");
        }
        return sb.append("</a:p>").toString();
    }

    /** A .docx with a chart part whose title rich text is the supplied {@code <a:p>} XML. */
    static byte[] createWithChart(final String titleAParagraphXml, final String... bodyParagraphs) {
        final TestPackage pkg = docx(paras(bodyParagraphs));
        pkg.rel(DOCUMENT, REL + "chart", "/word/charts/chart1.xml");
        pkg.part("/word/charts/chart1.xml", CT_CHART,
                "<c:chartSpace xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" "
                        + "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">"
                        + "<c:chart><c:title><c:tx><c:rich><a:bodyPr/><a:lstStyle/>" + titleAParagraphXml + "</c:rich></c:tx></c:title>"
                        + "<c:plotArea><c:layout/></c:plotArea></c:chart></c:chartSpace>");
        return pkg.toBytes();
    }

    /** The diagram data XML of a SmartArt part whose one node holds the supplied {@code <a:p>} XML. */
    static String smartArtXml(final String nodeAParagraphXml) {
        return "<dgm:dataModel xmlns:dgm=\"http://schemas.openxmlformats.org/drawingml/2006/diagram\" "
                + "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">"
                + "<dgm:ptLst><dgm:pt dgm:modelId=\"1\"><dgm:t><a:bodyPr/><a:lstStyle/>" + nodeAParagraphXml
                + "</dgm:t></dgm:pt></dgm:ptLst></dgm:dataModel>";
    }

    /** Adds a SmartArt data part to a .docx package. */
    static void addSmartArt(final TestPackage pkg, final String nodeAParagraphXml) {
        pkg.rel(DOCUMENT, REL + "diagramData", "/word/diagrams/data1.xml");
        pkg.part("/word/diagrams/data1.xml", CT_DIAGRAM_DATA, smartArtXml(nodeAParagraphXml));
    }

    /** A .docx with a SmartArt data part whose first node text is the supplied {@code <a:p>} XML. */
    static byte[] createWithSmartArt(final String nodeAParagraphXml, final String... bodyParagraphs) {
        final TestPackage pkg = docx(paras(bodyParagraphs));
        addSmartArt(pkg, nodeAParagraphXml);
        return pkg.toBytes();
    }

    private static String chartDrawingParagraph(final String relId) {
        return "<w:p><w:r><w:drawing>"
                + "<wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\">"
                + "<wp:extent cx=\"5000000\" cy=\"3000000\"/>"
                + "<wp:docPr id=\"1\" name=\"Chart 1\"/>"
                + "<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/chart\">"
                + "<c:chart xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" r:id=\"" + relId + "\"/>"
                + "</a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>";
    }

    /** A .docx embedding a full chart (title, series name, category, value) referenced from the body. */
    static byte[] createWithChartData(final String title, final String seriesName, final String category,
                                      final String... bodyParagraphs) {
        final TestPackage pkg = new TestPackage();
        pkg.rel("/", TestPackage.REL_OFFICE_DOCUMENT, DOCUMENT);
        final String relId = pkg.rel(DOCUMENT, REL + "chart", "/word/charts/chart1.xml");
        pkg.part("/word/charts/chart1.xml", CT_CHART, SpreadsheetDocs.chartSpaceXml(title, seriesName, category));
        pkg.part(DOCUMENT, TestPackage.CT_DOCX_MAIN, documentXml(paras(bodyParagraphs) + chartDrawingParagraph(relId)));
        return pkg.toBytes();
    }

    /** A .docx with a native chart whose embedded source workbook is {@code workbookBytes}. */
    static byte[] createWithChartEmbeddedWorkbook(final byte[] workbookBytes, final String seriesName, final String category) {
        final TestPackage pkg = new TestPackage();
        pkg.rel("/", TestPackage.REL_OFFICE_DOCUMENT, DOCUMENT);
        final String chart = "/word/charts/chart1.xml";
        final String relId = pkg.rel(DOCUMENT, REL + "chart", chart);
        pkg.rel(chart, "rIdEmb", REL + "package", "/word/embeddings/Microsoft_Excel_Worksheet1.xlsx");
        pkg.part("/word/embeddings/Microsoft_Excel_Worksheet1.xlsx", TestPackage.CT_XLSX, workbookBytes);
        pkg.part(chart, CT_CHART,
                "<c:chartSpace xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" "
                        + "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" "
                        + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                        + "<c:chart><c:plotArea><c:layout/><c:barChart><c:barDir val=\"col\"/><c:grouping val=\"clustered\"/>"
                        + "<c:ser><c:idx val=\"0\"/><c:order val=\"0\"/>"
                        + "<c:tx><c:strRef><c:f>Sheet1!$B$1</c:f><c:strCache><c:ptCount val=\"1\"/><c:pt idx=\"0\"><c:v>" + escape(seriesName) + "</c:v></c:pt></c:strCache></c:strRef></c:tx>"
                        + "<c:cat><c:strRef><c:f>Sheet1!$A$2</c:f><c:strCache><c:ptCount val=\"1\"/><c:pt idx=\"0\"><c:v>" + escape(category) + "</c:v></c:pt></c:strCache></c:strRef></c:cat>"
                        + "<c:val><c:numRef><c:f>Sheet1!$B$2</c:f><c:numCache><c:ptCount val=\"1\"/><c:pt idx=\"0\"><c:v>42</c:v></c:pt></c:numCache></c:numRef></c:val>"
                        + "</c:ser><c:axId val=\"111111111\"/><c:axId val=\"222222222\"/></c:barChart>"
                        + "<c:catAx><c:axId val=\"111111111\"/><c:scaling><c:orientation val=\"minMax\"/></c:scaling><c:delete val=\"0\"/>"
                        + "<c:axPos val=\"b\"/><c:crossAx val=\"222222222\"/></c:catAx>"
                        + "<c:valAx><c:axId val=\"222222222\"/><c:scaling><c:orientation val=\"minMax\"/></c:scaling><c:delete val=\"0\"/>"
                        + "<c:axPos val=\"l\"/><c:crossAx val=\"111111111\"/></c:valAx>"
                        + "</c:plotArea><c:plotVisOnly val=\"1\"/></c:chart>"
                        + "<c:externalData r:id=\"rIdEmb\"><c:autoUpdate val=\"0\"/></c:externalData></c:chartSpace>");
        pkg.part(DOCUMENT, TestPackage.CT_DOCX_MAIN, documentXml(chartDrawingParagraph(relId)));
        return pkg.toBytes();
    }

    /** A .docx with an embedded package of the given content type. */
    static byte[] createWithEmbeddedPackage(final byte[] bytes, final String contentType, final String extension,
                                            final String... bodyParagraphs) {
        final TestPackage pkg = docx(paras(bodyParagraphs));
        final String name = "/word/embeddings/package1." + extension;
        pkg.rel(DOCUMENT, "rIdEmb", REL + "package", name);
        pkg.part(name, contentType, bytes);
        return pkg.toBytes();
    }

    /** A .docx with an embedded object the redactor cannot inspect (an embedded PowerPoint package). */
    static byte[] createWithEmbeddedOleObject(final byte[] bytes, final String... bodyParagraphs) {
        return createWithEmbeddedPackage(bytes, CT_PPTX, "pptx", bodyParagraphs);
    }

    // --- raw XML snippets ----------------------------------------------------------------------

    static String paraXml(final String text) {
        return "<w:p>" + run(text) + "</w:p>";
    }

    static String paras(final String... paragraphs) {
        final StringBuilder sb = new StringBuilder();
        for (final String p : paragraphs) {
            sb.append(paraXml(p));
        }
        return sb.toString();
    }

    static String run(final String text) {
        return "<w:r>" + t(text) + "</w:r>";
    }

    static String t(final String text) {
        return "<w:t xml:space=\"preserve\">" + escape(text) + "</w:t>";
    }

    /** Drawing ids, unique within any document the fixtures build. */
    private static final java.util.concurrent.atomic.AtomicInteger DRAWING_IDS = new java.util.concurrent.atomic.AtomicInteger(100);

    /** A paragraph anchoring a DrawingML text box holding {@code boxText}, with optional text around it. */
    static String textBoxParaXml(final String boxText, final String before, final String after) {
        return "<w:p>" + (before == null ? "" : run(before))
                + "<w:r><w:drawing><wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\">"
                + "<wp:extent cx=\"2000000\" cy=\"500000\"/><wp:docPr id=\"" + DRAWING_IDS.incrementAndGet() + "\" name=\"Text Box\"/>"
                + "<a:graphic><a:graphicData uri=\"http://schemas.microsoft.com/office/word/2010/wordprocessingShape\">"
                + "<wps:wsp><wps:cNvSpPr txBox=\"1\"/><wps:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"2000000\" cy=\"500000\"/></a:xfrm>"
                + "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></wps:spPr>"
                + "<wps:txbx><w:txbxContent>" + paraXml(boxText)
                + "</w:txbxContent></wps:txbx><wps:bodyPr/></wps:wsp>"
                + "</a:graphicData></a:graphic></wp:inline></w:drawing></w:r>"
                + (after == null ? "" : run(after)) + "</w:p>";
    }

    /** A paragraph anchoring a legacy VML text box holding {@code boxText}. */
    static String vmlTextBoxParaXml(final String boxText) {
        return "<w:p><w:r><w:pict><v:shape id=\"TextBox" + DRAWING_IDS.incrementAndGet()
                + "\" style=\"width:200pt;height:50pt\" coordsize=\"21600,21600\" path=\"m,l,21600r21600,l21600,xe\">"
                + "<v:textbox><w:txbxContent>" + paraXml(boxText)
                + "</w:txbxContent></v:textbox></v:shape></w:pict></w:r></w:p>";
    }

    /** A paragraph holding a non-text drawing (a shape) and an optional caption. */
    static String drawingParaXml(final String caption) {
        return "<w:p>" + (caption == null ? "" : run(caption))
                + "<w:r><w:drawing><wp:inline><a:graphic><a:graphicData "
                + "uri=\"http://schemas.microsoft.com/office/word/2010/wordprocessingShape\">"
                + "<wps:wsp><wps:bodyPr/></wps:wsp>"
                + "</a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>";
    }

    // --- Excel fixtures the Word-area tests need ---------------------------------------------

    /** A single-sheet workbook with an embedded chart whose chart part is {@code chartSpaceXml}. */
    static byte[] createXlsxWithChartXml(final List<String[]> rows, final String chartSpaceXml) {
        final SpreadsheetDocs.Workbook wb = new SpreadsheetDocs.Workbook(rows, "Sheet1");
        final String drawing = "/xl/drawings/drawing1.xml";
        wb.pkg.rel(wb.sheetPart, "rIdDrawing1", REL + "drawing", drawing);
        wb.pkg.rel(drawing, "rIdChart1", REL + "chart", "/xl/charts/chart1.xml");
        wb.pkg.part("/xl/charts/chart1.xml", CT_CHART, chartSpaceXml);
        wb.pkg.part(drawing, CT_DRAWING,
                "<xdr:wsDr xmlns:xdr=\"http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing\" "
                        + "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" "
                        + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" "
                        + "xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\">"
                        + "<xdr:twoCellAnchor>"
                        + "<xdr:from><xdr:col>3</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>1</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:from>"
                        + "<xdr:to><xdr:col>9</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>15</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:to>"
                        + "<xdr:graphicFrame macro=\"\"><xdr:nvGraphicFramePr><xdr:cNvPr id=\"2\" name=\"Chart 1\"/><xdr:cNvGraphicFramePr/></xdr:nvGraphicFramePr>"
                        + "<xdr:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/></xdr:xfrm>"
                        + "<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/chart\"><c:chart r:id=\"rIdChart1\"/></a:graphicData></a:graphic>"
                        + "</xdr:graphicFrame><xdr:clientData/></xdr:twoCellAnchor></xdr:wsDr>");
        wb.afterSheetData.append("<drawing r:id=\"rIdDrawing1\"/>");
        return wb.toBytes();
    }

    static byte[] createXlsxWithChart(final List<String[]> rows, final String title, final String seriesName, final String category) {
        return createXlsxWithChartXml(rows, SpreadsheetDocs.chartSpaceXml(title, seriesName, category));
    }

    /** A single-sheet workbook with a text box whose one paragraph is the supplied {@code <a:p>} XML. */
    static byte[] createXlsxWithTextBoxParagraph(final List<String[]> rows, final String aParagraphXml) {
        final SpreadsheetDocs.Workbook wb = new SpreadsheetDocs.Workbook(rows, "Sheet1");
        final String drawing = "/xl/drawings/drawing1.xml";
        wb.pkg.rel(wb.sheetPart, "rIdDrawing1", REL + "drawing", drawing);
        wb.pkg.part(drawing, CT_DRAWING,
                "<xdr:wsDr xmlns:xdr=\"http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing\" "
                        + "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">"
                        + "<xdr:twoCellAnchor>"
                        + "<xdr:from><xdr:col>1</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>1</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:from>"
                        + "<xdr:to><xdr:col>5</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>6</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:to>"
                        + "<xdr:sp macro=\"\" textlink=\"\">"
                        + "<xdr:nvSpPr><xdr:cNvPr id=\"2\" name=\"TextBox 1\"/><xdr:cNvSpPr txBox=\"1\"/></xdr:nvSpPr>"
                        + "<xdr:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"1000000\" cy=\"500000\"/></a:xfrm>"
                        + "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></xdr:spPr>"
                        + "<xdr:txBody><a:bodyPr/><a:lstStyle/>" + aParagraphXml + "</xdr:txBody>"
                        + "</xdr:sp><xdr:clientData/></xdr:twoCellAnchor></xdr:wsDr>");
        wb.afterSheetData.append("<drawing r:id=\"rIdDrawing1\"/>");
        return wb.toBytes();
    }

    /** The concatenated XML of every chart part in a workbook. */
    static String allChartXml(final byte[] xlsx) {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, byte[]> e : TestPackage.entries(xlsx).entrySet()) {
            if (e.getKey().startsWith("xl/charts/") && e.getKey().endsWith(".xml")) {
                sb.append(new String(e.getValue(), StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }

    // --- reading ---------------------------------------------------------------------------------

    /** The text of each paragraph directly in the body, in order. */
    static List<String> bodyParagraphs(final byte[] docx) {
        final List<String> out = new ArrayList<>();
        final Element body = body(docx);
        for (Node n = body.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && W.equals(e.getNamespaceURI()) && "p".equals(e.getLocalName())) {
                out.add(e.getTextContent());
            }
        }
        return out;
    }

    static Element body(final byte[] docx) {
        return (Element) TestPackage.dom(docx, DOCUMENT_ENTRY).getElementsByTagNameNS(W, "body").item(0);
    }

    /** All body text concatenated and trimmed. */
    static String allBodyText(final byte[] docx) {
        return String.join("", bodyParagraphs(docx)).trim();
    }

    static String headersText(final byte[] docx) {
        return partsParagraphText(docx, "word/header");
    }

    static String footersText(final byte[] docx) {
        return partsParagraphText(docx, "word/footer");
    }

    private static String partsParagraphText(final byte[] docx, final String prefix) {
        final StringBuilder sb = new StringBuilder();
        for (final String name : TestPackage.entries(docx).keySet()) {
            if (name.startsWith(prefix) && name.endsWith(".xml") && !name.contains("/_rels/")) {
                TestPackage.texts(docx, name, W, "p").forEach(sb::append);
            }
        }
        return sb.toString();
    }

    /** The text of every tracked deletion in the body, in order. */
    static List<String> deletedTexts(final byte[] docx) {
        return TestPackage.texts(docx, DOCUMENT_ENTRY, W, "delText");
    }

    /** The text of each Word comment, in order. */
    static List<String> commentTexts(final byte[] docx) {
        return TestPackage.texts(docx, "word/comments.xml", W, "comment");
    }

    static List<String> footnoteTexts(final byte[] docx) {
        return nonEmpty(TestPackage.texts(docx, "word/footnotes.xml", W, "footnote"));
    }

    static List<String> endnoteTexts(final byte[] docx) {
        return nonEmpty(TestPackage.texts(docx, "word/endnotes.xml", W, "endnote"));
    }

    private static List<String> nonEmpty(final List<String> values) {
        return values.stream().filter(v -> !v.isEmpty()).toList();
    }

    /** The full raw document.xml. */
    static String documentXml(final byte[] docx) {
        return TestPackage.xml(docx, DOCUMENT_ENTRY);
    }

    static int occurrences(final byte[] docx, final String value) {
        return TestPackage.occurrences(documentXml(docx), value);
    }

    /** True if any entry in the package contains {@code value}. */
    static boolean anyPartContains(final byte[] pkg, final String value) {
        for (final byte[] bytes : TestPackage.entries(pkg).values()) {
            if (new String(bytes, StandardCharsets.UTF_8).contains(value)) {
                return true;
            }
        }
        return false;
    }

    /** Every external hyperlink relationship target in the package. */
    static List<String> hyperlinkTargets(final byte[] pkg) {
        final List<String> targets = new ArrayList<>();
        for (final String name : TestPackage.entries(pkg).keySet()) {
            if (!name.endsWith(".rels")) {
                continue;
            }
            for (final Element rel : TestPackage.elements(pkg, name, PKG_RELS, "Relationship")) {
                if ((REL + "hyperlink").equals(rel.getAttribute("Type")) && "External".equals(rel.getAttribute("TargetMode"))) {
                    targets.add(rel.getAttribute("Target"));
                }
            }
        }
        return targets;
    }

    /** True if every w:hyperlink r:id in every part resolves to a hyperlink relationship of that part. */
    static boolean hyperlinkIdsAllResolve(final byte[] pkg) {
        final Map<String, byte[]> entries = TestPackage.entries(pkg);
        for (final String name : entries.keySet()) {
            if (!name.endsWith(".xml") || name.contains("_rels/") || name.equals("[Content_Types].xml")) {
                continue;
            }
            final List<Element> links = TestPackage.elements(pkg, name, W, "hyperlink");
            if (links.isEmpty()) {
                continue;
            }
            final Set<String> ids = new LinkedHashSet<>();
            final int slash = name.lastIndexOf('/');
            final String relsName = name.substring(0, slash + 1) + "_rels/" + name.substring(slash + 1) + ".rels";
            for (final Element rel : TestPackage.elements(pkg, relsName, PKG_RELS, "Relationship")) {
                if ((REL + "hyperlink").equals(rel.getAttribute("Type"))) {
                    ids.add(rel.getAttribute("Id"));
                }
            }
            for (final Element link : links) {
                final String id = link.getAttributeNS(R, "id");
                if (!id.isEmpty() && !ids.contains(id)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** All DrawingML text ({@code <a:t>}) across every XML part, each followed by a newline. */
    static String allDrawingText(final byte[] pkg) {
        final StringBuilder sb = new StringBuilder();
        for (final String name : TestPackage.entries(pkg).keySet()) {
            if (name.endsWith(".xml") && !name.equals("[Content_Types].xml")) {
                for (final String text : TestPackage.texts(pkg, name, A, "t")) {
                    sb.append(text).append('\n');
                }
            }
        }
        return sb.toString();
    }

    // The first chart part's relationship to its embedded package, as an entry name, or null.
    private static String chartEmbeddedEntry(final byte[] docx) {
        final Map<String, byte[]> entries = TestPackage.entries(docx);
        for (final String name : entries.keySet()) {
            if (!name.startsWith("word/charts/_rels/") || !name.endsWith(".rels")) {
                continue;
            }
            for (final Element rel : TestPackage.elements(docx, name, PKG_RELS, "Relationship")) {
                if ((REL + "package").equals(rel.getAttribute("Type"))) {
                    final String target = rel.getAttribute("Target");
                    final String entry = target.startsWith("/") ? target.substring(1)
                            : java.net.URI.create("word/charts/").resolve(target).normalize().toString();
                    return entries.containsKey(entry) ? entry : null;
                }
            }
        }
        return null;
    }

    /** True if the chart part still has an embedded package (its source workbook). */
    static boolean hasChartEmbeddedWorkbook(final byte[] docx) {
        return chartEmbeddedEntry(docx) != null;
    }

    /** All cell text of the embedded workbook behind the chart part, or empty when there is none. */
    static String embeddedChartWorkbookAllText(final byte[] docx) {
        final String entry = chartEmbeddedEntry(docx);
        return entry == null ? "" : SpreadsheetDocs.allText(TestPackage.entries(docx).get(entry));
    }

    // --- DrawingML paragraphs ---------------------------------------------------------------------

    /** A DrawingML paragraph: each string is a run, and "\n" is an {@code <a:br/>}. */
    static String drawingParagraphXml(final String... parts) {
        final StringBuilder sb = new StringBuilder("<a:p>");
        for (final String part : parts) {
            sb.append("\n".equals(part) ? "<a:br/>" : "<a:r><a:t>" + escape(part) + "</a:t></a:r>");
        }
        return sb.append("</a:p>").toString();
    }

    /** A DrawingML paragraph as text: each run's text and each break as "\n", joined with "|". */
    static String renderDrawingParagraph(final Element paragraph) {
        final List<String> parts = new ArrayList<>();
        for (Node n = paragraph.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && A.equals(e.getNamespaceURI())) {
                if ("r".equals(e.getLocalName())) {
                    final StringBuilder sb = new StringBuilder();
                    final var texts = e.getElementsByTagNameNS(A, "t");
                    for (int i = 0; i < texts.getLength(); i++) {
                        sb.append(texts.item(i).getTextContent());
                    }
                    parts.add(sb.toString());
                } else if ("br".equals(e.getLocalName())) {
                    parts.add("\n");
                }
            }
        }
        return String.join("|", parts);
    }

    /** Every DrawingML paragraph holding text, in every XML part of the package, rendered. */
    static List<String> drawingParagraphs(final byte[] pkg) {
        final List<String> out = new ArrayList<>();
        for (final String name : TestPackage.entries(pkg).keySet()) {
            if (!name.endsWith(".xml") || name.equals("[Content_Types].xml")) {
                continue;
            }
            for (final Element p : TestPackage.elements(pkg, name, A, "p")) {
                if (p.getElementsByTagNameNS(A, "t").getLength() > 0) {
                    out.add(renderDrawingParagraph(p));
                }
            }
        }
        return out;
    }

    /** Parses XML into a namespace-aware DOM. */
    static Document parse(final String xml) {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
