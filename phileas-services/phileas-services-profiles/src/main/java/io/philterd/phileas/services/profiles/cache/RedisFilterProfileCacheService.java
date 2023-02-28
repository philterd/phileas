package io.philterd.phileas.services.profiles.cache;

import io.philterd.phileas.configuration.PhileasConfiguration;
import io.philterd.phileas.model.cache.AbstractRedisCacheService;
import io.philterd.phileas.model.services.FilterProfileCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedisFilterProfileCacheService extends AbstractRedisCacheService implements FilterProfileCacheService {

    private static final Logger LOGGER = LogManager.getLogger(RedisFilterProfileCacheService.class);

    private static final String CACHE_KEY = "filterProfiles";
    
    public RedisFilterProfileCacheService(PhileasConfiguration phileasConfiguration) throws IOException {
        super(phileasConfiguration);

        LOGGER.info("Initializing Reds filter profile cache.");
    }

    @Override
    public List<String> get() {

        // Get the names from the cache and return them.
        final RMap<String, String> names = redisson.getMap(CACHE_KEY);

        return new ArrayList<>(names.keySet());

    }

    @Override
    public String get(String filterProfileName) {

        // Get the filter profile from the cache and return it.
        final RMap<String, String> map = redisson.getMap(CACHE_KEY);

        return map.get(filterProfileName);

    }

    @Override
    public Map<String, String> getAll() {

        final long count = redisson.getKeys().countExists(CACHE_KEY);

        if(count != 0) {

            // Get the filter profiles from the cache and return them.
            final RMap<String, String> map = redisson.getMap(CACHE_KEY);

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
        final RMap<String, String> map = redisson.getMap(CACHE_KEY);
        map.put(filterProfileName, filterProfile);

    }

    @Override
    public void remove(String filterProfileName) {

        // Remove from the cache.
        final RMap<String, String> map = redisson.getMap(CACHE_KEY);
        map.remove(filterProfileName);

    }

    @Override
    public void clear() {

        // Clear the cache.
        final RMap<String, String> map = redisson.getMap(CACHE_KEY);
        map.clear();

    }

}