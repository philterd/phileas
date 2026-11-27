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
 * Something about the policy a caller should know about the redaction of one document, such as a column rule that
 * did nothing. A warning holds only values taken from the policy, never document content.
 */
public final class RedactionWarning {

    /** What a warning is about. */
    public enum Code {

        /** A column rule's {@code header} matches no column of a worksheet it applies to. */
        @SerializedName("unmatched-header")
        UNMATCHED_HEADER

    }

    private final Code code;
    private final String header;
    private final String worksheet;
    private final boolean embedded;

    private RedactionWarning(final Code code, final String header, final String worksheet, final boolean embedded) {
        this.code = Objects.requireNonNull(code);
        this.header = header;
        this.worksheet = worksheet;
        this.embedded = embedded;
    }

    /**
     * A column rule whose header matches no column.
     * @param header The rule's {@code header}.
     * @param worksheet The sheet entry's {@code worksheet}, or {@code null} when the entry names none.
     * @return The warning.
     */
    public static RedactionWarning unmatchedHeader(final String header, final String worksheet) {
        return new RedactionWarning(Code.UNMATCHED_HEADER, Objects.requireNonNull(header), worksheet, false);
    }

    /**
     * This warning, for a workbook embedded in the document being redacted.
     * @return A copy marked as embedded.
     */
    public RedactionWarning asEmbedded() {
        return new RedactionWarning(code, header, worksheet, true);
    }

    public Code getCode() {
        return code;
    }

    /**
     * The column rule's {@code header}, from the policy.
     * @return The header.
     */
    public String getHeader() {
        return header;
    }

    /**
     * The sheet entry's {@code worksheet}, from the policy.
     * @return The worksheet, or {@code null} when the entry names none.
     */
    public String getWorksheet() {
        return worksheet;
    }

    /**
     * Whether the warning is about a workbook embedded in the document, such as one in a Word document or behind a
     * chart, rather than the document itself.
     * @return {@code true} for an embedded workbook.
     */
    public boolean isEmbedded() {
        return embedded;
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof RedactionWarning other && code == other.code && Objects.equals(header, other.header)
                && Objects.equals(worksheet, other.worksheet) && embedded == other.embedded;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, header, worksheet, embedded);
    }

    /** A message for a log, from the policy's values only. */
    @Override
    public String toString() {
        return "The column rule for header '" + header + "' matches no column"
                + (worksheet == null ? "" : " in worksheet '" + worksheet + "'")
                + (embedded ? " of an embedded workbook." : ".");
    }

}
