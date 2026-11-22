package spike;

import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static spike.Office.*;
import static spike.Pkg.*;

/** Port of WordDocumentRedactor (Redact, Detect, ReadParagraphs; no ApplySpans or highlight). */
final class WordRedactor {

    static final String REDACTED_HYPERLINK_TARGET = "https://redacted.invalid/";
    static final String CHART_CT = "application/vnd.openxmlformats-officedocument.drawingml.chart+xml";

    record Result(byte[] document, List<OfficeSpan> spans) { }

    record Target(Element paragraph, boolean headerFooter, PackagePart part) { }

    static List<String> readParagraphs(final byte[] input) throws Exception {
        try (Pkg pkg = new Pkg(input)) {
            final List<String> out = new ArrayList<>();
            for (Target t : enumerateTargets(pkg)) {
                out.add(ownText(t.paragraph()));
            }
            return out;
        }
    }

    static Result redact(final byte[] input, final Filter filter) throws Exception {
        try (Pkg pkg = new Pkg(input)) {
            final List<OfficeSpan> captured = run(pkg, filter, true);
            final Order order = new Order();
            order.value = captured.size();
            EmbeddedObjects.process(pkg, filter, true, true, captured, order);
            return new Result(pkg.save(), captured);
        }
    }

    static List<OfficeSpan> detect(final byte[] input, final Filter filter) throws Exception {
        try (Pkg pkg = new Pkg(input)) {
            final List<OfficeSpan> captured = run(pkg, filter, false);
            final Order order = new Order();
            order.value = captured.size();
            EmbeddedObjects.process(pkg, filter, false, false, captured, order);
            return captured;
        }
    }

    /** The Redact core (write) or Detect core (no write), without embedded-object recursion. */
    static List<OfficeSpan> run(final Pkg pkg, final Filter filter, final boolean write) throws Exception {
        final List<OfficeSpan> captured = new ArrayList<>();
        final Order order = new Order();
        if (pkg.mainPart() == null) {
            return captured;
        }
        int paragraphIndex = 0;
        for (Target t : enumerateTargets(pkg)) {
            final String original = ownText(t.paragraph());
            if (!original.isEmpty()) {
                final TextFilterResult result = filter.apply(original);
                if (!result.getFilteredText().equals(original)) {
                    final List<Span> spans = validSorted(result, original);
                    if (write) {
                        applyRangesToParagraph(t.paragraph(), original, ranges(spans));
                        pkg.markDirty(t.part());
                    }
                    capture(result, original, paragraphIndex, null, captured, order);
                }
            }
            paragraphIndex++;
        }
        redactDrawingText(pkg, filter, write, captured, order);
        redactHyperlinkTargets(pkg, filter, write, captured, order);
        redactFieldInstructions(pkg, filter, write, captured, order);
        scanDeletedText(pkg, filter, write, captured, order);
        scanCharts(pkg, filter, write, captured, order);
        if (write) {
            removeThreadedCommentDuplicate(pkg);
        }
        return captured;
    }

    // --- enumeration --------------------------------------------------------------

    static List<Target> enumerateTargets(final Pkg pkg) throws Exception {
        final List<Target> targets = new ArrayList<>();
        final PackagePart main = pkg.mainPart();
        final Document doc = pkg.dom(main);
        if (doc == null) {
            return targets;
        }
        final Element body = first(desc(doc, W, "body"));
        if (body != null) {
            desc(body, W, "p").forEach(p -> targets.add(new Target(p, false, main)));
        }
        for (PackagePart part : orderedHeaderFooterParts(pkg, main, body)) {
            final Document d = pkg.dom(part);
            if (d != null) {
                desc(d, W, "p").forEach(p -> targets.add(new Target(p, true, part)));
            }
        }
        for (String type : List.of("footnotes", "endnotes", "comments")) {
            for (PackagePart part : pkg.relatedByType(main, REL + type)) {
                final Document d = pkg.dom(part);
                if (d != null) {
                    desc(d, W, "p").forEach(p -> targets.add(new Target(p, false, part)));
                }
                break; // one part of each kind, as MainDocumentPart.FootnotesPart etc.
            }
        }
        return targets;
    }

