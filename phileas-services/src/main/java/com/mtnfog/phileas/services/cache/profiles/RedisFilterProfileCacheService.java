package com.mtnfog.phileas.services.cache.profiles;

import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.services.FilterProfileCacheService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedisFilterProfileCacheService implements FilterProfileCacheService {

    private static final Logger LOGGER = LogManager.getLogger(RedisFilterProfileCacheService.class);

    private final RedissonClient redisson;

    public RedisFilterProfileCacheService(PhileasConfiguration phileasConfiguration) {

        final boolean cluster = phileasConfiguration.cacheRedisCluster();
        final String redisEndpoint = phileasConfiguration.cacheRedisHost();
        final int redisPort = phileasConfiguration.cacheRedisPort();
        final String authToken = phileasConfiguration.cacheRedisAuthToken();
        final boolean ssl = phileasConfiguration.cacheRedisSsl();

        final Config config = new Config();

        if(cluster) {

            final String protocol;

            if (ssl) {
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

            if (ssl) {
                protocol = "rediss://";
            } else {
                protocol = "redis://";
            }

            final String redisAddress = protocol + redisEndpoint + ":" + redisPort;
            LOGGER.info("Using single server redis connection {}", redisAddress);

            if(StringUtils.isNotEmpty(authToken)) {
                config.useSingleServer().setAddress(redisAddress).setPassword(authToken);
            } else {
                config.useSingleServer().setAddress(redisAddress);
            }

        }

        redisson = Redisson.create(config);

    }

    @Override
    public List<String> get() {

        // Get the names from the cache and return them.
        final RMap<String, String> names = redisson.getMap("filterProfiles");

        return new ArrayList<>(names.keySet());

    }

    @Override
    public String get(String filterProfileName) {

        // Get the filter profile from the cache and return it.
        final RMap<String, String> map = redisson.getMap("filterProfiles");

        return map.get(filterProfileName);

    }

    @Override
    public Map<String, String> getAll() {

        final long count = redisson.getKeys().countExists("filterProfiles");

        if(count != 0) {

            // Get the filter profiles from the cache and return them.
            final RMap<String, String> map = redisson.getMap("filterProfiles");

            Map<String, String> m = new HashMap<>();
            for(String k : map.keySet()) {
                m.put(k, map.get(k));
            }

            return m;

        } else {

            return new HashMap<>();

        }

    }

    @Override
    public void insert(String filterProfileName, String filterProfile) {

        // Insert into the the cache.
        final RMap<String, String> map = redisson.getMap("filterProfiles");
        map.put(filterProfileName, filterProfile);

    }

    @Override
    public void remove(String filterProfileName) {

        // Remove from the cache.
        final RMap<String, String> map = redisson.getMap("filterProfiles");
        map.remove(filterProfileName);

    }

    @Override
    public void clear() {

        // Clear the cache.
        final RMap<String, String> map = redisson.getMap("filterProfiles");
        map.clear();

    }

}