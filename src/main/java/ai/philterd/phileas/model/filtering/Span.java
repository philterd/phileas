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

import ai.philterd.phileas.model.formats.lapps.Annotation;
import ai.philterd.phileas.model.formats.lapps.Lapps;
import ai.philterd.phileas.model.formats.lapps.View;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Represents a location in text identified as sensitive information.
 */
public final class Span {

    @Expose
    private int characterStart;

    @Expose
    private int characterEnd;

    @Expose
    private FilterType filterType;

    @Expose
    private String context;

    @Expose
    private String classification;

    @Expose
    private double confidence;

    @Expose
    private String text;

    @Expose
    private String replacement;

    @Expose
    private String salt;

    @Expose
    private boolean ignored;

    @Expose
    private boolean applied;

    // The priority of the filter that identified this span.
    @Expose
    private int priority;

    // Encapsulates the characterStart and characterEnd for easy intersection functions.
    private transient Range<Integer> range;

    // The textual (non-compiled) regex expression, if any, used to identify the span.
    // This is used to validate a regex after finding.
    private transient String pattern;

    // The window of tokens around the span.
    private transient String[] window;

    // Whether the span should always pass validation.
    private transient boolean alwaysValid = false;

    private int lineNumber = 0;
    private int pageNumber = 0;
    private float lowerLeftX;
    private float lowerLeftY;
    private float upperRightX;
    private float upperRightY;

    private transient String lineHash;

    /**
     * Don't use this constructor.
     * Use <code>span.make()</code> instead.
     */
    public Span() {

    }

    /**
     * Creates a new span. Use the static <code>make</code> function to create a new {@link Span}.
     * @param characterStart The character-based index of the start of the span.
     * @param characterEnd The character-based index of the end of the span.
     * @param filterType The {@link FilterType type} of the span.
     * @param context The context.
     * @param confidence The confidence.
     * @param text The text identified by the span.
     * @param replacement The replacement (anonymized) value for the span.
     * @param ignored Whether the span was ignored.
     * @param applied Whether the span was applied.
     * @param window The tokens surrounding the span.
     */
    private Span(int characterStart, int characterEnd, FilterType filterType, String context,
                 double confidence, String text, String replacement, String salt, boolean ignored, boolean applied,
                 String[] window, int priority) {

        this.characterStart = characterStart;
        this.characterEnd = characterEnd;
        this.filterType = filterType;
        this.context = context;
        this.confidence = confidence;
        this.text = text;
        this.replacement = replacement;
        this.salt = salt;
        this.ignored = ignored;
        this.applied = applied;
        this.window = window;
        this.priority = priority;

    }

    /**
     * Creates a new span.
     * @param characterStart The character-based index of the start of the span.
     * @param characterEnd The character-based index of the end of the span.
     * @param filterType The {@link FilterType type} of the span.
     * @param context The context.
     * @param confidence The confidence.
     * @param text The text identified by the span.
     * @param replacement The replacement (anonymized) value for the span.
     * @param ignored Whether the found span is ultimately ignored.
     * @param priority The priority of the filter that identified this span.
     * @return A {@link Span} object with the given properties.
     */
    public static Span make(int characterStart, int characterEnd, FilterType filterType, String context,
                            double confidence, String text, String replacement, String salt,
                            boolean ignored, boolean applied, String[] window, int priority) {

        final Span span = new Span(characterStart, characterEnd, filterType, context, confidence, text,
                replacement, salt, ignored, applied, window, priority);

        // This is made here and not passed into the constructor because that would be redundant
        // given the characterStart and characterEnd parameters in the constructor.
        span.range = Range.of(characterStart, characterEnd);

        return span;

    }

    /**
     * Makes a copy of this span.
     * @return A copy of the span.
     */
    public Span copy() {

        final Span clone = Span.make(characterStart, characterEnd, filterType, context, confidence, text,
                replacement, salt, ignored, applied, window, priority);

        clone.range = range;

        return clone;

    }

    /**
     * Shift spans a given distance.
     * @param shift The distance to shift the spans.
     * @param ignoreSpan The {@link Span span} to ignore.
     * @param spans The list of {@link Span spans}.
     * @return A list of {@link Span spans} shifted per the input parameters.
     */
    public static List<Span> shiftSpans(final int shift, final Span ignoreSpan, final List<Span> spans) {

        final List<Span> shiftedSpans = new LinkedList<>();

        for(final Span span : spans) {

            if(span != ignoreSpan) {

                final int start = span.getCharacterStart() + shift;
                final int end = span.getCharacterEnd() + shift;

                shiftedSpans.add(Span.make(start, end, span.filterType, span.context, span.confidence,
                        span.text, span.replacement, span.salt, span.ignored, span.applied, span.window, span.priority));

            }

        }

        return shiftedSpans;

    }

