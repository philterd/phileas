/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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
