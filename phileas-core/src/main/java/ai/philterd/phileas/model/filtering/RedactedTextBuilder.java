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
package ai.philterd.phileas.model.filtering;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a document's {@link RedactedText} line by line as a redactor walks the document: labeled segments in the
 * order they are first added, each segment's lines joined with a line break, and where the redaction's
 * replacements are in each segment, for the leftover check.
 */
public class RedactedTextBuilder {

    /** The label of a metadata segment. */
    public static final String METADATA_LABEL = "metadata";

    /** The line break between a segment's lines. */
    public static final String LINE_SEPARATOR = "\n";

    /**
     * A segment's text with where the redaction's replacements are in it.
     * @param label The segment's label.
     * @param text The segment's text.
     * @param replacedRanges Where the replacements are in the text.
     */
    public record Part(String label, String text, List<ReplacedRange> replacedRanges) {
    }

    private static final class Segment {
        private final StringBuilder text = new StringBuilder();
        private final List<ReplacedRange> replacedRanges = new ArrayList<>();
        private boolean hasLines;
        private boolean scanned = true;
    }

    private final Map<String, Segment> segments = new LinkedHashMap<>();

    /**
     * Adds a segment, if it is not there yet, so it is reported even with no text.
     * @param label The segment's label.
     */
    public void ensure(final String label) {
        segments.computeIfAbsent(label, k -> new Segment());
    }

    /**
     * Marks a segment the policy leaves unscanned, so its text is left out.
     * @param label The segment's label.
     */
    public void notScanned(final String label) {
        final Segment segment = segments.computeIfAbsent(label, k -> new Segment());
        segment.scanned = false;
        segment.text.setLength(0);
        segment.replacedRanges.clear();
    }

    /**
     * Appends a line to a segment. A segment marked unscanned takes no lines.
     * @param label The segment's label.
     * @param text The line's redacted text.
     * @param replacedRanges Where the replacements are in the line.
     */
    public void line(final String label, final String text, final List<ReplacedRange> replacedRanges) {
        final Segment segment = segments.computeIfAbsent(label, k -> new Segment());
        if (!segment.scanned) {
            return;
        }
        if (segment.hasLines) {
            segment.text.append(LINE_SEPARATOR);
        }
        final int start = segment.text.length();
        segment.text.append(text);
        for (final ReplacedRange range : replacedRanges) {
            segment.replacedRanges.add(range.shift(start));
        }
        segment.hasLines = true;
    }

    /**
     * Adds another document's segments, each label prefixed, such as an email attachment's.
     * @param prefix The prefix, such as {@code attachment: report.pdf / }.
     * @param text The other document's text.
     * @param parts The other document's parts, from which the replacements are taken.
     */
    public void include(final String prefix, final RedactedText text, final List<Part> parts) {
        for (final TextSegment segment : text.getSegments()) {
            final String label = prefix + segment.getLabel();
            switch (segment.getStatus()) {
                case NOT_SCANNED -> notScanned(label);
                case NO_TEXT -> ensure(label);
                case TEXT -> {
                    final List<ReplacedRange> ranges = parts.stream().filter(p -> p.label().equals(segment.getLabel()))
                            .findFirst().map(Part::replacedRanges).orElse(List.of());
                    line(label, segment.getText(), ranges);
                }
            }
        }
    }

    /**
     * Whether a segment has had a line added.
     * @param label The segment's label.
     * @return {@code true} if it has.
     */
    public boolean hasLines(final String label) {
        final Segment segment = segments.get(label);
        return segment != null && segment.hasLines;
    }

    /**
     * The segments in the order they were first added. A scanned segment with no text other than white space is
     * {@link TextSegment.Status#NO_TEXT}.
     * @return The {@link RedactedText}.
     */
    public RedactedText build() {
        final List<TextSegment> out = new ArrayList<>();
        for (final Map.Entry<String, Segment> entry : segments.entrySet()) {
            final Segment segment = entry.getValue();
            final TextSegment.Status status = !segment.scanned ? TextSegment.Status.NOT_SCANNED
                    : segment.text.toString().isBlank() ? TextSegment.Status.NO_TEXT : TextSegment.Status.TEXT;
            out.add(new TextSegment(entry.getKey(), segment.text.toString(), status));
        }
        return new RedactedText(out);
    }

    /**
     * The segments that have text, for the leftover check.
     * @return The parts, in segment order.
     */
    public List<Part> parts() {
        final List<Part> parts = new ArrayList<>();
        for (final Map.Entry<String, Segment> entry : segments.entrySet()) {
            final Segment segment = entry.getValue();
            if (segment.scanned && !segment.text.toString().isBlank()) {
                parts.add(new Part(entry.getKey(), segment.text.toString(), List.copyOf(segment.replacedRanges)));
            }
        }
        return parts;
    }

    /**
     * Line breaks as spaces, one for one, so a value stays on its line and its offsets are kept.
     * @param text The text.
     * @return The text on one line.
     */
    public static String flatten(final String text) {
        return text.replace('\r', ' ').replace('\n', ' ');
    }

}
