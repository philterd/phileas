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
package ai.philterd.phileas.services.disambiguation.cache;

import ai.philterd.phileas.model.configuration.PhileasConfiguration;
import com.google.gson.Gson;
import ai.philterd.phileas.model.cache.AbstractRedisCacheService;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.objects.SpanVector;
import ai.philterd.phileas.model.services.SpanDisambiguationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RMap;

import java.io.IOException;
import java.util.Map;

public class SpanDisambiguationRedisCacheService extends AbstractRedisCacheService implements SpanDisambiguationCacheService {

    private static final Logger LOGGER = LogManager.getLogger(SpanDisambiguationRedisCacheService.class);

    private Gson gson = new Gson();

    public SpanDisambiguationRedisCacheService(PhileasConfiguration phileasConfiguration) throws IOException {
        super(phileasConfiguration);

        LOGGER.info("Initializing Redis span disambiguation cache.");
    }

    @Override
    public void hashAndInsert(String context, double[] hashes, Span span, int vectorSize) {

        final RMap<String, String> vectors = redisson.getMap(context);

        for(final double hash : hashes) {

            // Insert a new map for this context if it's needed to avoid an NPE.
            initialize(span.getFilterType(), context);

            final SpanVector sv = gson.fromJson(vectors.getOrDefault(span.getFilterType().name(), new SpanVector().toString()), SpanVector.class);

            final double val = sv.getVectorIndexes().getOrDefault(hash, 1.0);
            sv.getVectorIndexes().put(hash, val + 1.0);

            vectors.put(span.getFilterType().name(), gson.toJson(sv));

        }

    }

    @Override
    public Map<Double, Double> getVectorRepresentation(String context, FilterType filterType) {

        // Insert a new map for this context if it's needed to avoid an NPE.
        initialize(filterType, context);

        final Map<String, String> m = redisson.getMap(context);

        final SpanVector sv = gson.fromJson(m.get(filterType.name()), SpanVector.class);

        return sv.getVectorIndexes();

    }

    private void initialize(FilterType filterType, String context) {

        final RMap<String, String> vectors = redisson.getMap(context);
        vectors.putIfAbsent(filterType.name(), new SpanVector().toString());

    }

}