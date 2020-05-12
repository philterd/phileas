package com.mtnfog.phileas.services.anonymization.cache;

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
    public String generateKey(String context, String token) {

        return DigestUtils.md5Hex(context + "|" + token);

    }

    @Override
    public void put(String context, String token, String replacement) {

        cache.put(generateKey(context, token), replacement);

    }

    @Override
    public String get(String context, String token) {

        return cache.get(generateKey(context, token));

    }

    @Override
    public void remove(String context, String token) {

        cache.remove(generateKey(context, token));

    }

    @Override
    public boolean contains(String context, String token) {

        return cache.containsKey(generateKey(context, token));

    }

    @Override
    public boolean containsValue(String context, String replacement) {

        return cache.containsValue(replacement);

    }

}