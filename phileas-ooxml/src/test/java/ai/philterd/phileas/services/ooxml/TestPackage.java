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
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Builds Office Open XML packages for tests from raw XML, and reads parts back out of them. The
 * equivalent of the Open XML SDK calls phileas-dotnet's test fixtures make.
 */
final class TestPackage {

    static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    static final String A = "http://schemas.openxmlformats.org/drawingml/2006/main";
    static final String C = "http://schemas.openxmlformats.org/drawingml/2006/chart";
    static final String S = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    static final String R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    static final String PKG_RELS = "http://schemas.openxmlformats.org/package/2006/relationships";

    static final String REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/";
    static final String REL_OFFICE_DOCUMENT = REL + "officeDocument";

    static final String CT_RELS = "application/vnd.openxmlformats-package.relationships+xml";
    static final String CT_DOCX_MAIN = "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml";
    static final String CT_XLSX_MAIN = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml";
    static final String CT_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    static final String CT_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final Map<String, String> contentTypes = new LinkedHashMap<>();
    private final Map<String, byte[]> parts = new LinkedHashMap<>();
    private final Map<String, List<String[]>> rels = new LinkedHashMap<>();
    private int nextId = 1;
    private String defaultXmlContentType = "application/xml";

    /** Sets the content type the package's Default element gives every .xml part without an override. */
    TestPackage defaultXmlContentType(final String contentType) {
        this.defaultXmlContentType = contentType;
        return this;
    }

    /** Adds a part, such as {@code part("/word/document.xml", CT_DOCX_MAIN, xml)}. */
    TestPackage part(final String partName, final String contentType, final String xml) {
        return part(partName, contentType, xml.getBytes(StandardCharsets.UTF_8));
    }

    TestPackage part(final String partName, final String contentType, final byte[] bytes) {
        contentTypes.put(partName, contentType);
        parts.put(partName, bytes);
        return this;
    }

    /**
     * Adds an internal relationship from {@code source} ("/" for the package itself) to the absolute part
     * name {@code target}, and returns its generated id.
     */
    String rel(final String source, final String type, final String target) {
        final String id = "rId" + (nextId++);
        rel(source, id, type, target);
        return id;
    }

    TestPackage rel(final String source, final String id, final String type, final String target) {
        rels.computeIfAbsent(source, k -> new ArrayList<>()).add(new String[]{id, type, target, null});
        return this;
    }

    /** Adds an external relationship (for example a hyperlink) and returns its generated id. */
    String externalRel(final String source, final String type, final String target) {
        final String id = "rId" + (nextId++);
        rels.computeIfAbsent(source, k -> new ArrayList<>()).add(new String[]{id, type, target, "External"});
        return id;
    }

    byte[] toBytes() {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            final StringBuilder types = new StringBuilder(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                            + "<Default Extension=\"rels\" ContentType=\"" + CT_RELS + "\"/>"
                            + "<Default Extension=\"xml\" ContentType=\"" + defaultXmlContentType + "\"/>");
            for (final Map.Entry<String, String> e : contentTypes.entrySet()) {
                types.append("<Override PartName=\"").append(e.getKey()).append("\" ContentType=\"")
                        .append(e.getValue()).append("\"/>");
            }
            types.append("</Types>");
            write(zip, "[Content_Types].xml", types.toString().getBytes(StandardCharsets.UTF_8));
            for (final Map.Entry<String, List<String[]>> e : rels.entrySet()) {
                final StringBuilder xml = new StringBuilder(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"" + PKG_RELS + "\">");
                for (final String[] r : e.getValue()) {
                    xml.append("<Relationship Id=\"").append(r[0]).append("\" Type=\"").append(r[1])
                            .append("\" Target=\"").append(escapeAttribute(r[2])).append("\"");
                    if (r[3] != null) {
                        xml.append(" TargetMode=\"").append(r[3]).append("\"");
                    }
                    xml.append("/>");
                }
                xml.append("</Relationships>");
                write(zip, relsEntry(e.getKey()), xml.toString().getBytes(StandardCharsets.UTF_8));
            }
            for (final Map.Entry<String, byte[]> e : parts.entrySet()) {
                write(zip, e.getKey().substring(1), e.getValue());
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return buffer.toByteArray();
    }

    private static String relsEntry(final String source) {
        if ("/".equals(source)) {
            return "_rels/.rels";
        }
        final int slash = source.lastIndexOf('/');
        return source.substring(1, slash + 1) + "_rels/" + source.substring(slash + 1) + ".rels";
    }

    private static void write(final ZipOutputStream zip, final String name, final byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }

    static String escape(final String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static String escapeAttribute(final String text) {
        return escape(text).replace("\"", "&quot;");
    }

    // --- reading packages -----------------------------------------------------------

    /** Every zip entry of a package, by entry name (no leading slash). */
    static Map<String, byte[]> entries(final byte[] pkg) {
        final Map<String, byte[]> out = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(pkg))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    out.put(entry.getName(), zip.readAllBytes());
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }

    /** An entry's content as text, or null when the package has no such entry. */
    static String xml(final byte[] pkg, final String entryName) {
        final byte[] bytes = entries(pkg).get(entryName);
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    /** Every XML entry's text, concatenated, for "the value appears nowhere" checks. */
    static String allXml(final byte[] pkg) {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, byte[]> e : entries(pkg).entrySet()) {
            if (e.getKey().endsWith(".xml") || e.getKey().endsWith(".rels")) {
                sb.append(new String(e.getValue(), StandardCharsets.UTF_8)).append('\n');
            }
        }
        return sb.toString();
    }

    static Document dom(final byte[] pkg, final String entryName) {
        final byte[] bytes = entries(pkg).get(entryName);
        if (bytes == null) {
            return null;
        }
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** The text content of each element named {@code ns}:{@code local} in an entry, in document order. */
    static List<String> texts(final byte[] pkg, final String entryName, final String ns, final String local) {
        final List<String> out = new ArrayList<>();
        final Document doc = dom(pkg, entryName);
        if (doc == null) {
            return out;
        }
        final NodeList list = doc.getElementsByTagNameNS(ns, local);
        for (int i = 0; i < list.getLength(); i++) {
            out.add(list.item(i).getTextContent());
        }
        return out;
    }

    /** The elements named {@code ns}:{@code local} in an entry, in document order. */
    static List<Element> elements(final byte[] pkg, final String entryName, final String ns, final String local) {
        final List<Element> out = new ArrayList<>();
        final Document doc = dom(pkg, entryName);
        if (doc == null) {
            return out;
        }
        final NodeList list = doc.getElementsByTagNameNS(ns, local);
        for (int i = 0; i < list.getLength(); i++) {
            out.add((Element) list.item(i));
        }
        return out;
    }

    /** The number of non-overlapping occurrences of {@code value} in {@code text}. */
    static int occurrences(final String text, final String value) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(value, index)) >= 0) {
            count++;
            index += value.length();
        }
        return count;
    }

}
