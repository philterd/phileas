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
package ai.philterd.phileas.services.pdf;

import ai.philterd.phileas.model.filtering.RedactedText;
import ai.philterd.phileas.model.filtering.ReplacedRange;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextSegment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds a PDF's redacted text, one segment per page and an optional metadata segment, from the lines the
 * redaction filtered. Each page's lines are joined with a line break, or with a space where the extractor read
 * the next text as another word of the same line.
 */
public final class PdfRedactedText {

    /** The label of the metadata segment. */
    public static final String METADATA_LABEL = "metadata";

    /** The line break between a page's lines, and between metadata entries. */
    public static final String LINE_SEPARATOR = "\n";

    /** A line's redacted text and where its replacements are in it. */
    public record Line(String text, List<ReplacedRange> replacedRanges) {
    }

    /**
     * A segment's text with what a leftover check needs to place a finding.
     * @param label The segment's label.
     * @param pageNumber The page, counting from 0, or -1 for the metadata segment.
     * @param text The segment's text.
     * @param replacedRanges Where the redaction's replacements are in the text.
     * @param lineStarts Where each line starts in the text.
     * @param lineNumbers The document's number for each line, counting from 1.
     */
    public record Part(String label, int pageNumber, String text, List<ReplacedRange> replacedRanges,
                       List<Integer> lineStarts, List<Integer> lineNumbers) {

        /** The document's number of the line an offset is in, or 0 for the metadata segment. */
        public int lineNumberAt(final int offset) {
            int line = 0;
            for (int i = 0; i < lineStarts.size() && lineStarts.get(i) <= offset; i++) {
                line = lineNumbers.get(i);
            }
            return line;
        }

    }

    private static final class Page {
        private final StringBuilder text = new StringBuilder();
        private final List<ReplacedRange> replacedRanges = new ArrayList<>();
        private final List<Integer> lineStarts = new ArrayList<>();
        private final List<Integer> lineNumbers = new ArrayList<>();
        private boolean scanned = true;
        private boolean hasExtractedText;
        private String separator = LINE_SEPARATOR;
    }

    private final Page[] pages;
    private Line metadata;
    private boolean metadataScanned = true;

    /**
     * @param pageCount The number of pages in the document.
     */
    public PdfRedactedText(final int pageCount) {
        this.pages = new Page[pageCount];
        for (int i = 0; i < pageCount; i++) {
            pages[i] = new Page();
        }
    }

    /**
     * Marks a page the policy leaves unscanned, so its text is left out.
     * @param pageNumber The page, counting from 0.
     */
    public void notScanned(final int pageNumber) {
        final Page page = pages[pageNumber];
        page.scanned = false;
        page.text.setLength(0);
        page.replacedRanges.clear();
        page.lineStarts.clear();
        page.lineNumbers.clear();
    }

    /**
     * Adds a line's redacted text to its page.
     * @param pageNumber The page, counting from 0.
     * @param lineNumber The document's number for the line, counting from 1.
     * @param extracted Whether the line as extracted had any text other than white space.
     * @param line The line's redacted text.
     * @param separatorAfter What separates this line from the next on its page, as
     *                       {@link PdfLine#getSeparatorAfter()} gives it.
     */
    public void addLine(final int pageNumber, final int lineNumber, final boolean extracted, final Line line,
                        final String separatorAfter) {
        final Page page = pages[pageNumber];
        if (!page.scanned) {
            return;
        }
        page.hasExtractedText |= extracted;
        if (!page.lineStarts.isEmpty()) {
            page.text.append(page.separator);
        }
        page.separator = separatorAfter;
        final int start = page.text.length();
        page.lineStarts.add(start);
        page.lineNumbers.add(lineNumber);
        page.text.append(line.text());
        for (final ReplacedRange range : line.replacedRanges()) {
            page.replacedRanges.add(range.shift(start));
        }
    }

    /**
     * Sets the metadata segment.
     * @param metadata The metadata's redacted text, one {@code key: value} line per entry.
     */
    public void setMetadata(final Line metadata) {
        this.metadata = metadata;
    }

