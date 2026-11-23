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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ai.philterd.phileas.services.ooxml.TestPackage.REL;
import static ai.philterd.phileas.services.ooxml.TestPackage.W;

/**
 * The Word fixtures the embedded-object, byte-overload, and detect-unchanged tests need, ported from the
 * matching parts of phileas-dotnet's {@code WordDocs}.
 */
final class WordFixturesForEmbeddedTests {

    private static final String DOCUMENT = "/word/document.xml";
    private static final String DOCUMENT_RELS = "word/_rels/document.xml.rels";

    /** A .docx with one body paragraph per string. */
    static byte[] create(final String... paragraphs) {
        return OoxmlTestFixtures.docx(paragraphs);
    }

    /** A .docx with an embedded package of the given content type, related from the main part. */
    static byte[] createWithEmbeddedPackage(final byte[] bytes, final String contentType, final String... paragraphs) {
        final TestPackage pkg = OoxmlTestFixtures.docxPackage(paragraphs);
        final String ext = contentType.contains("spreadsheetml") ? "xlsx"
                : contentType.contains("wordprocessingml") ? "docx"
                : contentType.contains("presentationml") ? "pptx" : "bin";
        final String part = "/word/embeddings/package1." + ext;
        pkg.part(part, contentType, bytes);
        pkg.rel(DOCUMENT, "rIdEmb", REL + "package", part);
        return pkg.toBytes();
    }

    /** A .docx with an embedded object that cannot be inspected (a PowerPoint package, treated as opaque). */
    static byte[] createWithEmbeddedOleObject(final byte[] bytes, final String... paragraphs) {
        return createWithEmbeddedPackage(bytes, SpreadsheetDocs.CT_PPTX, paragraphs);
    }

    /** True if the main part still carries an embedded package or OLE object. */
    static boolean hasAnyEmbeddedObject(final byte[] docx) {
        return !embeddedTargets(docx).isEmpty();
    }

    /** All cell text of the first embedded package, read as an .xlsx (empty if none). */
    static String embeddedPackageAllTextAsXlsx(final byte[] docx) {
        final byte[] bytes = firstEmbeddedPackageBytes(docx);
        return bytes == null ? "" : SpreadsheetDocs.allText(bytes);
    }

    /** All body text of the first embedded package, read as a .docx (empty if none). */
    static String embeddedPackageAllTextAsDocx(final byte[] docx) {
        final byte[] bytes = firstEmbeddedPackageBytes(docx);
        return bytes == null ? "" : allBodyText(bytes);
    }

    private static byte[] firstEmbeddedPackageBytes(final byte[] docx) {
        final List<String> targets = SpreadsheetDocs.relTargets(docx, "word/document.xml", DOCUMENT_RELS, REL + "package");
        final Map<String, byte[]> entries = TestPackage.entries(docx);
        for (final String target : targets) {
            if (entries.containsKey(target)) {
                return entries.get(target);
            }
        }
        return null;
    }

    private static List<String> embeddedTargets(final byte[] docx) {
        final Map<String, byte[]> entries = TestPackage.entries(docx);
        return SpreadsheetDocs.relTargets(docx, "word/document.xml", DOCUMENT_RELS, REL + "package", REL + "oleObject")
                .stream().filter(entries::containsKey).toList();
    }

    /** True if any entry in the package contains {@code value} (strong leak check). */
    static boolean anyPartContains(final byte[] docx, final String value) {
        for (final byte[] bytes : TestPackage.entries(docx).values()) {
            if (new String(bytes, StandardCharsets.UTF_8).contains(value)) {
                return true;
            }
        }
        return false;
    }

    /** The body paragraphs' text, in order. */
    static List<String> bodyParagraphs(final byte[] docx) {
        final List<String> out = new ArrayList<>();
        final Document doc = TestPackage.dom(docx, "word/document.xml");
        final Element body = OoxmlPackage.firstChild(doc.getDocumentElement(), W, "body");
        for (final Element p : OoxmlPackage.children(body, W, "p")) {
            out.add(p.getTextContent());
        }
        return out;
    }

    /** All body text concatenated and trimmed. */
    static String allBodyText(final byte[] docx) {
        return String.join("", bodyParagraphs(docx)).trim();
    }

    private WordFixturesForEmbeddedTests() {
    }

}
