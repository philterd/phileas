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
package ai.philterd.phileas.pheye.onnx;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits text into words with their character offsets, matching GLiNER's
 * {@code WhitespaceTokenSplitter} (the model's {@code words_splitter_type} is "whitespace").
 *
 * <p>GLiNER uses the Python regex {@code \w+(?:[-_]\w+)*|\S}. Python's {@code re} treats
 * {@code \w} as Unicode for {@code str}, so {@link Pattern#UNICODE_CHARACTER_CLASS} is set
 * here to match accented names and non-ASCII word characters identically.
 */
public final class WordsSplitter {

    private static final Pattern PATTERN =
            Pattern.compile("\\w+(?:[-_]\\w+)*|\\S", Pattern.UNICODE_CHARACTER_CLASS);

    /** A word and its half-open character span [start, end) in the source text. */
    public record Word(String text, int start, int end) {}

    private WordsSplitter() {
    }

    public static List<Word> split(final String text) {

        final List<Word> words = new ArrayList<>();
        final Matcher matcher = PATTERN.matcher(text);

        while (matcher.find()) {
            words.add(new Word(matcher.group(), matcher.start(), matcher.end()));
        }

        return words;

    }

}
