package com.mtnfog.phileas.services.cache;

import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalAnonymizationCacheService implements AnonymizationCacheService {

    private Map<String, String> cache;

    public LocalAnonymizationCacheService() {
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public String generateKey(String context, String key) {

        return DigestUtils.md5Hex(context + "|" + key);

    }

    @Override
    public void put(String context, String key, String value) {

        cache.put(generateKey(context, key), value);

    }

    @Override
    public String get(String context, String key) {

        return cache.get(generateKey(context, key));

    }

    @Override
    public void remove(String context, String key) {

        cache.remove(generateKey(context, key));

    }

    @Override
    public boolean contains(String context, String key) {

        return cache.containsKey(generateKey(context, key));

    }

    @Override
    public boolean containsValue(String value) {

        return cache.containsValue(value);

    }

}