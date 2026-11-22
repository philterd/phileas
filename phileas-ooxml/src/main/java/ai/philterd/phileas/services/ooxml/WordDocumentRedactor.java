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

import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static ai.philterd.phileas.services.ooxml.OfficeSpanMath.Order;
import static ai.philterd.phileas.services.ooxml.OfficeSpanMath.ReplacementRange;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.REL;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.allDescendants;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.children;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.descendants;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.firstChild;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.is;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.nearestAncestor;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.removeChildren;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.setText;

/**
 * Redacts Word (.docx) documents. A port of phileas-dotnet's {@code WordDocumentRedactor}.
 *
 * <p>Each paragraph's text is filtered and, when the filter changes it, the paragraph is rebuilt from the
 * resulting spans. Rebuilding a changed paragraph flattens its inline run formatting (hyperlinks and
 * fields in it collapse to plain text); unchanged paragraphs are left exactly as they were. Further
 * passes cover DrawingML text, hyperlink targets, field instructions, tracked deletions, charts and their
 * embedded workbooks, and embedded objects.
 */
public final class WordDocumentRedactor {

    static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    static final String R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    static final String CHART_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.drawingml.chart+xml";

    private static final String XML_NS = "http://www.w3.org/XML/1998/namespace";
    private static final String DEFAULT_REPLACEMENT = "{{{REDACTED-custom}}}";

    /** A hyperlink target the policy flags is rewritten to this; ".invalid" never resolves (RFC 2606). */
    public static final String REDACTED_HYPERLINK_TARGET = "https://redacted.invalid/";
    static final String HYPERLINK_CLASSIFICATION = "hyperlink-target";
    static final String FIELD_INSTRUCTION_CLASSIFICATION = "field-instruction";

    private WordDocumentRedactor() {
    }

    // --- public API -------------------------------------------------------------------

    /**
     * The text of each redactable paragraph in the canonical order: body (with tables), headers and
     * footers, footnotes, endnotes, then comments. Index {@code i} is paragraph index {@code i}.
     */
    public static List<String> readParagraphs(final byte[] input) throws IOException {
        try (OoxmlPackage pkg = new OoxmlPackage(input)) {
            final List<String> out = new ArrayList<>();
            for (final Target t : enumerateTargets(pkg)) {
                out.add(ownText(t.paragraph()));
            }
            return out;
        }
    }

    /**
     * Every readable line for a before/after review: the paragraphs, as {@link #readParagraphs}, plus the
     * DrawingML text of shapes, SmartArt, and charts. Not paragraph-index aligned.
     */
    public static List<String> readReviewLines(final byte[] input) throws IOException {
        try (OoxmlPackage pkg = new OoxmlPackage(input)) {
            final List<String> lines = new ArrayList<>();
            for (final Target t : enumerateTargets(pkg)) {
                lines.add(ownText(t.paragraph()));
            }
            for (final PackagePart part : pkg.allParts(pkg.mainPart())) {
                final Document d = pkg.dom(part);
                if (d == null) {
                    continue;
                }
                for (final Element paragraph : descendants(d, ChartRedactor.A, "p")) {
                    if (ChartRedactor.hasText(paragraph)) {
                        lines.add(ChartRedactor.paragraphText(paragraph));
                    }
                }
            }
            return lines;
        }
    }

    /** Detects what {@link #redact} would redact, without producing a document. */
    public static List<OfficeRedactionSpan> detect(final byte[] input, final TextFilter filter,
                                                   final OoxmlRedactionOptions options) throws IOException {
        try (OoxmlPackage pkg = new OoxmlPackage(input)) {
            final List<OfficeRedactionSpan> captured = run(pkg, filter, false, false,
                    options.isRedactHeadersFooters(), options.isRedactCharts());
            EmbeddedObjectRedactor.process(pkg, filter, false, false, captured, new Order(captured.size()));
            return captured;
        }
    }

