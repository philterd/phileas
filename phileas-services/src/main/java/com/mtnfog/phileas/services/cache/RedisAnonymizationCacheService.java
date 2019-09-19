package com.mtnfog.phileas.services.cache;

import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.codec.digest.DigestUtils;
import redis.clients.jedis.Jedis;

public class RedisAnonymizationCacheService implements AnonymizationCacheService {

    private Jedis jedis;

    public RedisAnonymizationCacheService(String host, int port, boolean ssl) {

        this.jedis = new Jedis(host, port, ssl);

    }

    @Override
    public String generateKey(String context, String key) {

        return DigestUtils.md5Hex(context + "|" + key);

    }

    @Override
    public void put(String context, String key, String value) {

        jedis.set(generateKey(context, key), value);

    }

    @Override
    public String get(String context, String key) {

        return jedis.get(generateKey(context, key));

    }

    @Override
    public void remove(String context, String key) {

        jedis.del(generateKey(context, key));
    }

    @Override
    public boolean contains(String context, String key) {

        return jedis.exists(generateKey(context, key));

    }

    @Override
    public boolean containsValue(String value) {

        // TODO: Figure out how to search by value in redis.
        return false;

    }

}
