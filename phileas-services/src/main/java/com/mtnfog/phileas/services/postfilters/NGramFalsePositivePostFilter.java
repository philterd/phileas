package com.mtnfog.phileas.services.postfilters;

import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.PostFilter;

/**
 * Implementation of {@link PostFilter} that performs false positive
 * filtering by querying of an index of n-grams.
 */
public class NGramFalsePositivePostFilter extends PostFilter {

    private final int maxN;

    /**
     * Creates a new post filter.
     * @param maxN The maximum size of the n-grams.
     */
    public NGramFalsePositivePostFilter(int maxN) {
        this.maxN = maxN;
    }

    @Override
    protected boolean process(String text, Span span) {

        // TODO: Implement this.

        return false;

    }

}