    /** Adds a metadata segment the policy leaves unscanned, so the metadata is left out. */
    public void metadataNotScanned() {
        this.metadata = new Line("", List.of());
        this.metadataScanned = false;
    }

    /** The segments, pages first, then the metadata if it was set. */
    public RedactedText build() {
        final List<TextSegment> segments = new ArrayList<>();
        for (int i = 0; i < pages.length; i++) {
            final Page page = pages[i];
            final TextSegment.Status status = !page.scanned ? TextSegment.Status.NOT_SCANNED
                    : page.hasExtractedText ? TextSegment.Status.TEXT : TextSegment.Status.NO_TEXT;
            segments.add(new TextSegment(pageLabel(i), page.text.toString(), status));
        }
        if (metadata != null) {
            segments.add(new TextSegment(METADATA_LABEL, metadata.text(), !metadataScanned ? TextSegment.Status.NOT_SCANNED
                    : metadata.text().isBlank() ? TextSegment.Status.NO_TEXT : TextSegment.Status.TEXT));
        }
        return new RedactedText(segments);
    }

    /** The segments that have text, for the leftover check. */
    public List<Part> parts() {
        final List<Part> parts = new ArrayList<>();
        for (int i = 0; i < pages.length; i++) {
            final Page page = pages[i];
            if (page.scanned && page.hasExtractedText && !page.text.toString().isBlank()) {
                parts.add(new Part(pageLabel(i), i, page.text.toString(), List.copyOf(page.replacedRanges),
                        List.copyOf(page.lineStarts), List.copyOf(page.lineNumbers)));
            }
        }
        if (metadata != null && metadataScanned && !metadata.text().isBlank()) {
            parts.add(new Part(METADATA_LABEL, -1, metadata.text(), metadata.replacedRanges(), List.of(), List.of()));
        }
        return parts;
    }

    /**
     * The label of a page's segment.
     * @param pageNumber The page, counting from 0.
     * @return The label, such as {@code page 1}, counting from 1.
     */
    public static String pageLabel(final int pageNumber) {
        return "page " + (pageNumber + 1);
    }

    /**
     * A line's redacted text: the filtered text, less any characters a bounding box covers. A replacement is
     * kept where it overlaps a box, since it holds nothing from the document.
     * @param original The line as extracted.
     * @param filtered The line's filtered text.
     * @param replacedRanges Where the replacements are in the filtered text.
     * @param appliedSpans The applied spans, with offsets into the original line.
     * @param covered The characters of the original line a bounding box covers, or {@code null} for none.
     * @return The line's redacted text.
     */
    public static Line redactLine(final String original, final String filtered, final List<ReplacedRange> replacedRanges,
                                  final List<Span> appliedSpans, final boolean[] covered) {

        if (covered == null) {
            return new Line(filtered, List.copyOf(replacedRanges));
        }

        // Rebuilt from the spans as the filtered text was, leaving out covered characters.
        final List<Span> spans = appliedSpans.stream().sorted(Comparator.comparingInt(Span::getCharacterStart)).toList();
        final StringBuilder sb = new StringBuilder(original.length());
        final List<ReplacedRange> ranges = new ArrayList<>(spans.size());
        int copiedTo = 0;
        for (final Span span : spans) {
            appendUncovered(sb, original, copiedTo, span.getCharacterStart(), covered);
            final int start = sb.length();
            sb.append(span.getReplacement());
            ranges.add(new ReplacedRange(start, sb.length()));
            copiedTo = span.getCharacterEnd();
        }
        appendUncovered(sb, original, copiedTo, original.length(), covered);

        return new Line(sb.toString(), ranges);

    }

    private static void appendUncovered(final StringBuilder sb, final String original, final int from, final int to,
                                        final boolean[] covered) {
        for (int i = from; i < to; i++) {
            if (!covered[i]) {
                sb.append(original.charAt(i));
            }
        }
    }

}
