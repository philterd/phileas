package com.mtnfog.phileas.services.disambiguation.cache;

import com.google.gson.Gson;
import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.cache.AbstractRedisCacheService;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.objects.SpanVector;
import com.mtnfog.phileas.model.services.SpanDisambiguationCacheService;
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
    }

    @Override
    public void hashAndInsert(String context, double[] hashes, Span span, int vectorSize) {

        final RMap<String, String> vectors = redisson.getMap(context);

        for(final double hash : hashes) {

            // Insert it into the appropriate vector.
            vectors.putIfAbsent(span.getFilterType().name(), new SpanVector().toString());

            final SpanVector sv = gson.fromJson(vectors.getOrDefault(span.getFilterType().name(), new SpanVector().toString()), SpanVector.class);

            final double val = sv.getVectorIndexes().getOrDefault(hash, 1.0);
            sv.getVectorIndexes().put(hash, val + 1.0);

            vectors.put(span.getFilterType().name(), gson.toJson(sv));

        }

    }

    @Override
    public Map<Double, Double> getVectorRepresentation(String context, FilterType filterType) {

        final Map<String, String> m = redisson.getMap(context);

        final SpanVector sv = gson.fromJson(m.get(filterType.name()), SpanVector.class);

        return sv.getVectorIndexes();

    }

}