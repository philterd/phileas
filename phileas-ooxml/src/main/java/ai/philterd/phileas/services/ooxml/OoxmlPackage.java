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

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * An Office Open XML package opened for redaction.
 *
 * <p>POI's OPC layer reads the package: parts, relationships, and content types, with POI's limits on
 * zip expansion. Each XML part is parsed to a W3C DOM on demand. The output is written here rather than
 * by POI, so that every zip entry the redaction did not change is copied byte for byte: POI's own save
 * rewrites every relationship part, always writes {@code docProps/core.xml}, and can add a content-type
 * override for a part that does not exist.
 */
final class OoxmlPackage implements AutoCloseable {

    static final String REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/";
    static final String OFFICE_DOCUMENT = REL + "officeDocument";

    private static final String PACKAGE_RELATIONSHIPS = "http://schemas.openxmlformats.org/package/2006/relationships";
    private static final String CONTENT_TYPES_NS = "http://schemas.openxmlformats.org/package/2006/content-types";
    private static final String CONTENT_TYPES_ENTRY = "[Content_Types].xml";

    private final OPCPackage opc;
    private final DocumentBuilderFactory documentBuilderFactory = newDocumentBuilderFactory();

    /** Input zip entries in their original order, keyed by entry name. */
    private final LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
    private final Map<String, Long> entryTimes = new HashMap<>();
    /** Lower-cased part name (e.g. "/word/document.xml") to zip entry name. */
    private final Map<String, String> entryByPartName = new HashMap<>();

    private final Map<String, Document> doms = new HashMap<>();
    private final Set<String> dirtyParts = new HashSet<>();
    private final Map<String, byte[]> replacedParts = new HashMap<>();
    private final Set<String> deletedParts = new HashSet<>();
    private Document contentTypes;
    private boolean contentTypesDirty;

