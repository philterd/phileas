package com.mtnfog.test.phileas.services.cache;

import com.mtnfog.phileas.services.cache.anonymization.RedisAnonymizationCacheService;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class RedisAnonymizationCacheServiceIT {

    @Test
    public void putAndContains() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("cache.redis.enabled", "true");
        properties.setProperty("cache.redis.host", "master.philter.fl8lv7.use1.cache.amazonaws.com");
        properties.setProperty("cache.redis.port", "6379");

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService(properties);

        cache.put("context", "k", "v");

        Assert.assertTrue(cache.contains("context", "k"));

    }

    @Test
    public void containsValue() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("cache.redis.enabled", "true");
        properties.setProperty("cache.redis.host", "master.philter.fl8lv7.use1.cache.amazonaws.com");
        properties.setProperty("cache.redis.port", "6379");

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService(properties);

        cache.put("context", "k", "v");

        Assert.assertTrue(cache.containsValue("context", "k"));
        Assert.assertFalse(cache.containsValue("context", "v"));

    }

    @Test
    public void getAndPut() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("cache.redis.enabled", "true");
        properties.setProperty("cache.redis.host", "master.philter.fl8lv7.use1.cache.amazonaws.com");
        properties.setProperty("cache.redis.port", "6379");

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService(properties);

        cache.put("context", "k", "v");
        final String value = cache.get("context", "k");
        Assert.assertEquals("v", value);

        final String value2 = cache.get("context", "doesnotexist");
        Assert.assertNull(value2);

    }

    @Test
    public void putAndRemove() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("cache.redis.enabled", "true");
        properties.setProperty("cache.redis.host", "master.philter.fl8lv7.use1.cache.amazonaws.com");
        properties.setProperty("cache.redis.port", "6379");

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService(properties);

        cache.put("context", "k", "v");
        final String value = cache.get("context", "k");
        Assert.assertEquals("v", value);

        cache.remove("context", "k");
        final String value2 = cache.get("context", "k");
        Assert.assertNull(value2);

    }

    @Test
    public void generateKey() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("cache.redis.enabled", "true");
        properties.setProperty("cache.redis.host", "master.philter.fl8lv7.use1.cache.amazonaws.com");
        properties.setProperty("cache.redis.port", "6379");

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService(properties);

        final String hash = cache.generateKey("context", "k");

        Assert.assertTrue(hash.matches("^[a-f0-9]{32}$"));
        Assert.assertEquals("84e86fe7599f42d95d8ef20375b5a66e", hash);

    }

}
