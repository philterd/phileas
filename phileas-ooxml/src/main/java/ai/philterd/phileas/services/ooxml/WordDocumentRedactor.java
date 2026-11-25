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
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.childElements;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.children;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.descendants;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.firstChild;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.is;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.nearestAncestor;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.qualified;
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
        return detect(input, filter, options, null);
    }

    /**
     * As {@link #detect(byte[], TextFilter, OoxmlRedactionOptions)}, with the policy's column rules for the
     * workbooks embedded in the document's charts and as objects.
     * @param rules The column rules, or {@code null} for none.
     */
    public static List<OfficeRedactionSpan> detect(final byte[] input, final TextFilter filter,
                                                   final OoxmlRedactionOptions options, final ColumnRules rules) throws IOException {
        try (OoxmlPackage pkg = new OoxmlPackage(input)) {
            final List<OfficeRedactionSpan> captured = run(pkg, filter, rules, options, false);
            EmbeddedObjectRedactor.process(pkg, filter, rules, options, false, false, captured, new Order(captured.size()));
            return captured;
        }
    }

    /** Redacts the document and returns the redacted bytes and the redactions made. */
    public static OoxmlRedactionResult redact(final byte[] input, final TextFilter filter,
                                              final OoxmlRedactionOptions options) throws IOException {
        return redact(input, filter, options, null);
    }

    /**
     * As {@link #redact(byte[], TextFilter, OoxmlRedactionOptions)}, with the policy's column rules for the
     * workbooks embedded in the document's charts and as objects.
     * @param rules The column rules, or {@code null} for none.
     */
    public static OoxmlRedactionResult redact(final byte[] input, final TextFilter filter,
                                              final OoxmlRedactionOptions options, final ColumnRules rules) throws IOException {
        try (OoxmlPackage pkg = new OoxmlPackage(input)) {
            final List<OfficeRedactionSpan> captured = run(pkg, filter, rules, options, true);
            EmbeddedObjectRedactor.process(pkg, filter, rules, options, options.wordRemovesUninspectableObjects(), true,
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
        return applySpans(input, spans, options, drawingFilter, null);
    }

    /**
     * As {@link #applySpans(byte[], List, OoxmlRedactionOptions, TextFilter)}, with the policy's column rules for
     * the embedded workbooks re-redacted with {@code drawingFilter}.
     * @param rules The column rules, or {@code null} for none.
     */
    public static byte[] applySpans(final byte[] input, final List<OfficeRedactionSpan> spans,
                                    final OoxmlRedactionOptions options, final TextFilter drawingFilter,
                                    final ColumnRules rules) throws IOException {
        final Map<Integer, List<OfficeRedactionSpan>> byParagraph = new HashMap<>();
        for (final OfficeRedactionSpan s : spans) {
            byParagraph.computeIfAbsent(s.getParagraphIndex(), k -> new ArrayList<>()).add(s);
        }
        try (OoxmlPackage pkg = new OoxmlPackage(input)) {
            // As on redaction, so the paragraphs are numbered the same.
            prepare(pkg, options);
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
                        applyRangesToParagraph(t.paragraph(), original, resolved, options.wordHighlight());
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
                if (options.wordRedactsCharts()) {
                    scanCharts(pkg, drawingFilter, rules, options, true, null, order);
                }
                EmbeddedObjectRedactor.process(pkg, drawingFilter, rules, options, options.wordRemovesUninspectableObjects(),
                        true, null, order);
            }
            removeThreadedCommentDuplicate(pkg);
            AuthorshipCleaner.clearWord(pkg);
            if (options.wordRemovesDocumentProperties()) {
                AuthorshipCleaner.clearContentProperties(pkg);
            }
            return pkg.save();
        }
    }

    // --- core --------------------------------------------------------------------------

    /** Redact (write) or Detect (no write), without the embedded-object pass, which runs at the top level only. */
    static List<OfficeRedactionSpan> run(final OoxmlPackage pkg, final TextFilter filter, final ColumnRules rules,
                                         final OoxmlRedactionOptions options, final boolean write) {
        final List<OfficeRedactionSpan> captured = new ArrayList<>();
        final Order order = new Order();
        if (pkg.mainPart() == null) {
            return captured;
        }
        final boolean highlight = options.wordHighlight();
        final boolean redactHeadersFooters = options.wordRedactsHeadersFooters();
        prepare(pkg, options);
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
        if (options.wordRedactsCharts()) {
            scanCharts(pkg, filter, rules, options, write, captured, order);
        }
        if (write) {
            removeThreadedCommentDuplicate(pkg);
            AuthorshipCleaner.clearWord(pkg);
            if (options.wordRemovesDocumentProperties()) {
                AuthorshipCleaner.clearContentProperties(pkg);
            }
        }
        return captured;
    }

    /**
     * Redacts an embedded .docx with the standard passes; null when it cannot be opened as one. A failure of
     * the filter itself propagates rather than being taken for an unreadable document.
     */
    static byte[] redactEmbeddedBytes(final byte[] bytes, final TextFilter filter, final ColumnRules rules,
                                      final OoxmlRedactionOptions options, final List<OfficeRedactionSpan> spans) {
        final OoxmlPackage pkg = openEmbedded(bytes);
        if (pkg == null) {
            return null;
        }
        try (pkg) {
            if (pkg.mainPart() == null || pkg.dom(pkg.mainPart()) == null) {
                return null;
            }
            final List<OfficeRedactionSpan> redacted = run(pkg, filter, rules, options.forEmbedded(), true);
            final byte[] output = pkg.save();
            spans.addAll(redacted);
            return output;
        } catch (final IOException e) {
            return null;
        }
    }

    static List<OfficeRedactionSpan> detectEmbeddedBytes(final byte[] bytes, final TextFilter filter, final ColumnRules rules,
                                                         final OoxmlRedactionOptions options) {
        final OoxmlPackage pkg = openEmbedded(bytes);
        if (pkg == null) {
            return new ArrayList<>();
        }
        try (pkg) {
            return pkg.mainPart() == null ? new ArrayList<>() : run(pkg, filter, rules, options.forEmbedded(), false);
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

    // --- comments, tracked changes, and hidden text -----------------------------------------------

    // Revision records that carry only an author and date, removed when changes are accepted.
    private static final Set<String> REVISION_MARKS = Set.of("rPrChange", "pPrChange", "tblPrChange", "trPrChange",
            "tcPrChange", "tblPrExChange", "sectPrChange", "numberingChange", "moveFromRangeStart",
            "moveFromRangeEnd", "moveToRangeStart", "moveToRangeEnd");

    /*
     * Removes what the Word settings remove, before any text is read, so redaction, detection, and applying
     * spans number the same paragraphs: comments, tracked changes (accepted), and hidden text.
     */
    static void prepare(final OoxmlPackage pkg, final OoxmlRedactionOptions options) {
        final boolean accept = options.wordAcceptsTrackedChanges();
        final boolean comments = options.wordRemovesComments();
        final boolean hidden = options.wordRemovesHiddenText();
        if (pkg.mainPart() == null || (!accept && !comments && !hidden)) {
            return;
        }
        for (final PackagePart part : pkg.allParts(pkg.mainPart())) {
            final Document d = pkg.dom(part);
            if (d == null || !W.equals(d.getDocumentElement().getNamespaceURI())) {
                continue;
            }
            final Element root = d.getDocumentElement();
            boolean changed = false;
            if (accept) {
                changed |= acceptRevisions(root);
            }
            if (comments) {
                changed |= removeCommentMarks(root);
            }
            if (hidden) {
                changed |= removeHiddenRuns(root);
            }
            if (changed) {
                pkg.markDirty(part);
            }
        }
        if (comments) {
            removeCommentParts(pkg);
        }
    }

    // Accepts every insertion and deletion: deleted content and deleted table rows are removed, inserted
    // content is kept without its revision wrapper, and the remaining revision records are removed.
    private static boolean acceptRevisions(final Element root) {
        boolean changed = false;
        for (final Element row : descendants(root, W, "tr")) {
            final Element trPr = firstChild(row, W, "trPr");
            if (trPr != null && firstChild(trPr, W, "del") != null && row.getParentNode() != null) {
                final Node table = row.getParentNode();
                table.removeChild(row);
                changed = true;
                // A table left without rows is replaced by an empty paragraph, which is valid wherever a table is.
                if (table instanceof Element t && children(t, W, "tr").isEmpty() && t.getParentNode() != null) {
                    t.getParentNode().replaceChild(t.getOwnerDocument().createElementNS(W, qualified(t, "p")), t);
                }
            }
        }
        for (final String name : List.of("del", "moveFrom", "delText", "delInstrText")) {
            for (final Element e : descendants(root, W, name)) {
                if (e.getParentNode() != null) {
                    e.getParentNode().removeChild(e);
                    changed = true;
                }
            }
        }
        for (final String name : List.of("ins", "moveTo")) {
            for (final Element e : descendants(root, W, name)) {
                unwrap(e);
                changed = true;
            }
        }
        for (final Element e : allDescendants(root)) {
            if (W.equals(e.getNamespaceURI()) && REVISION_MARKS.contains(e.getLocalName()) && e.getParentNode() != null) {
                e.getParentNode().removeChild(e);
                changed = true;
            }
        }
        return changed;
    }

    // Comment ranges and references. A run left holding only its properties is removed with its reference.
    private static boolean removeCommentMarks(final Element root) {
        boolean changed = false;
        for (final String name : List.of("commentRangeStart", "commentRangeEnd")) {
            for (final Element e : descendants(root, W, name)) {
                e.getParentNode().removeChild(e);
                changed = true;
            }
        }
        for (final Element reference : descendants(root, W, "commentReference")) {
            final Node run = reference.getParentNode();
            run.removeChild(reference);
            changed = true;
            if (run instanceof Element r && is(r, W, "r") && r.getParentNode() != null) {
                boolean onlyProperties = true;
                for (final Element child : childElements(r)) {
                    onlyProperties &= is(child, W, "rPr");
                }
                if (onlyProperties) {
                    r.getParentNode().removeChild(r);
                }
            }
        }
        return changed;
    }

    // The comments part and its companions: the extended, ids, and extensible parts and the threaded comments.
    private static void removeCommentParts(final OoxmlPackage pkg) {
        final PackagePart main = pkg.mainPart();
        final List<PackageRelationship> remove = new ArrayList<>();
        for (final PackageRelationship rel : pkg.relationships(main)) {
            final String type = rel.getRelationshipType();
            final PackagePart part = pkg.related(main, rel);
            final boolean threaded = part != null
                    && part.getContentType().toLowerCase(Locale.ROOT).contains("threadedcomment");
            if (threaded || type.endsWith("/comments") || type.endsWith("/commentsExtended")
                    || type.endsWith("/commentsIds") || type.endsWith("/commentsExtensible")) {
                remove.add(rel);
            }
        }
        for (final PackageRelationship rel : remove) {
            pkg.deletePart(main, rel);
        }
    }

    // Runs whose own properties mark them hidden (w:vanish not turned off). A hidden run holding a field marker
    // keeps the marker and loses its text, so a field whose markers are not all hidden stays well formed.
    private static boolean removeHiddenRuns(final Element root) {
        boolean changed = false;
        for (final Element run : descendants(root, W, "r")) {
            final Element rPr = firstChild(run, W, "rPr");
            final Element vanish = rPr == null ? null : firstChild(rPr, W, "vanish");
            if (vanish == null || isOff(vanish) || run.getParentNode() == null) {
                continue;
            }
            if (firstChild(run, W, "fldChar") != null) {
                for (final Element t : children(run, W, "t")) {
                    run.removeChild(t);
                }
            } else {
                run.getParentNode().removeChild(run);
            }
            changed = true;
        }
        return changed;
    }

    private static boolean isOff(final Element toggle) {
        final String value = toggle.getAttributeNS(W, "val");
        return "0".equals(value) || "false".equals(value) || "off".equals(value);
    }

    // Replaces an element with its children.
    private static void unwrap(final Element element) {
        final Node parent = element.getParentNode();
        if (parent == null) {
            return;
        }
        while (element.getFirstChild() != null) {
            parent.insertBefore(element.getFirstChild(), element);
        }
        parent.removeChild(element);
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

    // --- in-place redaction ------------------------------------------------------------------------

    // rPr children that follow w:highlight in the schema's order, so a highlight is inserted before them.
    private static final List<String> AFTER_HIGHLIGHT = List.of("u", "effect", "bdr", "shd", "fitText", "vertAlign",
            "rtl", "cs", "em", "lang", "eastAsianLayout", "specVanish", "oMath", "rPrChange");

    /*
     * Replaces each range of the paragraph's own text in place, in the runs that hold it, so everything else in
     * the paragraph stays as it was: run formatting, hidden marks, tracked insertions and deletions, comment
     * anchors, hyperlinks, fields, and drawings. A replacement takes the place of the range's first character;
     * a break or tab inside the range is kept after it, and other characters in the range are removed.
     */
    private static void applyRangesToParagraph(final Element paragraph, final String ownText,
                                               final List<ReplacementRange> ranges, final boolean highlight) {
        final List<ReplacementRange> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparingInt(ReplacementRange::start));
        final List<ReplacementRange> applied = new ArrayList<>();
        int last = 0;
        for (final ReplacementRange range : sorted) {
            if (range.start() < last || range.end() > ownText.length() || range.end() <= range.start()) {
                continue;
            }
            applied.add(range);
            last = range.end();
        }
        // From the last range back, so the offsets of earlier ranges are not moved.
        for (int i = applied.size() - 1; i >= 0; i--) {
            replaceInPlace(paragraph, applied.get(i), highlight);
        }
    }

    private static void replaceInPlace(final Element paragraph, final ReplacementRange range, final boolean highlight) {

        splitTextAt(paragraph, range.start());
        splitTextAt(paragraph, range.end());

        final List<Element> inRange = new ArrayList<>();
        int offset = 0;
        for (final Content c : ownContent(paragraph)) {
            final int length = c.text().length();
            final boolean inside = length == 0
                    ? offset > range.start() && offset < range.end()
                    : offset >= range.start() && offset + length <= range.end();
            if (inside) {
                inRange.add(c.element());
            }
            offset += length;
        }
        if (inRange.isEmpty()) {
            return;
        }

        final String replacement = range.replacement() == null ? "" : range.replacement();
        final Element first = inRange.get(0);
        final Element text;
        if (is(first, W, "t")) {
            text = first;
            setPreservedText(text, replacement);
        } else {
            text = preservedText(first.getOwnerDocument(), replacement);
            first.getParentNode().insertBefore(text, first);
        }

        final List<Element> runs = new ArrayList<>();
        for (final Element e : inRange) {
            final Node run = e.getParentNode();
            if (run instanceof Element r && !runs.contains(r)) {
                runs.add(r);
            }
            if (e != text && !isLayout(e)) {
                run.removeChild(e);
            }
        }

        if (highlight) {
            highlightAlone(text);
        }

        for (final Element run : runs) {
            if (run != text.getParentNode() && run.getParentNode() != null && onlyProperties(run)) {
                run.getParentNode().removeChild(run);
            }
        }

    }

    // Splits the w:t that has the offset inside it into two, so the offset falls between elements.
    private static void splitTextAt(final Element paragraph, final int at) {
        int offset = 0;
        for (final Content c : ownContent(paragraph)) {
            final int length = c.text().length();
            if (at > offset && at < offset + length && is(c.element(), W, "t")) {
                final Element t = c.element();
                final String value = c.text();
                final Element second = preservedText(t.getOwnerDocument(), value.substring(at - offset));
                setPreservedText(t, value.substring(0, at - offset));
                t.getParentNode().insertBefore(second, t.getNextSibling());
                return;
            }
            offset += length;
        }
    }

    // Puts the text element in a run of its own, a copy of its run, and highlights that run.
    private static void highlightAlone(final Element text) {
        final Element run = (Element) text.getParentNode();
        splitRunBefore(run, text);
        final Element next = nextElement(text);
        if (next != null) {
            splitRunBefore((Element) text.getParentNode(), next);
        }
        final Element own = (Element) text.getParentNode();
        Element rPr = firstChild(own, W, "rPr");
        if (rPr == null) {
            rPr = own.getOwnerDocument().createElementNS(W, qualified(own, "rPr"));
            own.insertBefore(rPr, own.getFirstChild());
        }
        Element mark = firstChild(rPr, W, "highlight");
        if (mark == null) {
            mark = rPr.getOwnerDocument().createElementNS(W, qualified(rPr, "highlight"));
            Node before = null;
            for (final Element child : childElements(rPr)) {
                if (W.equals(child.getNamespaceURI()) && AFTER_HIGHLIGHT.contains(child.getLocalName())) {
                    before = child;
                    break;
                }
            }
            rPr.insertBefore(mark, before);
        }
        mark.setAttributeNS(W, qualified(rPr, "val"), "yellow");
    }

    // The run holding child and everything after it: run itself when child is its first content, otherwise a
    // copy of run, with run's properties, inserted after it. A copy's formatting-change record is left out, since
    // its id must be unique.
    private static Element splitRunBefore(final Element run, final Element child) {
        boolean first = true;
        for (Node n = run.getFirstChild(); n != null && n != child; n = n.getNextSibling()) {
            if (n instanceof Element e && !is(e, W, "rPr")) {
                first = false;
                break;
            }
        }
        if (first) {
            return run;
        }
        final Element copy = (Element) run.cloneNode(false);
        final Element rPr = firstChild(run, W, "rPr");
        if (rPr != null) {
            final Element properties = (Element) rPr.cloneNode(true);
            for (final Element change : children(properties, W, "rPrChange")) {
                properties.removeChild(change);
            }
            copy.appendChild(properties);
        }
        Node n = child;
        while (n != null) {
            final Node next = n.getNextSibling();
            copy.appendChild(n);
            n = next;
        }
        run.getParentNode().insertBefore(copy, run.getNextSibling());
        return copy;
    }

    private static Element nextElement(final Node node) {
        for (Node n = node.getNextSibling(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e) {
                return e;
            }
        }
        return null;
    }

    private static boolean onlyProperties(final Element run) {
        for (final Element child : childElements(run)) {
            if (!is(child, W, "rPr")) {
                return false;
            }
        }
        return true;
    }

    private static void setPreservedText(final Element t, final String text) {
        t.setAttributeNS(XML_NS, "xml:space", "preserve");
        t.setTextContent(text);
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
    private static void scanCharts(final OoxmlPackage pkg, final TextFilter filter, final ColumnRules rules,
                                   final OoxmlRedactionOptions options, final boolean write,
                                   final List<OfficeRedactionSpan> captured, final Order order) {
        for (final PackagePart part : pkg.allParts(pkg.mainPart())) {
            if (!CHART_CONTENT_TYPE.equals(part.getContentType())) {
                continue;
            }
            final Document d = pkg.dom(part);
            if (d != null && ChartRedactor.redactChartPart(d.getDocumentElement(), filter, write, captured, order) && write) {
                pkg.markDirty(part);
            }
            XlsxRedactor.redactChartEmbeddedWorkbooks(pkg, part, filter, rules, options, write, captured, order);
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
