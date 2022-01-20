package com.mtnfog.phileas.model.objects;

import java.util.List;

public class FilterResult {

    private String documentId;
    private String context;
    private List<Span> spans;

    public FilterResult(String context, String documentId, List<Span> spans) {

        this.documentId = documentId;
        this.context = context;
        this.spans = spans;

    }

    public String getDocumentId() {
        return documentId;
    }

    public String getContext() {
        return context;
    }

    public List<Span> getSpans() {
        return spans;
    }

}
