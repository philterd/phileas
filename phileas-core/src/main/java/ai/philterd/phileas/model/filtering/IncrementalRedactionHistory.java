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
package ai.philterd.phileas.model.filtering;

import java.util.List;

/**
 * The input text of one filtered piece and the replacements applied to it, in order of position.
 * The {@link IncrementalRedaction}s of the piece share one history, and each builds its text from
 * it on request, so memory grows with the length of the input plus the number of replacements
 * rather than their product.
 */
public final class IncrementalRedactionHistory {

    private final String input;
    private final int[] starts;
    private final int[] ends;
    private final String[] replacements;

    /**
     * @param input The text the replacements were applied to.
     * @param spans The applied spans, in order of position and not overlapping. Their positions and
     *              replacements are copied, so later changes to the spans do not change the history.
     */
    public IncrementalRedactionHistory(final String input, final List<Span> spans) {

        this.input = input;
        this.starts = new int[spans.size()];
        this.ends = new int[spans.size()];
        this.replacements = new String[spans.size()];

        for(int i = 0; i < spans.size(); i++) {

            final Span span = spans.get(i);

            if(i > 0 && span.getCharacterStart() < ends[i - 1]) {
                throw new IllegalArgumentException("Spans must be in order of position and must not overlap.");
            }

            starts[i] = span.getCharacterStart();
            ends[i] = span.getCharacterEnd();
            replacements[i] = span.getReplacement();

        }

    }

    /** The number of replacements in the history. */
    public int size() {
        return replacements.length;
    }

    /**
     * Returns the text after the replacements up to and including the given step, followed by the
     * rest of the input.
     *
     * @param step The index of a replacement, from 0 to {@link #size()} - 1.
     * @return The incrementally redacted text at that step.
     */
    public String textAfter(final int step) {

        if(step < 0 || step >= replacements.length) {
            throw new IndexOutOfBoundsException("No replacement at step " + step);
        }

        final StringBuilder sb = new StringBuilder(input.length());
        int copiedTo = 0;

        for(int i = 0; i <= step; i++) {
            sb.append(input, copiedTo, starts[i]).append(replacements[i]);
            copiedTo = ends[i];
        }

        return sb.append(input, copiedTo, input.length()).toString();

    }

}
