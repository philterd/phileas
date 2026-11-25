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
     * @param paragraphNumber For a Word or Excel document, the paragraph or cell, or -1 for other content; otherwise 0.
     * @param matchedText The matched text, or {@code null} when it was not asked for.
     */
    public LeftoverFinding(final FilterType filterType, final String classification, final int characterStart,
                           final int characterEnd, final double confidence, final int pageNumber,
                           final int lineNumber, final int paragraphNumber, final String matchedText) {
        this.filterType = filterType;
        this.classification = classification;
        this.characterStart = characterStart;
        this.characterEnd = characterEnd;
        this.confidence = confidence;
        this.pageNumber = pageNumber;
        this.lineNumber = lineNumber;
        this.paragraphNumber = paragraphNumber;
        this.matchedText = matchedText;
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
