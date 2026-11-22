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
package ai.philterd.phileas.services.ooxml;

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Span;

import java.util.ArrayList;
import java.util.List;

/**
 * One redaction made in a Word or Excel document, as phileas-dotnet's {@code OfficeRedactionSpan}.
 *
 * <p>{@code paragraphIndex} is the Word paragraph or Excel cell ordinal in the canonical order that
 * {@code applySpans} re-applies by. It is -1 for content that is not a body paragraph or cell (drawing
 * and chart text, comments in Excel, headers and footers in Excel, hyperlink targets, field instructions,
 * tracked deletions, pivot caches, and embedded objects), which is re-redacted with the policy instead.
 */
public final class OfficeRedactionSpan {

    private int order;
    private int paragraphIndex = -1;
    private int characterStart;
    private int characterEnd;
    private String text = "";
    private String replacement = "";
    private String classification = "";
    private String filterType = "";
    private double confidence;
    private String pattern = "";
    private List<String> window = new ArrayList<>();
    private transient Span source;

    public OfficeRedactionSpan() {
    }

    public OfficeRedactionSpan(final int order, final int paragraphIndex, final int characterStart, final int characterEnd,
                               final String text, final String replacement, final String classification) {
        this.order = order;
        this.paragraphIndex = paragraphIndex;
        this.characterStart = characterStart;
        this.characterEnd = characterEnd;
        this.text = text == null ? "" : text;
        this.replacement = replacement == null ? "" : replacement;
        this.classification = classification == null ? "" : classification;
    }

    /** Copies the engine's explanation detail from the span that produced this redaction. */
    void explain(final Span span) {
        this.source = span;
        this.filterType = span.getFilterType() == null ? "" : span.getFilterType().toString();
        this.confidence = span.getConfidence();
        this.pattern = span.getPattern() == null ? "" : span.getPattern();
        this.window = span.getWindow() == null ? new ArrayList<>() : new ArrayList<>(List.of(span.getWindow()));
    }

    /**
     * The redaction as a Phileas {@link Span}: offsets relative to the paragraph or cell, and
     * {@link Span#getParagraphNumber()} set to {@link #getParagraphIndex()}. A redaction no detection
     * produced, such as a fully redacted Excel column, has filter type {@link FilterType#OTHER}. Null for a
     * marker, such as a removed embedded object, which redacts no text.
     */
    public Span toSpan() {
        if (source == null) {
            if (paragraphIndex < 0 || characterEnd <= characterStart) {
                return null;
            }
            final Span span = Span.make(characterStart, characterEnd, FilterType.OTHER, "", 1.0, text, replacement,
                    "", false, true, new String[0], 0);
            span.setClassification(classification);
            span.setParagraphNumber(paragraphIndex);
            return span;
        }
        final Span span = source.copy();
        span.setCharacterStart(characterStart);
        span.setCharacterEnd(characterEnd);
        span.setText(text);
        span.setReplacement(replacement);
        span.setClassification(classification);
        span.setParagraphNumber(paragraphIndex);
        span.setPattern(source.getPattern());
        span.setWindow(source.getWindow());
        return span;
    }

    /** The span to re-apply for a Phileas {@link Span}, as {@link #toSpan()} produces. */
    public static OfficeRedactionSpan fromSpan(final Span span) {
        return new OfficeRedactionSpan(0, span.getParagraphNumber(), span.getCharacterStart(), span.getCharacterEnd(),
                span.getText(), span.getReplacement(), span.getClassification());
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public int getParagraphIndex() {
        return paragraphIndex;
    }

    public void setParagraphIndex(final int paragraphIndex) {
        this.paragraphIndex = paragraphIndex;
    }

    public int getCharacterStart() {
        return characterStart;
    }

    public void setCharacterStart(final int characterStart) {
        this.characterStart = characterStart;
    }

    public int getCharacterEnd() {
        return characterEnd;
    }

    public void setCharacterEnd(final int characterEnd) {
        this.characterEnd = characterEnd;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(final String replacement) {
        this.replacement = replacement;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(final String classification) {
        this.classification = classification;
    }

    public String getFilterType() {
        return filterType;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getPattern() {
        return pattern;
    }

    public List<String> getWindow() {
        return window;
    }

    @Override
    public String toString() {
        return "OfficeRedactionSpan{order=" + order + ", paragraphIndex=" + paragraphIndex + ", characterStart="
                + characterStart + ", characterEnd=" + characterEnd + ", text='" + text + "', replacement='"
                + replacement + "', classification='" + classification + "'}";
    }

}
