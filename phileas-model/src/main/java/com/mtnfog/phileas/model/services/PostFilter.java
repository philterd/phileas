package com.mtnfog.phileas.model.services;

import com.mtnfog.phileas.model.objects.PostFilterResult;
import com.mtnfog.phileas.model.objects.Span;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * Performs post-filtering.
 */
public abstract class PostFilter implements Serializable {

    protected static final Logger LOGGER = LogManager.getLogger(PostFilter.class);

    /**
     * Performs post-filtering on a list of spans.
     * @param text The input text.
     * @param span The {@link Span} to process.
     * @return <code>true</code> if the span should not be filtered.
     * <code>false</code> if the span should be filtered.
     */
    protected abstract PostFilterResult process(String text, Span span);

    /**
     * Filters a list of spans per the implementation of <code>process</code>.
     * @param text The input text.
     * @param spans A list of {@link Span spans}.
     * @return A filtered list of {@link Span spans}.
     */
    public List<Span> filter(String text, List<Span> spans) {

        final Iterator<Span> i = spans.iterator();

        while (i.hasNext()) {

            final Span span = i.next();
            final PostFilterResult postFilterResult = process(text, span);

            if(postFilterResult.isPostFiltered()) {
                i.remove();
            }

        }

        return spans;

    }

}
