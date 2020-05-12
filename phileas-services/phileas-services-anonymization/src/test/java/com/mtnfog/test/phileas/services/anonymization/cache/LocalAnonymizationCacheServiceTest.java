package com.mtnfog.test.phileas.services.anonymization.cache;

import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.junit.Assert;
import org.junit.Test;

public class LocalAnonymizationCacheServiceTest {

    @Test
    public void putAndContains() {

        final LocalAnonymizationCacheService cache = new LocalAnonymizationCacheService();

        cache.put("context", "k", "v");

        Assert.assertTrue(cache.contains("context", "k"));

    }

    @Test
    public void containsValue() {

        final LocalAnonymizationCacheService cache = new LocalAnonymizationCacheService();

        cache.put("context", "k", "v");

        Assert.assertTrue(cache.containsValue("context", "v"));
        Assert.assertFalse(cache.containsValue("context", "k"));

    }

    @Test
    public void getAndPut() {

        final LocalAnonymizationCacheService cache = new LocalAnonymizationCacheService();

        cache.put("context", "k", "v");
        final String value = cache.get("context", "k");
        Assert.assertEquals("v", value);

        final String value2 = cache.get("context", "doesnotexist");
        Assert.assertNull(value2);

    }

    @Test
    public void putAndRemove() {

        final LocalAnonymizationCacheService cache = new LocalAnonymizationCacheService();

        cache.put("context", "k", "v");
        final String value = cache.get("context", "k");
        Assert.assertEquals("v", value);

        cache.remove("context", "k");
        final String value2 = cache.get("context", "k");
        Assert.assertNull(value2);

    }

    @Test
    public void generateKey() {

        final LocalAnonymizationCacheService cache = new LocalAnonymizationCacheService();

        final String hash = cache.generateKey("context", "k");

        Assert.assertTrue(hash.matches("^[a-f0-9]{32}$"));
        Assert.assertEquals("84e86fe7599f42d95d8ef20375b5a66e", hash);

    }

}