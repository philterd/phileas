/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.model.services;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Span;

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
