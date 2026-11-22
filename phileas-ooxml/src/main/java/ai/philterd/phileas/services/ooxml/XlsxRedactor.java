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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ai.philterd.phileas.services.ooxml.OfficeSpanMath.Order;
import static ai.philterd.phileas.services.ooxml.OfficeSpanMath.ReplacementRange;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.REL;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.childElements;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.children;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.descendants;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.firstChild;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.qualified;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.removeChildren;

/**
 * Redacts Excel (.xlsx) workbooks. A port of phileas-dotnet's {@code XlsxRedactor}.
 *
 * <p>Cells are visited in a canonical order (sheets in workbook order, then rows, then cells), which
 * defines the cell index spans are re-applied by. A redacted cell is rewritten as an inline string.
 * Further passes cover print headers and footers, comments, shapes, charts and their embedded workbooks,
 * pivot caches, formula caches, shared strings left orphaned by redaction, and embedded objects.
 */
public final class XlsxRedactor {

    static final String S = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    static final String R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    static final String THREADED = "http://schemas.microsoft.com/office/spreadsheetml/2018/threadedcomments";
    static final String REL_THREADED_COMMENT = "http://schemas.microsoft.com/office/2017/10/relationships/threadedComment";
    static final String REL_PERSON = "http://schemas.microsoft.com/office/2017/10/relationships/person";

    private static final String XML_NS = "http://www.w3.org/XML/1998/namespace";
    private static final String DEFAULT_REPLACEMENT = "{{{REDACTED-custom}}}";
    private static final String HEADER_CONTEXT_SEPARATOR = ": ";

    /** The replacement written into each data cell of a fully redacted column. */
    public static final String COLUMN_REPLACEMENT = "{{{REDACTED}}}";
    /** The classification of a fully redacted column's cells. */
    public static final String COLUMN_CLASSIFICATION = "full-column";

    private XlsxRedactor() {
    }

    // --- public API -------------------------------------------------------------------

    /** Redacts the workbook and returns the redacted bytes and the redactions made. */
    public static OoxmlRedactionResult redact(final byte[] input, final TextFilter filter,
                                              final OoxmlRedactionOptions options) throws IOException {
        try (OoxmlPackage pkg = new OoxmlPackage(input)) {
            final List<OfficeRedactionSpan> captured = redactOpen(pkg, filter, options);
            final Order order = new Order(captured.size());
            scanChartEmbeddings(pkg, options.getWorksheet(), filter, true, captured, order);
            EmbeddedObjectRedactor.process(pkg, filter, options.isRemoveUninspectableEmbeddedObjects(), true, captured, order);
            return new OoxmlRedactionResult(pkg.save(), captured);
        }
    }

    /** Detects what {@link #redact} would redact in the workbook's content, without producing a document. */
    public static List<OfficeRedactionSpan> detect(final byte[] input, final TextFilter filter,
                                                   final OoxmlRedactionOptions options) throws IOException {
        try (OoxmlPackage pkg = new OoxmlPackage(input)) {
            final List<OfficeRedactionSpan> captured = detectOpen(pkg, filter, options.getWorksheet(),
                    options.isRedactHeadersFooters(), options.isRedactCharts());
            final Order order = new Order(captured.size());
            scanChartEmbeddings(pkg, options.getWorksheet(), filter, false, captured, order);
            EmbeddedObjectRedactor.process(pkg, filter, false, false, captured, order);
            return captured;
        }
    }

