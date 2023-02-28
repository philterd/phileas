package io.philterd.phileas.model.objects;

import java.util.List;

public class FilterResult {

    private String documentId;
    private String context;
    private int piece;
    private List<Span> spans;

    public FilterResult(String context, String documentId, List<Span> spans) {

        this.documentId = documentId;
        this.context = context;
        this.piece = 0;
        this.spans = spans;

    }

    public FilterResult(String context, String documentId, int piece, List<Span> spans) {

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

    public int getPiece() {
        return piece;
    }

    public List<Span> getSpans() {
        return spans;
    }

}
