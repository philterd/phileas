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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static ai.philterd.phileas.services.ooxml.TestPackage.REL;
import static ai.philterd.phileas.services.ooxml.TestPackage.escape;

/** Fixtures for tests outside this package, such as the filter service's. */
public final class OoxmlTestFixtures {

    private static final String DOCUMENT = "/word/document.xml";

    /** A .docx with one body paragraph per string. */
    public static byte[] docx(final String... paragraphs) {
        return docxPackage(paragraphs).toBytes();
    }

    /** A .docx with a default header and the given body paragraph. */
    public static byte[] docxWithHeader(final String header, final String paragraph) {
        final TestPackage pkg = new TestPackage();
        pkg.rel("/", TestPackage.REL_OFFICE_DOCUMENT, DOCUMENT);
        final String headerId = pkg.rel(DOCUMENT, REL + "header", "/word/header1.xml");
        pkg.part(DOCUMENT, TestPackage.CT_DOCX_MAIN, document(para(paragraph)
                + "<w:sectPr><w:headerReference w:type=\"default\" r:id=\"" + headerId + "\"/></w:sectPr>"));
        pkg.part("/word/header1.xml", "application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml",
                "<w:hdr xmlns:w=\"" + TestPackage.W + "\">" + para(header) + "</w:hdr>");
        return pkg.toBytes();
    }

    /** A .docx whose content types give .xml parts the main document's type by default (no override). */
    public static byte[] docxWithMainPartDefaultContentType(final String paragraph) {
        final TestPackage pkg = new TestPackage().defaultXmlContentType(TestPackage.CT_DOCX_MAIN);
        pkg.rel("/", TestPackage.REL_OFFICE_DOCUMENT, DOCUMENT);
        // No override for the main part: its type comes from the Default element.
        return withoutOverride(pkg, document(para(paragraph)));
    }

    private static byte[] withoutOverride(final TestPackage pkg, final String documentXml) {
        pkg.part(DOCUMENT, TestPackage.CT_DOCX_MAIN, documentXml);
        final byte[] bytes = pkg.toBytes();
        // Rewrite [Content_Types].xml without the main part's override.
        final Map<String, byte[]> entries = TestPackage.entries(bytes);
        final String types = new String(entries.get("[Content_Types].xml"), StandardCharsets.UTF_8)
                .replaceAll("<Override PartName=\"/word/document.xml\"[^>]*/>", "");
        entries.put("[Content_Types].xml", types.getBytes(StandardCharsets.UTF_8));
        return zip(entries);
    }

    /** A .docx with one paragraph holding a hyperlink to an external target. */
    public static byte[] docxWithHyperlink(final String displayText, final String target) {
        final TestPackage pkg = new TestPackage();
        pkg.rel("/", TestPackage.REL_OFFICE_DOCUMENT, DOCUMENT);
        final String id = pkg.externalRel(DOCUMENT, REL + "hyperlink", target);
        pkg.part(DOCUMENT, TestPackage.CT_DOCX_MAIN, document("<w:p><w:hyperlink r:id=\"" + id + "\"><w:r><w:t xml:space=\"preserve\">"
                + escape(displayText) + "</w:t></w:r></w:hyperlink></w:p>"));
        return pkg.toBytes();
    }

    /** A .docx with one paragraph and an embedded Excel workbook (Insert, Object). */
    public static byte[] docxWithEmbeddedXlsx(final String paragraph, final byte[] workbook) {
        final TestPackage pkg = docxPackage(paragraph);
        pkg.rel(DOCUMENT, REL + "package", "/word/embeddings/Microsoft_Excel_Worksheet.xlsx");
        pkg.part("/word/embeddings/Microsoft_Excel_Worksheet.xlsx", TestPackage.CT_XLSX, workbook);
        return pkg.toBytes();
    }

    /**
     * A .docx in the Strict Open XML format's shape: its main part is reached through the Strict
     * officeDocument relationship type.
     */
    public static byte[] strictDocx(final String paragraph) {
        final TestPackage pkg = new TestPackage();
        pkg.rel("/", "http://purl.oclc.org/ooxml/officeDocument/relationships/officeDocument", DOCUMENT);
        pkg.part(DOCUMENT, TestPackage.CT_DOCX_MAIN, document(para(paragraph)));
        return pkg.toBytes();
    }

    /** A single-sheet .xlsx. */
    public static byte[] xlsx(final List<String[]> rows) {
        return SpreadsheetDocs.createXlsx(rows);
    }

    /** A zip holding one text file, which is not an Office document. */
    public static byte[] zip(final String name, final String content) {
        return zip(Map.of(name, content.getBytes(StandardCharsets.UTF_8)));
    }

    /** A filter result that changes nothing. */
    public static ai.philterd.phileas.model.filtering.TextFilterResult unchanged(final String text) {
        return TestFilters.result(text, List.of());
    }

    public static Map<String, byte[]> entries(final byte[] pkg) {
        return TestPackage.entries(pkg);
    }

    public static String entry(final byte[] pkg, final String name) {
        return TestPackage.xml(pkg, name);
    }

    public static String allXml(final byte[] pkg) {
        return TestPackage.allXml(pkg);
    }

    // --- builders ---------------------------------------------------------------------------

    static TestPackage docxPackage(final String... paragraphs) {
        final TestPackage pkg = new TestPackage();
        pkg.rel("/", TestPackage.REL_OFFICE_DOCUMENT, DOCUMENT);
        final StringBuilder body = new StringBuilder();
        for (final String p : paragraphs) {
            body.append(para(p));
        }
        pkg.part(DOCUMENT, TestPackage.CT_DOCX_MAIN, document(body.toString()));
        return pkg;
    }

    static String document(final String bodyInnerXml) {
        return "<w:document xmlns:w=\"" + TestPackage.W + "\" xmlns:r=\"" + TestPackage.R + "\"><w:body>"
                + bodyInnerXml + "</w:body></w:document>";
    }

    static String para(final String text) {
        return "<w:p><w:r><w:t xml:space=\"preserve\">" + escape(text) + "</w:t></w:r></w:p>";
    }

    private static byte[] zip(final Map<String, byte[]> entries) {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            for (final Map.Entry<String, byte[]> e : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey()));
                zip.write(e.getValue());
                zip.closeEntry();
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return buffer.toByteArray();
    }

    private OoxmlTestFixtures() {
    }

}
