package spike;

import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/** Shared types: the span record, the filter delegate, and span math. */
final class Office {

    /** Port of OfficeRedactionSpan (explanation fields omitted). */
    record OfficeSpan(int order, int paragraphIndex, int start, int end, String text, String replacement,
                      String classification) { }

    /** The filter delegate: Func&lt;string, TextFilterResult&gt;. */
    interface Filter extends Function<String, TextFilterResult> { }

    record Range(int start, int end, String replacement) { }

    /** Mutable order counter, standing in for the C# {@code ref int order}. */
    static final class Order {
        int value;
        int next() {
            return value++;
        }
    }

    static List<Span> spans(final TextFilterResult result) {
        return result.getExplanation() == null ? List.of() : result.getExplanation().appliedSpans();
    }

    static List<Span> validSorted(final TextFilterResult result, final String original) {
        final List<Span> out = new ArrayList<>();
        for (Span s : spans(result)) {
            if (s.getCharacterStart() >= 0 && s.getCharacterEnd() <= original.length()
                    && s.getCharacterEnd() > s.getCharacterStart()) {
                out.add(s);
            }
        }
        out.sort(Comparator.comparingInt(Span::getCharacterStart));
        return out;
    }

    static List<Range> resolveNonOverlapping(final List<Range> ranges) {
        final List<Range> sorted = new ArrayList<>(ranges.stream().filter(r -> r.end() > r.start()).toList());
        sorted.sort(Comparator.comparingInt(Range::start).thenComparing(Comparator.comparingInt(Range::end).reversed()));
        final List<Range> result = new ArrayList<>();
        int lastEnd = -1;
        for (Range r : sorted) {
            if (r.start() >= lastEnd) {
                result.add(r);
                lastEnd = r.end();
            }
        }
        return result;
    }

    static List<Range> ranges(final List<Span> spans) {
        return resolveNonOverlapping(spans.stream()
                .map(s -> new Range(s.getCharacterStart(), s.getCharacterEnd(), nz(s.getReplacement()))).toList());
    }

    static void capture(final TextFilterResult result, final String original, final int paragraphIndex,
                        final String defaultClassification, final List<OfficeSpan> captured, final Order order) {
        if (captured == null) {
            return;
        }
        for (Span s : validSorted(result, original)) {
            final String cls = nz(s.getClassification()).isEmpty() && defaultClassification != null
                    ? defaultClassification : nz(s.getClassification());
            captured.add(new OfficeSpan(order.next(), paragraphIndex, s.getCharacterStart(), s.getCharacterEnd(),
                    original.substring(s.getCharacterStart(), s.getCharacterEnd()), nz(s.getReplacement()), cls));
        }
    }

    static String nz(final String s) {
        return s == null ? "" : s;
    }

    private Office() { }
}
