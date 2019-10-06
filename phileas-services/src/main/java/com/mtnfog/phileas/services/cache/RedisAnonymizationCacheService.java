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
    public String generateKey(String context, String token) {

        return DigestUtils.md5Hex(context + "|" + token);

    }

    @Override
    public void put(String context, String token, String replacement) {

        jedis.set(generateKey(context, token), replacement);
        jedis.hset(context, token, replacement);


    }

    @Override
    public String get(String context, String token) {

        return jedis.get(generateKey(context, token));

    }

    @Override
    public void remove(String context, String token) {

        jedis.del(generateKey(context, token));

    }

    @Override
    public boolean contains(String context, String token) {

        return jedis.exists(generateKey(context, token));

    }

    @Override
    public boolean containsValue(String context, String replacement) {

        return jedis.hexists(context, replacement);

    }

}
