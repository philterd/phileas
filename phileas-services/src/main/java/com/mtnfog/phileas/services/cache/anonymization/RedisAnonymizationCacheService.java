package com.mtnfog.phileas.services.cache.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.Properties;

public class RedisAnonymizationCacheService implements AnonymizationCacheService {

    private static final String CACHE_ENTRY_NAME = "anonymization";
    
    private final RedissonClient redisson;

    public RedisAnonymizationCacheService(Properties applicationProperties) throws Exception {

        final String cluster = applicationProperties.getProperty("cache.redis.cluster");
        final String redisEndpoint = applicationProperties.getProperty("cache.redis.host");
        final String redisPort = applicationProperties.getProperty("cache.redis.port");
        final String authToken = applicationProperties.getProperty("cache.redis.auth.token");
        final String ssl = applicationProperties.getProperty("cache.redis.ssl");

        final Config config = new Config();

        if(StringUtils.equalsIgnoreCase(cluster, "true")) {

            if (StringUtils.equalsIgnoreCase("ssl", "true")) {

                config.useClusterServers()
                        .setScanInterval(2000)
                        .addNodeAddress("rediss://" + redisEndpoint + ":" + redisPort)
                        .setPassword(authToken);

            } else {

                config.useClusterServers()
                        .setScanInterval(2000)
                        .addNodeAddress("redis://" + redisEndpoint + ":" + redisPort)
                        .setPassword(authToken);

            }

        } else {

            config.useSingleServer().setAddress("redis://" + redisEndpoint + ":" + redisPort);

        }

        redisson = Redisson.create(config);

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
