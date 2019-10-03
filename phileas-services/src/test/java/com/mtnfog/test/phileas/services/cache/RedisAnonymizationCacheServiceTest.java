package com.mtnfog.test.phileas.services.cache;

import com.mtnfog.phileas.services.cache.RedisAnonymizationCacheService;
import org.junit.*;
import redis.embedded.RedisServer;

import java.io.IOException;

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
    public void test() {

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService("localhost", port, false);

        cache.put("context", "k", "v");

        Assert.assertTrue(cache.contains("context", "k"));

    }

}
