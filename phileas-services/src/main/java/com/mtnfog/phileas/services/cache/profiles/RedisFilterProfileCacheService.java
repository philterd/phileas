package com.mtnfog.phileas.services.cache.profiles;

import com.mtnfog.phileas.model.services.FilterProfileCacheService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class RedisFilterProfileCacheService implements FilterProfileCacheService {

    private static final Logger LOGGER = LogManager.getLogger(RedisFilterProfileCacheService.class);

    private final RedissonClient redisson;

    public RedisFilterProfileCacheService(Properties applicationProperties) {

        final String cluster = applicationProperties.getProperty("cache.redis.cluster");
        final String redisEndpoint = applicationProperties.getProperty("cache.redis.host");
        final String redisPort = applicationProperties.getProperty("cache.redis.port");
        final String authToken = applicationProperties.getProperty("cache.redis.auth.token");
        // TODO: Configure ssl.
        final String ssl = applicationProperties.getProperty("cache.redis.ssl");

        // Configure the connection to the cache.
        // use "rediss://" for SSL connection

        // TODO: Configure SSL: https://gist.github.com/eransharv/9de8e94faae5bde70dfcdfa7d8e6157b
        // https://github.com/redisson/redisson/wiki/2.-Configuration

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
    public List<String> get() throws IOException {

        // Get the names from the cache and return them.
        final RMap<String, String> names = redisson.getMap("filterProfiles");

        return new ArrayList<>(names.keySet());

    }

    @Override
    public String get(String filterProfileName) throws IOException {

        // Get the filter profile from the cache and return it.
        final Map<String, String> map = redisson.getMap("filterProfiles");

        return map.get(filterProfileName);

    }

    @Override
    public Map<String, String> getAll() throws IOException {

        // Get the filter profiles from the cache and return them.
        final Map<String, String> map = redisson.getMap("filterProfiles");

        return map;

    }

    @Override
    public void insert(String filterProfileName, String filterProfile) {

        // Insert into the the cache.
        redisson.getMap("filterProfiles").put(filterProfileName, filterProfile);

    }

    @Override
    public void remove(String filterProfileName) {

        // Remove from the cache.
        redisson.getMap("filterProfiles").remove(filterProfileName);

    }

    @Override
    public void clear() throws IOException {

        // Clear the cache.
        redisson.getMap("filterProfiles").clear();

    }

}