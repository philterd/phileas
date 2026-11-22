package spike;

import ai.philterd.phileas.model.filtering.TextFilterResult;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static spike.Office.*;
import static spike.Pkg.*;

/** Port of XlsxRedactor (Redact and Detect with default options; no ApplySpans, columns, or header context). */
final class XlsxRedactor {

    static final String TC = "http://schemas.microsoft.com/office/spreadsheetml/2018/threadedcomments";
    static final String REL_THREADED = "http://schemas.microsoft.com/office/2017/10/relationships/threadedComment";
    static final String REL_PERSON = "http://schemas.microsoft.com/office/2017/10/relationships/person";

    record Cell(Element cell, PackagePart sheetPart) { }

    static WordRedactor.Result redact(final byte[] input, final Filter filter) throws Exception {
        try (Pkg pkg = new Pkg(input)) {
            final List<OfficeSpan> captured = run(pkg, filter, true);
            final Order order = new Order();
            order.value = captured.size();
            scanChartEmbeddings(pkg, filter, true, captured, order);
            EmbeddedObjects.process(pkg, filter, true, true, captured, order);
            return new WordRedactor.Result(pkg.save(), captured);
        }
    }

    static List<OfficeSpan> detect(final byte[] input, final Filter filter) throws Exception {
        try (Pkg pkg = new Pkg(input)) {
            final List<OfficeSpan> captured = run(pkg, filter, false);
            final Order order = new Order();
            order.value = captured.size();
            scanChartEmbeddings(pkg, filter, false, captured, order);
            EmbeddedObjects.process(pkg, filter, false, false, captured, order);
            return captured;
        }
    }

    static List<OfficeSpan> run(final Pkg pkg, final Filter filter, final boolean write) throws Exception {
        final List<OfficeSpan> captured = new ArrayList<>();
        final Order order = new Order();
        final PackagePart wb = pkg.mainPart();
        if (wb == null || pkg.dom(wb) == null) {
            return captured;
        }
        final List<String> sst = sharedStrings(pkg);
        int cellIndex = 0;
        boolean anyCellRedacted = false;
        for (Cell c : enumerateCells(pkg)) {
            final int currentIndex = cellIndex++;
            if (!isScannableCell(c.cell())) {
                continue;
            }
            final String original = getCellText(c.cell(), sst);
            if (original.isEmpty()) {
                continue;
            }
            final TextFilterResult result = filter.apply(original);
            if (result.getFilteredText().equals(original)) {
                continue;
            }
            if (write) {
                setCellText(c.cell(), result.getFilteredText());
                pkg.markDirty(c.sheetPart());
                anyCellRedacted = true;
            }
            capture(result, original, currentIndex, null, captured, order);
        }

        for (PackagePart sheet : worksheetParts(pkg)) {
            final Document d = pkg.dom(sheet);
            // Print headers/footers.
            final Element hf = firstChild(d.getDocumentElement(), S, "headerFooter");
            if (hf != null) {
                for (org.w3c.dom.Node n = hf.getFirstChild(); n != null; n = n.getNextSibling()) {
                    if (n instanceof Element e && leaf(pkg, sheet, e, filter, write, captured, order, null)) {
                        // marked dirty in leaf
                    }
                }
            }
        }
        // Comments: legacy text and authors, threaded text, then workbook persons.
        for (PackagePart sheet : worksheetParts(pkg)) {
            for (PackagePart cp : pkg.relatedByType(sheet, REL + "comments")) {
                final Document d = pkg.dom(cp);
                if (d == null) {
                    continue;
                }
                for (Element t : desc(d, S, "t")) {
                    leaf(pkg, cp, t, filter, write, captured, order, null);
                }
                for (Element a : desc(d, S, "author")) {
                    leaf(pkg, cp, a, filter, write, captured, order, null);
                }
            }
            for (PackagePart tp : pkg.relatedByType(sheet, REL_THREADED)) {
                final Document d = pkg.dom(tp);
                if (d != null) {
                    for (Element t : desc(d, TC, "text")) {
                        leaf(pkg, tp, t, filter, write, captured, order, null);
                    }
                }
            }
        }
        for (PackagePart pp : pkg.relatedByType(wb, REL_PERSON)) {
            final Document d = pkg.dom(pp);
            if (d == null) {
                continue;
            }
            for (Element person : desc(d, TC, "person")) {
                final String original = person.getAttribute("displayName");
                if (original.isEmpty()) {
                    continue;
                }
                final TextFilterResult result = filter.apply(original);
                if (result.getFilteredText().equals(original)) {
                    continue;
                }
                if (write) {
                    person.setAttribute("displayName", result.getFilteredText());
                    pkg.markDirty(pp);
                }
                capture(result, original, -1, null, captured, order);
            }
        }
        // Shapes, then charts.
        for (PackagePart sheet : worksheetParts(pkg)) {
            for (PackagePart dp : pkg.relatedByType(sheet, REL + "drawing")) {
                final Document d = pkg.dom(dp);
                if (d != null && ChartRedactor.redactDrawingText(d.getDocumentElement(), filter, write, captured, order) && write) {
                    pkg.markDirty(dp);
                }
            }
        }
        for (PackagePart sheet : worksheetParts(pkg)) {
            for (PackagePart dp : pkg.relatedByType(sheet, REL + "drawing")) {
                for (PackagePart chart : pkg.relatedByType(dp, REL + "chart")) {
                    final Document d = pkg.dom(chart);
                    if (d != null && ChartRedactor.redactChartPart(d.getDocumentElement(), filter, write, captured, order) && write) {
                        pkg.markDirty(chart);
                    }
                }
            }
        }
        scanPivotCaches(pkg, filter, write, captured, order);
        if (write && anyCellRedacted) {
            clearFormulaCachesAndForceRecalc(pkg);
        }
        if (write) {
            pruneUnusedSharedStrings(pkg);
        }
        return captured;
    }

