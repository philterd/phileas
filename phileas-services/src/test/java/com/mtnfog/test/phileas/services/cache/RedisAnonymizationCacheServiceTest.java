package com.mtnfog.test.phileas.services.cache;

import com.mtnfog.phileas.services.cache.RedisAnonymizationCacheService;
import org.junit.*;
import redis.embedded.RedisServer;

import java.io.IOException;

@Ignore
public class RedisAnonymizationCacheServiceTest {

    private int port = 31000;
    private final RedisServer server = RedisServer.builder().port(port).build();

    @BeforeClass
    public static void beforeClass() throws IOException {
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

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService("localhost", port);

        cache.put("context", "k", "v");

        Assert.assertTrue(cache.contains("context", "k"));

    }

    @Test
    public void containsValue() throws Exception {

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService("localhost", port);

        cache.put("context", "k", "v");

        Assert.assertTrue(cache.containsValue("context", "k"));
        Assert.assertFalse(cache.containsValue("context", "v"));

    }

    @Test
    public void getAndPut() throws Exception {

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService("localhost", port);

        cache.put("context", "k", "v");
        final String value = cache.get("context", "k");
        Assert.assertEquals("v", value);

        final String value2 = cache.get("context", "doesnotexist");
        Assert.assertNull(value2);

    }

    @Test
    public void putAndRemove() throws Exception {

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService("localhost", port);

        cache.put("context", "k", "v");
        final String value = cache.get("context", "k");
        Assert.assertEquals("v", value);

        cache.remove("context", "k");
        final String value2 = cache.get("context", "k");
        Assert.assertNull(value2);

    }

    @Test
    public void generateKey() throws Exception {

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService("localhost", port);

        final String hash = cache.generateKey("context", "k");

        Assert.assertTrue(hash.matches("^[a-f0-9]{32}$"));
        Assert.assertEquals("84e86fe7599f42d95d8ef20375b5a66e", hash);

    }

}