    private static List<PackagePart> orderedHeaderFooterParts(final Pkg pkg, final PackagePart main, final Element body) {
        final List<PackagePart> out = new ArrayList<>();
        if (body == null) {
            return out;
        }
        final List<Element> sectPrs = desc(body, W, "sectPr");
        final Set<String> seen = new HashSet<>();
        for (String kind : List.of("headerReference", "footerReference")) {
            final List<Element> refs = new ArrayList<>();
            sectPrs.forEach(s -> refs.addAll(children(s, W, kind)));
            refs.sort(Comparator.comparingInt(WordRedactor::rank)); // stable, as LINQ OrderBy
            for (Element ref : refs) {
                final PackagePart part = pkg.relatedById(main, ref.getAttributeNS(R, "id"));
                if (part != null && seen.add(part.getPartName().getName())) {
                    out.add(part);
                }
            }
        }
        return out;
    }

    private static int rank(final Element ref) {
        final String type = ref.getAttributeNS(W, "type");
        if (type.isEmpty()) {
            return 3;
        }
        return switch (type) {
            case "first" -> 0;
            case "even" -> 1;
            default -> 2;
        };
    }

    // --- paragraph text --------------------------------------------------------------

    private record Content(String text, Element element) { }

    static String ownText(final Element paragraph) {
        final StringBuilder sb = new StringBuilder();
        ownContent(paragraph).forEach(c -> sb.append(c.text()));
        return sb.toString();
    }

    private static List<Content> ownContent(final Element paragraph) {
        final List<Content> out = new ArrayList<>();
        for (Element e : allDesc(paragraph)) {
            if (is(e, W, "t")) {
                if (belongsDirectlyTo(paragraph, e)) {
                    out.add(new Content(e.getTextContent(), e));
                }
            } else {
                final String c = runCharacter(e);
                if (c != null && isOwnRunCharacter(paragraph, e)) {
                    out.add(new Content(c, e));
                }
            }
        }
        return out;
    }

    private static String runCharacter(final Element e) {
        if (!W.equals(e.getNamespaceURI())) {
            return null;
        }
        return switch (e.getLocalName()) {
            case "br", "cr" -> "\n";
            case "tab", "ptab" -> "\t";
            case "noBreakHyphen" -> "-";
            case "sym" -> " ";
            case "softHyphen" -> "";
            default -> null;
        };
    }

    private static boolean isLayout(final Element e) {
        final String n = e.getLocalName();
        return n.equals("br") || n.equals("cr") || n.equals("tab") || n.equals("ptab");
    }

    private static boolean isOwnRunCharacter(final Element paragraph, final Element e) {
        final Node parent = e.getParentNode();
        return is(parent, W, "r")
                && belongsDirectlyTo(paragraph, e)
                && nearestAncestor(e, W, "del") == null
                && nearestAncestor(e, W, "moveFrom") == null
                && !hasDrawing((Element) parent);
    }

    private static boolean hasDrawing(final Element e) {
        return !desc(e, W, "drawing").isEmpty() || !desc(e, W, "pict").isEmpty();
    }

    private static boolean belongsDirectlyTo(final Element owner, final Element e) {
        return nearestAncestor(e, W, "p") == owner;
    }

    private record RunCharacters(Map<Integer, Element> characters, Map<Integer, List<Element>> zeroWidth) { }

    private static RunCharacters ownRunCharacters(final Element paragraph) {
        final Map<Integer, Element> characters = new HashMap<>();
        final Map<Integer, List<Element>> zeroWidth = new HashMap<>();
        int offset = 0;
        for (Content c : ownContent(paragraph)) {
            if (!is(c.element(), W, "t")) {
                if (c.text().isEmpty()) {
                    zeroWidth.computeIfAbsent(offset, k -> new ArrayList<>()).add(c.element());
                } else {
                    characters.put(offset, c.element());
                }
            }
            offset += c.text().length();
        }
        return new RunCharacters(characters, zeroWidth);
    }

