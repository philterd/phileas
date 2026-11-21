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

public class IncrementalRedaction {

    private final String hash;
    private final Span span;

    // Set by the constructor that is given the text. Otherwise the text is built from the shared
    // history on request, so a document's redactions do not each hold a copy of it.
    private final String incrementallyRedactedText;
    private final IncrementalRedactionHistory history;
    private final int step;

    public IncrementalRedaction(final String hash, final Span span, final String incrementallyRedactedText) {
        this.hash = hash;
        this.span = span;
        this.incrementallyRedactedText = incrementallyRedactedText;
        this.history = null;
        this.step = -1;
    }

    /**
     * Creates an incremental redaction whose text is built from <code>history</code> when {@link
     * #getIncrementallyRedactedText()} is called.
     *
     * @param hash The SHA-256 hash of the incrementally redacted text.
     * @param span The span applied at this step.
     * @param history The input and replacements of the filtered piece.
     * @param step The index of this span's replacement in <code>history</code>.
     */
    public IncrementalRedaction(final String hash, final Span span, final IncrementalRedactionHistory history, final int step) {
        if(step < 0 || step >= history.size()) {
            throw new IndexOutOfBoundsException("No replacement at step " + step);
        }
        this.hash = hash;
        this.span = span;
        this.incrementallyRedactedText = null;
        this.history = history;
        this.step = step;
    }

    @Override
    public String toString() {
        return "IncrementalRedaction{" +
                "hash='" + hash + '\'' +
                ", span=" + span +
                ", incrementallyRedactedText='" + getIncrementallyRedactedText() + '\'' +
                '}';
    }

    public String getHash() {
        return hash;
    }

    public Span getSpan() {
        return span;
    }

    /**
     * Returns the text after this span's replacement and the replacements before it. Built on each call
     * when the redaction was created from a history.
     */
    public String getIncrementallyRedactedText() {
        return history == null ? incrementallyRedactedText : history.textAfter(step);
    }

}
