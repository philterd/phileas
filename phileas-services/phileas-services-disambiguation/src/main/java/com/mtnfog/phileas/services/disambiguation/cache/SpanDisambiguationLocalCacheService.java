package com.mtnfog.phileas.services.disambiguation.cache;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.objects.SpanVector;
import com.mtnfog.phileas.model.services.SpanDisambiguationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class SpanDisambiguationLocalCacheService implements SpanDisambiguationCacheService {

    private static final Logger LOGGER = LogManager.getLogger(SpanDisambiguationLocalCacheService.class);

    private Map<String, Map<FilterType, SpanVector>> vectors;

    public SpanDisambiguationLocalCacheService() {

       this.vectors = new HashMap<>();

    }

    @Override
    public void hashAndInsert(String context, double[] hashes, Span span, int vectorSize) {

        // Initialize the cached map for all filter types if it does not already exist.
        if(vectors.get(context) == null) {

            final Map<FilterType, SpanVector> vector = new HashMap<>();

            for(final FilterType filterType : FilterType.values()) {
                vector.put(filterType, new SpanVector());
            }

            vectors.put(context, vector);

        }

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

        return vectors.get(context).get(filterType).getVectorIndexes();

    }

}