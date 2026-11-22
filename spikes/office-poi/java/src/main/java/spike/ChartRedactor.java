package spike;

import ai.philterd.phileas.model.filtering.TextFilterResult;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

import static spike.Office.*;
import static spike.Pkg.*;

/** Port of ChartRedactor: DrawingML paragraph text and chart cached values. */
final class ChartRedactor {

    static boolean redactChartPart(final Element root, final Filter filter, final boolean write,
                                   final List<OfficeSpan> captured, final Order order) {
        boolean changed = redactDrawingText(root, filter, write, captured, order);
        for (Element v : desc(root, C, "v")) {
            changed |= redactLeaf(v, filter, write, captured, order);
        }
        return changed;
    }

    /** Returns true when anything was (or would be) rewritten. */
    static boolean redactDrawingText(final Element root, final Filter filter, final boolean write,
                                     final List<OfficeSpan> captured, final Order order) {
        boolean changed = false;
        for (Element p : desc(root, A, "p")) {
            changed |= redactDrawingParagraph(p, filter, write, captured, order);
        }
        return changed;
    }

    private record Item(Element element, String text, boolean isText) { }

    static List<Item> content(final Element paragraph) {
        final List<Item> items = new ArrayList<>();
        for (Element e : allDesc(paragraph)) {
            if (is(e, A, "t")) {
                items.add(new Item(e, e.getTextContent(), true));
            } else if (is(e, A, "br")) {
                items.add(new Item(e, "\n", false));
            }
        }
        return items;
    }

    static String paragraphText(final Element paragraph) {
        final StringBuilder sb = new StringBuilder();
        content(paragraph).forEach(i -> sb.append(i.text()));
        return sb.toString();
    }

    private static boolean redactDrawingParagraph(final Element paragraph, final Filter filter, final boolean write,
                                                  final List<OfficeSpan> captured, final Order order) {
        final List<Item> content = content(paragraph);
        if (content.stream().noneMatch(Item::isText)) {
            return false;
        }
        final String original = paragraphText(paragraph);
        if (original.isEmpty()) {
            return false;
        }
        final TextFilterResult result = filter.apply(original);
        if (result.getFilteredText().equals(original)) {
            return false;
        }
        if (write) {
            final List<Range> ranges = ranges(validSorted(result, original));
            if (!rewrite(content, original, ranges, result.getFilteredText())) {
                final List<Element> texts = content.stream().filter(Item::isText).map(Item::element).toList();
                setText(texts.get(0), result.getFilteredText().replace("\n", " "));
                for (int i = 1; i < texts.size(); i++) {
                    setText(texts.get(i), "");
                }
            }
        }
        capture(result, original, -1, null, captured, order);
        return true;
    }

    private static boolean rewrite(final List<Item> content, final String original, final List<Range> ranges,
                                   final String filteredText) {
        final int[] rangeAt = new int[original.length()];
        java.util.Arrays.fill(rangeAt, -1);
        for (int r = 0; r < ranges.size(); r++) {
            for (int i = ranges.get(r).start(); i < ranges.get(r).end(); i++) {
                rangeAt[i] = r;
            }
        }
        final boolean[] written = new boolean[ranges.size()];
        final List<Object[]> rewrites = new ArrayList<>();
        final StringBuilder produced = new StringBuilder();
        int offset = 0;
        for (Item item : content) {
            if (!item.isText()) {
                if (rangeAt[offset] < 0) {
                    produced.append(item.text());
                }
            } else {
                final StringBuilder rewritten = new StringBuilder();
                for (int i = offset; i < offset + item.text().length(); i++) {
                    final int r = rangeAt[i];
                    if (r < 0) {
                        rewritten.append(original.charAt(i));
                    } else if (!written[r]) {
                        rewritten.append(ranges.get(r).replacement());
                        written[r] = true;
                    }
                }
                rewrites.add(new Object[]{item.element(), rewritten.toString()});
                produced.append(rewritten);
            }
            offset += item.text().length();
        }
        if (!produced.toString().equals(filteredText)) {
            return false;
        }
        for (Object[] rw : rewrites) {
            setText((Element) rw[0], (String) rw[1]);
        }
        return true;
    }

    static boolean redactLeaf(final Element element, final Filter filter, final boolean write,
                              final List<OfficeSpan> captured, final Order order) {
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
        capture(result, original, -1, null, captured, order);
        return true;
    }

    private ChartRedactor() { }
}
