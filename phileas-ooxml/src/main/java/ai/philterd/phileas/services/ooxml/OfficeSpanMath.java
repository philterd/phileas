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

import ai.philterd.phileas.model.filtering.ReplacedRange;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Span arithmetic shared by the Word, Excel, and DrawingML redactors. */
final class OfficeSpanMath {

    record ReplacementRange(int start, int end, String replacement) {
    }

    /** Mutable redaction order, standing in for phileas-dotnet's {@code ref int order}. */
    static final class Order {
        private int value;

        Order() {
        }

        Order(final int start) {
            this.value = start;
        }

        int next() {
            return value++;
        }

        int value() {
            return value;
        }
    }

    /** Keeps the earliest-starting (then longest) of any overlapping ranges. */
    static List<ReplacementRange> resolveNonOverlapping(final List<ReplacementRange> ranges) {
        final List<ReplacementRange> sorted = new ArrayList<>();
        for (final ReplacementRange r : ranges) {
            if (r.end() > r.start()) {
                sorted.add(r);
            }
        }
        sorted.sort(Comparator.comparingInt(ReplacementRange::start)
                .thenComparing(Comparator.comparingInt(ReplacementRange::end).reversed()));
        final List<ReplacementRange> result = new ArrayList<>();
        int lastEnd = -1;
        for (final ReplacementRange r : sorted) {
            if (r.start() >= lastEnd) {
                result.add(r);
                lastEnd = r.end();
            }
        }
        return result;
    }

    /** The result's applied spans that fall within {@code original}, ordered by start. */
    static List<Span> validSpans(final TextFilterResult result, final String original) {
        final List<Span> out = new ArrayList<>();
        if (result.getExplanation() != null && result.getExplanation().appliedSpans() != null) {
            for (final Span s : result.getExplanation().appliedSpans()) {
                if (s.getCharacterStart() >= 0 && s.getCharacterEnd() <= original.length()
                        && s.getCharacterEnd() > s.getCharacterStart()) {
                    out.add(s);
                }
            }
        }
        out.sort(Comparator.comparingInt(Span::getCharacterStart));
        return out;
    }

    static List<Span> appliedSpans(final TextFilterResult result) {
        return result.getExplanation() == null || result.getExplanation().appliedSpans() == null
                ? List.of() : result.getExplanation().appliedSpans();
    }

    static List<ReplacementRange> ranges(final List<Span> spans) {
        final List<ReplacementRange> ranges = new ArrayList<>();
        for (final Span s : spans) {
            ranges.add(new ReplacementRange(s.getCharacterStart(), s.getCharacterEnd(), nz(s.getReplacement())));
        }
        return resolveNonOverlapping(ranges);
    }

    /** Records each detection in {@code result}, at {@code paragraphIndex}, onto {@code captured}. */
    static void capture(final TextFilterResult result, final String original, final int paragraphIndex,
                        final String defaultClassification, final List<OfficeRedactionSpan> captured, final Order order) {
        if (captured == null) {
            return;
        }
        for (final Span s : validSpans(result, original)) {
            captured.add(entity(s, original, paragraphIndex, defaultClassification, order));
        }
    }

    static OfficeRedactionSpan entity(final Span s, final String original, final int paragraphIndex,
                                      final String defaultClassification, final Order order) {
        final String classification = nz(s.getClassification()).isEmpty() && defaultClassification != null
                ? defaultClassification : nz(s.getClassification());
        final OfficeRedactionSpan entity = new OfficeRedactionSpan(order.next(), paragraphIndex, s.getCharacterStart(),
                s.getCharacterEnd(), original.substring(s.getCharacterStart(), s.getCharacterEnd()),
                nz(s.getReplacement()), classification);
        entity.explain(s);
        return entity;
    }

    static String applyRanges(final String original, final List<ReplacementRange> ranges) {
        final StringBuilder sb = new StringBuilder(original);
        final List<ReplacementRange> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparingInt(ReplacementRange::start).reversed());
        for (final ReplacementRange r : sorted) {
            if (r.start() < 0 || r.end() > original.length() || r.end() <= r.start()) {
                continue;
            }
            sb.replace(r.start(), r.end(), nz(r.replacement()));
        }
        return sb.toString();
    }

    /** Text with replacements applied, and where each replacement is in it. */
    record Applied(String text, List<ReplacedRange> replacedRanges) {
    }

    /** As {@link #applyRanges}, also reporting where each replacement lands. The ranges must not overlap. */
    static Applied applyRangesTracked(final String original, final List<ReplacementRange> ranges) {
        final List<ReplacementRange> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparingInt(ReplacementRange::start));
        final StringBuilder sb = new StringBuilder(original.length());
        final List<ReplacedRange> replaced = new ArrayList<>();
        int copiedTo = 0;
        for (final ReplacementRange r : sorted) {
            if (r.start() < copiedTo || r.end() > original.length() || r.end() <= r.start()) {
                continue;
            }
            sb.append(original, copiedTo, r.start());
            final int start = sb.length();
            sb.append(nz(r.replacement()));
            replaced.add(new ReplacedRange(start, sb.length()));
            copiedTo = r.end();
        }
        sb.append(original, copiedTo, original.length());
        return new Applied(sb.toString(), replaced);
    }

    /**
     * A leaf's text as a redactor that writes the filter's output writes it, with where its replacements are.
     * When the spans do not rebuild that output, the ranges are left empty.
     */
    static Applied asWritten(final TextFilterResult result, final String original) {
        if (result.getFilteredText().equals(original)) {
            return new Applied(original, List.of());
        }
        final Applied applied = applyRangesTracked(original, ranges(validSpans(result, original)));
        return applied.text().equals(result.getFilteredText()) ? applied : new Applied(result.getFilteredText(), List.of());
    }

    static String nz(final String s) {
        return s == null ? "" : s;
    }

    private OfficeSpanMath() {
    }

}