    private static List<Element> ownTextRuns(final Element paragraph) {
        final List<Element> out = new ArrayList<>();
        for (Element r : desc(paragraph, W, "r")) {
            if (!belongsDirectlyTo(paragraph, r) || hasDrawing(r)) {
                continue;
            }
            boolean own = !children(r, W, "t").isEmpty();
            for (Node n = r.getFirstChild(); !own && n != null; n = n.getNextSibling()) {
                own = n instanceof Element e && runCharacter(e) != null && isOwnRunCharacter(paragraph, e);
            }
            if (own) {
                out.add(r);
            }
        }
        return out;
    }

    private static boolean containsDrawing(final Element paragraph) {
        return hasDrawing(paragraph) || !desc(paragraph, W, "p").isEmpty();
    }

    // --- rebuild --------------------------------------------------------------------

    private static void applyRangesToParagraph(final Element paragraph, final String ownText, final List<Range> ranges) {
        if (ranges.isEmpty()) {
            return;
        }
        if (containsDrawing(paragraph)) {
            rebuildComplexParagraph(paragraph, ownText, ranges);
        } else {
            rebuildParagraph(paragraph, ownText, ranges);
        }
    }

    private static void rebuildParagraph(final Element paragraph, final String original, final List<Range> ranges) {
        final RunCharacters marks = ownRunCharacters(paragraph);
        final Element properties = firstChild(paragraph, W, "pPr");
        removeChildren(paragraph);
        if (properties != null) {
            paragraph.appendChild(properties);
        }
        for (Element run : buildRuns(paragraph.getOwnerDocument(), original, ranges, marks)) {
            paragraph.appendChild(run);
        }
    }

    private static void rebuildComplexParagraph(final Element paragraph, final String ownText, final List<Range> ranges) {
        final List<Element> ownRuns = ownTextRuns(paragraph);
        if (ownRuns.isEmpty()) {
            return;
        }
        final RunCharacters marks = ownRunCharacters(paragraph);
        final Element anchor = ownRuns.get(0);
        final Node parent = anchor.getParentNode();
        for (Element run : buildRuns(paragraph.getOwnerDocument(), ownText, ranges, marks)) {
            parent.insertBefore(run, anchor);
        }
        for (Element run : ownRuns) {
            run.getParentNode().removeChild(run);
        }
    }

