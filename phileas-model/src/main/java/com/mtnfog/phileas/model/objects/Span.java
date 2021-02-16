package com.mtnfog.phileas.model.objects;

import com.google.gson.annotations.Expose;
import com.mtnfog.phileas.model.enums.FilterType;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Represents a location in text identified as sensitive information.
 */
public final class Span {

    private static final Logger LOGGER = LogManager.getLogger(Span.class);

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

    // Encapsulates the characterStart and characterEnd for easy intersection functions.
    private transient Range<Integer> range;

    // The textual (nonn-compiled) regex expression, if any, used to identify the span.
    // This is used to validate a regex after finding.
    private transient String pattern;

    // The window of tokens around the span.
    private transient String[] window;

    // Whether or not the span should always pass validation.
    private transient boolean alwaysValid = false;

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
     * @param ignored Whether or not the span was ignored.
     * @param window The tokens surrounding the span.
     */
    private Span(int characterStart, int characterEnd, FilterType filterType, String context, String documentId,
                 double confidence, String text, String replacement, String salt, boolean ignored, String[] window) {

        this.characterStart = characterStart;
        this.characterEnd = characterEnd;
        this.filterType = filterType;
        this.context = context;
        this.documentId = documentId;
        this.confidence = confidence;
        this.text = text;
        this.replacement = replacement;
        this.salt = salt;
        this.ignored = ignored;
        this.window = window;

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
     * @param ignored Whether or not the found span is ultimately ignored.
     * @return A {@link Span} object with the given properties.
     */
    public static Span make(int characterStart, int characterEnd, FilterType filterType, String context,
                            String documentId, double confidence, String text, String replacement, String salt, boolean ignored, String[] window) {

        final Span span = new Span(characterStart, characterEnd, filterType, context, documentId, confidence, text, replacement, salt, ignored, window);

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

        final Span clone = Span.make(characterStart, characterEnd, filterType, context, documentId, confidence, text, replacement, salt, ignored, window);

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
    public static List<Span> shiftSpans(int shift, Span ignoreSpan, List<Span> spans) {

        final List<Span> shiftedSpans = new LinkedList<>();

        for(final Span span : spans) {

            if(span != ignoreSpan) {

                final int start = span.getCharacterStart() + shift;
                final int end = span.getCharacterEnd() + shift;

                shiftedSpans.add(Span.make(start, end, span.filterType, span.context, span.documentId, span.confidence,
                        span.text, span.replacement, span.salt, span.ignored, span.window));

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

                shiftedSpans.add(Span.make(start, end, span.filterType, span.context, span.documentId, span.confidence,
                        span.text, span.replacement, span.salt, span.ignored, span.window));

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
     * Get all spans having the given {@link FilterType}.
     * @param spans A list of spans.
     * @param filterType The {@link FilterType}.
     * @return A list of spans having the given {@link FilterType}.
     */
    public static List<Span> getSpansOfFilterType(List<Span> spans, FilterType filterType) {

        return spans.stream().filter(c -> filterType == filterType).collect(Collectors.toList());

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
                    && !span1.equals(span)) {

                identicalSpans.add(span1);

            }

        }

        return new LinkedList<>(identicalSpans);

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

        if(StringUtils.isWhitespace(separators) || StringUtils.equals(",", separators.trim())) {

            // The two spans are only separated by whitespace.
            return true;

        }

        return false;

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

                // Ignore if the span is the same.
                if (!span.equals(span2)) {

                    //LOGGER.info("{} - {}", span.getCharacterStart(), span2.getCharacterStart());
                    //LOGGER.info("{} - {}", span2.getCharacterStart(), span2.getCharacterStart());

                    if (span.range.isOverlappedBy(span2.range)) {

                        //System.out.println("span is overlapped by span2");

                        // Get the span with the longest length.

                        final int spanLength = span.getCharacterEnd() - span.getCharacterStart();
                        final int span2Length = span2.getCharacterEnd() - span2.getCharacterStart();

                        if ((span2Length > spanLength) || (span2Length == spanLength && span2.confidence > span.confidence)) {

                            overlapping = true;
                            nonOverlappingSpans.add(span2);

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
                append(documentId).
                append(text).
                append(replacement).
                append(salt).
                append(ignored).
                append(classification).
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
                + " salt: " + salt + "; "
                + " ignored: " + ignored + "; "
                + " classification: " + classification + "; "
                ;

    }

    public String toCSV() {

        return
                characterStart + "," +
                characterEnd + "," +
                filterType + "," +
                context + "," +
                documentId + "," +
                confidence + "," +
                "\"" + text + "\"";

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

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
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
}
