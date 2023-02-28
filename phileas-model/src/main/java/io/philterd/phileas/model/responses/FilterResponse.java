package io.philterd.phileas.model.responses;

import com.google.gson.Gson;
import io.philterd.phileas.model.objects.Explanation;
import io.philterd.phileas.model.objects.Span;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response to a filter operation.
 */
public final class FilterResponse {
	
	private final String filteredText;
	private final String context;
    private final String documentId;
    private final int piece;
    private final Explanation explanation;

    /**
     * Creates a new response.
     * @param filteredText The filtered text.
     * @param context The context.
     * @param documentId The document ID.
     * @param explanation A {@link Explanation}.
     */
    public FilterResponse(String filteredText, String context, String documentId, int piece, Explanation explanation) {

        this.filteredText = filteredText;
        this.context = context;
        this.documentId = documentId;
        this.piece = piece;
        this.explanation = explanation;

    }

    /**
     * Combine multiple {@link FilterResponse} objects into a single {@link FilterResponse}.
     * The list of {@link FilterResponse} objects must be in order from first to last.
     * @param filterResponses A list of {@link FilterResponse} objects to combine.
     *                        Objects must be in order from first to last.
     * @param context The context for the returned {@link FilterResponse}.
     * @param documentId The document ID for the returned {@link FilterResponse}.
     * @return A single, combined {@link FilterResponse}.
     */
    public static FilterResponse combine(List<FilterResponse> filterResponses, String context, String documentId, String separator) {

        // Combine the results into a single filterResponse object.
        final StringBuilder filteredText = new StringBuilder();
        final List<Span> appliedSpans = new LinkedList<>();
        final List<Span> identifiedSpans = new LinkedList<>();

        // Order the filter responses by piece number, lowest to greatest.
        final List<FilterResponse> sortedFilterResponses = filterResponses.stream().sorted(Comparator.comparing(FilterResponse::getPiece)).collect(Collectors.toList());

        // Tracks the document offset for each piece so the span locations can be adjusted.
        int documentOffset = 0;

        // Loop over each filter response and build the combined filter response.
        for(final FilterResponse filterResponse : sortedFilterResponses) {

            // The text is the filtered text plus the separator.
            final String pieceFilteredText = filterResponse.getFilteredText() + separator;

            // Append the filtered text.
            filteredText.append(pieceFilteredText);

            // Adjust the character offsets when combining.
            appliedSpans.addAll(Span.shiftSpans(documentOffset, filterResponse.getExplanation().getAppliedSpans()));
            identifiedSpans.addAll(Span.shiftSpans(documentOffset, filterResponse.getExplanation().getIdentifiedSpans()));

            // Adjust the document offset.
            documentOffset += pieceFilteredText.length();

        }

        // Return the newly built FilterResponse.
        return new FilterResponse(filteredText.toString().trim(), context, documentId, 0, new Explanation(appliedSpans, identifiedSpans));

    }

    @Override
    public int hashCode() {

        return new HashCodeBuilder(17, 37).
                append(filteredText).
                append(context).
                append(documentId).
                append(piece).
                append(explanation).
                toHashCode();

    }

    @Override
    public String toString() {

        final Gson gson = new Gson();
        return gson.toJson(this);

    }

    @Override
    public boolean equals(Object o) {

        return EqualsBuilder.reflectionEquals(this, o);

    }

    public String getFilteredText() {
        return filteredText;
    }

    public String getDocumentId() {
        return documentId;
    }

    public int getPiece() {
        return piece;
    }

    public String getContext() {
        return context;
    }

    public Explanation getExplanation() {
        return explanation;
    }

}
