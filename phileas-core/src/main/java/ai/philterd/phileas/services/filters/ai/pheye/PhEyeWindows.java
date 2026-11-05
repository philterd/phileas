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
package ai.philterd.phileas.services.filters.ai.pheye;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Divides text into overlapping windows that each fit in what a PhEye model examines in one call. A
 * GLiNER model reads at most {@code max_len} words and ignores the rest, so text longer than that is
 * detected one window at a time.
 *
 * <p>Words are counted as GLiNER's whitespace splitter counts them, so a punctuation mark is a word.
 * Each window owns the words between the midpoints of its overlaps with its neighbors, and a span is
 * kept only from the window that owns its first word. With half the overlap at least the longest span
 * a model reports ({@code max_width}), a kept span lies wholly inside its window, and no span is
 * reported twice.
 */
final class PhEyeWindows {

    /** GLiNER's whitespace word splitter: {@code \w+(?:[-_]\w+)*|\S}, with Unicode classes as Python's. */
    static final Pattern WORD = Pattern.compile("\\w+(?:[-_]\\w+)*|\\S", Pattern.UNICODE_CHARACTER_CLASS);

    /** The fewest words two windows share, for context on both sides of a span near their seam. */
    static final int MIN_OVERLAP = 64;

    /**
     * A window of the text, by character offsets: {@code [start, end)} is the text the model sees and
     * {@code [ownedStart, ownedEnd)} the part whose spans this window reports.
     */
    record Window(int start, int end, int ownedStart, int ownedEnd) {

        boolean owns(final int offset) {
            return offset >= ownedStart && offset < ownedEnd;
        }

    }

    private PhEyeWindows() {
    }

    /**
     * The windows for a text. Text of at most {@code maxWords} words is one window covering all of it.
     * @param text The text.
     * @param maxWords The most words the model examines in one call.
     * @param maxWidth The most words one span the model reports can cover.
     * @return The windows, in order.
     */
    static List<Window> of(final String text, final int maxWords, final int maxWidth) {

        final List<int[]> words = new ArrayList<>();
        final Matcher matcher = WORD.matcher(text);
        while (matcher.find()) {
            words.add(new int[] {matcher.start(), matcher.end()});
        }

        final int count = words.size();
        if (maxWords < 1 || count <= maxWords) {
            return List.of(new Window(0, text.length(), 0, text.length()));
        }

        final int overlap = overlap(maxWords, maxWidth);
        final int half = overlap / 2;
        final int step = maxWords - 2 * half;

        final List<Window> windows = new ArrayList<>();
        for (int first = 0; ; first += step) {
            final boolean firstWindow = first == 0;
            final boolean lastWindow = first + maxWords >= count;
            final int start = firstWindow ? 0 : words.get(first)[0];
            final int end = lastWindow ? text.length() : words.get(first + maxWords - 1)[1];
            final int ownedStart = firstWindow ? 0 : words.get(first + half)[0];
            final int ownedEnd = lastWindow ? text.length() : words.get(first + maxWords - half)[0];
            windows.add(new Window(start, end, ownedStart, ownedEnd));
            if (lastWindow) {
                return windows;
            }
        }

    }

    /**
     * The words two neighboring windows share: at least {@link #MIN_OVERLAP} and twice {@code maxWidth},
     * and at most half a window, so each window still advances.
     */
    static int overlap(final int maxWords, final int maxWidth) {
        return Math.min(Math.max(MIN_OVERLAP, 2 * maxWidth), maxWords / 2);
    }

}