    private static boolean leaf(final Pkg pkg, final PackagePart part, final Element e, final Filter filter,
                                final boolean write, final List<OfficeSpan> captured, final Order order,
                                final String cls) {
        final String original = e.getTextContent();
        if (original.isEmpty()) {
            return false;
        }
        final TextFilterResult result = filter.apply(original);
        if (result.getFilteredText().equals(original)) {
            return false;
        }
        if (write) {
            setText(e, result.getFilteredText());
            pkg.markDirty(part);
        }
        capture(result, original, -1, cls, captured, order);
        return true;
    }

    // --- cells ---------------------------------------------------------------------

    static List<PackagePart> worksheetParts(final Pkg pkg) throws Exception {
        final PackagePart wb = pkg.mainPart();
        final List<PackagePart> out = new ArrayList<>();
        final Document d = pkg.dom(wb);
        for (Element sheet : desc(d, S, "sheet")) {
            final PackagePart p = pkg.relatedById(wb, sheet.getAttributeNS(R, "id"));
            if (p != null && p.getContentType().contains("worksheet") && pkg.dom(p) != null) {
                out.add(p);
            }
        }
        return out;
    }

    static List<Cell> enumerateCells(final Pkg pkg) throws Exception {
        final List<Cell> out = new ArrayList<>();
        for (PackagePart sheet : worksheetParts(pkg)) {
            final Element sheetData = firstChild(pkg.dom(sheet).getDocumentElement(), S, "sheetData");
            if (sheetData == null) {
                continue;
            }
            for (Element row : children(sheetData, S, "row")) {
                for (Element c : children(row, S, "c")) {
                    out.add(new Cell(c, sheet));
                }
            }
        }
        return out;
    }

    private static List<String> sharedStrings(final Pkg pkg) throws Exception {
        final List<String> out = new ArrayList<>();
        for (PackagePart p : pkg.relatedByType(pkg.mainPart(), REL + "sharedStrings")) {
            final Document d = pkg.dom(p);
            if (d != null) {
                for (Element si : children(d.getDocumentElement(), S, "si")) {
                    out.add(si.getTextContent());
                }
            }
        }
        return out;
    }

    private static boolean isScannableCell(final Element c) {
        final String t = c.getAttribute("t");
        return t.isEmpty() || t.equals("n") || t.equals("s") || t.equals("inlineStr") || t.equals("str") || t.equals("d");
    }

