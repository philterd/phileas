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

import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Clears who wrote and edited a document, whatever the policy: an author name is wholly identifying, so it is
 * replaced rather than run through detection. Parts with nothing to clear are left unchanged.
 */
final class AuthorshipCleaner {

    /** Written where the format requires an author: Word comments and revisions, Excel comment authors and persons. */
    static final String NEUTRAL_AUTHOR = "Redacted";

    static final String REL_PEOPLE = "http://schemas.microsoft.com/office/2011/relationships/people";

    private static final String CORE = "http://schemas.openxmlformats.org/package/2006/metadata/core-properties";
    private static final String DC = "http://purl.org/dc/elements/1.1/";
    private static final String EXTENDED = "http://schemas.openxmlformats.org/officeDocument/2006/extended-properties";
    private static final String REL_CORE = "http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties";
    private static final String REL_EXTENDED = OoxmlPackage.REL + "extended-properties";

    private AuthorshipCleaner() {
    }

    /** Word: revision and comment authors, comment initials, {@code word/people.xml}, and document properties. */
    static void clearWord(final OoxmlPackage pkg) {
        final PackagePart main = pkg.mainPart();
        if (main != null) {
            // people.xml maps each author to a presence identifier, often an email address or directory ID.
            for (final PackageRelationship rel : pkg.relationshipsByType(main, REL_PEOPLE)) {
                pkg.deletePart(main, rel);
            }
            for (final PackagePart part : pkg.allParts(main)) {
                if (!contentType(part).contains("wordprocessingml")) {
                    continue;
                }
                final Document d = pkg.dom(part);
                if (d != null && clearWordAuthors(d)) {
                    pkg.markDirty(part);
                }
            }
        }
        clearDocumentProperties(pkg);
    }

    /** Excel: legacy comment authors, threaded-comment persons, and document properties, on every sheet. */
    static void clearSpreadsheet(final OoxmlPackage pkg) {
        for (final PackagePart part : pkg.allPackageParts()) {
            final String contentType = contentType(part);
            final boolean comments = contentType.endsWith("spreadsheetml.comments+xml");
            final boolean persons = contentType.endsWith("ms-excel.person+xml");
            if (!comments && !persons) {
                continue;
            }
            final Document d = pkg.dom(part);
            if (d == null) {
                continue;
            }
            boolean changed = false;
            if (comments) {
                // Comments refer to authors by index, so each is replaced rather than removed.
                for (final Element author : elements(d, XlsxRedactor.S, "author")) {
                    changed |= setText(author, NEUTRAL_AUTHOR);
                }
            } else {
                for (final Element person : elements(d, XlsxRedactor.THREADED, "person")) {
                    changed |= setAttribute(person, "displayName", NEUTRAL_AUTHOR);
                    changed |= removeAttribute(person, "userId");
                    changed |= removeAttribute(person, "providerId");
                }
            }
            if (changed) {
                pkg.markDirty(part);
            }
        }
        clearDocumentProperties(pkg);
    }

    // Every element in the WordprocessingML namespace with a w:author (comments and all revision elements).
    private static boolean clearWordAuthors(final Document d) {
        boolean changed = false;
        final NodeList all = d.getElementsByTagNameNS(WordDocumentRedactor.W, "*");
        for (int i = 0; i < all.getLength(); i++) {
            final Element e = (Element) all.item(i);
            if (e.hasAttributeNS(WordDocumentRedactor.W, "author")
                    && !NEUTRAL_AUTHOR.equals(e.getAttributeNS(WordDocumentRedactor.W, "author"))) {
                e.setAttributeNS(WordDocumentRedactor.W, e.getAttributeNodeNS(WordDocumentRedactor.W, "author").getName(),
                        NEUTRAL_AUTHOR);
                changed = true;
            }
            if ("comment".equals(e.getLocalName()) && e.hasAttributeNS(WordDocumentRedactor.W, "initials")) {
                e.removeAttributeNS(WordDocumentRedactor.W, "initials");
                changed = true;
            }
        }
        return changed;
    }

    // dc:creator and cp:lastModifiedBy in docProps/core.xml; Company and Manager in docProps/app.xml.
    private static void clearDocumentProperties(final OoxmlPackage pkg) {
        clearText(pkg, pkg.packagePartByRelationshipType(REL_CORE), List.of(DC, "creator", CORE, "lastModifiedBy"));
        clearText(pkg, pkg.packagePartByRelationshipType(REL_EXTENDED), List.of(EXTENDED, "Company", EXTENDED, "Manager"));
    }

    // Empties each named element's text, keeping the element, as Word and Excel write an unset property.
    private static void clearText(final OoxmlPackage pkg, final PackagePart part, final List<String> namespacesAndNames) {
        final Document d = part == null ? null : pkg.dom(part);
        if (d == null) {
            return;
        }
        boolean changed = false;
        for (int i = 0; i < namespacesAndNames.size(); i += 2) {
            for (final Element e : elements(d, namespacesAndNames.get(i), namespacesAndNames.get(i + 1))) {
                changed |= setText(e, "");
            }
        }
        if (changed) {
            pkg.markDirty(part);
        }
    }

    private static List<Element> elements(final Document d, final String namespace, final String name) {
        final List<Element> out = new ArrayList<>();
        final NodeList nodes = d.getElementsByTagNameNS(namespace, name);
        for (int i = 0; i < nodes.getLength(); i++) {
            out.add((Element) nodes.item(i));
        }
        return out;
    }

    private static boolean setText(final Element e, final String text) {
        if (text.equals(e.getTextContent())) {
            return false;
        }
        e.setTextContent(text);
        return true;
    }

    private static boolean setAttribute(final Element e, final String name, final String value) {
        if (value.equals(e.getAttribute(name))) {
            return false;
        }
        e.setAttribute(name, value);
        return true;
    }

    private static boolean removeAttribute(final Element e, final String name) {
        if (!e.hasAttribute(name)) {
            return false;
        }
        e.removeAttribute(name);
        return true;
    }

    private static String contentType(final PackagePart part) {
        return part.getContentType().toLowerCase(Locale.ROOT);
    }

}
