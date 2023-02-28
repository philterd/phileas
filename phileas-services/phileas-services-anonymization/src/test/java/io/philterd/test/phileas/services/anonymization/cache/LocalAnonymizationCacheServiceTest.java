package io.philterd.test.phileas.services.anonymization.cache;

import io.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LocalAnonymizationCacheServiceTest {

    @Test
    public void putAndContains() {

        final LocalAnonymizationCacheService cache = new LocalAnonymizationCacheService();

        cache.put("context", "k", "v");

        Assertions.assertTrue(cache.contains("context", "k"));

    }

    @Test
    public void containsValue() {

        final LocalAnonymizationCacheService cache = new LocalAnonymizationCacheService();

        cache.put("context", "k", "v");

        Assertions.assertTrue(cache.containsValue("context", "v"));
        Assertions.assertFalse(cache.containsValue("context", "k"));

    }

    @Test
    public void getAndPut() {

        final LocalAnonymizationCacheService cache = new LocalAnonymizationCacheService();

        cache.put("context", "k", "v");
        final String value = cache.get("context", "k");
        Assertions.assertEquals("v", value);

        final String value2 = cache.get("context", "doesnotexist");
        Assertions.assertNull(value2);

    }

    @Test
    public void putAndRemove() {

        final LocalAnonymizationCacheService cache = new LocalAnonymizationCacheService();

        cache.put("context", "k", "v");
        final String value = cache.get("context", "k");
        Assertions.assertEquals("v", value);

        cache.remove("context", "k");
        final String value2 = cache.get("context", "k");
        Assertions.assertNull(value2);

    }

    @Test
    public void generateKey() {

        final LocalAnonymizationCacheService cache = new LocalAnonymizationCacheService();

        final String hash = cache.generateKey("context", "k");

        Assertions.assertTrue(hash.matches("^[a-f0-9]{32}$"));
        Assertions.assertEquals("84e86fe7599f42d95d8ef20375b5a66e", hash);

    }

}