    private static String getCellText(final Element c, final List<String> sst) {
        final String t = c.getAttribute("t");
        final Element v = firstChild(c, S, "v");
        if (t.equals("s")) {
            try {
                final int index = Integer.parseInt(v == null ? "" : v.getTextContent().trim());
                return index >= 0 && index < sst.size() ? sst.get(index) : "";
            } catch (NumberFormatException e) {
                return "";
            }
        }
        if (t.equals("inlineStr")) {
            final Element is = firstChild(c, S, "is");
            if (is == null) {
                return "";
            }
            final Element text = firstChild(is, S, "t");
            return text != null ? text.getTextContent() : is.getTextContent();
        }
        return v == null ? "" : v.getTextContent();
    }

    private static void setCellText(final Element c, final String value) {
        removeChildren(c);
        c.setAttribute("t", "inlineStr");
        final Document doc = c.getOwnerDocument();
        final Element is = doc.createElementNS(S, prefixed(c, "is"));
        final Element t = doc.createElementNS(S, prefixed(c, "t"));
        t.setAttributeNS("http://www.w3.org/XML/1998/namespace", "xml:space", "preserve");
        t.setTextContent(value);
        is.appendChild(t);
        c.appendChild(is);
    }

    // Uses the cell's own prefix (usually none) so new elements match the part's namespace style.
    private static String prefixed(final Element context, final String local) {
        return context.getPrefix() == null ? local : context.getPrefix() + ":" + local;
    }

    private static void clearFormulaCachesAndForceRecalc(final Pkg pkg) throws Exception {
        for (Cell c : enumerateCells(pkg)) {
            if (firstChild(c.cell(), S, "f") != null) {
                final Element v = firstChild(c.cell(), S, "v");
                if (v != null) {
                    c.cell().removeChild(v);
                    pkg.markDirty(c.sheetPart());
                }
            }
        }
        final PackagePart wbPart = pkg.mainPart();
        final Element workbook = pkg.dom(wbPart).getDocumentElement();
        Element calcPr = firstChild(workbook, S, "calcPr");
        if (calcPr == null) {
            calcPr = workbook.getOwnerDocument().createElementNS(S, prefixed(workbook, "calcPr"));
            Element anchor = firstChild(workbook, S, "definedNames");
            if (anchor == null) {
                anchor = firstChild(workbook, S, "sheets");
            }
            if (anchor != null) {
                workbook.insertBefore(calcPr, anchor.getNextSibling());
            } else {
                workbook.appendChild(calcPr);
            }
        }
        calcPr.setAttribute("fullCalcOnLoad", "1");
        pkg.markDirty(wbPart);
    }

    private static void pruneUnusedSharedStrings(final Pkg pkg) throws Exception {
        for (PackagePart p : pkg.relatedByType(pkg.mainPart(), REL + "sharedStrings")) {
            final Document d = pkg.dom(p);
            if (d == null) {
                continue;
            }
            final Set<Integer> referenced = new HashSet<>();
            for (Cell c : enumerateCells(pkg)) {
                final Element v = firstChild(c.cell(), S, "v");
                if ("s".equals(c.cell().getAttribute("t")) && v != null) {
                    try {
                        referenced.add(Integer.parseInt(v.getTextContent().trim()));
                    } catch (NumberFormatException ignored) {
                        // not an index
                    }
                }
            }
            int i = 0;
            boolean changed = false;
            for (Element si : children(d.getDocumentElement(), S, "si")) {
                if (!referenced.contains(i)) {
                    removeChildren(si);
                    si.appendChild(d.createElementNS(S, prefixed(si, "t")));
                    changed = true;
                }
                i++;
            }
            if (changed) {
                pkg.markDirty(p);
            }
        }
    }

    // --- pivot caches ----------------------------------------------------------------

    private static void scanPivotCaches(final Pkg pkg, final Filter filter, final boolean write,
                                        final List<OfficeSpan> captured, final Order order) throws Exception {
        for (PackagePart def : pkg.relatedByType(pkg.mainPart(), REL + "pivotCacheDefinition")) {
            boolean redacted = false;
            final Document defDom = pkg.dom(def);
            if (defDom != null) {
                for (Element s : desc(defDom, S, "s")) {
                    redacted |= pivotString(pkg, def, s, filter, write, captured, order);
                }
            }
            for (PackagePart rec : pkg.relatedByType(def, REL + "pivotCacheRecords")) {
                final Document recDom = pkg.dom(rec);
                if (recDom != null) {
                    for (Element s : desc(recDom, S, "s")) {
                        redacted |= pivotString(pkg, rec, s, filter, write, captured, order);
                    }
                }
            }
            if (write && redacted && defDom != null) {
                defDom.getDocumentElement().setAttribute("refreshOnLoad", "1");
                pkg.markDirty(def);
            }
        }
        for (PackagePart sheet : pkg.allParts(pkg.mainPart())) {
            if (!sheet.getContentType().contains("worksheet")) {
                continue;
            }
            for (PackagePart pt : pkg.relatedByType(sheet, REL + "pivotTable")) {
                final Document d = pkg.dom(pt);
                if (d != null) {
                    for (Element s : desc(d, S, "s")) {
                        pivotString(pkg, pt, s, filter, write, captured, order);
                    }
                }
            }
        }
    }