    private static List<Element> buildRuns(final Document doc, final String text, final List<Range> ranges,
                                           final RunCharacters marks) {
        final List<Element> runs = new ArrayList<>();
        int last = 0;
        final List<Range> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparingInt(Range::start));
        for (Range range : sorted) {
            if (range.start() < last || range.end() > text.length()) {
                continue;
            }
            if (range.start() > last) {
                runs.add(makeRun(doc, text, last, range.start(), marks));
            }
            final Element replacement = newRun(doc);
            replacement.appendChild(preservedText(doc, range.replacement()));
            marks.characters().entrySet().stream()
                    .filter(c -> c.getKey() >= range.start() && c.getKey() < range.end() && isLayout(c.getValue()))
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(c -> replacement.appendChild(c.getValue().cloneNode(true)));
            runs.add(replacement);
            last = range.end();
        }
        if (last < text.length()) {
            runs.add(makeRun(doc, text, last, text.length(), marks));
        }
        return runs;
    }

    private static Element makeRun(final Document doc, final String text, final int start, final int end,
                                   final RunCharacters marks) {
        final Element run = newRun(doc);
        final int[] segment = {start};
        final java.util.function.IntConsumer flush = upTo -> {
            if (upTo > segment[0]) {
                run.appendChild(preservedText(doc, text.substring(segment[0], upTo)));
            }
            segment[0] = upTo;
        };
        final java.util.function.IntConsumer emitZeroWidth = offset -> {
            final List<Element> elements = marks.zeroWidth().get(offset);
            if (elements != null) {
                flush.accept(offset);
                elements.forEach(e -> run.appendChild(e.cloneNode(true)));
            }
        };
        if (start == 0) {
            emitZeroWidth.accept(0);
        }
        for (int i = start; i < end; i++) {
            final Element element = marks.characters().get(i);
            if (element != null) {
                flush.accept(i);
                run.appendChild(element.cloneNode(true));
                segment[0] = i + 1;
            }
            emitZeroWidth.accept(i + 1);
        }
        flush.accept(end);
        return run;
    }

    private static Element newRun(final Document doc) {
        return doc.createElementNS(W, "w:r");
    }

    private static Element preservedText(final Document doc, final String text) {
        final Element t = doc.createElementNS(W, "w:t");
        t.setAttributeNS("http://www.w3.org/XML/1998/namespace", "xml:space", "preserve");
        t.setTextContent(text);
        return t;
    }

    // --- other passes ---------------------------------------------------------------

    private static void redactDrawingText(final Pkg pkg, final Filter filter, final boolean write,
                                          final List<OfficeSpan> captured, final Order order) throws Exception {
        for (PackagePart part : pkg.allParts(pkg.mainPart())) {
            if (CHART_CT.equals(part.getContentType())) {
                continue;
            }
            final Document d = pkg.dom(part);
            if (d != null && ChartRedactor.redactDrawingText(d.getDocumentElement(), filter, write, captured, order) && write) {
                pkg.markDirty(part);
            }
        }
    }

    private static void scanDeletedText(final Pkg pkg, final Filter filter, final boolean write,
                                        final List<OfficeSpan> captured, final Order order) throws Exception {
        for (PackagePart part : pkg.allParts(pkg.mainPart())) {
            final Document d = pkg.dom(part);
            if (d == null) {
                continue;
            }
            for (Element deleted : desc(d, W, "delText")) {
                final String original = deleted.getTextContent();
                if (original.isEmpty()) {
                    continue;
                }
                final TextFilterResult result = filter.apply(original);
                if (result.getFilteredText().equals(original)) {
                    continue;
                }
                if (write) {
                    setText(deleted, result.getFilteredText());
                    pkg.markDirty(part);
                }
                capture(result, original, -1, null, captured, order);
            }
        }
    }

    private static void scanCharts(final Pkg pkg, final Filter filter, final boolean write,
                                   final List<OfficeSpan> captured, final Order order) throws Exception {
        for (PackagePart part : pkg.allParts(pkg.mainPart())) {
            if (!CHART_CT.equals(part.getContentType())) {
                continue;
            }
            final Document d = pkg.dom(part);
            if (d != null && ChartRedactor.redactChartPart(d.getDocumentElement(), filter, write, captured, order) && write) {
                pkg.markDirty(part);
            }
            XlsxRedactor.redactChartEmbeddedWorkbooks(pkg, part, filter, write, captured, order);
        }
    }

    private static void redactHyperlinkTargets(final Pkg pkg, final Filter filter, final boolean write,
                                               final List<OfficeSpan> captured, final Order order) throws Exception {
        for (PackagePart part : pkg.allParts(pkg.mainPart())) {
            if (part.isRelationshipPart()) {
                continue;
            }
            final List<PackageRelationship> rels = new ArrayList<>();
            part.getRelationshipsByType(REL + "hyperlink").forEach(rels::add);
            for (PackageRelationship rel : rels) {
                if (rel.getTargetMode() != TargetMode.EXTERNAL) {
                    continue;
                }
                final String target = rel.getTargetURI().toString();
                if (target.isEmpty() || target.equalsIgnoreCase(REDACTED_HYPERLINK_TARGET)) {
                    continue;
                }
                final TextFilterResult result = filter.apply(target);
                if (spans(result).isEmpty()) {
                    continue;
                }
                if (write) {
                    final String id = rel.getId();
                    part.removeRelationship(id);
                    part.addExternalRelationship(REDACTED_HYPERLINK_TARGET, REL + "hyperlink", id);
                }
                captured.add(new OfficeSpan(order.next(), -1, 0, target.length(), target,
                        REDACTED_HYPERLINK_TARGET, "hyperlink-target"));
            }
        }
    }

    private static void redactFieldInstructions(final Pkg pkg, final Filter filter, final boolean write,
                                                final List<OfficeSpan> captured, final Order order) throws Exception {
        for (PackagePart part : pkg.allParts(pkg.mainPart())) {
            final Document d = pkg.dom(part);
            if (d == null) {
                continue;
            }
            for (Element field : desc(d, W, "fldSimple")) {
                final String original = field.getAttributeNS(W, "instr");
                if (original.isEmpty()) {
                    continue;
                }
                final TextFilterResult result = filter.apply(original);
                if (result.getFilteredText().equals(original)) {
                    continue;
                }
                if (write) {
                    field.setAttributeNS(W, "w:instr", result.getFilteredText());
                    pkg.markDirty(part);
                }
                capture(result, original, -1, "field-instruction", captured, order);
            }
            for (List<Element> group : groupFieldCodes(d)) {
                final StringBuilder sb = new StringBuilder();
                group.forEach(c -> sb.append(c.getTextContent()));
                final String original = sb.toString();
                if (original.isEmpty()) {
                    continue;
                }
                final TextFilterResult result = filter.apply(original);
                if (result.getFilteredText().equals(original)) {
                    continue;
                }
                if (write) {
                    setText(group.get(0), result.getFilteredText());
                    for (int i = 1; i < group.size(); i++) {
                        setText(group.get(i), "");
                    }
                    pkg.markDirty(part);
                }
                capture(result, original, -1, "field-instruction", captured, order);
            }
        }
    }

    private static List<List<Element>> groupFieldCodes(final Document d) {
        final List<List<Element>> fields = new ArrayList<>();
        List<Element> current = null;
        for (Element run : desc(d, W, "r")) {
            final Element fldChar = firstChild(run, W, "fldChar");
            if (fldChar != null && fldChar.hasAttributeNS(W, "fldCharType")) {
                if ("begin".equals(fldChar.getAttributeNS(W, "fldCharType"))) {
                    current = new ArrayList<>();
                } else {
                    if (current != null && !current.isEmpty()) {
                        fields.add(current);
                    }
                    current = null;
                }
            }
            if (current != null) {
                current.addAll(children(run, W, "instrText"));
            }
        }
        if (current != null && !current.isEmpty()) {
            fields.add(current);
        }
        return fields;
    }

    private static void removeThreadedCommentDuplicate(final Pkg pkg) throws Exception {
        final PackagePart main = pkg.mainPart();
        final List<PackageRelationship> threaded = new ArrayList<>();
        final List<PackageRelationship> companions = new ArrayList<>();
        for (PackageRelationship rel : main.getRelationships()) {
            final PackagePart p = pkg.related(main, rel);
            if (p == null) {
                continue;
            }
            final String ct = p.getContentType().toLowerCase();
            if (ct.contains("threadedcomment") || p.getPartName().getName().toLowerCase().endsWith("threadedcomments.xml")) {
                threaded.add(rel);
            } else if (ct.contains("commentsextended") || ct.contains("commentsids")) {
                companions.add(rel);
            }
        }
        if (threaded.isEmpty()) {
            return;
        }
        final Set<PackageRelationship> all = new LinkedHashSet<>(threaded);
        all.addAll(companions);
        for (PackageRelationship rel : all) {
            pkg.deletePart(main, rel);
        }
    }

    private static Element first(final List<Element> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    private WordRedactor() { }
}
