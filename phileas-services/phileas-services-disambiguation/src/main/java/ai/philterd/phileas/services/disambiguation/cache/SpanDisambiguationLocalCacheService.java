/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services.disambiguation.cache;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.objects.SpanVector;
import ai.philterd.phileas.model.services.SpanDisambiguationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class SpanDisambiguationLocalCacheService implements SpanDisambiguationCacheService {

    private static final Logger LOGGER = LogManager.getLogger(SpanDisambiguationLocalCacheService.class);

    private Map<String, Map<FilterType, SpanVector>> vectors;

    public SpanDisambiguationLocalCacheService() {

        LOGGER.info("Initializing local span disambiguation cache.");
        this.vectors = new HashMap<>();

    }

    @Override
    public void hashAndInsert(String context, double[] hashes, Span span, int vectorSize) {

        // Insert a new map for this context if it's needed to avoid an NPE.
        initialize(context);

        for(double i = 0; i < hashes.length; i++) {

            if(hashes[(int) i] != 0) {

                if (vectors.get(context).get(span.getFilterType()).getVectorIndexes().get(i) == null) {
                    vectors.get(context).get(span.getFilterType()).getVectorIndexes().putIfAbsent(i, 0.0);
                }

                final double value = vectors.get(context).get(span.getFilterType()).getVectorIndexes().get(i);
                vectors.get(context).get(span.getFilterType()).getVectorIndexes().put(i, value + 1.0);

            }

        }

    }

    @Override
    public Map<Double, Double> getVectorRepresentation(String context, FilterType filterType) {

        // Insert a new map for this context if it's needed to avoid an NPE.
        initialize(context);

        return vectors.get(context).get(filterType).getVectorIndexes();

    }

    private void initialize(String context) {

        // Initialize the cached map for all filter types if it does not already exist.
        if(vectors.get(context) == null) {

            final Map<FilterType, SpanVector> vector = new HashMap<>();

            for(final FilterType filterType : FilterType.values()) {
                vector.put(filterType, new SpanVector());
            }

            vectors.put(context, vector);

        }

    }

}