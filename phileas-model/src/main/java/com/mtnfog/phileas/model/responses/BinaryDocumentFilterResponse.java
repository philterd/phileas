package com.mtnfog.phileas.model.responses;

import com.google.gson.Gson;
import com.mtnfog.phileas.model.objects.Explanation;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public final class BinaryDocumentFilterResponse {

    private transient final byte[] document;
	private final String context;
    private final String documentId;
    private final Explanation explanation;

    public BinaryDocumentFilterResponse(byte[] document, String context, String documentId, Explanation explanation) {

        this.document = document;
        this.context = context;
        this.documentId = documentId;
        this.explanation = explanation;

    }

    @Override
    public int hashCode() {

        return new HashCodeBuilder(17, 37).
                append(document).
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

    public byte[] getDocument() {
        return document;
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
