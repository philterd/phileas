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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Splits input text into pieces for filtering and supplies the separator to rejoin them. */
public interface SplitService {

    Logger SPLIT_LOGGER = LogManager.getLogger(SplitService.class);

    List<String> split(String input);

    String getSeparator();

    /**
     * Splits the input into pieces that each carry the trailing <code>overlap</code> characters of
     * the previous piece, so an entity straddling a piece boundary is seen whole in the later piece.
     * Each piece is returned with its absolute offset in the input.
     *
     * @param input The text to split.
     * @param overlap The number of characters each piece shares with the previous one.
     * @return The pieces with their offsets, or empty when the pieces are not verbatim substrings of
     *         the input and so cannot be located in it. Callers must then split without an overlap.
     */
    default Optional<List<TextSplit>> splitWithOverlap(final String input, final int overlap) {

        final List<String> pieces = split(input);
        final List<TextSplit> splits = new ArrayList<>(pieces.size());

        int cursor = 0;

        for(final String piece : pieces) {

            // Each piece must begin at the cursor, give or take the whitespace the splitter consumed.
            // Searching further would risk matching identical text elsewhere and mislocating spans.
            int start = cursor;
            while(start < input.length() && Character.isWhitespace(input.charAt(start))) {
                start++;
            }

            if(!input.startsWith(piece, start)) {
                SPLIT_LOGGER.warn("Split pieces are not verbatim in the input; splitting without an overlap.");
                return Optional.empty();
            }

            // The first piece has no previous piece to share with.
            final int from = splits.isEmpty() ? start : Math.max(0, start - Math.max(0, overlap));
            splits.add(new TextSplit(input.substring(from, start + piece.length()), from));

            cursor = start + piece.length();

        }

        return Optional.of(splits);

    }

}
