package com.mtnfog.test.phileas.services.cache;

import com.mtnfog.phileas.services.cache.anonymization.RedisAnonymizationCacheService;
import com.mtnfog.test.phileas.services.registry.S3FilterProfileServiceTest;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Properties;

public class RedisAnonymizationCacheServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(RedisAnonymizationCacheServiceTest.class);

    private final RedisServer server = RedisServer.builder().port(31000).build();

    private Properties getProperties() {

        final Properties properties = new Properties();

        final String redisHost = System.getenv("PHILTER_REDIS_HOST");
        final String redisPort = System.getenv("PHILTER_REDIS_PORT");
        final String redisSsl = System.getenv("PHILTER_REDIS_SSL");

        properties.setProperty("filter.profiles", "s3");
        properties.setProperty("filter.profiles.s3.bucket", "profiles");
        properties.setProperty("filter.profiles.s3.prefix", "/");

        properties.setProperty("cache.redis.enabled", "true");

        if(StringUtils.isNotEmpty(redisHost)) {

            LOGGER.info("Using redis host: {}", redisHost);

            properties.setProperty("cache.redis.host", redisHost);
            properties.setProperty("cache.redis.port", redisPort);
            properties.setProperty("cache.redis.ssl.enabled", redisSsl);

        } else {

            LOGGER.info("Using local redis host.");

            properties.setProperty("cache.redis.host", "localhost");
            properties.setProperty("cache.redis.port", "31000");
            properties.setProperty("cache.redis.ssl.enabled", "false");

        }

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
