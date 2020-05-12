package com.mtnfog.phileas.services.disambiguation;

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
    public void hashAndInsert(String context, Span span, int vectorSize) {

        // Initialize the cached map for all filter types if it does not already exist.
        if(vectors.get(context) == null) {

            final Map<FilterType, SpanVector> vector = new HashMap<>();

            for(final FilterType filterType : FilterType.values()) {
                //LOGGER.info("Putting entry of {} - {}", context, filterType.name());
                vector.put(filterType, new SpanVector());
            }

            vectors.put(context, vector);

        }

        final String[] window = span.getWindow();

        for(final String token : window) {

            // TODO: Hash the token with an actual algorithm.
            final int hash = Math.abs(token.hashCode() % vectorSize);

            //LOGGER.info("Getting entry of {} - {}", context, span.getFilterType().name());

            final SpanVector sv = vectors.get(context).get(span.getFilterType());

            if(vectors.get(context).get(span.getFilterType()).getVectorIndexes().get(hash) == null) {
                vectors.get(context).get(span.getFilterType()).getVectorIndexes().putIfAbsent(hash, 0);
            }

            int value = vectors.get(context).get(span.getFilterType()).getVectorIndexes().get(hash);
            vectors.get(context).get(span.getFilterType()).getVectorIndexes().put(hash, value + 1);

        }

    }

    @Override
    public Map<Integer, Integer> getVectorRepresentation(String context, FilterType filterType) {

        return vectors.get(context).get(filterType).getVectorIndexes();

    }

}