    /** Redacts the document and returns the redacted bytes and the redactions made. */
    public static OoxmlRedactionResult redact(final byte[] input, final TextFilter filter,
                                              final OoxmlRedactionOptions options) throws IOException {
        try (OoxmlPackage pkg = new OoxmlPackage(input)) {
            final List<OfficeRedactionSpan> captured = run(pkg, filter, true, options.isHighlight(),
                    options.isRedactHeadersFooters(), options.isRedactCharts());
            EmbeddedObjectRedactor.process(pkg, filter, options.isRemoveUninspectableEmbeddedObjects(), true,
                    captured, new Order(captured.size()));
            return new OoxmlRedactionResult(pkg.save(), captured);
        }
    }

    /**
     * Applies spans (detected or added by a reviewer) by position: each span's paragraph index and its
     * character offsets within that paragraph. Spans with paragraph index -1 cannot be placed; supply
     * {@code drawingFilter} to re-redact that content (drawings, hyperlink targets, field instructions,
     * tracked deletions, charts, and embedded objects) with the policy.
     */
    public static byte[] applySpans(final byte[] input, final List<OfficeRedactionSpan> spans,
                                    final OoxmlRedactionOptions options, final TextFilter drawingFilter) throws IOException {
        final Map<Integer, List<OfficeRedactionSpan>> byParagraph = new HashMap<>();
        for (final OfficeRedactionSpan s : spans) {
            byParagraph.computeIfAbsent(s.getParagraphIndex(), k -> new ArrayList<>()).add(s);
        }
        try (OoxmlPackage pkg = new OoxmlPackage(input)) {
            int paragraphIndex = 0;
            for (final Target t : enumerateTargets(pkg)) {
                final String original = ownText(t.paragraph());
                final List<OfficeRedactionSpan> paragraphSpans = byParagraph.get(paragraphIndex);
                if (!original.isEmpty() && paragraphSpans != null) {
                    final List<ReplacementRange> ranges = new ArrayList<>();
                    for (final OfficeRedactionSpan s : paragraphSpans) {
                        if (s.getCharacterStart() >= 0 && s.getCharacterEnd() <= original.length()
                                && s.getCharacterEnd() > s.getCharacterStart()) {
                            final String replacement = s.getReplacement() == null || s.getReplacement().isEmpty()
                                    ? DEFAULT_REPLACEMENT : s.getReplacement();
                            ranges.add(new ReplacementRange(s.getCharacterStart(), s.getCharacterEnd(), replacement));
                        }
                    }
                    final List<ReplacementRange> resolved = OfficeSpanMath.resolveNonOverlapping(ranges);
                    if (!resolved.isEmpty()) {
                        applyRangesToParagraph(t.paragraph(), original, resolved, options.isHighlight());
                        pkg.markDirty(t.part());
                    }
                }
                paragraphIndex++;
            }
            if (drawingFilter != null) {
                final Order order = new Order();
                redactDrawingText(pkg, drawingFilter, true, null, order);
                redactHyperlinkTargets(pkg, drawingFilter, true, null, order);
                redactFieldInstructions(pkg, drawingFilter, true, null, order);
                scanDeletedText(pkg, drawingFilter, true, null, order);
                if (options.isRedactCharts()) {
                    scanCharts(pkg, drawingFilter, true, null, order);
                }
                EmbeddedObjectRedactor.process(pkg, drawingFilter, options.isRemoveUninspectableEmbeddedObjects(),
                        true, null, order);
            }
            removeThreadedCommentDuplicate(pkg);
            return pkg.save();
        }
    }

    // --- core --------------------------------------------------------------------------

