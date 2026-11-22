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

import ai.philterd.phileas.model.filtering.TextFilterResult;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ai.philterd.phileas.services.ooxml.OfficeSpanMath.Order;
import static ai.philterd.phileas.services.ooxml.OfficeSpanMath.ReplacementRange;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.allDescendants;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.descendants;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.is;
import static ai.philterd.phileas.services.ooxml.OoxmlPackage.setText;

/**
 * Redacts DrawingML text, shared by Word and Excel: chart titles, axis and data-label text, shape, text
 * box, and SmartArt text ({@code <a:t>}), and a chart's cached series, category, and series-name values
 * ({@code <c:v>}). Redactions are captured with paragraph index -1.
 */
final class ChartRedactor {

    static final String A = "http://schemas.openxmlformats.org/drawingml/2006/main";
    static final String C = "http://schemas.openxmlformats.org/drawingml/2006/chart";

    /** Redacts (or, without {@code write}, detects) a chart part. Returns true when anything changed. */
    static boolean redactChartPart(final Element root, final TextFilter filter, final boolean write,
                                   final List<OfficeRedactionSpan> captured, final Order order) {
        boolean changed = redactDrawingText(root, filter, write, captured, order);
        for (final Element value : descendants(root, C, "v")) {
            changed |= redactLeaf(value, filter, write, captured, order);
        }
        return changed;
    }

    /** Redacts every DrawingML paragraph under {@code root}. Returns true when anything changed. */
    static boolean redactDrawingText(final Element root, final TextFilter filter, final boolean write,
                                     final List<OfficeRedactionSpan> captured, final Order order) {
        boolean changed = false;
        for (final Element paragraph : descendants(root, A, "p")) {
            changed |= redactDrawingParagraph(paragraph, filter, write, captured, order);
        }
        return changed;
    }

    private record Item(Element element, String text, boolean isText) {
    }

    // A DrawingML paragraph's text elements and line breaks in document order. A line break is "\n".
    private static List<Item> content(final Element paragraph) {
        final List<Item> items = new ArrayList<>();
        for (final Element e : allDescendants(paragraph)) {
            if (is(e, A, "t")) {
                items.add(new Item(e, e.getTextContent(), true));
            } else if (is(e, A, "br")) {
                items.add(new Item(e, "\n", false));
            }
        }
        return items;
    }

    /** The paragraph's text as the filter sees it. */
    static String paragraphText(final Element paragraph) {
        final StringBuilder sb = new StringBuilder();
        for (final Item item : content(paragraph)) {
            sb.append(item.text());
        }
        return sb.toString();
    }

    static boolean hasText(final Element paragraph) {
        return content(paragraph).stream().anyMatch(Item::isText);
    }

    private static boolean redactDrawingParagraph(final Element paragraph, final TextFilter filter, final boolean write,
                                                  final List<OfficeRedactionSpan> captured, final Order order) {
        final List<Item> content = content(paragraph);
        if (content.stream().noneMatch(Item::isText)) {
            return false;
        }
        final StringBuilder sb = new StringBuilder();
        content.forEach(i -> sb.append(i.text()));
        final String original = sb.toString();
        if (original.isEmpty()) {
            return false;
        }
        final TextFilterResult result = filter.apply(original);
        if (result.getFilteredText().equals(original)) {
            return false;
        }
        if (write) {
            final List<ReplacementRange> ranges = OfficeSpanMath.ranges(OfficeSpanMath.validSpans(result, original));
            if (!rewrite(content, original, ranges, result.getFilteredText())) {
                // The spans do not account for the filtered text. Write the filtered text whole rather than
                // risk leaving a value in place; its "\n"s become spaces, as the <a:br> elements remain.
                final List<Element> texts = content.stream().filter(Item::isText).map(Item::element).toList();
                setText(texts.get(0), result.getFilteredText().replace("\n", " "));
                for (int i = 1; i < texts.size(); i++) {
                    setText(texts.get(i), "");
                }
            }
        }
        OfficeSpanMath.capture(result, original, -1, null, captured, order);
        return true;
    }

    // Writes each replacement into the text element where its span starts and removes the rest of the span
    // from whichever elements hold it, leaving line breaks in place. Changes nothing and returns false when
    // the replacements do not produce filteredText.
    private static boolean rewrite(final List<Item> content, final String original, final List<ReplacementRange> ranges,
                                   final String filteredText) {
        final int[] rangeAt = new int[original.length()];
        Arrays.fill(rangeAt, -1);
        for (int r = 0; r < ranges.size(); r++) {
            for (int i = ranges.get(r).start(); i < ranges.get(r).end(); i++) {
                rangeAt[i] = r;
            }
        }
        final boolean[] written = new boolean[ranges.size()];
        final List<Element> elements = new ArrayList<>();
        final List<String> texts = new ArrayList<>();
        final StringBuilder produced = new StringBuilder(filteredText.length());
        int offset = 0;
        for (final Item item : content) {
            if (!item.isText()) {
                if (rangeAt[offset] < 0) {
                    produced.append(item.text());
                }
            } else {
                final StringBuilder rewritten = new StringBuilder(item.text().length());
                for (int i = offset; i < offset + item.text().length(); i++) {
                    final int r = rangeAt[i];
                    if (r < 0) {
                        rewritten.append(original.charAt(i));
                    } else if (!written[r]) {
                        rewritten.append(ranges.get(r).replacement());
                        written[r] = true;
                    }
                }
                elements.add(item.element());
                texts.add(rewritten.toString());
                produced.append(rewritten);
            }
            offset += item.text().length();
        }
        if (!produced.toString().equals(filteredText)) {
            return false;
        }
        for (int i = 0; i < elements.size(); i++) {
            setText(elements.get(i), texts.get(i));
        }
        return true;
    }

    /** Redacts a text-only element (a chart value, a comment run, a header) in place. */
    static boolean redactLeaf(final Element element, final TextFilter filter, final boolean write,
                              final List<OfficeRedactionSpan> captured, final Order order) {
        return redactLeaf(element, filter, write, captured, order, null);
    }

    static boolean redactLeaf(final Element element, final TextFilter filter, final boolean write,
                              final List<OfficeRedactionSpan> captured, final Order order, final String defaultClassification) {
        final String original = element.getTextContent();
        if (original == null || original.isEmpty()) {
            return false;
        }
        final TextFilterResult result = filter.apply(original);
        if (result.getFilteredText().equals(original)) {
            return false;
        }
        if (write) {
            setText(element, result.getFilteredText());
        }
        OfficeSpanMath.capture(result, original, -1, defaultClassification, captured, order);
        return true;
    }

    private ChartRedactor() {
    }

}
