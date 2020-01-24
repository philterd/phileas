package com.mtnfog.phileas.model.objects;

public class PostFilterResult {

    private boolean isPostFiltered;
    private Span span;

    public PostFilterResult(Span span, boolean isPostFiltered) {
        this.span = span;
        this.isPostFiltered = isPostFiltered;
    }

    public boolean isPostFiltered() {
        return isPostFiltered;
    }

    public Span getSpan() {
        return span;
    }

}
