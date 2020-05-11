package com.mtnfog.phileas.model.services;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;

import java.util.List;

/**
 * Disambiguates the types of spans having duplicate start and end indexes.
 */
public interface SpanDisambiguationService {

    /**
     * Hashes and inserts the span into the cache.
     * @param context The context.
     * @param span The {@link Span}.
     */
    void hashAndInsert(String context, Span span);

    /**
     * Disambiguates two identical spans differing only by their filter types.
     * @param context The context.
     * @param filterTypes A list of identified {@link FilterType}/
     * @param ambiguousSpan The ambiguous {@link Span}.
     * @return The filter type most closely matching the ambiguous span.
     */
    FilterType disambiguate(String context, List<FilterType> filterTypes, Span ambiguousSpan);

    /**
     * Disambiguates two identical spans differing only by their filter types.
     * @param context The context.
     * @param spans A list of ambiguous spans.
     * @return A list of disambiguated spans.
     */
    List<Span> disambiguate(String context, List<Span> spans);

    /**
     * Gets a boolean indicating if the disambiguation service is enabled.
     * @return A boolean indicating if the disambiguation service is enabled.
     */
    boolean isEnabled();

}
