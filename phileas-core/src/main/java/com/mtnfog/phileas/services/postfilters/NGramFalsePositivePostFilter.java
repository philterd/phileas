package com.mtnfog.phileas.services.postfilters;

import com.mtnfog.phileas.model.objects.PostFilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.PostFilter;
import org.apache.commons.lang3.NotImplementedException;

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
    protected PostFilterResult process(String text, Span span) {

        throw new NotImplementedException("Not yet implemented.");

    }

}
