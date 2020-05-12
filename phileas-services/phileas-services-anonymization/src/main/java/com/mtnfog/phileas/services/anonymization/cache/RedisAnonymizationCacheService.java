package com.mtnfog.phileas.services.anonymization.cache;

import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedisAnonymizationCacheService implements AnonymizationCacheService {

    private static final Logger LOGGER = LogManager.getLogger(RedisAnonymizationCacheService.class);

    private static final String CACHE_ENTRY_NAME = "anonymization";
    
    private final RedissonClient redisson;

    public RedisAnonymizationCacheService(PhileasConfiguration phileasConfiguration) {

        final boolean cluster = phileasConfiguration.cacheRedisCluster();
        final String redisEndpoint = phileasConfiguration.cacheRedisHost();
        final int redisPort = phileasConfiguration.cacheRedisPort();
        final String authToken = phileasConfiguration.cacheRedisAuthToken();
        final boolean ssl = phileasConfiguration.cacheRedisSsl();

        final Config config = new Config();

        if (cluster) {

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
            config.useSingleServer().setAddress(redisAddress);

            if(StringUtils.isNotEmpty(authToken)) {
                config.useSingleServer().setAddress(redisAddress).setPassword(authToken);
            } else {
                config.useSingleServer().setAddress(redisAddress);
            }

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
