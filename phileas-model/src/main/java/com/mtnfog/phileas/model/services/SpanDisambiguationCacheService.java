package com.mtnfog.phileas.model.services;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;

import java.util.Map;

/**
 * Stores span hashes to later be used for span disambiguation.
 */
public interface SpanDisambiguationCacheService {

    /**
     * Hashes and inserts the span into the cache.
     * @param context The context.
     * @param span The {@link Span}.
     * @param vectorSize The size of the vector.
     */
    void hashAndInsert(String context, Span span, int vectorSize);

    /**
     * Gets a vector representation for a {@link Span} given a context.
     * @param context The context.
     * @param filterType The {@link FilterType} whose vector representation to get.
     * @return A map of integers representing the vector for the span.
     */
    Map<Integer, Integer> getVectorRepresentation(String context, FilterType filterType);

}
