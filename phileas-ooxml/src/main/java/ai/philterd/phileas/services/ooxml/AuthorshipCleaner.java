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
    static final String REL_ATTACHED_TEMPLATE = OoxmlPackage.REL + "attachedTemplate";

    private static final String CORE = "http://schemas.openxmlformats.org/package/2006/metadata/core-properties";
    private static final String DC = "http://purl.org/dc/elements/1.1/";
    private static final String EXTENDED = "http://schemas.openxmlformats.org/officeDocument/2006/extended-properties";
    private static final String REL_CORE = "http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties";
    private static final String REL_EXTENDED = OoxmlPackage.REL + "extended-properties";
    private static final String XML_NS = "http://www.w3.org/XML/1998/namespace";

    private AuthorshipCleaner() {
    }

    /**
     * Word: revision and comment authors, comment initials, {@code word/people.xml}, the attached template's
     * path, and document properties.
     */
    static void clearWord(final OoxmlPackage pkg) {
        final PackagePart main = pkg.mainPart();
        if (main != null) {
            // people.xml maps each author to a presence identifier, often an email address or directory ID.
            for (final PackageRelationship rel : pkg.relationshipsByType(main, REL_PEOPLE)) {
                pkg.deletePart(main, rel);
            }
            for (final PackagePart part : pkg.allParts(main)) {
                final String contentType = contentType(part);
                if (!contentType.contains("wordprocessingml")) {
                    continue;
                }
                final Document d = pkg.dom(part);
                boolean changed = d != null && clearWordAuthors(d);
                if (contentType.endsWith("wordprocessingml.settings+xml")) {
                    changed |= removeAttachedTemplate(pkg, part, d);
                }
                if (changed) {
                    pkg.markDirty(part);
                }
            }
        }
        clearDocumentProperties(pkg);
    }

    /**
     * Excel, on every sheet: legacy comment authors and the author name Excel starts a note's text with,
     * threaded-comment persons, the write-reservation user, shared-workbook revision users, and document
     * properties. Runs before comment text is redacted, so a note's name is matched as Excel wrote it.
     */
    static void clearSpreadsheet(final OoxmlPackage pkg) {
        final PackagePart main = pkg.mainPart();
        for (final PackagePart part : pkg.allPackageParts()) {
            final String contentType = contentType(part);
            final boolean comments = contentType.endsWith("spreadsheetml.comments+xml");
            final boolean persons = contentType.endsWith("ms-excel.person+xml");
            final boolean workbook = main != null && part.getPartName().equals(main.getPartName());
            final boolean revisionHeaders = contentType.endsWith("spreadsheetml.revisionheaders+xml");
            final boolean revisionLog = contentType.endsWith("spreadsheetml.revisionlog+xml");
            final boolean userNames = contentType.endsWith("spreadsheetml.usernames+xml");
            if (!comments && !persons && !workbook && !revisionHeaders && !revisionLog && !userNames) {
                continue;
            }
            final Document d = pkg.dom(part);
            if (d == null) {
                continue;
            }
            boolean changed = false;
            if (comments) {
                changed = clearCommentAuthors(d);
            } else if (persons) {
                for (final Element person : elements(d, XlsxRedactor.THREADED, "person")) {
                    changed |= setAttribute(person, "displayName", NEUTRAL_AUTHOR);
                    changed |= removeAttribute(person, "userId");
                    changed |= removeAttribute(person, "providerId");
                }
            } else if (workbook) {
                // The user who set the write-reservation password, shown when the workbook is opened.
                changed = replaceAttributes(d, "fileSharing", "userName");
            } else if (revisionHeaders) {
                changed = replaceAttributes(d, "header", "userName");
            } else if (revisionLog) {
                changed = replaceAttributes(d, "rcmt", "author");
            } else {
                changed = replaceAttributes(d, "userInfo", "name");
            }
            if (changed) {
                pkg.markDirty(part);
            }
        }
        clearDocumentProperties(pkg);
    }

    // Comments refer to authors by index, so each is replaced rather than removed. Excel starts a note's text
    // with its author's name and a colon, usually as a bold first run; that name is replaced too.
    private static boolean clearCommentAuthors(final Document d) {
        final List<Element> authors = elements(d, XlsxRedactor.S, "author");
        final List<String> names = new ArrayList<>();
        authors.forEach(a -> names.add(a.getTextContent()));
        boolean changed = false;
        for (final Element comment : elements(d, XlsxRedactor.S, "comment")) {
            final int authorId = parseIndex(comment.getAttribute("authorId"));
            if (authorId >= 0 && authorId < names.size()) {
                changed |= replaceNotePrefix(comment, names.get(authorId));
            }
        }
        for (final Element author : authors) {
            changed |= setText(author, NEUTRAL_AUTHOR);
        }
        return changed;
    }

    // Replaces "<name>:" at the start of the note's text, which may span runs, with "Redacted:".
    private static boolean replaceNotePrefix(final Element comment, final String name) {
        if (name.isBlank() || NEUTRAL_AUTHOR.equals(name)) {
            return false;
        }
        final List<Element> texts = noteTexts(comment);
        final StringBuilder all = new StringBuilder();
        texts.forEach(t -> all.append(t.getTextContent()));
        final String prefix = name + ":";
        if (!all.toString().startsWith(prefix)) {
            return false;
        }
        int remaining = prefix.length();
        for (final Element t : texts) {
            if (remaining == 0) {
                break;
            }
            final String text = t.getTextContent();
            final int cut = Math.min(remaining, text.length());
            setPreservedText(t, text.substring(cut));
            remaining -= cut;
        }
        setPreservedText(texts.get(0), NEUTRAL_AUTHOR + ":" + texts.get(0).getTextContent());
        return true;
    }

    // The note's text elements in order: <text><t> or <text><r><t>, leaving out phonetic runs.
    private static List<Element> noteTexts(final Element comment) {
        final List<Element> out = new ArrayList<>();
        for (final Element text : OoxmlPackage.children(comment, XlsxRedactor.S, "text")) {
            for (final Element child : OoxmlPackage.childElements(text)) {
                if (OoxmlPackage.is(child, XlsxRedactor.S, "t")) {
                    out.add(child);
                } else if (OoxmlPackage.is(child, XlsxRedactor.S, "r")) {
                    out.addAll(OoxmlPackage.children(child, XlsxRedactor.S, "t"));
                }
            }
        }
        return out;
    }

    private static void setPreservedText(final Element t, final String text) {
        t.setTextContent(text);
        if (!text.equals(text.strip())) {
            t.setAttributeNS(XML_NS, "xml:space", "preserve");
        }
    }

    private static int parseIndex(final String value) {
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            return -1;
        }
    }

    // The template path in settings usually sits in the user's profile, so the template is detached: the
    // relationship and w:attachedTemplate are removed, and Word attaches Normal on open.
    private static boolean removeAttachedTemplate(final OoxmlPackage pkg, final PackagePart settings, final Document d) {
        boolean changed = false;
        for (final PackageRelationship rel : pkg.relationshipsByType(settings, REL_ATTACHED_TEMPLATE)) {
            pkg.removeRelationship(settings, rel.getId());
        }
        if (d != null) {
            for (final Element e : elements(d, WordDocumentRedactor.W, "attachedTemplate")) {
                e.getParentNode().removeChild(e);
                changed = true;
            }
        }
        return changed;
    }

    // Replaces the attribute with the neutral author on each named element in the main SpreadsheetML namespace
    // that has it. Each is required where it appears, except fileSharing's.
    private static boolean replaceAttributes(final Document d, final String element, final String attribute) {
        boolean changed = false;
        for (final Element e : elements(d, XlsxRedactor.S, element)) {
            if (e.hasAttribute(attribute)) {
                changed |= setAttribute(e, attribute, NEUTRAL_AUTHOR);
            }
        }
        return changed;
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
