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
package ai.philterd.phileas.model.filtering;

/**
 * Where a cue word that a value was found after lies in the text, by character offsets.
 * @param start The offset of the cue word's first character.
 * @param end The offset after the cue word's last character, including punctuation that follows it.
 */
public record Cue(int start, int end) {

    /**
     * Whether this cue word is nearer a value after both than another: it ends later, or, ending at the same
     * place, it is the longer phrase.
     * @param other The other cue word.
     * @return {@code true} if this cue word is nearer.
     */
    public boolean isNearerThan(final Cue other) {
        return end > other.end || (end == other.end && start < other.start);
    }

    /**
     * This cue word moved a given distance, as its span is when its offsets are made relative to other text.
     * @param shift The distance.
     * @return The moved cue word.
     */
    public Cue shift(final int shift) {
        return new Cue(start + shift, end + shift);
    }

}
