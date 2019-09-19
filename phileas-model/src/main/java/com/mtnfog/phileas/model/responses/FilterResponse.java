package com.mtnfog.phileas.model.responses;

import com.google.gson.Gson;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

/**
 * Response to a filter operation.
 */
public final class FilterResponse implements Serializable {
	
	private final String filteredText;
	private final String context;
    private final String documentId;

    /**
     * Creates a new response.
     * @param filteredText The filtered text.
     * @param context The context.
     * @param documentId The document ID.
     */
    public FilterResponse(String filteredText, String context, String documentId) {

        this.filteredText = filteredText;
        this.context = context;
        this.documentId = documentId;

    }

    @Override
    public int hashCode() {

        return new HashCodeBuilder(17, 37).
                append(filteredText).
                append(context).
                append(documentId).
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

}
