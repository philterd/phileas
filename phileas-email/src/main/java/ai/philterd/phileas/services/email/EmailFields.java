/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services.email;

import ai.philterd.phileas.model.filtering.ReplacedRange;
import ai.philterd.phileas.model.filtering.Span;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detection and replacement over one email field.
 */
public final class EmailFields {

    // A reference to a part of the message, such as an inline image. Its Content-ID is kept in the part's own
    // headers, so a value found in it is left alone rather than breaking the reference.
    private static final Pattern CONTENT_ID_REFERENCE = Pattern.compile("(?i)cid:[^\\s\"'<>()]+");

    /** Finds spans in text. */
    public interface Detector {

        /**
         * Finds spans in text.
         * @param text The text.
         * @return The spans, with offsets into {@code text} and their replacements.
         * @throws Exception Thrown if the text cannot be scanned.
         */
        List<Span> detect(String text) throws Exception;

    }

    private EmailFields() {
    }

    /**
     * Finds the spans in HTML, as Philter Desktop does: once in the markup, which reaches attribute values such as
     * a {@code mailto:} link, and once in the visible text, which reaches a value split by tags or written with
     * character references. Visible-text spans are mapped back to the markup, and overlapping spans are merged. A
     * value inside a {@code cid:} reference is left alone, since it names a part of the message.
     * @param html The markup.
     * @param detector What finds the spans.
     * @return Non-overlapping spans with offsets into the markup, in order.
     * @throws Exception Thrown if the text cannot be scanned.
     */
    public static List<Span> detectHtml(final String html, final Detector detector) throws Exception {

        final List<Span> spans = new ArrayList<>();

        final List<int[]> references = new ArrayList<>();
        final Matcher matcher = CONTENT_ID_REFERENCE.matcher(html);
        while (matcher.find()) {
            references.add(new int[]{matcher.start(), matcher.end()});
        }

        for (final Span span : detector.detect(html)) {
            if (isWithin(span, html.length()) && !isInReference(span, references)) {
                spans.add(span.copy());
            }
        }

        final HtmlText visible = HtmlText.of(html);
        if (!visible.text().isEmpty() && !visible.text().equals(html)) {
            for (final Span span : detector.detect(visible.text())) {
                if (isWithin(span, visible.text().length())) {
                    final Span mapped = span.copy();
                    mapped.setCharacterStart(visible.rawStart(span.getCharacterStart()));
                    mapped.setCharacterEnd(visible.rawEnd(span.getCharacterEnd()));
                    spans.add(mapped);
                }
            }
        }

        return merge(spans);

    }

    /**
     * Replaces spans in text.
     * @param text The text.
     * @param spans Non-overlapping spans with offsets into {@code text}.
     * @param fallbackReplacement The replacement for a span that has none.
     * @return The text with each span replaced, and where the replacements are.
     */
    public static EmailRedactor.FieldRedaction replace(final String text, final List<Span> spans,
                                                       final String fallbackReplacement) {

        final List<Span> ordered = new ArrayList<>(spans);
        ordered.sort(Comparator.comparingInt(Span::getCharacterStart));

        final StringBuilder output = new StringBuilder(text.length());
        final List<ReplacedRange> ranges = new ArrayList<>();
        int copiedTo = 0;

        for (final Span span : ordered) {
            final String replacement = span.getReplacement() == null || span.getReplacement().isEmpty()
                    ? fallbackReplacement : span.getReplacement();
            output.append(text, copiedTo, span.getCharacterStart());
            final int start = output.length();
            output.append(replacement);
            ranges.add(new ReplacedRange(start, output.length()));
            copiedTo = span.getCharacterEnd();
        }

        output.append(text, copiedTo, text.length());
        return new EmailRedactor.FieldRedaction(output.toString(), ordered, ranges);

    }

    /**
     * The spans that fall within text, with overlaps merged.
     * @param spans The spans.
     * @param length The length of the text.
     * @return Non-overlapping spans, in order.
     */
    public static List<Span> resolve(final List<Span> spans, final int length) {
        final List<Span> within = new ArrayList<>();
        for (final Span span : spans) {
            if (isWithin(span, length)) {
                within.add(span.copy());
            }
        }
        return merge(within);
    }

    private static boolean isInReference(final Span span, final List<int[]> references) {
        for (final int[] reference : references) {
            if (span.getCharacterStart() >= reference[0] && span.getCharacterEnd() <= reference[1]) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWithin(final Span span, final int length) {
        return span.getCharacterStart() >= 0 && span.getCharacterEnd() <= length
                && span.getCharacterEnd() > span.getCharacterStart();
    }

    // A span overlapping an earlier one extends it, and the earlier one's replacement is kept.
    private static List<Span> merge(final List<Span> spans) {

        final List<Span> sorted = new ArrayList<>(spans);
        sorted.sort(Comparator.comparingInt(Span::getCharacterStart)
                .thenComparing(Comparator.comparingInt(Span::getCharacterEnd).reversed()));

        final List<Span> merged = new ArrayList<>();
        for (final Span span : sorted) {
            if (!merged.isEmpty() && span.getCharacterStart() < merged.get(merged.size() - 1).getCharacterEnd()) {
                final Span last = merged.get(merged.size() - 1);
                if (span.getCharacterEnd() > last.getCharacterEnd()) {
                    last.setCharacterEnd(span.getCharacterEnd());
                }
                continue;
            }
            merged.add(span);
        }

        return merged;

    }

}
