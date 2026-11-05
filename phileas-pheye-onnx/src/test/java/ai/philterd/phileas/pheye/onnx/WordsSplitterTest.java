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
package ai.philterd.phileas.pheye.onnx;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link WordsSplitter}. The splitter feeds word-to-character offset mapping in the
 * detector, so an off-by-one here would shift every redaction span; these assert the GLiNER
 * {@code \w+(?:[-_]\w+)*|\S} behaviour and that the returned offsets reconstruct the source text.
 */
class WordsSplitterTest {

    @Test
    void splitsSimpleWordsWithOffsets() {
        final List<WordsSplitter.Word> words = WordsSplitter.split("Alice Bob");
        assertEquals(2, words.size());
        assertWord(words.get(0), "Alice", 0, 5);
        assertWord(words.get(1), "Bob", 6, 9);
    }

    @Test
    void keepsHyphenatedAndUnderscoredWordsTogether() {
        // \w+(?:[-_]\w+)* keeps internal hyphens and underscores inside a single token.
        assertEquals(List.of("Jean-Paul"), texts(WordsSplitter.split("Jean-Paul")));
        assertEquals(List.of("first_name"), texts(WordsSplitter.split("first_name")));
        assertEquals(List.of("a-b_c-d"), texts(WordsSplitter.split("a-b_c-d")));
    }

    @Test
    void treatsLeadingHyphenAsItsOwnToken() {
        // A separator is only kept when it sits between two word chunks; a leading '-' is a lone \S.
        assertEquals(List.of("-", "Paul"), texts(WordsSplitter.split("-Paul")));
    }

    @Test
    void emitsPunctuationAsSingleCharacterTokens() {
        final List<WordsSplitter.Word> words = WordsSplitter.split("Hello, world!");
        assertEquals(List.of("Hello", ",", "world", "!"), texts(words));
        assertWord(words.get(1), ",", 5, 6);
        assertWord(words.get(3), "!", 12, 13);
    }

    @Test
    void offsetsSurviveIrregularWhitespace() {
        // Leading, trailing, and repeated whitespace must not corrupt the offsets of real words.
        final String text = "  John\t\tSmith \n";
        final List<WordsSplitter.Word> words = WordsSplitter.split(text);
        assertEquals(2, words.size());
        assertWord(words.get(0), "John", 2, 6);
        assertWord(words.get(1), "Smith", 8, 13);
        for (final WordsSplitter.Word w : words) {
            assertEquals(w.text(), text.substring(w.start(), w.end()), "offsets must reconstruct the word");
        }
    }

    @Test
    void treatsUnicodeLettersAndDigitsAsWordCharacters() {
        // UNICODE_CHARACTER_CLASS: accented names are one word, matching Python's \w for str.
        assertEquals(List.of("José"), texts(WordsSplitter.split("José")));
        assertEquals(List.of("Müller"), texts(WordsSplitter.split("Müller")));
        assertEquals(List.of("abc123"), texts(WordsSplitter.split("abc123")));
    }

    @Test
    void returnsEmptyForEmptyAndWhitespaceOnlyInput() {
        assertTrue(WordsSplitter.split("").isEmpty());
        assertTrue(WordsSplitter.split("   \t\n ").isEmpty());
    }

    private static List<String> texts(final List<WordsSplitter.Word> words) {
        return words.stream().map(WordsSplitter.Word::text).toList();
    }

    private static void assertWord(final WordsSplitter.Word word, final String text, final int start, final int end) {
        assertEquals(text, word.text());
        assertEquals(start, word.start());
        assertEquals(end, word.end());
    }

}
