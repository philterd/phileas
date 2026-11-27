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

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

/**
 * One labeled part of a document's redacted plain text, such as a PDF page.
 */
public final class TextSegment {

    /** Whether a segment has text, and why not. */
    public enum Status {

        /** The segment holds the part's redacted text. */
        @SerializedName("text")
        TEXT,

        /** The part has no extractable text, such as a scanned PDF page. A caller can treat it as unprocessed. */
        @SerializedName("noText")
        NO_TEXT,

        /** The policy leaves the part unscanned, so its text is left out rather than returned unredacted. */
        @SerializedName("notScanned")
        NOT_SCANNED

    }

    private final String label;
    private final String text;
    private final Status status;

    public TextSegment(final String label, final String text, final Status status) {
        this.label = Objects.requireNonNull(label);
        this.text = status == Status.TEXT ? Objects.requireNonNull(text) : "";
        this.status = Objects.requireNonNull(status);
    }

    /**
     * The segment's label, such as {@code page 1} or {@code metadata}.
     * @return The label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * The redacted text. Empty unless the status is {@link Status#TEXT}.
     * @return The text.
     */
    public String getText() {
        return text;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TextSegment other)) {
            return false;
        }
        return label.equals(other.label) && text.equals(other.text) && status == other.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, text, status);
    }

    @Override
    public String toString() {
        // The label and status only, so the text does not reach a log.
        return label + " (" + status + ")";
    }

}