    /**
     * Applies cell-indexed spans by position. Content that is not a cell (headers and footers, comments,
     * shapes, charts, pivot caches, and embedded objects) is re-redacted with {@code policyFilter} when one
     * is supplied.
     */
    public static byte[] applySpans(final byte[] input, final List<OfficeRedactionSpan> spans,
                                    final OoxmlRedactionOptions options, final TextFilter policyFilter) throws IOException {
        final Map<Integer, List<OfficeRedactionSpan>> byCell = new HashMap<>();
        for (final OfficeRedactionSpan s : spans) {
            byCell.computeIfAbsent(s.getParagraphIndex(), k -> new ArrayList<>()).add(s);
        }
        final String worksheet = options.getWorksheet();
        try (OoxmlPackage pkg = new OoxmlPackage(input)) {
            final PackagePart workbook = pkg.mainPart();
            if (workbook == null || pkg.dom(workbook) == null) {
                return pkg.save();
            }
            final List<String> sharedStrings = sharedStrings(pkg);
            int cellIndex = 0;
            boolean anyCellRedacted = false;
            for (final CellRef c : enumerateCells(pkg, worksheet)) {
                final int currentIndex = cellIndex++;
                final List<OfficeRedactionSpan> cellSpans = byCell.get(currentIndex);
                if ((hasFormula(c.cell()) && !options.isRedactFormulaValues()) || cellSpans == null) {
                    continue;
                }
                final String original = getCellText(c.cell(), sharedStrings);
                if (original.isEmpty()) {
                    continue;
                }
                final List<ReplacementRange> ranges = new ArrayList<>();
                for (final OfficeRedactionSpan s : cellSpans) {
                    if (s.getCharacterStart() >= 0 && s.getCharacterEnd() <= original.length()
                            && s.getCharacterEnd() > s.getCharacterStart()) {
                        final String replacement = s.getReplacement() == null || s.getReplacement().isEmpty()
                                ? DEFAULT_REPLACEMENT : s.getReplacement();
                        ranges.add(new ReplacementRange(s.getCharacterStart(), s.getCharacterEnd(), replacement));
                    }
                }
                final List<ReplacementRange> resolved = OfficeSpanMath.resolveNonOverlapping(ranges);
                if (!resolved.isEmpty()) {
                    setCellText(c.cell(), OfficeSpanMath.applyRanges(original, resolved));
                    pkg.markDirty(c.part());
                    anyCellRedacted = true;
                }
            }
            if (options.isRedactFormulaValues() && anyCellRedacted) {
                clearFormulaCachesAndForceRecalc(pkg, worksheet);
            }
            if (policyFilter != null) {
                final Order order = new Order();
                if (options.isRedactHeadersFooters()) {
                    scanHeadersFooters(pkg, worksheet, policyFilter, true, null, order);
                }
                scanComments(pkg, worksheet, policyFilter, true, null, order);
                scanShapes(pkg, worksheet, policyFilter, true, null, order);
                if (options.isRedactCharts()) {
                    scanCharts(pkg, worksheet, policyFilter, true, null, order);
                }
                if (options.isRedactPivotCaches()) {
                    scanPivotCaches(pkg, policyFilter, true, null, order);
                }
                scanChartEmbeddings(pkg, worksheet, policyFilter, true, null, order);
                EmbeddedObjectRedactor.process(pkg, policyFilter, options.isRemoveUninspectableEmbeddedObjects(),
                        true, null, order);
            }
            pruneUnusedSharedStrings(pkg);
            return pkg.save();
        }
    }