    /** Redact (write) or Detect (no write), without the embedded-object pass, which runs at the top level only. */
    static List<OfficeRedactionSpan> run(final OoxmlPackage pkg, final TextFilter filter, final boolean write,
                                         final boolean highlight, final boolean redactHeadersFooters,
                                         final boolean redactCharts) {
        final List<OfficeRedactionSpan> captured = new ArrayList<>();
        final Order order = new Order();
        if (pkg.mainPart() == null) {
            return captured;
        }
        int paragraphIndex = 0;
        for (final Target t : enumerateTargets(pkg)) {
            final String original = ownText(t.paragraph());
            if (!original.isEmpty() && (redactHeadersFooters || !t.headerFooter())) {
                final TextFilterResult result = filter.apply(original);
                // Spans are reported only when the filter changed the paragraph, so Detect and Redact agree
                // (a strategy whose replacement equals the original changes nothing).
                if (!result.getFilteredText().equals(original)) {
                    final List<Span> spans = OfficeSpanMath.validSpans(result, original);
                    if (write) {
                        applyRangesToParagraph(t.paragraph(), original, OfficeSpanMath.ranges(spans), highlight);
                        pkg.markDirty(t.part());
                    }
                    OfficeSpanMath.capture(result, original, paragraphIndex, null, captured, order);
                }
            }
            paragraphIndex++;
        }
        redactDrawingText(pkg, filter, write, captured, order);
        redactHyperlinkTargets(pkg, filter, write, captured, order);
        redactFieldInstructions(pkg, filter, write, captured, order);
        scanDeletedText(pkg, filter, write, captured, order);
        if (redactCharts) {
            scanCharts(pkg, filter, write, captured, order);
        }
        if (write) {
            removeThreadedCommentDuplicate(pkg);
        }
        return captured;
    }

    /**
     * Redacts an embedded .docx with the standard passes; null when it cannot be opened as one. A failure of
     * the filter itself propagates rather than being taken for an unreadable document.
     */
    static byte[] redactEmbeddedBytes(final byte[] bytes, final TextFilter filter, final List<OfficeRedactionSpan> spans) {
        final OoxmlPackage pkg = openEmbedded(bytes);
        if (pkg == null) {
            return null;
        }
        try (pkg) {
            if (pkg.mainPart() == null || pkg.dom(pkg.mainPart()) == null) {
                return null;
            }
            final List<OfficeRedactionSpan> redacted = run(pkg, filter, true, false, true, true);
            final byte[] output = pkg.save();
            spans.addAll(redacted);
            return output;
        } catch (final IOException e) {
            return null;
        }
    }

    static List<OfficeRedactionSpan> detectEmbeddedBytes(final byte[] bytes, final TextFilter filter) {
        final OoxmlPackage pkg = openEmbedded(bytes);
        if (pkg == null) {
            return new ArrayList<>();
        }
        try (pkg) {
            return pkg.mainPart() == null ? new ArrayList<>() : run(pkg, filter, false, false, true, true);
        }
    }

    /** The embedded package, or null when it is not a readable Office Open XML package. */
    static OoxmlPackage openEmbedded(final byte[] bytes) {
        try {
            return new OoxmlPackage(bytes);
        } catch (final IOException | RuntimeException e) {
            return null;
        }
    }

    // --- enumeration ---------------------------------------------------------------------

    record Target(Element paragraph, boolean headerFooter, PackagePart part) {
    }

    // Body (including tables), then header and footer parts (First, Even, Default), then footnotes,
    // endnotes, and comments. Redact and applySpans must agree on this order.
    static List<Target> enumerateTargets(final OoxmlPackage pkg) {
        final List<Target> targets = new ArrayList<>();
        final PackagePart main = pkg.mainPart();
        final Document doc = pkg.dom(main);
        if (doc == null) {
            return targets;
        }
        final List<Element> bodies = descendants(doc, W, "body");
        final Element body = bodies.isEmpty() ? null : bodies.get(0);
        if (body != null) {
            for (final Element p : descendants(body, W, "p")) {
                targets.add(new Target(p, false, main));
            }
        }
        for (final PackagePart part : orderedHeaderFooterParts(pkg, main, body)) {
            final Document d = pkg.dom(part);
            if (d != null) {
                for (final Element p : descendants(d, W, "p")) {
                    targets.add(new Target(p, true, part));
                }
            }
        }
        for (final String type : List.of("footnotes", "endnotes", "comments")) {
            final List<PackagePart> parts = pkg.relatedByType(main, REL + type);
            if (!parts.isEmpty()) {
                final Document d = pkg.dom(parts.get(0));
                if (d != null) {
                    for (final Element p : descendants(d, W, "p")) {
                        targets.add(new Target(p, false, parts.get(0)));
                    }
                }
            }
        }
        return targets;
    }