    private static boolean pivotString(final Pkg pkg, final PackagePart part, final Element item, final Filter filter,
                                       final boolean write, final List<OfficeSpan> captured, final Order order) {
        final String original = item.getAttribute("v");
        if (original.isEmpty()) {
            return false;
        }
        final TextFilterResult result = filter.apply(original);
        if (result.getFilteredText().equals(original)) {
            return false;
        }
        if (write) {
            item.setAttribute("v", result.getFilteredText());
            pkg.markDirty(part);
        }
        capture(result, original, -1, "pivot-cache", captured, order);
        return true;
    }

    // --- chart embedded workbooks ------------------------------------------------------

    private static void scanChartEmbeddings(final Pkg pkg, final Filter filter, final boolean write,
                                            final List<OfficeSpan> captured, final Order order) throws Exception {
        for (PackagePart sheet : worksheetParts(pkg)) {
            for (PackagePart dp : pkg.relatedByType(sheet, REL + "drawing")) {
                for (PackagePart chart : pkg.relatedByType(dp, REL + "chart")) {
                    redactChartEmbeddedWorkbooks(pkg, chart, filter, write, captured, order);
                }
            }
        }
    }

    static void redactChartEmbeddedWorkbooks(final Pkg pkg, final PackagePart chart, final Filter filter,
                                             final boolean write, final List<OfficeSpan> captured,
                                             final Order order) throws Exception {
        final List<PackageRelationship> rels = new ArrayList<>();
        chart.getRelationshipsByType(REL + "package").forEach(rels::add);
        for (PackageRelationship rel : rels) {
            final PackagePart emb = pkg.related(chart, rel);
            if (emb == null) {
                continue;
            }
            final byte[] bytes = pkg.readBytes(emb);
            if (bytes.length == 0) {
                continue;
            }
            if (!write) {
                for (OfficeSpan s : detectEmbedded(bytes, filter)) {
                    captured.add(new OfficeSpan(order.next(), -1, s.start(), s.end(), s.text(), s.replacement(), s.classification()));
                }
                continue;
            }
            final List<OfficeSpan> embSpans = new ArrayList<>();
            final byte[] redacted = redactEmbedded(bytes, filter, embSpans);
            if (redacted != null) {
                pkg.writeBytes(emb, redacted);
                for (OfficeSpan s : embSpans) {
                    captured.add(new OfficeSpan(order.next(), -1, s.start(), s.end(), s.text(), s.replacement(), s.classification()));
                }
            } else {
                final Document d = pkg.dom(chart);
                if (d != null) {
                    for (Element ext : desc(d, C, "externalData")) {
                        ext.getParentNode().removeChild(ext);
                    }
                    pkg.markDirty(chart);
                }
                pkg.deletePart(chart, rel);
                captured.add(new OfficeSpan(order.next(), -1, 0, 0, "[embedded workbook]", "[removed]", "embedded-workbook-removed"));
            }
        }
    }

    static byte[] redactEmbedded(final byte[] bytes, final Filter filter, final List<OfficeSpan> spans) {
        try (Pkg pkg = new Pkg(bytes)) {
            spans.addAll(run(pkg, filter, true));
            return pkg.save();
        } catch (Exception e) {
            spans.clear();
            return null;
        }
    }

    static List<OfficeSpan> detectEmbedded(final byte[] bytes, final Filter filter) {
        try (Pkg pkg = new Pkg(bytes)) {
            return run(pkg, filter, false);
        } catch (Exception e) {
            return List.of();
        }
    }

    private XlsxRedactor() { }
}
