package spike;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackageRelationship;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An OPC package opened with POI, with each XML part parsed to a W3C DOM on demand. Only parts that were
 * marked dirty are serialized back, so untouched parts keep their original bytes. This is the analogue of
 * the Open XML SDK's part tree plus its element tree.
 */
final class Pkg implements AutoCloseable {

    static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    static final String A = "http://schemas.openxmlformats.org/drawingml/2006/main";
    static final String C = "http://schemas.openxmlformats.org/drawingml/2006/chart";
    static final String R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    static final String S = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    static final String REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/";
    static final String OFFICE_DOCUMENT = REL + "officeDocument";

    final OPCPackage opc;
    private final Map<String, Document> doms = new HashMap<>();
    private final Set<String> dirty = new LinkedHashSet<>();

    Pkg(final byte[] bytes) throws Exception {
        this.opc = OPCPackage.open(new ByteArrayInputStream(bytes));
    }

    PackagePart mainPart() throws Exception {
        for (PackageRelationship rel : opc.getRelationshipsByType(OFFICE_DOCUMENT)) {
            return opc.getPart(rel);
        }
        return null;
    }

    /** The part's DOM, or null when the part is not XML or does not parse. */
    Document dom(final PackagePart part) {
        if (part == null || !isXml(part)) {
            return null;
        }
        final String name = part.getPartName().getName();
        if (doms.containsKey(name)) {
            return doms.get(name);
        }
        Document doc = null;
        try (InputStream in = part.getInputStream()) {
            doc = builder().parse(in);
        } catch (Exception e) {
            // Mirrors the .NET code skipping a part whose RootElement throws.
        }
        doms.put(name, doc);
        return doc;
    }

    void markDirty(final PackagePart part) {
        dirty.add(part.getPartName().getName());
    }

    static boolean isXml(final PackagePart part) {
        final String ct = part.getContentType();
        return (ct.endsWith("+xml") || ct.endsWith("/xml")) && !ct.contains("vmlDrawing");
    }

    /** The internal part a relationship of {@code source} points to, or null. */
    PackagePart related(final PackagePart source, final PackageRelationship rel) {
        if (rel.getTargetMode() == TargetMode.EXTERNAL) {
            return null;
        }
        try {
            return source.getRelatedPart(rel);
        } catch (Exception e) {
            return null;
        }
    }

    List<PackagePart> relatedByType(final PackagePart source, final String type) throws Exception {
        final List<PackagePart> parts = new ArrayList<>();
        for (PackageRelationship rel : source.getRelationshipsByType(type)) {
            final PackagePart p = related(source, rel);
            if (p != null) {
                parts.add(p);
            }
        }
        return parts;
    }

    PackagePart relatedById(final PackagePart source, final String id) {
        try {
            final PackageRelationship rel = source.getRelationship(id);
            return rel == null ? null : related(source, rel);
        } catch (Exception e) {
            return null;
        }
    }

    /** Every distinct part reachable from {@code root} (itself included), breadth-first. */
    List<PackagePart> allParts(final PackagePart root) throws Exception {
        final List<PackagePart> out = new ArrayList<>();
        final Set<String> seen = new LinkedHashSet<>();
        final Deque<PackagePart> queue = new ArrayDeque<>();
        seen.add(root.getPartName().getName());
        queue.add(root);
        while (!queue.isEmpty()) {
            final PackagePart part = queue.poll();
            out.add(part);
            if (part.isRelationshipPart()) {
                continue;
            }
            for (PackageRelationship rel : part.getRelationships()) {
                final PackagePart child = related(part, rel);
                if (child != null && seen.add(child.getPartName().getName())) {
                    queue.add(child);
                }
            }
        }
        return out;
    }

    /** Removes the relationship and the part it targets (with the part's own relationships). */
    void deletePart(final PackagePart parent, final PackageRelationship rel) throws Exception {
        final PackagePart target = related(parent, rel);
        parent.removeRelationship(rel.getId());
        if (target != null) {
            final PackagePartName name = target.getPartName();
            doms.remove(name.getName());
            dirty.remove(name.getName());
            opc.deletePartRecursive(name);
        }
    }

    byte[] readBytes(final PackagePart part) throws Exception {
        try (InputStream in = part.getInputStream()) {
            return in.readAllBytes();
        }
    }

    void writeBytes(final PackagePart part, final byte[] bytes) throws Exception {
        try (OutputStream out = part.getOutputStream()) {
            out.write(bytes);
        }
    }

    byte[] save() throws Exception {
        for (String name : dirty) {
            final Document doc = doms.get(name);
            final PackagePart part = opc.getPart(org.apache.poi.openxml4j.opc.PackagingURIHelper.createPartName(name));
            if (doc == null || part == null) {
                continue;
            }
            final ByteArrayOutputStream buf = new ByteArrayOutputStream();
            serialize(doc, buf);
            writeBytes(part, buf.toByteArray());
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        opc.save(out);
        return out.toByteArray();
    }

    @Override
    public void close() {
        opc.revert();
    }

    // --- DOM helpers ---------------------------------------------------------------

    static DocumentBuilder builder() throws Exception {
        final DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setExpandEntityReferences(false);
        return f.newDocumentBuilder();
    }

    static void serialize(final Document doc, final OutputStream out) throws Exception {
        doc.setXmlStandalone(true);
        final Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        t.transform(new DOMSource(doc), new StreamResult(out));
    }

    static List<Element> desc(final Node root, final String ns, final String local) {
        final NodeList list = root instanceof Document d
                ? d.getElementsByTagNameNS(ns, local)
                : ((Element) root).getElementsByTagNameNS(ns, local);
        final List<Element> out = new ArrayList<>(list.getLength());
        for (int i = 0; i < list.getLength(); i++) {
            out.add((Element) list.item(i));
        }
        return out;
    }

    static List<Element> allDesc(final Element root) {
        return desc(root, "*", "*");
    }

    static List<Element> children(final Element parent, final String ns, final String local) {
        final List<Element> out = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && is(e, ns, local)) {
                out.add(e);
            }
        }
        return out;
    }

    static Element firstChild(final Element parent, final String ns, final String local) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && is(e, ns, local)) {
                return e;
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
}
