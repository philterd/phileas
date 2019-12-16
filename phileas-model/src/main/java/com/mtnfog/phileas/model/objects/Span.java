package com.mtnfog.phileas.model.objects;

import com.google.gson.annotations.Expose;
import com.mtnfog.phileas.model.enums.FilterType;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Represents a location in text identified as PII or PHI.
 */
public final class Span implements Serializable {

    // This is not @Expose'd because the user does not need to see it.
    // It is not transient because it won't get persisted to Mongo.
    private String id;

    @Expose
    private int characterStart;

    @Expose
    private int characterEnd;

    @Expose
    private FilterType filterType;

    @Expose
    private String context;

    @Expose
    private String documentId;

    @Expose
    private double confidence;

    @Expose
    private String text;

    @Expose
    private String replacement;

    @Expose
    private boolean ignored;

    // Encapsulates the characterStart and characterEnd for easy intersection functions.
    private transient Range<Integer> range;

    /**
     * Creates a new span. Use the static <code>make</code> function to create a new {@link Span}.
     * @param characterStart The character-based index of the start of the span.
     * @param characterEnd The character-based index of the end of the span.
     * @param filterType The {@link FilterType type} of the span.
     * @param context The context.
     * @param documentId The document ID.
     * @param confidence The confidence.
     * @param text The text identified by the span.
     * @param replacement The replacement (anonymized) value for the span.
     */
    private Span(int characterStart, int characterEnd, FilterType filterType, String context, String documentId, double confidence, String text, String replacement, boolean ignored) {

        this.id = UUID.randomUUID().toString();
        this.characterStart = characterStart;
        this.characterEnd = characterEnd;
        this.filterType = filterType;
        this.context = context;
        this.documentId = documentId;
        this.confidence = confidence;
        this.text = text;
        this.replacement = replacement;
        this.ignored = ignored;

    }

    /**
     * Creates a new span.
     * @param characterStart The character-based index of the start of the span.
     * @param characterEnd The character-based index of the end of the span.
     * @param filterType The {@link FilterType type} of the span.
     * @param context The context.
     * @param documentId The document ID.
     * @param confidence The confidence.
     * @param text The text identified by the span.
     * @param replacement The replacement (anonymized) value for the span.
     * @return A {@link Span} object with the given properties.
     */
    public static Span make(int characterStart, int characterEnd, FilterType filterType, String context,
                            String documentId, double confidence, String text, String replacement, boolean ignored) {

        final Span span = new Span(characterStart, characterEnd, filterType, context, documentId, confidence, text, replacement, ignored);

        // This is made here and not passed into the constructor because that would be redundant
        // given the characterStart and characterEnd parameters in the constructor.
        span.range = Range.between(characterStart, characterEnd);

        return span;

    }

    /**
     * Creates a new span. Use the static <code>make</code> instead of this constructor.
     */
    public Span() {
        // Used by MongoDB when reading objects from the database.
    }

    /**
     * Makes a copy of this span.
     * @return A copy of the span.
     */
    public Span copy() {

        final Span clone = Span.make(characterStart, characterEnd, filterType, context, documentId, confidence, text, replacement, ignored);

        clone.id = id;
        clone.range = range;

        return clone;

    }

    /**
     * Gets the span window for a token.
     * @param tokens The array of tokens.
     * @param tokenIndex The index of the token to make the window around.
     * @param windowSize The size of the window to make.
     * @return An array of tokens that is the window.
     */
    public static String[] getSpanWindow(String[] tokens, int tokenIndex, int windowSize) {

        // windowSize has to be odd so we have the same number of tokens on each side in the window.
        if(windowSize % 2 == 0) {
            throw new IllegalArgumentException("The windowSize must be odd.");
        }

        final String[] window = new String[windowSize];

        final int windowStart = tokenIndex - ((windowSize - 1 ) / 2);
        final int windowEnd = tokenIndex + ((windowSize - 1) / 2);

        final List<Integer> indexes = IntStream.rangeClosed(windowStart, windowEnd).boxed().collect(Collectors.toList());

        for(int i = 0; i < windowSize; i++) {

            String token;

            if(indexes.get(i) < 0) {
                token = "_";
            } else if (indexes.get(i) >= tokens.length) {
                token = "_";
            } else {
                token = tokens[windowStart + i];
            }

            window[i] = token;

        }

        return window;

    }

    /**
     * Shift spans a given distance.
     * @param shift The distance to shift the spans.
     * @param ignoreSpan The {@link Span span} to ignore.
     * @param spans The list of {@link Span spans}.
     * @return A list of {@link Span spans} shifted per the input parameters.
     */
    public static List<Span> shiftSpans(int shift, Span ignoreSpan, List<Span> spans) {

        final List<Span> shiftedSpans = new LinkedList<>();

        for(Span span : spans) {

            if(span != ignoreSpan) {

                final int start = span.getCharacterStart() + shift;
                final int end = span.getCharacterEnd() + shift;

                shiftedSpans.add(Span.make(start, end, span.filterType, span.context, span.documentId, span.confidence, span.text, span.replacement, span.ignored));

            }

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

    /**
     * Gets the text the span covers.
     * @param text The text.
     * @return The text the span covers.
     */
    public String getText(String text) {

        return text.substring(characterStart, characterEnd);

    }

    /**
     * Drop overlapping spans that were for text that was ignored.
     * @param spans A list of {@link Span spans} that may or may not contain ignored spans.
     * @return A list of {@link Span spans} without ignored spans.
     */
    public static List<Span> dropIgnoredSpans(List<Span> spans) {

        final List<Span> nonIgnoredSpans = new LinkedList<>();

        for(final Span span : spans) {

            if(!span.isIgnored()) {
                nonIgnoredSpans.add(span);
            }

        }

        return nonIgnoredSpans;

    }

    /**
     * Drop overlapping spans that are shorter.
     * @param spans A list of {@link Span spans} that may or may not contain overlapping spans.
     * @return A list of {@link Span spans} without overlapping spans.
     */
    public static List<Span> dropOverlappingSpans(List<Span> spans) {

        final List<Span> nonOverlappingSpans = new LinkedList<>();

        for(final Span span : spans) {

            boolean overlapping = false;

            for(final Span span2 : spans) {

                //LOGGER.info("{} - {}", span.getCharacterStart(), span2.getCharacterStart());
                //LOGGER.info("{} - {}", span2.getCharacterStart(), span2.getCharacterStart());

                if(span.range.isOverlappedBy(span2.range)) {

                    final int spanLength = span.getCharacterEnd() - span.getCharacterStart();
                    final int span2Length = span2.getCharacterEnd() - span2.getCharacterStart();

                    if((span2Length > spanLength) || (span2Length == spanLength && span2.confidence > span.confidence)) {

                        overlapping = true;
                        nonOverlappingSpans.add(span2);

                    }

                }

            }

            if(!overlapping) {
                nonOverlappingSpans.add(span);
            }

        }

        // If there are two spans in the list that have the same character start the one(s)
        // appear later in the list should be removed. If they have the same start they will
        // have to have the same end.
        final HashSet<Integer> seen = new HashSet<>();
        nonOverlappingSpans.removeIf(e -> !seen.add(e.getCharacterStart()));

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
                append(documentId).
                append(text).
                append(replacement).
                append(ignored).
                append(id).
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
                + " documentId: " + documentId + "; "
                + " confidence: " + confidence + "; "
                + " text: " + text + "; "
                + " replacement: " + replacement + "; "
                + " ignored: " + ignored + "; "
                + " id: " + id;

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

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

}