    /**
     * Shift spans a given distance.
     * @param shift The distance to shift the spans.
     * @param spans The list of {@link Span spans}.
     * @return A list of {@link Span spans} shifted per the input parameters.
     */
    public static List<Span> shiftSpans(int shift, List<Span> spans) {

        final List<Span> shiftedSpans = new LinkedList<>();

        for(final Span span : spans) {

                final int start = span.getCharacterStart() + shift;
                final int end = span.getCharacterEnd() + shift;

                shiftedSpans.add(Span.make(start, end, span.filterType, span.context, span.confidence,
                        span.text, span.replacement, span.salt, span.ignored, span.applied, span.window, span.priority));

        }

        return shiftedSpans;

    }

    /**
     * Determine if an index is in a span.
     * @param index The index.
     * @param spans A list of {@link Span spans} to check.
     * @return The span encasing the index if found. Otherwise, <code>null</code>.
     */
    public static Span doesIndexStartSpan(final int index, final List<Span> spans) {

        for(final Span span : spans) {

            if(index == span.getCharacterStart()) {
                return span;
            }

        }

        return null;

    }

    public static boolean doesSpanExist(final int startIndex, final int endIndex, final List<Span> spans) {

        for(final Span span : spans) {

            if(startIndex == span.getCharacterStart() && endIndex == span.getCharacterEnd()) {
                return true;
            }

        }

        return false;

    }

    /**
     * Gets the text the span covers.
     * @param text The text.
     * @return The text the span covers.
     */
    public String getText(String text) {

        return text.substring(characterStart, characterEnd);

    }

    /**
     * Gets the identical spans that differ only by their filter type.
     * @param spans A list of {@link Span spans}.
     * @return A list of the identical {@link Span spans} from the input list.
     */
    public static List<Span> getIdenticalSpans(Span span, List<Span> spans) {

        final Set<Span> identicalSpans = new LinkedHashSet<>();

        for(final Span span1 : spans) {

            // Matching the character start and end indexes is sufficient.
            // If the confidence is not equal don't add it.
            // The span with the highest confidence will be used.
            if(span1.getCharacterStart() == span.getCharacterStart()
                    && span1.getCharacterEnd() == span.getCharacterEnd()
                    && span1.getFilterType() != span.getFilterType()
                    && span1.getConfidence() == span.getConfidence()
                    && span1.getLowerLeftX() == span.getLowerLeftX()
                    && span1.getLowerLeftY() == span.getLowerLeftY()
                    && span1.getUpperRightX() == span.getUpperRightX()
                    && span1.getUpperRightY() == span.getUpperRightY()
                    && !span1.equals(span)) {

                identicalSpans.add(span1);

            }

        }

        return new LinkedList<>(identicalSpans);

    }

    /**
     * Determines if two spans are immediately adjacent.
     * <code>span1</code> must occur in the test prior to <code>span2</code>.
     * @param span1 The first span.
     * @param span2 The second span.
     * @return <code>true</code> if the spans are immediately adjacent.
     */
    public static boolean areSpansAdjacent(final Span span1, final Span span2, final String text) {

        if(span1.getCharacterStart() > span2.getCharacterStart()) {
            return false;
        }

        if(span1.getCharacterEnd() == span1.getCharacterStart() + 1) {

            // The two spans are right next to each other.
            return true;

        }

        final String separators = text.substring(span1.getCharacterEnd(), span2.getCharacterStart() - 1);

        // The two spans are only separated by whitespace.
        return StringUtils.isWhitespace(separators) || StringUtils.equals(",", separators.trim());

    }

    /**
     * Create a list of spans from LAPPS JSON.
     * @param lappsJson LAPPS JSON.
     * @return A list of spans.
     */
    public static List<Span> fromLappsJson(String lappsJson) {

        final List<Span> spans = new LinkedList<>();

        final Gson gson = new Gson();
        final Lapps lapps = gson.fromJson(lappsJson, Lapps.class);

        for(final View view : lapps.getViews()) {

            for(final Annotation annotation : view.getAnnotations()) {

                if(StringUtils.equalsIgnoreCase(Lapps.NAMED_ENTITY, annotation.getType())) {

                    if(annotation.getFeatures() != null) {

                        final Span span = new Span();

                        span.setCharacterStart(annotation.getStart());
                        span.setCharacterEnd(annotation.getEnd());
                        span.setText(lapps.getText().getValue().substring(annotation.getStart(), annotation.getEnd()));

                        if(StringUtils.equalsIgnoreCase("PER", annotation.getFeatures().getCategory())) {

                            span.setFilterType(FilterType.PERSON);
                            span.setClassification("PER");

                        } else {

                            // The other types are defined in Inception.
                            // DATE EMAIL PER PHONE SSN STATE ...
                            // If I make their names in Inception make the FilterType I can just use those names.
                            span.setFilterType(FilterType.valueOf(annotation.getFeatures().getCategory()));

                        }

                        spans.add(span);

                    }

                }

            }

        }

        return spans;

    }

