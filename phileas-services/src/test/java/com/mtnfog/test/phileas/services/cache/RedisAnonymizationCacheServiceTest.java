package com.mtnfog.test.phileas.services.cache;

import com.mtnfog.phileas.services.cache.RedisAnonymizationCacheService;
import org.junit.*;
import org.springframework.util.SocketUtils;
import redis.embedded.RedisServer;

public class RedisAnonymizationCacheServiceTest {

    private final int port = SocketUtils.findAvailableTcpPort();
    private final RedisServer server = RedisServer.builder().port(port).build();

    @BeforeClass
    public static void beforeClass() {
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("win"));
    }

    @Before
    public void before() {
        server.start();
    }

    @After
    public void after() {
        server.stop();
    }

    @Test
    public void test() {

        final RedisAnonymizationCacheService cache = new RedisAnonymizationCacheService("localhost", port, false);

        cache.put("context", "k", "v");

        Assert.assertTrue(cache.contains("context", "k"));

    }

}
