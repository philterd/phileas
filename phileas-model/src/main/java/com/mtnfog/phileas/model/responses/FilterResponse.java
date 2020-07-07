package com.mtnfog.phileas.model.responses;

import com.google.gson.Gson;
import com.mtnfog.phileas.model.objects.Explanation;
import com.mtnfog.phileas.model.objects.Span;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Response to a filter operation.
 */
public final class FilterResponse {
	
	private final String filteredText;
	private final String context;
    private final String documentId;
    private final Explanation explanation;

    /**
     * Creates a new response.
     * @param filteredText The filtered text.
     * @param context The context.
     * @param documentId The document ID.
     * @param explanation A {@link Explanation}.
     */
    public FilterResponse(String filteredText, String context, String documentId, Explanation explanation) {

        this.filteredText = filteredText;
        this.context = context;
        this.documentId = documentId;
        this.explanation = explanation;

    }

    /**
     * Creates a new response without an explanation.
     * @param filteredText The filtered text.
     * @param context The context.
     * @param documentId The document ID.
     */
    public FilterResponse(String filteredText, String context, String documentId) {

        this.filteredText = filteredText;
        this.context = context;
        this.documentId = documentId;
        this.explanation = null;

    }

    public static FilterResponse combine(final List<FilterResponse> filterResponses, String context, String documentId) {

        // Combine the results into a single filterResponse object.

        final StringBuilder filteredText = new StringBuilder();
        final List<Span> appliedSpans = new LinkedList<>();
        final List<Span> identifiedSpans = new LinkedList<>();

        for(final FilterResponse filterResponse : filterResponses) {

            // Append the filtered text.
            filteredText.append(filterResponse.getFilteredText());

            // TODO: Adjust the character offsets when combining.
            appliedSpans.addAll(filterResponse.getExplanation().getAppliedSpans());
            identifiedSpans.addAll(filterResponse.getExplanation().getIdentifiedSpans());

        }

        return new FilterResponse(filteredText.toString(), context, documentId, new Explanation(appliedSpans, identifiedSpans));

    }

    @Override
    public int hashCode() {

        return new HashCodeBuilder(17, 37).
                append(filteredText).
                append(context).
                append(documentId).
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

    public String getContext() {
        return context;
    }

    public Explanation getExplanation() {
        return explanation;
    }

}
