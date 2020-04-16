package com.mtnfog.test.phileas.services.cache;

import com.mtnfog.phileas.services.cache.anonymization.RedisAnonymizationCacheService;
import org.junit.*;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Properties;

@Ignore("For some reason the mock redis server doesn't start on Jenkins")
public class RedisAnonymizationCacheServiceTest {

    private final RedisServer server = RedisServer.builder().port(31000).build();

    private Properties getProperties() {

        final Properties properties = new Properties();

        properties.setProperty("cache.redis.enabled", "true");
        properties.setProperty("cache.redis.host", "localhost");
        properties.setProperty("cache.redis.port", "31000");

        return properties;

    }

    @BeforeClass
    public static void beforeClass() {
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("win"));
    }

    @Before
    public void before() throws IOException {
        server.start();
    }

    @After
    public void after() throws IOException {
        server.stop();
    }

    @Test
    public void putAndContains() throws Exception {

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService(getProperties());

        cache.put("context", "k", "v");

        Assert.assertTrue(cache.contains("context", "k"));

    }

    @Test
    public void containsValue() throws Exception {

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService(getProperties());

        cache.put("context", "k", "v");

        Assert.assertTrue(cache.containsValue("context", "v"));
        Assert.assertFalse(cache.containsValue("context", "k"));

    }

    @Test
    public void getAndPut() throws Exception {

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService(getProperties());

        cache.put("context", "k", "v");
        final String value = cache.get("context", "k");
        Assert.assertEquals("v", value);

        final String value2 = cache.get("context", "doesnotexist");
        Assert.assertNull(value2);

    }

    @Test
    public void putAndRemove() throws Exception {

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService(getProperties());

        cache.put("context", "k", "v");
        final String value = cache.get("context", "k");
        Assert.assertEquals("v", value);

        cache.remove("context", "k");
        final String value2 = cache.get("context", "k");
        Assert.assertNull(value2);

    }

    @Test
    public void generateKey() throws Exception {

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService(getProperties());

        final String hash = cache.generateKey("context", "k");

        Assert.assertTrue(hash.matches("^[a-f0-9]{32}$"));
        Assert.assertEquals("84e86fe7599f42d95d8ef20375b5a66e", hash);

    }

}