    /** The worksheet names in workbook order. */
    public static List<String> readSheetNames(final byte[] input) throws IOException {
        try (OoxmlPackage pkg = new OoxmlPackage(input)) {
            final List<String> names = new ArrayList<>();
            final Document workbook = pkg.dom(pkg.mainPart());
            if (workbook == null) {
                return names;
            }
            for (final Element sheet : descendants(workbook, S, "sheet")) {
                final String name = sheet.getAttribute("name");
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
            return names;
        }
    }

    /**
     * The columns of a worksheet (the first when {@code sheetName} is null or empty) that hold data, with
     * their letter and first-row header text.
     */
    public static List<SpreadsheetColumn> readColumns(final byte[] input, final String sheetName) throws IOException {
        try (OoxmlPackage pkg = new OoxmlPackage(input)) {
            final List<SpreadsheetColumn> columns = new ArrayList<>();
            final PackagePart sheetPart = findWorksheetPart(pkg, sheetName);
            final Document sheet = sheetPart == null ? null : pkg.dom(sheetPart);
            final Element sheetData = sheet == null ? null : firstChild(sheet.getDocumentElement(), S, "sheetData");
            if (sheetData == null) {
                return columns;
            }
            final List<String> sharedStrings = sharedStrings(pkg);
            final List<Element> rows = children(sheetData, S, "row");
            final Map<Integer, String> headers = new HashMap<>();
            if (!rows.isEmpty()) {
                for (final Element cell : children(rows.get(0), S, "c")) {
                    final int col = SpreadsheetColumn.letterToIndex(cell.getAttribute("r"));
                    if (col > 0) {
                        headers.put(col, getCellText(cell, sharedStrings));
                    }
                }
            }
            int maxColumn = 0;
            for (final Element row : rows) {
                for (final Element cell : children(row, S, "c")) {
                    maxColumn = Math.max(maxColumn, SpreadsheetColumn.letterToIndex(cell.getAttribute("r")));
                }
            }
            for (int col = 1; col <= maxColumn; col++) {
                columns.add(new SpreadsheetColumn(col, SpreadsheetColumn.indexToLetter(col), headers.getOrDefault(col, "")));
            }
            return columns;
        }
    }

    // --- core --------------------------------------------------------------------------

    static List<OfficeRedactionSpan> redactOpen(final OoxmlPackage pkg, final TextFilter filter,
                                                final OoxmlRedactionOptions options) {
        final List<OfficeRedactionSpan> captured = new ArrayList<>();
        final Order order = new Order();
        final PackagePart workbook = pkg.mainPart();
        if (workbook == null || pkg.dom(workbook) == null) {
            return captured;
        }
        final String worksheet = options.getWorksheet();
        final Set<Integer> fullColumns = options.getFullyRedactedColumns();
        final List<String> sharedStrings = sharedStrings(pkg);
        final Map<Integer, String> columnHeaders = new HashMap<>();
        int cellIndex = 0;
        boolean anyCellRedacted = false;

        for (final CellRef c : enumerateCells(pkg, worksheet)) {
            final int currentIndex = cellIndex++;
            final Element cell = c.cell();
            // A formula cell's cached value can copy a now-redacted value. With the option off, formula
            // cells are left alone; with it on, the cached value is scanned like any other cell.
            if (hasFormula(cell) && !options.isRedactFormulaValues()) {
                continue;
            }
            final String original = getCellText(cell, sharedStrings);
            if (original.isEmpty()) {
                continue;
            }
            if (options.isUseHeaderContext() && c.headerRow()) {
                columnHeaders.put(c.column(), original);
            }
            if (fullColumns.contains(c.column())) {
                if (c.headerRow()) {
                    continue; // the column's header label is kept
                }
                setCellText(cell, COLUMN_REPLACEMENT);
                pkg.markDirty(c.part());
                anyCellRedacted = true;
                captured.add(new OfficeRedactionSpan(order.next(), currentIndex, 0, original.length(), original,
                        COLUMN_REPLACEMENT, COLUMN_CLASSIFICATION));
                continue;
            }
            if (!isScannableCell(cell)) {
                continue; // boolean and error cells carry no free text
            }
            final String header = options.isUseHeaderContext() && !c.headerRow() ? columnHeaders.get(c.column()) : null;
            final DetectedCell detected = detectCell(original, header, filter);
            if (detected.filteredText().equals(original)) {
                continue;
            }
            setCellText(cell, detected.filteredText());
            pkg.markDirty(c.part());
            anyCellRedacted = true;
            final List<Span> sorted = new ArrayList<>(detected.spans());
            sorted.sort(Comparator.comparingInt(Span::getCharacterStart));
            for (final Span s : sorted) {
                captured.add(OfficeSpanMath.entity(s, original, currentIndex, null, order));
            }
        }

        if (options.isRedactHeadersFooters()) {
            scanHeadersFooters(pkg, worksheet, filter, true, captured, order);
        }
        scanComments(pkg, worksheet, filter, true, captured, order);
        scanShapes(pkg, worksheet, filter, true, captured, order);
        if (options.isRedactCharts()) {
            scanCharts(pkg, worksheet, filter, true, captured, order);
        }
        if (options.isRedactPivotCaches()) {
            scanPivotCaches(pkg, filter, true, captured, order);
        }
        if (options.isRedactFormulaValues() && anyCellRedacted) {
            clearFormulaCachesAndForceRecalc(pkg, worksheet);
        }
        pruneUnusedSharedStrings(pkg);
        return captured;
    }

    private static List<OfficeRedactionSpan> detectOpen(final OoxmlPackage pkg, final TextFilter filter,
                                                        final String worksheet, final boolean redactHeadersFooters,
                                                        final boolean redactCharts) {
        final List<OfficeRedactionSpan> captured = new ArrayList<>();
        final Order order = new Order();
        final PackagePart workbook = pkg.mainPart();
        if (workbook == null || pkg.dom(workbook) == null) {
            return captured;
        }
        final List<String> sharedStrings = sharedStrings(pkg);
        int cellIndex = 0;
        for (final CellRef c : enumerateCells(pkg, worksheet)) {
            final int currentIndex = cellIndex++;
            // Formula caches are scanned regardless of the redaction option, so verification catches them.
            if (!isScannableCell(c.cell())) {
                continue;
            }
            final String original = getCellText(c.cell(), sharedStrings);
            if (original.isEmpty()) {
                continue;
            }
            final TextFilterResult result = filter.apply(original);
            if (result.getFilteredText().equals(original)) {
                continue;
            }
            OfficeSpanMath.capture(result, original, currentIndex, null, captured, order);
        }
        if (redactHeadersFooters) {
            scanHeadersFooters(pkg, worksheet, filter, false, captured, order);
        }
        scanComments(pkg, worksheet, filter, false, captured, order);
        scanShapes(pkg, worksheet, filter, false, captured, order);
        if (redactCharts) {
            scanCharts(pkg, worksheet, filter, false, captured, order);
        }
        // The pivot cache is scanned regardless of the option, so verification flags a residual copy.
        scanPivotCaches(pkg, filter, false, captured, order);
        return captured;
    }

    private record DetectedCell(String filteredText, List<Span> spans) {
    }

    // With a header, detects on "header: cell" and maps spans wholly inside the cell back onto it.
    private static DetectedCell detectCell(final String cellText, final String header, final TextFilter filter) {
        if (header == null || header.isEmpty()) {
            final TextFilterResult plain = filter.apply(cellText);
            final List<Span> spans = new ArrayList<>();
            for (final Span s : OfficeSpanMath.appliedSpans(plain)) {
                if (s.getCharacterStart() >= 0 && s.getCharacterEnd() <= cellText.length() && s.getCharacterEnd() > s.getCharacterStart()) {
                    spans.add(s);
                }
            }
            return new DetectedCell(plain.getFilteredText(), spans);
        }
        final String prefix = header + HEADER_CONTEXT_SEPARATOR;
        final int cellStart = prefix.length();
        final int cellEnd = cellStart + cellText.length();
        final TextFilterResult result = filter.apply(prefix + cellText);
        final List<Span> cellSpans = new ArrayList<>();
        for (final Span s : OfficeSpanMath.appliedSpans(result)) {
            if (s.getCharacterStart() < cellStart || s.getCharacterEnd() > cellEnd || s.getCharacterEnd() <= s.getCharacterStart()) {
                continue;
            }
            final Span shifted = s.copy();
            shifted.setPattern(s.getPattern());
            shifted.setWindow(s.getWindow());
            shifted.setCharacterStart(s.getCharacterStart() - cellStart);
            shifted.setCharacterEnd(s.getCharacterEnd() - cellStart);
            cellSpans.add(shifted);
        }
        final List<ReplacementRange> ranges = new ArrayList<>();
        for (final Span s : cellSpans) {
            ranges.add(new ReplacementRange(s.getCharacterStart(), s.getCharacterEnd(), s.getReplacement()));
        }
        return new DetectedCell(OfficeSpanMath.applyRanges(cellText, ranges), cellSpans);
    }

    // --- cells ---------------------------------------------------------------------------

    record CellRef(Element cell, boolean headerRow, int column, PackagePart part) {
    }

    // The worksheet parts processed: one named sheet, or all of them.
    static List<PackagePart> worksheetParts(final OoxmlPackage pkg, final String worksheet) {
        final List<PackagePart> out = new ArrayList<>();
        final PackagePart workbook = pkg.mainPart();
        final Document doc = pkg.dom(workbook);
        if (doc == null) {
            return out;
        }
        for (final Element sheet : descendants(doc, S, "sheet")) {
            if (worksheet != null && !worksheet.isEmpty() && !worksheet.equals(sheet.getAttribute("name"))) {
                continue;
            }
            final PackagePart part = pkg.relatedById(workbook, sheet.getAttributeNS(R, "id"));
            if (part != null && part.getContentType().contains("worksheet") && pkg.dom(part) != null) {
                out.add(part);
            }
        }
        return out;
    }

    private static PackagePart findWorksheetPart(final OoxmlPackage pkg, final String sheetName) {
        final PackagePart workbook = pkg.mainPart();
        final Document doc = pkg.dom(workbook);
        if (doc == null) {
            return null;
        }
        for (final Element sheet : descendants(doc, S, "sheet")) {
            if (sheetName == null || sheetName.isEmpty() || sheetName.equals(sheet.getAttribute("name"))) {
                final PackagePart part = pkg.relatedById(workbook, sheet.getAttributeNS(R, "id"));
                return part != null && part.getContentType().contains("worksheet") ? part : null;
            }
        }
        return null;
    }

    // Sheets in workbook order, then rows, then cells. A cell without a reference takes the previous
    // cell's column + 1.
    static List<CellRef> enumerateCells(final OoxmlPackage pkg, final String worksheet) {
        final List<CellRef> out = new ArrayList<>();
        for (final PackagePart part : worksheetParts(pkg, worksheet)) {
            final Element sheetData = firstChild(pkg.dom(part).getDocumentElement(), S, "sheetData");
            if (sheetData == null) {
                continue;
            }
            boolean headerRow = true;
            for (final Element row : children(sheetData, S, "row")) {
                int column = 0;
                for (final Element cell : children(row, S, "c")) {
                    final int referenced = SpreadsheetColumn.letterToIndex(cell.getAttribute("r"));
                    column = referenced > 0 ? referenced : column + 1;
                    out.add(new CellRef(cell, headerRow, column, part));
                }
                headerRow = false;
            }
        }
        return out;
    }

    private static List<String> sharedStrings(final OoxmlPackage pkg) {
        final List<String> out = new ArrayList<>();
        final List<PackagePart> parts = pkg.relatedByType(pkg.mainPart(), REL + "sharedStrings");
        if (!parts.isEmpty()) {
            final Document d = pkg.dom(parts.get(0));
            if (d != null) {
                for (final Element si : children(d.getDocumentElement(), S, "si")) {
                    out.add(si.getTextContent());
                }
            }
        }
        return out;
    }

    private static boolean hasFormula(final Element cell) {
        return firstChild(cell, S, "f") != null;
    }

    // Numbers and dates are scanned too: an SSN or account number is often stored as a bare number.
    private static boolean isScannableCell(final Element cell) {
        final String t = cell.getAttribute("t");
        return t.isEmpty() || "n".equals(t) || "s".equals(t) || "inlineStr".equals(t) || "str".equals(t) || "d".equals(t);
    }

    private static String getCellText(final Element cell, final List<String> sharedStrings) {
        final String t = cell.getAttribute("t");
        final Element v = firstChild(cell, S, "v");
        if ("s".equals(t)) {
            final Integer index = parseIndex(v);
            return index != null && index >= 0 && index < sharedStrings.size() ? sharedStrings.get(index) : "";
        }
        if ("inlineStr".equals(t)) {
            final Element is = firstChild(cell, S, "is");
            if (is == null) {
                return "";
            }
            final Element text = firstChild(is, S, "t");
            return text != null ? text.getTextContent() : is.getTextContent();
        }
        return v == null ? "" : v.getTextContent();
    }

    private static Integer parseIndex(final Element v) {
        if (v == null) {
            return null;
        }
        try {
            return Integer.parseInt(v.getTextContent().trim());
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    // Writes the cell as an inline string, detaching it from the shared-string table and dropping any formula.
    private static void setCellText(final Element cell, final String value) {
        removeChildren(cell);
        cell.setAttribute("t", "inlineStr");
        final Document doc = cell.getOwnerDocument();
        final Element is = doc.createElementNS(S, qualified(cell, "is"));
        final Element t = doc.createElementNS(S, qualified(cell, "t"));
        t.setAttributeNS(XML_NS, "xml:space", "preserve");
        t.setTextContent(value);
        is.appendChild(t);
        cell.appendChild(is);
    }

    // Clears each formula cell's cached result and sets the workbook to recalculate fully on open.
    private static void clearFormulaCachesAndForceRecalc(final OoxmlPackage pkg, final String worksheet) {
        for (final CellRef c : enumerateCells(pkg, worksheet)) {
            if (hasFormula(c.cell())) {
                final Element v = firstChild(c.cell(), S, "v");
                if (v != null) {
                    c.cell().removeChild(v);
                    pkg.markDirty(c.part());
                }
            }
        }
        final PackagePart workbookPart = pkg.mainPart();
        final Element workbook = pkg.dom(workbookPart).getDocumentElement();
        Element calcPr = firstChild(workbook, S, "calcPr");
        if (calcPr == null) {
            calcPr = workbook.getOwnerDocument().createElementNS(S, qualified(workbook, "calcPr"));
            // calcPr follows the last of sheets, functionGroups, externalReferences, and definedNames present.
            Element anchor = null;
            for (final String name : List.of("definedNames", "externalReferences", "functionGroups", "sheets")) {
                anchor = firstChild(workbook, S, name);
                if (anchor != null) {
                    break;
                }
            }
            if (anchor != null) {
                workbook.insertBefore(calcPr, anchor.getNextSibling());
            } else {
                workbook.appendChild(calcPr);
            }
        }
        calcPr.setAttribute("fullCalcOnLoad", "1");
        pkg.markDirty(workbookPart);
    }

    // Blanks shared-string entries no cell references any more, in place, so later indices do not shift.
    private static void pruneUnusedSharedStrings(final OoxmlPackage pkg) {
        final List<PackagePart> parts = pkg.relatedByType(pkg.mainPart(), REL + "sharedStrings");
        if (parts.isEmpty()) {
            return;
        }
        final Document table = pkg.dom(parts.get(0));
        if (table == null) {
            return;
        }
        final Set<Integer> referenced = new HashSet<>();
        for (final CellRef c : enumerateCells(pkg, null)) {
            if ("s".equals(c.cell().getAttribute("t"))) {
                final Integer index = parseIndex(firstChild(c.cell(), S, "v"));
                if (index != null) {
                    referenced.add(index);
                }
            }
        }
        int i = 0;
        boolean changed = false;
        for (final Element si : children(table.getDocumentElement(), S, "si")) {
            if (!referenced.contains(i)) {
                final boolean alreadyBlank = si.getTextContent().isEmpty() && childElements(si).size() == 1;
                if (!alreadyBlank) {
                    removeChildren(si);
                    si.appendChild(table.createElementNS(S, qualified(si, "t")));
                    changed = true;
                }
            }
            i++;
        }
        if (changed) {
            pkg.markDirty(parts.get(0));
        }
    }

    // --- other passes -------------------------------------------------------------------------

    // Print headers and footers (odd, even, and first-page). Excel's field codes pass through the filter.
    private static void scanHeadersFooters(final OoxmlPackage pkg, final String worksheet, final TextFilter filter,
                                           final boolean write, final List<OfficeRedactionSpan> captured, final Order order) {
        for (final PackagePart part : worksheetParts(pkg, worksheet)) {
            final Element headerFooter = firstChild(pkg.dom(part).getDocumentElement(), S, "headerFooter");
            if (headerFooter == null) {
                continue;
            }
            for (final Element element : childElements(headerFooter)) {
                if (ChartRedactor.redactLeaf(element, filter, write, captured, order) && write) {
                    pkg.markDirty(part);
                }
            }
        }
    }

    // Legacy comments (text and authors), threaded comments, and the workbook's threaded-comment persons.
    private static void scanComments(final OoxmlPackage pkg, final String worksheet, final TextFilter filter,
                                     final boolean write, final List<OfficeRedactionSpan> captured, final Order order) {
        for (final PackagePart sheet : worksheetParts(pkg, worksheet)) {
            for (final PackagePart commentsPart : pkg.relatedByType(sheet, REL + "comments")) {
                final Document d = pkg.dom(commentsPart);
                if (d == null) {
                    continue;
                }
                for (final Element t : descendants(d, S, "t")) {
                    if (ChartRedactor.redactLeaf(t, filter, write, captured, order) && write) {
                        pkg.markDirty(commentsPart);
                    }
                }
                for (final Element author : descendants(d, S, "author")) {
                    if (ChartRedactor.redactLeaf(author, filter, write, captured, order) && write) {
                        pkg.markDirty(commentsPart);
                    }
                }
            }
            for (final PackagePart threadedPart : pkg.relatedByType(sheet, REL_THREADED_COMMENT)) {
                final Document d = pkg.dom(threadedPart);
                if (d == null) {
                    continue;
                }
                for (final Element text : descendants(d, THREADED, "text")) {
                    if (ChartRedactor.redactLeaf(text, filter, write, captured, order) && write) {
                        pkg.markDirty(threadedPart);
                    }
                }
            }
        }
        for (final PackagePart personPart : pkg.relatedByType(pkg.mainPart(), REL_PERSON)) {
            final Document d = pkg.dom(personPart);
            if (d == null) {
                continue;
            }
            for (final Element person : children(d.getDocumentElement(), THREADED, "person")) {
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
                    pkg.markDirty(personPart);
                }
                OfficeSpanMath.capture(result, original, -1, null, captured, order);
            }
        }
    }

    // Shapes and text boxes drawn on the processed sheets.
    private static void scanShapes(final OoxmlPackage pkg, final String worksheet, final TextFilter filter,
                                   final boolean write, final List<OfficeRedactionSpan> captured, final Order order) {
        for (final PackagePart sheet : worksheetParts(pkg, worksheet)) {
            for (final PackagePart drawing : pkg.relatedByType(sheet, REL + "drawing")) {
                final Document d = pkg.dom(drawing);
                if (d != null && ChartRedactor.redactDrawingText(d.getDocumentElement(), filter, write, captured, order) && write) {
                    pkg.markDirty(drawing);
                }
            }
        }
    }

    // Charts on the processed sheets: title and label text and cached values.
    private static void scanCharts(final OoxmlPackage pkg, final String worksheet, final TextFilter filter,
                                   final boolean write, final List<OfficeRedactionSpan> captured, final Order order) {
        for (final PackagePart chart : chartParts(pkg, worksheet)) {
            final Document d = pkg.dom(chart);
            if (d != null && ChartRedactor.redactChartPart(d.getDocumentElement(), filter, write, captured, order) && write) {
                pkg.markDirty(chart);
            }
        }
    }

    private static List<PackagePart> chartParts(final OoxmlPackage pkg, final String worksheet) {
        final List<PackagePart> charts = new ArrayList<>();
        for (final PackagePart sheet : worksheetParts(pkg, worksheet)) {
            for (final PackagePart drawing : pkg.relatedByType(sheet, REL + "drawing")) {
                charts.addAll(pkg.relatedByType(drawing, REL + "chart"));
            }
        }
        return charts;
    }

    // A pivot cache is a denormalized copy of the source data. String items (<s v="...">) in the cache
    // definition, its records, and the pivot tables are filtered; a redacted cache refreshes on open.
    private static void scanPivotCaches(final OoxmlPackage pkg, final TextFilter filter, final boolean write,
                                        final List<OfficeRedactionSpan> captured, final Order order) {
        final PackagePart workbook = pkg.mainPart();
        for (final PackagePart definition : pkg.relatedByType(workbook, REL + "pivotCacheDefinition")) {
            boolean redacted = false;
            final Document definitionDom = pkg.dom(definition);
            if (definitionDom != null) {
                for (final Element item : descendants(definitionDom, S, "s")) {
                    redacted |= redactPivotString(pkg, definition, item, filter, write, captured, order);
                }
            }
            for (final PackagePart records : pkg.relatedByType(definition, REL + "pivotCacheRecords")) {
                final Document recordsDom = pkg.dom(records);
                if (recordsDom != null) {
                    for (final Element item : descendants(recordsDom, S, "s")) {
                        redacted |= redactPivotString(pkg, records, item, filter, write, captured, order);
                    }
                }
            }
            if (write && redacted && definitionDom != null) {
                definitionDom.getDocumentElement().setAttribute("refreshOnLoad", "1");
                pkg.markDirty(definition);
            }
        }
        for (final PackagePart sheet : pkg.relatedByType(workbook, REL + "worksheet")) {
            for (final PackagePart table : pkg.relatedByType(sheet, REL + "pivotTable")) {
                final Document d = pkg.dom(table);
                if (d != null) {
                    for (final Element item : descendants(d, S, "s")) {
                        redactPivotString(pkg, table, item, filter, write, captured, order);
                    }
                }
            }
        }
    }

    private static boolean redactPivotString(final OoxmlPackage pkg, final PackagePart part, final Element item,
                                             final TextFilter filter, final boolean write,
                                             final List<OfficeRedactionSpan> captured, final Order order) {
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
        OfficeSpanMath.capture(result, original, -1, "pivot-cache", captured, order);
        return true;
    }

    // --- chart embedded workbooks ----------------------------------------------------------------

    private static void scanChartEmbeddings(final OoxmlPackage pkg, final String worksheet, final TextFilter filter,
                                            final boolean write, final List<OfficeRedactionSpan> captured, final Order order) {
        if (pkg.mainPart() == null) {
            return;
        }
        for (final PackagePart chart : chartParts(pkg, worksheet)) {
            redactChartEmbeddedWorkbooks(pkg, chart, filter, write, captured, order);
        }
    }

    /**
     * Redacts (or detects in) each workbook embedded under a chart part, shared by Word and Excel. A chart's
     * embedded workbook holds its full source data, often more than is plotted. On write, one that cannot
     * be parsed is removed along with the chart's reference to it.
     */
    static void redactChartEmbeddedWorkbooks(final OoxmlPackage pkg, final PackagePart chart, final TextFilter filter,
                                             final boolean write, final List<OfficeRedactionSpan> captured, final Order order) {
        for (final PackageRelationship rel : pkg.relationshipsByType(chart, REL + "package")) {
            final PackagePart embedded = pkg.related(chart, rel);
            if (embedded == null) {
                continue;
            }
            final byte[] bytes = pkg.partBytes(embedded);
            if (bytes == null || bytes.length == 0) {
                continue;
            }
            if (!write) {
                for (final OfficeRedactionSpan span : detectEmbeddedBytes(bytes, filter)) {
                    addEmbedded(captured, order, span);
                }
                continue;
            }
            final List<OfficeRedactionSpan> embeddedSpans = new ArrayList<>();
            final byte[] redacted = redactEmbeddedBytes(bytes, filter, embeddedSpans);
            if (redacted != null) {
                pkg.replacePart(embedded, redacted);
                for (final OfficeRedactionSpan span : embeddedSpans) {
                    addEmbedded(captured, order, span);
                }
            } else {
                final Document d = pkg.dom(chart);
                if (d != null) {
                    for (final Element ext : descendants(d, ChartRedactor.C, "externalData")) {
                        ext.getParentNode().removeChild(ext);
                    }
                    pkg.markDirty(chart);
                }
                pkg.deletePart(chart, rel);
                if (captured != null) {
                    captured.add(new OfficeRedactionSpan(order.next(), -1, 0, 0, "[embedded workbook]", "[removed]",
                            "embedded-workbook-removed"));
                }
            }
        }
    }

    static void addEmbedded(final List<OfficeRedactionSpan> captured, final Order order, final OfficeRedactionSpan span) {
        if (captured != null) {
            span.setOrder(order.next());
            span.setParagraphIndex(-1);
            captured.add(span);
        }
    }

    /**
     * Redacts an embedded .xlsx with the standard passes; null when it cannot be opened as one. A failure of
     * the filter itself propagates rather than being taken for an unreadable workbook.
     */
    static byte[] redactEmbeddedBytes(final byte[] bytes, final TextFilter filter, final List<OfficeRedactionSpan> spans) {
        final OoxmlPackage pkg = WordDocumentRedactor.openEmbedded(bytes);
        if (pkg == null) {
            return null;
        }
        try (pkg) {
            if (pkg.mainPart() == null || pkg.dom(pkg.mainPart()) == null) {
                return null;
            }
            final List<OfficeRedactionSpan> redacted = redactOpen(pkg, filter, OoxmlRedactionOptions.defaults());
            final byte[] output = pkg.save();
            spans.addAll(redacted);
            return output;
        } catch (final IOException e) {
            return null;
        }
    }

    static List<OfficeRedactionSpan> detectEmbeddedBytes(final byte[] bytes, final TextFilter filter) {
        final OoxmlPackage pkg = WordDocumentRedactor.openEmbedded(bytes);
        if (pkg == null) {
            return new ArrayList<>();
        }
        try (pkg) {
            return detectOpen(pkg, filter, null, true, true);
        }
    }

}
