package io.philterd.phileas.model.services;

import io.philterd.phileas.model.enums.FilterType;
import io.philterd.phileas.model.objects.Span;

import java.util.Map;

/**
 * Stores span hashes to later be used for span disambiguation.
 */
public interface SpanDisambiguationCacheService {

    /**
     * Hashes and inserts the span into the cache.
     * @param context The context.
     * @param span The {@link Span} containing the window.
     * @param vectorSize The size of the vector.
     */
    void hashAndInsert(String context, double[] hashes, Span span, int vectorSize);

    /**
     * Gets a vector representation for a {@link Span} given a context.
     * @param context The context.
     * @param filterType The {@link FilterType} whose vector representation to get.
     * @return A map of integers representing the vector for the span.
     */
    Map<Double, Double> getVectorRepresentation(String context, FilterType filterType);

}
