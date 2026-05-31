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
package ai.philterd.phileas.services.disambiguation.vector;

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.SpanVector;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link VectorService} that stores everything in memory.
 *
 * <p>Documents are routinely processed concurrently, so all mutation here is done with the atomic
 * primitives of {@link ConcurrentHashMap} ({@code computeIfAbsent}, {@code merge}) rather than
 * check-then-act sequences. A check-then-act increment would lose counts under contention, and a
 * check-then-put initialization could replace an already-populated context map and discard
 * accumulated training data.
 */
public class InMemoryVectorService implements VectorService {

    protected final Map<String, Map<FilterType, SpanVector>> vectorCache;

    public InMemoryVectorService() {
        this.vectorCache = new ConcurrentHashMap<>();
    }

    // For disambiguation

    @Override
    public void hashAndInsert(String context, double[] hashes, Span span, int vectorSize) {

        final Map<Double, Double> vectorIndexes = initializeVectorCache(context)
                .get(span.getFilterType()).getVectorIndexes();

        for(int i = 0; i < hashes.length; i++) {

            if(hashes[i] != 0) {

                // Atomically accumulate the count for this index. We only care that the token was
                // present in the window; the magnitude is the number of windows that hit this index.
                vectorIndexes.merge((double) i, 1.0, Double::sum);

            }

        }

    }

    @Override
    public Map<Double, Double> getVectorRepresentation(String context, FilterType filterType) {

        return initializeVectorCache(context).get(filterType).getVectorIndexes();

    }

    /**
     * Returns the per-filter-type vectors for the context, creating them atomically on first use.
     * The map is fully populated for every {@link FilterType} before it is published, so callers
     * never see a partially-initialized context.
     */
    private Map<FilterType, SpanVector> initializeVectorCache(String context) {

        return vectorCache.computeIfAbsent(context, c -> {

            final Map<FilterType, SpanVector> vector = new HashMap<>();

            for(final FilterType filterType : FilterType.values()) {
                vector.put(filterType, new SpanVector());
            }

            return vector;

        });

    }

}
