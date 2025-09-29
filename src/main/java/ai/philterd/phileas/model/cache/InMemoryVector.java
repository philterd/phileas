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
package ai.philterd.phileas.model.cache;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.objects.SpanVector;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.model.services.VectorService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link VectorService} that stores everything in memory.
 */
public class InMemoryVector implements VectorService {

    private final Map<String, Map<FilterType, SpanVector>> vectorCache;
    private final Map<String, Policy> policyCache;
    private final Map<String, String> anonymizationCache;

    public InMemoryVector() {
        this.vectorCache = new ConcurrentHashMap<>();
        this.policyCache = new ConcurrentHashMap<>();
        this.anonymizationCache = new ConcurrentHashMap<>();
    }

    // For disambiguation

    @Override
    public void hashAndInsert(String context, double[] hashes, Span span, int vectorSize) {

        // Insert a new map for this context if it's needed to avoid an NPE.
        initializeVectorCache(context);

        for(double i = 0; i < hashes.length; i++) {

            if(hashes[(int) i] != 0) {

                if (vectorCache.get(context).get(span.getFilterType()).getVectorIndexes().get(i) == null) {
                    vectorCache.get(context).get(span.getFilterType()).getVectorIndexes().putIfAbsent(i, 0.0);
                }

                final double value = vectorCache.get(context).get(span.getFilterType()).getVectorIndexes().get(i);
                vectorCache.get(context).get(span.getFilterType()).getVectorIndexes().put(i, value + 1.0);

            }

        }

    }

    @Override
    public Map<Double, Double> getVectorRepresentation(String context, FilterType filterType) {

        // Insert a new map for this context if it's needed to avoid an NPE.
        initializeVectorCache(context);

        return vectorCache.get(context).get(filterType).getVectorIndexes();

    }

    private void initializeVectorCache(String context) {

        // Initialize the cached map for all filter types if it does not already exist.
        if(vectorCache.get(context) == null) {

            final Map<FilterType, SpanVector> vector = new HashMap<>();

            for(final FilterType filterType : FilterType.values()) {
                vector.put(filterType, new SpanVector());
            }

            vectorCache.put(context, vector);

        }

    }

}
