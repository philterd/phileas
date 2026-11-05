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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class PhEyeWindowsTest {

    private static List<int[]> words(final String text) {
        final List<int[]> words = new ArrayList<>();
        final Matcher matcher = PhEyeWindows.WORD.matcher(text);
        while (matcher.find()) {
            words.add(new int[] {matcher.start(), matcher.end()});
        }
        return words;
    }

    private static String text(final int words) {
        final StringBuilder text = new StringBuilder();
        for (int i = 0; i < words; i++) {
            text.append(i == 0 ? "" : " ").append("w").append(i);
        }
        return text.toString();
    }

    @Test
    public void textThatFitsIsOneWindow() {
        final String text = "  " + text(50) + "  ";
        Assertions.assertEquals(List.of(new PhEyeWindows.Window(0, text.length(), 0, text.length())),
                PhEyeWindows.of(text, 50, 12));
        Assertions.assertEquals(1, PhEyeWindows.of("", 50, 12).size());
    }

    @Test
    public void oneWordOverTheLimitIsTwoWindows() {
        Assertions.assertEquals(2, PhEyeWindows.of(text(51), 50, 12).size());
    }

    @Test
    public void punctuationCountsAsAWord() {
        // Eleven words to the model, though five characters are letters.
        Assertions.assertEquals(11, words("a,b,c,d,e,f").size());
        Assertions.assertEquals(2, PhEyeWindows.of("a,b,c,d,e,f", 10, 1).size());
    }

    @Test
    public void wordsWithHyphensAndUnderscoresAreOneWord() {
        Assertions.assertEquals(3, words("well-known snake_case Zoë").size());
    }

    @Test
    public void overlapIsAtLeastTwiceTheWidthAndAtMostHalfAWindow() {
        Assertions.assertEquals(64, PhEyeWindows.overlap(512, 12));
        Assertions.assertEquals(100, PhEyeWindows.overlap(512, 50));
        Assertions.assertEquals(25, PhEyeWindows.overlap(50, 12));
    }

    @Test
    public void windowsFitTheModelAndEverySpanIsOwnedByExactlyOneWindowThatContainsIt() {

        for (final int[] limits : new int[][] {{512, 12}, {256, 12}, {50, 3}, {10, 2}, {7, 3}}) {

            final int maxWords = limits[0];
            final int maxWidth = limits[1];

            for (final int count : new int[] {maxWords + 1, 2 * maxWords, 5 * maxWords + 3, 1000}) {

                final String text = text(count) + ", and; more.";
                final List<int[]> words = words(text);
                final List<PhEyeWindows.Window> windows = PhEyeWindows.of(text, maxWords, maxWidth);
                final String label = "maxWords " + maxWords + ", maxWidth " + maxWidth + ", " + words.size() + " words";

                Assertions.assertEquals(0, windows.get(0).ownedStart(), label);
                Assertions.assertEquals(text.length(), windows.get(windows.size() - 1).ownedEnd(), label);

                for (int i = 0; i < windows.size(); i++) {
                    final PhEyeWindows.Window window = windows.get(i);
                    Assertions.assertTrue(words(text.substring(window.start(), window.end())).size() <= maxWords, label);
                    if (i > 0) {
                        Assertions.assertEquals(windows.get(i - 1).ownedEnd(), window.ownedStart(), label);
                    }
                }

                // Every span of up to maxWidth words: exactly one window owns its first word, and that window
                // contains all of it.
                for (int first = 0; first < words.size(); first++) {
                    final int start = words.get(first)[0];
                    final int end = words.get(Math.min(first + maxWidth, words.size()) - 1)[1];
                    final List<PhEyeWindows.Window> owners = windows.stream().filter(w -> w.owns(start)).toList();
                    Assertions.assertEquals(1, owners.size(), label + ", word " + first);
                    if (2 * maxWidth <= PhEyeWindows.overlap(maxWords, maxWidth)) {
                        Assertions.assertTrue(owners.get(0).start() <= start && end <= owners.get(0).end(),
                                label + ", word " + first);
                    }
                }

            }

        }

    }

}
