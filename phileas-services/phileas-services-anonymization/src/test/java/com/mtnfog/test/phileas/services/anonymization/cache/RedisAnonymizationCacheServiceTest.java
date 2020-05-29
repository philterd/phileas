package com.mtnfog.test.phileas.services.anonymization.cache;

import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.services.anonymization.cache.RedisAnonymizationCacheService;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import redis.embedded.RedisServer;

import java.util.Properties;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class RedisAnonymizationCacheServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(RedisAnonymizationCacheServiceTest.class);

    private RedisServer redisServer;
    private static boolean isExternalRedis = false;

    private PhileasConfiguration getConfiguration() {

        final Properties properties = new Properties();

        final String redisHost = System.getenv("PHILTER_REDIS_HOST");
        final String redisPort = System.getenv("PHILTER_REDIS_PORT");
        final String redisSsl = System.getenv("PHILTER_REDIS_SSL");
        final String redisToken = System.getenv("PHILTER_REDIS_AUTH_TOKEN");
        final String redisClustered = System.getenv("PHILTER_REDIS_CLUSTERED");

        properties.setProperty("filter.profiles", "s3");
        properties.setProperty("filter.profiles.s3.bucket", "profiles");
        properties.setProperty("filter.profiles.s3.prefix", "/");

        properties.setProperty("cache.redis.enabled", "true");

        if(StringUtils.isNotEmpty(redisHost)) {

            LOGGER.info("Using redis host: {}", redisHost);

            properties.setProperty("cache.redis.host", redisHost);
            properties.setProperty("cache.redis.port", redisPort);
            properties.setProperty("cache.redis.ssl", redisSsl);
            properties.setProperty("cache.redis.auth.token", redisToken);
            properties.setProperty("cache.redis.cluster", redisClustered);

        } else {

            LOGGER.info("Using local redis host.");

            properties.setProperty("cache.redis.host", "localhost");
            properties.setProperty("cache.redis.port", "31000");
            properties.setProperty("cache.redis.ssl", "false");
            properties.setProperty("cache.redis.auth.token", "");
            properties.setProperty("cache.redis.cluster", "false");

        }

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        return phileasConfiguration;

    }

    @BeforeAll
    public static void beforeClass() {

        assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("win"));

        final String redisHost = System.getenv("PHILTER_REDIS_HOST");

        if(StringUtils.isNotEmpty(redisHost)) {
            isExternalRedis = true;
        }

    }

    @BeforeEach
    public void before() {

        if(!isExternalRedis) {
            redisServer = RedisServer.builder().port(31000).build();
            redisServer.start();
        }

    }

    @AfterEach
    public void after() {

        if(!isExternalRedis) {
            redisServer.stop();
        }

    }

    @Test
    public void putAndContains() throws Exception {

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService(getConfiguration());

        cache.put("context", "k", "v");

        Assertions.assertTrue(cache.contains("context", "k"));

    }

    @Test
    public void containsValue() throws Exception {

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService(getConfiguration());

        cache.put("context", "k", "v");

        Assertions.assertTrue(cache.containsValue("context", "v"));
        Assertions.assertFalse(cache.containsValue("context", "k"));

    }

    @Test
    public void getAndPut() throws Exception {

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService(getConfiguration());

        cache.put("context", "k", "v");
        final String value = cache.get("context", "k");
        Assertions.assertEquals("v", value);

        final String value2 = cache.get("context", "doesnotexist");
        Assertions.assertNull(value2);

    }

    @Test
    public void putAndRemove() throws Exception {

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService(getConfiguration());

        cache.put("context", "k", "v");
        final String value = cache.get("context", "k");
        Assertions.assertEquals("v", value);

        cache.remove("context", "k");
        final String value2 = cache.get("context", "k");
        Assertions.assertNull(value2);

    }

    @Test
    public void generateKey() throws Exception {

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService(getConfiguration());

        final String hash = cache.generateKey("context", "k");

        Assertions.assertTrue(hash.matches("^[a-f0-9]{32}$"));
        Assertions.assertEquals("84e86fe7599f42d95d8ef20375b5a66e", hash);

    }

}
