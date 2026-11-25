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

/**
 * Where a replacement was put in redacted text: from {@code start} (inclusive) to {@code end} (exclusive).
 * @param start The start offset.
 * @param end The end offset.
 */
public record ReplacedRange(int start, int end) {

    /**
     * This range moved by an offset.
     * @param offset The offset to add.
     * @return The moved range.
     */
    public ReplacedRange shift(final int offset) {
        return new ReplacedRange(start + offset, end + offset);
    }

    /**
     * Whether a span of text lies wholly in this range.
     * @param spanStart The span's start.
     * @param spanEnd The span's end.
     * @return <code>true</code> if the span is inside this range.
     */
    public boolean contains(final int spanStart, final int spanEnd) {
        return spanStart >= start && spanEnd <= end;
    }

}
