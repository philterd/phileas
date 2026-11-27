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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Something the leftover check still detected in the redacted output: its type, its position in the output,
 * and the confidence of the detection. The matched text is held only when the caller asked for it, and is
 * never serialized.
 */
public final class LeftoverFinding {

    private final FilterType filterType;
    private final String classification;
    private final int characterStart;
    private final int characterEnd;
    private final double confidence;
    private final int pageNumber;
    private final int lineNumber;
    private final int paragraphNumber;

    // For an email, the attachment the finding is in; otherwise null.
    private final String attachment;

    // For a CSV document, the field's row and column, counting from 1; otherwise null, and left out of JSON.
    private final Integer row;
    private final Integer column;

    // For a check of text output, the label of the segment the finding is in; otherwise null, and left out of JSON.
    private final String segment;

    // In-process only, and only when asked for.
    private final transient String matchedText;

    /**
     * Creates a finding.
     * @param filterType The type of the detection.
     * @param classification The classification of the detection, such as a custom identifier's.
     * @param characterStart The start of the finding in the output.
     * @param characterEnd The end of the finding in the output.
     * @param confidence The confidence of the detection.
     * @param pageNumber For a PDF, the page, counting from 0; otherwise 0.
     * @param lineNumber For a PDF, the line, counting from 1; otherwise 0.
     * @param paragraphNumber For a Word or Excel document, the paragraph or cell, or -1 for other content; for an
     *                        email, the field; otherwise 0.
     * @param matchedText The matched text, or {@code null} when it was not asked for.
     */
    public LeftoverFinding(final FilterType filterType, final String classification, final int characterStart,
                           final int characterEnd, final double confidence, final int pageNumber,
                           final int lineNumber, final int paragraphNumber, final String matchedText) {
        this(filterType, classification, characterStart, characterEnd, confidence, pageNumber, lineNumber,
                paragraphNumber, null, matchedText);
    }

    /**
     * Creates a finding in an email attachment.
     * @param filterType The type of the detection.
     * @param classification The classification of the detection, such as a custom identifier's.
     * @param characterStart The start of the finding.
     * @param characterEnd The end of the finding.
     * @param confidence The confidence of the detection.
     * @param pageNumber For a PDF, the page, counting from 0; otherwise 0.
     * @param lineNumber For a PDF, the line, counting from 1; otherwise 0.
     * @param paragraphNumber For a Word or Excel document, the paragraph or cell; for an email, the field.
     * @param attachment The filename of the email attachment the finding is in, or {@code null}.
     * @param matchedText The matched text, or {@code null} when it was not asked for.
     */
    public LeftoverFinding(final FilterType filterType, final String classification, final int characterStart,
                           final int characterEnd, final double confidence, final int pageNumber,
                           final int lineNumber, final int paragraphNumber, final String attachment,
                           final String matchedText) {
        this.filterType = filterType;
        this.classification = classification;
        this.characterStart = characterStart;
        this.characterEnd = characterEnd;
        this.confidence = confidence;
        this.pageNumber = pageNumber;
        this.lineNumber = lineNumber;
        this.paragraphNumber = paragraphNumber;
        this.attachment = attachment;
        this.row = null;
        this.column = null;
        this.segment = null;
        this.matchedText = matchedText;
    }

    private LeftoverFinding(final LeftoverFinding finding, final String attachment, final Integer row, final Integer column,
                            final String segment) {
        this.filterType = finding.filterType;
        this.classification = finding.classification;
        this.characterStart = finding.characterStart;
        this.characterEnd = finding.characterEnd;
        this.confidence = finding.confidence;
        this.pageNumber = finding.pageNumber;
        this.lineNumber = finding.lineNumber;
        this.paragraphNumber = finding.paragraphNumber;
        this.attachment = attachment;
        this.row = row;
        this.column = column;
        this.segment = segment;
        this.matchedText = finding.matchedText;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public String getClassification() {
        return classification;
    }

    /**
     * The start of the finding. In text, an offset into the filtered text. In a PDF, an offset into the
     * output's text with its lines joined. In a Word or Excel document, an offset into the paragraph or cell.
     * @return The start offset.
     */
    public int getCharacterStart() {
        return characterStart;
    }

    public int getCharacterEnd() {
        return characterEnd;
    }

    public double getConfidence() {
        return confidence;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getParagraphNumber() {
        return paragraphNumber;
    }

    /**
     * For a finding in an email attachment, the attachment's filename. The finding's position is then in the
     * attachment, as for a document of its type.
     * @return The attachment's filename, or {@code null} when the finding is not in an attachment.
     */
    public String getAttachment() {
        return attachment;
    }

    /**
     * This finding, placed in an email attachment.
     * @param attachment The attachment's filename.
     * @return A copy of this finding with the attachment set.
     */
    public LeftoverFinding inAttachment(final String attachment) {
        return new LeftoverFinding(this, attachment, row, column, segment);
    }

    /**
     * This finding, placed in a field of a CSV document. Its offsets are then into the field's value.
     * @param row The field's row, counting from 1.
     * @param column The field's column, counting from 1.
     * @return A copy of this finding with the row and column set.
     */
    public LeftoverFinding inField(final int row, final int column) {
        return new LeftoverFinding(this, attachment, row, column, segment);
    }

    /**
     * For a CSV document, the row of the field the finding is in, counting from 1.
     * @return The row, or {@code null} when the finding is not in a CSV document.
     */
    /**
     * This finding, placed in a segment of text output.
     * @param segment The segment's label.
     * @return A copy of this finding with the segment set.
     */
    public LeftoverFinding inSegment(final String segment) {
        return new LeftoverFinding(this, attachment, row, column, segment);
    }

    /**
     * For a check of text output, the label of the segment the finding is in. The finding's offsets are then
     * into that segment's text.
     * @return The segment's label, or {@code null} when the check was of a document.
     */
    public String getSegment() {
        return segment;
    }

    public Integer getRow() {
        return row;
    }

    /**
     * For a CSV document, the column of the field the finding is in, counting from 1.
     * @return The column, or {@code null} when the finding is not in a CSV document.
     */
    public Integer getColumn() {
        return column;
    }

    /**
     * The text that was detected, only when the caller asked for it with
     * {@code FilterOptions.withLeftoverCheckMatchedText}. It is never part of the serialized result.
     * @return The matched text, or {@code null}.
     */
    public String getMatchedText() {
        return matchedText;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

}
