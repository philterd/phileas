package com.mtnfog.phileas.services.disambiguation;

import com.google.gson.Gson;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.objects.SpanVector;
import com.mtnfog.phileas.model.services.SpanDisambiguationCacheService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.Map;
import java.util.Properties;

public class SpanDisambiguationRedisCacheService implements SpanDisambiguationCacheService {

    private static final Logger LOGGER = LogManager.getLogger(com.mtnfog.phileas.services.cache.profiles.RedisFilterProfileCacheService.class);

    private final RedissonClient redisson;

    private Gson gson = new Gson();

    public SpanDisambiguationRedisCacheService(Properties applicationProperties) {

        final String cluster = applicationProperties.getProperty("cache.redis.cluster");
        final String redisEndpoint = applicationProperties.getProperty("cache.redis.host");
        final String redisPort = applicationProperties.getProperty("cache.redis.port");
        final String authToken = applicationProperties.getProperty("cache.redis.auth.token");
        final String ssl = applicationProperties.getProperty("cache.redis.ssl");

        final Config config = new Config();

        if (StringUtils.equalsIgnoreCase(cluster, "true")) {

            final String protocol;

            if (StringUtils.equalsIgnoreCase(ssl, "true")) {
                protocol = "rediss://";
            } else {
                protocol = "redis://";
            }

            final String redisAddress = protocol + redisEndpoint + ":" + redisPort;
            LOGGER.info("Using clustered redis connection: {}", redisAddress);

            config.useClusterServers()
                    .setScanInterval(2000)
                    .addNodeAddress(redisAddress)
                    .setPassword(authToken);

        } else {

            final String protocol;

            if (StringUtils.equalsIgnoreCase(ssl, "true")) {
                protocol = "rediss://";
            } else {
                protocol = "redis://";
            }

            final String redisAddress = protocol + redisEndpoint + ":" + redisPort;
            LOGGER.info("Using single server redis connection {}", redisAddress);

            if (StringUtils.isNotEmpty(authToken)) {
                config.useSingleServer().setAddress(redisAddress).setPassword(authToken);
            } else {
                config.useSingleServer().setAddress(redisAddress);
            }

        }

        redisson = Redisson.create(config);

    }

    @Override
    public void hashAndInsert(String context, Span span, int vectorSize) {

        // Initialize the cached map for all filter types if it does not already exist.
        //if(vectors.get(context) == null) {

            //final RMap<FilterType, SpanVector> vector = new HashMap<>();
            final RMap<String, String> vectors = redisson.getMap(context);

            /*for(final FilterType filterType : FilterType.values()) {
                vector.put(filterType, new SpanVector());
            }

            vectors.put(context, vector);*/

        //}

        final String[] window = span.getWindow();

        for(final String token : window) {

            // TODO: Hash the token with an actual algorithm.
            final int hash = Math.abs(token.hashCode() % vectorSize);

            System.out.println(token + ": hash = " + hash);

            // Insert it into the appropriate vector.
            vectors.putIfAbsent(span.getFilterType().name(), new SpanVector().toString());

            final SpanVector sv = gson.fromJson(vectors.getOrDefault(span.getFilterType().name(), new SpanVector().toString()), SpanVector.class);

            int val = sv.getVectorIndexes().getOrDefault(hash, 0);
            sv.getVectorIndexes().put(hash, val + 1);
         //   int val = sv.getVectorIndexes().getOrDefault(hash, 0) + 1;

            vectors.put(span.getFilterType().name(), gson.toJson(sv));

        }

    }

    @Override
    public Map<Integer, Integer> getVectorRepresentation(String context, FilterType filterType) {

        Map<String, String> m = redisson.getMap(context);

        final SpanVector sv = gson.fromJson(m.get(filterType.name()), SpanVector.class);

        return sv.getVectorIndexes();

    }

}