    private static List<PackagePart> orderedHeaderFooterParts(final OoxmlPackage pkg, final PackagePart main, final Element body) {
        final List<PackagePart> out = new ArrayList<>();
        if (body == null) {
            return out;
        }
        final List<Element> sectPrs = descendants(body, W, "sectPr");
        final Set<String> seen = new HashSet<>();
        for (final String kind : List.of("headerReference", "footerReference")) {
            final List<Element> refs = new ArrayList<>();
            for (final Element sectPr : sectPrs) {
                refs.addAll(children(sectPr, W, kind));
            }
            refs.sort(Comparator.comparingInt(WordDocumentRedactor::rank));
            for (final Element ref : refs) {
                final PackagePart part = pkg.relatedById(main, ref.getAttributeNS(R, "id"));
                if (part != null && seen.add(part.getPartName().getName().toLowerCase(Locale.ROOT))) {
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

    // --- paragraph text --------------------------------------------------------------------

    private record Content(String text, Element element) {
    }

    /**
     * The paragraph's own visible text, excluding text in a nested text box. A break or carriage return is
     * "\n", a tab "\t", a non-breaking hyphen "-", a symbol-font character a space, and a soft hyphen
     * nothing, so each character element maps back to one offset.
     */
    static String ownText(final Element paragraph) {
        final StringBuilder sb = new StringBuilder();
        for (final Content c : ownContent(paragraph)) {
            sb.append(c.text());
        }
        return sb.toString();
    }

    private static List<Content> ownContent(final Element paragraph) {
        final List<Content> out = new ArrayList<>();
        for (final Element e : allDescendants(paragraph)) {
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

    // Breaks and tabs are layout: one inside a redacted value is kept after its replacement.
    private static boolean isLayout(final Element e) {
        final String n = e.getLocalName();
        return "br".equals(n) || "cr".equals(n) || "tab".equals(n) || "ptab".equals(n);
    }

    // In this paragraph's own run (not a nested text box), not in a tracked deletion or move-from, and not
    // in a drawing run, which the rebuild leaves in place.
    private static boolean isOwnRunCharacter(final Element paragraph, final Element e) {
        final Node parent = e.getParentNode();
        return is(parent, W, "r")
                && belongsDirectlyTo(paragraph, e)
                && nearestAncestor(e, W, "del") == null
                && nearestAncestor(e, W, "moveFrom") == null
                && !hasDrawing((Element) parent);
    }

    private static boolean hasDrawing(final Element e) {
        return !descendants(e, W, "drawing").isEmpty() || !descendants(e, W, "pict").isEmpty();
    }

    private static boolean belongsDirectlyTo(final Element owner, final Element e) {
        return nearestAncestor(e, W, "p") == owner;
    }

    // The paragraph's run-level character elements by their offset in ownText. A soft hyphen has no
    // character, so it is keyed by the offset that follows it.
    private record RunCharacters(Map<Integer, Element> characters, Map<Integer, List<Element>> zeroWidth) {
    }

    private static RunCharacters ownRunCharacters(final Element paragraph) {
        final Map<Integer, Element> characters = new TreeMap<>();
        final Map<Integer, List<Element>> zeroWidth = new HashMap<>();
        int offset = 0;
        for (final Content c : ownContent(paragraph)) {
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

    // Runs holding the paragraph's own text or run-level characters, excluding drawing runs and runs in a
    // nested text box.
    private static List<Element> ownTextRuns(final Element paragraph) {
        final List<Element> out = new ArrayList<>();
        for (final Element r : descendants(paragraph, W, "r")) {
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

    // A paragraph with a drawing, picture, or text box is redacted run by run rather than rebuilt, so the
    // drawing survives.
    private static boolean containsDrawing(final Element paragraph) {
        return hasDrawing(paragraph) || !descendants(paragraph, W, "p").isEmpty();
    }

    // --- rebuild ------------------------------------------------------------------------------

    private static void applyRangesToParagraph(final Element paragraph, final String ownText,
                                               final List<ReplacementRange> ranges, final boolean highlight) {
        if (ranges.isEmpty()) {
            return;
        }
        if (containsDrawing(paragraph)) {
            rebuildComplexParagraph(paragraph, ownText, ranges, highlight);
        } else {
            rebuildParagraph(paragraph, ownText, ranges, highlight);
        }
    }

    private static void rebuildParagraph(final Element paragraph, final String original,
                                         final List<ReplacementRange> ranges, final boolean highlight) {
        final RunCharacters marks = ownRunCharacters(paragraph);
        final Element properties = firstChild(paragraph, W, "pPr");
        removeChildren(paragraph);
        if (properties != null) {
            paragraph.appendChild(properties);
        }
        for (final Element run : buildRuns(paragraph.getOwnerDocument(), original, ranges, highlight, marks)) {
            paragraph.appendChild(run);
        }
    }

    private static void rebuildComplexParagraph(final Element paragraph, final String ownText,
                                                final List<ReplacementRange> ranges, final boolean highlight) {
        final List<Element> ownRuns = ownTextRuns(paragraph);
        if (ownRuns.isEmpty()) {
            return;
        }
        final RunCharacters marks = ownRunCharacters(paragraph);
        final Element anchor = ownRuns.get(0);
        final Node parent = anchor.getParentNode();
        for (final Element run : buildRuns(paragraph.getOwnerDocument(), ownText, ranges, highlight, marks)) {
            parent.insertBefore(run, anchor);
        }
        for (final Element run : ownRuns) {
            run.getParentNode().removeChild(run);
        }
    }

    // Plain runs for kept text and one run per replacement. A break or tab inside a replaced span follows
    // the replacement; a hyphen, symbol, or soft hyphen inside it goes with the value.
    private static List<Element> buildRuns(final Document doc, final String text, final List<ReplacementRange> ranges,
                                           final boolean highlight, final RunCharacters marks) {
        final List<Element> runs = new ArrayList<>();
        int last = 0;
        final List<ReplacementRange> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparingInt(ReplacementRange::start));
        for (final ReplacementRange range : sorted) {
            if (range.start() < last || range.end() > text.length()) {
                continue;
            }
            if (range.start() > last) {
                runs.add(makeRun(doc, text, last, range.start(), marks));
            }
            final Element replacement = newRun(doc, highlight);
            replacement.appendChild(preservedText(doc, range.replacement() == null ? "" : range.replacement()));
            for (final Map.Entry<Integer, Element> c : marks.characters().entrySet()) {
                if (c.getKey() >= range.start() && c.getKey() < range.end() && isLayout(c.getValue())) {
                    replacement.appendChild(c.getValue().cloneNode(true));
                }
            }
            runs.add(replacement);
            last = range.end();
        }
        if (last < text.length()) {
            runs.add(makeRun(doc, text, last, text.length(), marks));
        }
        return runs;
    }

    // A run for text[start, end), with run-level characters emitted as their elements and soft hyphens
    // after the character they followed.
    private static Element makeRun(final Document doc, final String text, final int start, final int end,
                                   final RunCharacters marks) {
        final Element run = newRun(doc, false);
        int segment = start;
        if (start == 0) {
            segment = emitZeroWidth(doc, run, text, segment, 0, marks);
        }
        for (int i = start; i < end; i++) {
            final Element element = marks.characters().get(i);
            if (element != null) {
                if (i > segment) {
                    run.appendChild(preservedText(doc, text.substring(segment, i)));
                }
                run.appendChild(element.cloneNode(true));
                segment = i + 1;
            }
            segment = emitZeroWidth(doc, run, text, segment, i + 1, marks);
        }
        if (end > segment) {
            run.appendChild(preservedText(doc, text.substring(segment, end)));
        }
        return run;
    }

    private static int emitZeroWidth(final Document doc, final Element run, final String text, final int segment,
                                     final int offset, final RunCharacters marks) {
        final List<Element> elements = marks.zeroWidth().get(offset);
        if (elements == null) {
            return segment;
        }
        if (offset > segment) {
            run.appendChild(preservedText(doc, text.substring(segment, offset)));
        }
        for (final Element element : elements) {
            run.appendChild(element.cloneNode(true));
        }
        return Math.max(segment, offset);
    }

    private static Element newRun(final Document doc, final boolean highlight) {
        final Element run = doc.createElementNS(W, "w:r");
        if (highlight) {
            final Element rPr = doc.createElementNS(W, "w:rPr");
            final Element mark = doc.createElementNS(W, "w:highlight");
            mark.setAttributeNS(W, "w:val", "yellow");
            rPr.appendChild(mark);
            run.appendChild(rPr);
        }
        return run;
    }

    private static Element preservedText(final Document doc, final String text) {
        final Element t = doc.createElementNS(W, "w:t");
        t.setAttributeNS(XML_NS, "xml:space", "preserve");
        t.setTextContent(text);
        return t;
    }

    // --- other passes ----------------------------------------------------------------------------

    // DrawingML text (shapes, text boxes, SmartArt) in every part but charts, which the chart pass handles.
    private static void redactDrawingText(final OoxmlPackage pkg, final TextFilter filter, final boolean write,
                                          final List<OfficeRedactionSpan> captured, final Order order) {
        for (final PackagePart part : pkg.allParts(pkg.mainPart())) {
            if (CHART_CONTENT_TYPE.equals(part.getContentType())) {
                continue;
            }
            final Document d = pkg.dom(part);
            if (d != null && ChartRedactor.redactDrawingText(d.getDocumentElement(), filter, write, captured, order) && write) {
                pkg.markDirty(part);
            }
        }
    }

    // Tracked deletions (w:delText) keep their text recoverable through Reject Changes.
    private static void scanDeletedText(final OoxmlPackage pkg, final TextFilter filter, final boolean write,
                                        final List<OfficeRedactionSpan> captured, final Order order) {
        for (final PackagePart part : pkg.allParts(pkg.mainPart())) {
            final Document d = pkg.dom(part);
            if (d == null) {
                continue;
            }
            for (final Element deleted : descendants(d, W, "delText")) {
                if (ChartRedactor.redactLeaf(deleted, filter, write, captured, order) && write) {
                    pkg.markDirty(part);
                }
            }
        }
    }

    // Chart text and cached values, then each chart's embedded source workbook.
    private static void scanCharts(final OoxmlPackage pkg, final TextFilter filter, final boolean write,
                                   final List<OfficeRedactionSpan> captured, final Order order) {
        for (final PackagePart part : pkg.allParts(pkg.mainPart())) {
            if (!CHART_CONTENT_TYPE.equals(part.getContentType())) {
                continue;
            }
            final Document d = pkg.dom(part);
            if (d != null && ChartRedactor.redactChartPart(d.getDocumentElement(), filter, write, captured, order) && write) {
                pkg.markDirty(part);
            }
            XlsxRedactor.redactChartEmbeddedWorkbooks(pkg, part, filter, write, captured, order);
        }
    }

    // External hyperlink targets (mailto: addresses, URLs, file paths) are stored as relationships. A
    // target the policy flags is rewritten to a placeholder that keeps the relationship id.
    private static void redactHyperlinkTargets(final OoxmlPackage pkg, final TextFilter filter, final boolean write,
                                               final List<OfficeRedactionSpan> captured, final Order order) {
        for (final PackagePart part : pkg.allParts(pkg.mainPart())) {
            for (final PackageRelationship rel : pkg.relationshipsByType(part, REL + "hyperlink")) {
                if (rel.getTargetMode() != TargetMode.EXTERNAL) {
                    continue;
                }
                final String target = rel.getTargetURI() == null ? "" : rel.getTargetURI().toString();
                if (target.isEmpty() || target.equalsIgnoreCase(REDACTED_HYPERLINK_TARGET)) {
                    continue;
                }
                final TextFilterResult result = filter.apply(target);
                final List<Span> spans = OfficeSpanMath.appliedSpans(result);
                if (spans.isEmpty()) {
                    continue;
                }
                if (write) {
                    pkg.setExternalTarget(part, rel, REDACTED_HYPERLINK_TARGET);
                }
                if (captured != null) {
                    final OfficeRedactionSpan entity = new OfficeRedactionSpan(order.next(), -1, 0, target.length(),
                            target, REDACTED_HYPERLINK_TARGET, HYPERLINK_CLASSIFICATION);
                    entity.explain(spans.get(0));
                    captured.add(entity);
                }
            }
        }
    }

    // Field instructions: a simple field's w:instr attribute, and a complex field's w:instrText runs.
    private static void redactFieldInstructions(final OoxmlPackage pkg, final TextFilter filter, final boolean write,
                                                final List<OfficeRedactionSpan> captured, final Order order) {
        for (final PackagePart part : pkg.allParts(pkg.mainPart())) {
            final Document d = pkg.dom(part);
            if (d == null) {
                continue;
            }
            for (final Element field : descendants(d, W, "fldSimple")) {
                final String original = field.getAttributeNS(W, "instr");
                if (original.isEmpty()) {
                    continue;
                }
                final TextFilterResult result = filter.apply(original);
                if (result.getFilteredText().equals(original)) {
                    continue;
                }
                if (write) {
                    field.setAttributeNS(W, field.getAttributeNodeNS(W, "instr").getName(), result.getFilteredText());
                    pkg.markDirty(part);
                }
                OfficeSpanMath.capture(result, original, -1, FIELD_INSTRUCTION_CLASSIFICATION, captured, order);
            }
            for (final List<Element> group : groupFieldCodes(d)) {
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
                OfficeSpanMath.capture(result, original, -1, FIELD_INSTRUCTION_CLASSIFICATION, captured, order);
            }
        }
    }

    // Groups each complex field's w:instrText elements (begin ... separate or end) so an instruction split
    // across runs is filtered whole.
    private static List<List<Element>> groupFieldCodes(final Document d) {
        final List<List<Element>> fields = new ArrayList<>();
        List<Element> current = null;
        for (final Element run : descendants(d, W, "r")) {
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

    // Word 2019 and later mirror comment text into word/threadedComments.xml with commentsExtended and
    // commentsIds. The canonical comments part is redacted, so the threaded duplicate and its companions
    // are removed.
    private static void removeThreadedCommentDuplicate(final OoxmlPackage pkg) {
        final PackagePart main = pkg.mainPart();
        if (main == null) {
            return;
        }
        final Map<PackageRelationship, Boolean> found = new LinkedHashMap<>();
        boolean anyThreaded = false;
        for (final PackageRelationship rel : pkg.relationships(main)) {
            final PackagePart p = pkg.related(main, rel);
            if (p == null) {
                continue;
            }
            final String contentType = p.getContentType().toLowerCase(Locale.ROOT);
            final String name = p.getPartName().getName().toLowerCase(Locale.ROOT);
            if (contentType.contains("threadedcomment") || name.endsWith("threadedcomments.xml")) {
                found.put(rel, true);
                anyThreaded = true;
            } else if (rel.getRelationshipType().endsWith("/commentsExtended")
                    || rel.getRelationshipType().endsWith("/commentsIds")) {
                found.put(rel, false);
            }
        }
        if (!anyThreaded) {
            return;
        }
        for (final PackageRelationship rel : found.keySet()) {
            pkg.deletePart(main, rel);
        }
    }

}
