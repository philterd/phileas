package com.mtnfog.phileas.services.anonymization.cache;

import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.cache.AbstractRedisCacheService;
import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RMap;

import java.io.IOException;

public class RedisAnonymizationCacheService extends AbstractRedisCacheService implements AnonymizationCacheService {

    private static final Logger LOGGER = LogManager.getLogger(RedisAnonymizationCacheService.class);

    private static final String CACHE_ENTRY_NAME = "anonymization";

    public RedisAnonymizationCacheService(PhileasConfiguration phileasConfiguration) throws IOException {
        super(phileasConfiguration);
    }

    @Override
    public String generateKey(String context, String token) {

        return DigestUtils.md5Hex(context + "|" + token);

    }

    @Override
    public void put(String context, String token, String replacement) {

        final String key = generateKey(context, token);
        redisson.getMap(CACHE_ENTRY_NAME).put(key, replacement);

    }

    @Override
    public String get(String context, String token) {

        final String key = generateKey(context, token);
        final RMap<String, String> map = redisson.getMap(CACHE_ENTRY_NAME);

        return map.get(key);

    }

    @Override
    public void remove(String context, String token) {

        final String key = generateKey(context, token);
        final RMap<String, String> map = redisson.getMap(CACHE_ENTRY_NAME);

        map.remove(key);

    }

    @Override
    public boolean contains(String context, String token) {

        final String key = generateKey(context, token);
        final RMap<String, String> map = redisson.getMap(CACHE_ENTRY_NAME);

        return map.containsKey(key);

    }

    @Override
    public boolean containsValue(String context, String replacement) {

        final RMap<String, String> map = redisson.getMap(CACHE_ENTRY_NAME);

        return map.containsValue(replacement);

    }

}