    OoxmlPackage(final byte[] input) throws IOException {
        try {
            this.opc = OPCPackage.open(new ByteArrayInputStream(input));
        } catch (final Exception e) {
            throw new IOException("The document is not a valid Office Open XML package.", e);
        }
        try (ZipArchiveInputStream zip = new ZipArchiveInputStream(new ByteArrayInputStream(input), "UTF-8", true, true)) {
            ZipArchiveEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                final String name = entry.getName();
                entries.put(name, zip.readAllBytes());
                entryTimes.put(name, entry.getTime());
                entryByPartName.put(partKey("/" + name), name);
            }
        }
    }

    // --- parts and relationships --------------------------------------------------

    /** The package's main part (the document or workbook), or null. */
    PackagePart mainPart() {
        for (final PackageRelationship rel : opc.getRelationshipsByType(OFFICE_DOCUMENT)) {
            try {
                return opc.getPart(rel);
            } catch (final Exception e) {
                return null;
            }
        }
        return null;
    }

    /** The internal part a relationship of {@code source} points to, or null when it is external or missing. */
    PackagePart related(final PackagePart source, final PackageRelationship rel) {
        if (rel.getTargetMode() == TargetMode.EXTERNAL || isDeleted(rel, source)) {
            return null;
        }
        try {
            return source.getRelatedPart(rel);
        } catch (final Exception e) {
            return null;
        }
    }

    private boolean isDeleted(final PackageRelationship rel, final PackagePart source) {
        try {
            final PackagePartName target = PackagingURIHelper.createPartName(
                    PackagingURIHelper.resolvePartUri(source.getPartName().getURI(), rel.getTargetURI()));
            return deletedParts.contains(partKey(target.getName()));
        } catch (final Exception e) {
            return false;
        }
    }

    List<PackageRelationship> relationships(final PackagePart source) {
        final List<PackageRelationship> out = new ArrayList<>();
        try {
            for (final PackageRelationship rel : source.getRelationships()) {
                out.add(rel);
            }
        } catch (final Exception e) {
            // A part without a readable relationship part has no relationships.
        }
        return out;
    }

    List<PackageRelationship> relationshipsByType(final PackagePart source, final String type) {
        final List<PackageRelationship> out = new ArrayList<>();
        for (final PackageRelationship rel : relationships(source)) {
            if (type.equals(rel.getRelationshipType())) {
                out.add(rel);
            }
        }
        return out;
    }

    List<PackagePart> relatedByType(final PackagePart source, final String type) {
        final List<PackagePart> parts = new ArrayList<>();
        for (final PackageRelationship rel : relationshipsByType(source, type)) {
            final PackagePart part = related(source, rel);
            if (part != null) {
                parts.add(part);
            }
        }
        return parts;
    }

    PackagePart relatedById(final PackagePart source, final String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        try {
            final PackageRelationship rel = source.getRelationship(id);
            return rel == null ? null : related(source, rel);
        } catch (final Exception e) {
            return null;
        }
    }

    /** Every distinct part reachable from {@code root} (itself included), breadth-first. */
    List<PackagePart> allParts(final PackagePart root) {
        final List<PackagePart> out = new ArrayList<>();
        if (root == null) {
            return out;
        }
        final Set<String> seen = new HashSet<>();
        final Deque<PackagePart> queue = new ArrayDeque<>();
        seen.add(partKey(root));
        queue.add(root);
        while (!queue.isEmpty()) {
            final PackagePart part = queue.poll();
            out.add(part);
            for (final PackageRelationship rel : relationships(part)) {
                final PackagePart child = related(part, rel);
                if (child != null && seen.add(partKey(child))) {
                    queue.add(child);
                }
            }
        }
        return out;
    }

    /** Every distinct part reachable from the package's own relationships. */
    List<PackagePart> allPackageParts() {
        final List<PackagePart> out = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (final PackageRelationship rel : opc.getRelationships()) {
            if (rel.getTargetMode() == TargetMode.EXTERNAL) {
                continue;
            }
            try {
                final PackagePart top = opc.getPart(rel);
                if (top == null || deletedParts.contains(partKey(top))) {
                    continue;
                }
                for (final PackagePart part : allParts(top)) {
                    if (seen.add(partKey(part))) {
                        out.add(part);
                    }
                }
            } catch (final Exception e) {
                // Skip a dangling package relationship.
            }
        }
        return out;
    }

    // --- XML parts -------------------------------------------------------------------

    /** The part's DOM, or null when the part is not XML or does not parse. */
    Document dom(final PackagePart part) {
        if (part == null || !isXml(part)) {
            return null;
        }
        final String key = partKey(part);
        if (doms.containsKey(key)) {
            return doms.get(key);
        }
        Document doc = null;
        final byte[] bytes = partBytes(part);
        if (bytes != null) {
            try {
                doc = parse(bytes);
            } catch (final Exception e) {
                // A part that does not parse is skipped, as the Open XML SDK's RootElement throw is.
            }
        }
        doms.put(key, doc);
        return doc;
    }

    void markDirty(final PackagePart part) {
        dirtyParts.add(partKey(part));
    }

    static boolean isXml(final PackagePart part) {
        final String contentType = part.getContentType();
        return (contentType.endsWith("+xml") || contentType.endsWith("/xml")) && !contentType.contains("vmlDrawing");
    }

    /** The part's current bytes (replaced bytes if it was replaced), or null. */
    byte[] partBytes(final PackagePart part) {
        final String key = partKey(part);
        if (replacedParts.containsKey(key)) {
            return replacedParts.get(key);
        }
        final String entry = entryByPartName.get(key);
        if (entry != null) {
            return entries.get(entry);
        }
        try (InputStream in = part.getInputStream()) {
            return in.readAllBytes();
        } catch (final Exception e) {
            return null;
        }
    }

    void replacePart(final PackagePart part, final byte[] bytes) {
        final String key = partKey(part);
        replacedParts.put(key, bytes);
        doms.remove(key);
        dirtyParts.remove(key);
    }

    // --- relationship edits ------------------------------------------------------------

    /** Points an external relationship of {@code source} at a new target, keeping its id. */
    void setExternalTarget(final PackagePart source, final PackageRelationship rel, final String target) {
        final Element element = relationshipElement(source, rel.getId());
        if (element != null) {
            element.setAttribute("Target", target);
            markDirtyKey(relationshipPartKey(source));
        }
        source.removeRelationship(rel.getId());
        source.addExternalRelationship(target, rel.getRelationshipType(), rel.getId());
    }

    /**
     * Removes a relationship of {@code parent} and deletes the part it targets, with that part's own
     * relationship part and any of its children nothing else references. A target that another
     * relationship still references is kept.
     */
    void deletePart(final PackagePart parent, final PackageRelationship rel) {
        final PackagePart target = related(parent, rel);
        removeRelationship(parent, rel.getId());
        if (target != null && referenceCount(target) == 0) {
            deleteRecursively(target);
        }
    }

    private void removeRelationship(final PackagePart source, final String id) {
        final Element element = relationshipElement(source, id);
        if (element != null) {
            element.getParentNode().removeChild(element);
            markDirtyKey(relationshipPartKey(source));
        }
        source.removeRelationship(id);
    }

    private void deleteRecursively(final PackagePart part) {
        final List<PackagePart> children = new ArrayList<>();
        for (final PackageRelationship rel : relationships(part)) {
            final PackagePart child = related(part, rel);
            if (child != null) {
                children.add(child);
            }
        }
        final String key = partKey(part);
        deletedParts.add(key);
        deletedParts.add(relationshipPartKey(part));
        doms.remove(key);
        dirtyParts.remove(key);
        replacedParts.remove(key);
        removeContentTypeOverride(part.getPartName().getName());
        for (final PackagePart child : children) {
            if (!deletedParts.contains(partKey(child)) && referenceCount(child) == 0) {
                deleteRecursively(child);
            }
        }
    }

    // Relationships from parts that are not deleted, and from the package, that point at the part.
    private int referenceCount(final PackagePart part) {
        final String key = partKey(part);
        int count = 0;
        for (final PackageRelationship rel : opc.getRelationships()) {
            try {
                if (rel.getTargetMode() != TargetMode.EXTERNAL && key.equals(partKey(opc.getPart(rel)))) {
                    count++;
                }
            } catch (final Exception e) {
                // dangling
            }
        }
        for (final PackagePart source : allPackageParts()) {
            for (final PackageRelationship rel : relationships(source)) {
                final PackagePart target = related(source, rel);
                if (target != null && key.equals(partKey(target))) {
                    count++;
                }
            }
        }
        return count;
    }

    private Element relationshipElement(final PackagePart source, final String id) {
        final Document rels = relationshipDom(source);
        if (rels == null) {
            return null;
        }
        final NodeList list = rels.getElementsByTagNameNS(PACKAGE_RELATIONSHIPS, "Relationship");
        for (int i = 0; i < list.getLength(); i++) {
            final Element element = (Element) list.item(i);
            if (id.equals(element.getAttribute("Id"))) {
                return element;
            }
        }
        return null;
    }

    private Document relationshipDom(final PackagePart source) {
        final String key = relationshipPartKey(source);
        if (doms.containsKey(key)) {
            return doms.get(key);
        }
        Document doc = null;
        final String entry = entryByPartName.get(key);
        if (entry != null) {
            try {
                doc = parse(entries.get(entry));
            } catch (final Exception e) {
                // An unreadable relationship part cannot be edited.
            }
        }
        doms.put(key, doc);
        return doc;
    }

    private void removeContentTypeOverride(final String partName) {
        final Document types = contentTypesDom();
        if (types == null) {
            return;
        }
        final NodeList list = types.getElementsByTagNameNS(CONTENT_TYPES_NS, "Override");
        for (int i = list.getLength() - 1; i >= 0; i--) {
            final Element element = (Element) list.item(i);
            if (element.getAttribute("PartName").equalsIgnoreCase(partName)) {
                element.getParentNode().removeChild(element);
                contentTypesDirty = true;
            }
        }
    }

    private Document contentTypesDom() {
        if (contentTypes == null) {
            final String entry = contentTypesEntry();
            if (entry != null) {
                try {
                    contentTypes = parse(entries.get(entry));
                } catch (final Exception e) {
                    return null;
                }
            }
        }
        return contentTypes;
    }

    private String contentTypesEntry() {
        for (final String name : entries.keySet()) {
            if (name.equalsIgnoreCase(CONTENT_TYPES_ENTRY)) {
                return name;
            }
        }
        return null;
    }

    private void markDirtyKey(final String key) {
        dirtyParts.add(key);
    }

    // --- output ------------------------------------------------------------------------

    /** Writes the package: changed entries re-serialized, deleted ones dropped, every other entry copied. */
    byte[] save() throws IOException {
        final String contentTypesEntry = contentTypesEntry();
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            for (final Map.Entry<String, byte[]> entry : entries.entrySet()) {
                final String name = entry.getKey();
                final String key = partKey("/" + name);
                if (deletedParts.contains(key)) {
                    continue;
                }
                final byte[] bytes;
                if (name.equals(contentTypesEntry) && contentTypesDirty) {
                    bytes = serialize(contentTypes);
                } else if (replacedParts.containsKey(key)) {
                    bytes = replacedParts.get(key);
                } else if (dirtyParts.contains(key) && doms.get(key) != null) {
                    bytes = serialize(doms.get(key));
                } else {
                    bytes = entry.getValue();
                }
                final ZipEntry out = new ZipEntry(name);
                final Long time = entryTimes.get(name);
                if (time != null && time >= 0) {
                    out.setTime(time);
                }
                zip.putNextEntry(out);
                zip.write(bytes);
                zip.closeEntry();
            }
        }
        return buffer.toByteArray();
    }

    @Override
    public void close() {
        opc.revert();
    }

    // --- helpers -----------------------------------------------------------------------

    private static String partKey(final PackagePart part) {
        return partKey(part.getPartName().getName());
    }

    private static String partKey(final String partName) {
        String name = partName;
        try {
            name = PackagingURIHelper.createPartName(partName).getName();
        } catch (final Exception e) {
            // Not a valid part name (for example "[Content_Types].xml"); use it as written.
        }
        return name.toLowerCase(Locale.ROOT);
    }

    private static String relationshipPartKey(final PackagePart part) {
        try {
            return partKey(PackagingURIHelper.getRelationshipPartName(part.getPartName()).getName());
        } catch (final Exception e) {
            return partKey(part) + ".rels";
        }
    }

    private Document parse(final byte[] bytes) throws Exception {
        final DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(bytes));
    }

    private static DocumentBuilderFactory newDocumentBuilderFactory() {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory;
        } catch (final Exception e) {
            throw new IllegalStateException("Unable to configure the XML parser.", e);
        }
    }

    static byte[] serialize(final Document doc) throws IOException {
        try {
            doc.setXmlStandalone(true);
            final TransformerFactory factory = TransformerFactory.newInstance();
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            final Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(doc), new StreamResult(out));
            return out.toByteArray();
        } catch (final Exception e) {
            throw new IOException("Unable to write an XML part.", e);
        }
    }

    // --- DOM utilities shared by the redactors ------------------------------------------------

    static List<Element> descendants(final Node root, final String ns, final String local) {
        final NodeList list = root instanceof Document document
                ? document.getElementsByTagNameNS(ns, local)
                : ((Element) root).getElementsByTagNameNS(ns, local);
        final List<Element> out = new ArrayList<>(list.getLength());
        for (int i = 0; i < list.getLength(); i++) {
            out.add((Element) list.item(i));
        }
        return out;
    }

    static List<Element> allDescendants(final Element root) {
        return descendants(root, "*", "*");
    }

    static List<Element> children(final Element parent, final String ns, final String local) {
        final List<Element> out = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (is(n, ns, local)) {
                out.add((Element) n);
            }
        }
        return out;
    }

    static List<Element> childElements(final Element parent) {
        final List<Element> out = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element element) {
                out.add(element);
            }
        }
        return out;
    }

    static Element firstChild(final Element parent, final String ns, final String local) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (is(n, ns, local)) {
                return (Element) n;
            }
        }
        return null;
    }

    static boolean is(final Node n, final String ns, final String local) {
        return n instanceof Element && ns.equals(n.getNamespaceURI()) && local.equals(n.getLocalName());
    }

    static Element nearestAncestor(final Node n, final String ns, final String local) {
        for (Node p = n.getParentNode(); p != null; p = p.getParentNode()) {
            if (is(p, ns, local)) {
                return (Element) p;
            }
        }
        return null;
    }

    static void removeChildren(final Element e) {
        while (e.getFirstChild() != null) {
            e.removeChild(e.getFirstChild());
        }
    }

    static void setText(final Element e, final String text) {
        removeChildren(e);
        e.appendChild(e.getOwnerDocument().createTextNode(text));
    }

    /** A qualified name in {@code context}'s prefix style, so new elements match the part's namespaces. */
    static String qualified(final Element context, final String local) {
        return context.getPrefix() == null ? local : context.getPrefix() + ":" + local;
    }

}
