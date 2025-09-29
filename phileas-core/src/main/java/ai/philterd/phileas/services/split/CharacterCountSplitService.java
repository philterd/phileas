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
package ai.philterd.phileas.services.split;

import ai.philterd.phileas.model.services.SplitService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class CharacterCountSplitService extends AbstractSplitService implements SplitService {

    private static final Logger LOGGER = LogManager.getLogger(CharacterCountSplitService.class);

    private final int maxChunkSize;

    public CharacterCountSplitService(final int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    @Override
    public List<String> split(final String input) {

        final List<String> splits = new ArrayList<>();

        if (input == null || input.trim().isEmpty()) {
            return splits;
        }

        // Split the text into sentences. The regex uses a positive lookbehind to keep the sentence-ending
        // punctuation with the sentence. It splits on space(s) that follow a period, question mark,
        // or exclamation mark.
        final String[] sentences = input.split("(?<=[.?!])\\s");

        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            // If adding the next sentence would exceed the max chunk size,
            // and the current chunk is not empty, then finalize the current chunk.
            if (!currentChunk.isEmpty() && currentChunk.length() + sentence.length() > maxChunkSize) {
                splits.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }

            // Append the sentence. The sentence string already contains any leading
            // whitespace, so we don't need to add an extra space.
            currentChunk.append(sentence).append(" ");
        }

        // Add the last remaining chunk to the list if it exists.
        if (!currentChunk.isEmpty()) {
            splits.add(currentChunk.toString().trim());
        }

        return splits;

    }

    @Override
    public String getSeparator() {
        return " ";
    }

}