    /**
     * Drop overlapping spans that are shorter.
     * @param spans A list of {@link Span spans} that may or may not contain overlapping spans.
     * @return A list of {@link Span spans} without overlapping spans.
     */
    public static List<Span> dropOverlappingSpans(List<Span> spans) {

        List<Span> nonOverlappingSpans = new LinkedList<>();

        // Order spans by length and reverse so the spans
        // are sorted longest to shortest.
        spans.sort((s1, s2) -> (s1.length()));
        Collections.reverse(spans);

        // Loop over each span.
        for (final Span span : spans) {

            boolean overlapping = false;

            // Loop over each span.
            for (final Span span2 : spans) {

                // Ignore if the span is not the same.
                if (!span.equals(span2)) {

                    if (span.range.isOverlappedBy(span2.range)) {

                        final int spanLength = span.getCharacterEnd() - span.getCharacterStart();
                        final int span2Length = span2.getCharacterEnd() - span2.getCharacterStart();

                        if ((span2Length > spanLength)) {

                            overlapping = true;
                            nonOverlappingSpans.add(span2);

                        } else if (span2Length == spanLength && span2.confidence > span.confidence) {

                            overlapping = true;

                        } else if(span2Length == spanLength && span2.confidence == span.confidence) {

                            // Use the span from the filter with the highest priority.
                            if(span2.getPriority() > span.getPriority()) {
                                overlapping = true;
                                nonOverlappingSpans.add(span2);
                            }

                        }

                    }

                }

            }

            if (!overlapping) {
                nonOverlappingSpans.add(span);
            }

        }

        // If there are two spans in the list that have the same character start and
        // the same confidence, the one(s) appear later in the list will be kept.
        final HashSet<Integer> seen = new HashSet<>();
        nonOverlappingSpans.removeIf(e -> !seen.add(e.getCharacterStart()));

        // Keep calling this until the sizes of the input and output are equal.
        int removed = spans.size() - nonOverlappingSpans.size();
        if(removed > 0) {
            nonOverlappingSpans = dropOverlappingSpans(nonOverlappingSpans);
        }

        return nonOverlappingSpans;

    }

    @Override
    public int hashCode() {

        return new HashCodeBuilder(17, 37).
                append(characterStart).
                append(characterEnd).
                append(filterType).
                append(confidence).
                append(context).
                append(text).
                append(replacement).
                append(salt).
                append(ignored).
                append(applied).
                append(classification).
                append(priority).
                append(lowerLeftX).
                append(lowerLeftY).
                append(upperRightX).
                append(upperRightY).
                append(lineNumber).
                append(pageNumber).
                toHashCode();

    }

    @Override
    public boolean equals(Object o) {

        return EqualsBuilder.reflectionEquals(this, o);

    }

    @Override
    public String toString() {

        return "characterStart: " + characterStart + "; "
                + " characterEnd: " + characterEnd + "; "
                + " filterType: " + filterType.getType() + "; "
                + " context: " + context + "; "
                + " confidence: " + confidence + "; "
                + " text: " + text + "; "
                + " replacement: " + replacement + "; "
                + " salt: " + salt + "; "
                + " ignored: " + ignored + "; "
                + " applied: " + applied + "; "
                + " classification: " + classification + "; "
                + " priority: " + priority + "; "
                + " line number: " + lineNumber + ";"
                + " page number: " + pageNumber + ";"
                + " line hash: " + lineHash;

    }

    public int length() {
        return characterEnd - characterStart;
    }

    public int getCharacterStart() {
        return characterStart;
    }

    public void setCharacterStart(int characterStart) {
        this.characterStart = characterStart;
    }

    public int getCharacterEnd() {
        return characterEnd;
    }

    public void setCharacterEnd(int characterEnd) {
        this.characterEnd = characterEnd;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public void setFilterType(FilterType filterType) {
        this.filterType = filterType;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String[] getWindow() {
        return window;
    }

    public void setWindow(String[] window) {
        this.window = window;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public boolean isAlwaysValid() {
        return alwaysValid;
    }

    public void setAlwaysValid(boolean alwaysValid) {
        this.alwaysValid = alwaysValid;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getLineHash() {
        return lineHash;
    }

    public void setLineHash(String lineHash) {
        this.lineHash = lineHash;
    }

    public float getLowerLeftX() {
        return lowerLeftX;
    }

    public void setLowerLeftX(float lowerLeftX) {
        this.lowerLeftX = lowerLeftX;
    }

    public float getLowerLeftY() {
        return lowerLeftY;
    }

    public void setLowerLeftY(float lowerLeftY) {
        this.lowerLeftY = lowerLeftY;
    }

    public float getUpperRightX() {
        return upperRightX;
    }

    public void setUpperRightX(float upperRightX) {
        this.upperRightX = upperRightX;
    }

    public float getUpperRightY() {
        return upperRightY;
    }

    public void setUpperRightY(float upperRightY) {
        this.upperRightY = upperRightY;
    }

}
