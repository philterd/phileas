package com.mtnfog.test.phileas.services.alerts;

import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Alert;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.alerts.RedisAlertService;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class RedisAlertServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(RedisAlertServiceTest.class);

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

        return ConfigFactory.create(PhileasConfiguration.class, properties);

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
    public void before() throws IOException {

        if(!isExternalRedis) {
            redisServer = RedisServer.builder().port(31000).build();
            redisServer.start();
        } else {
            // Clear alerts from the cache.
            final AlertService alertService = new RedisAlertService(getConfiguration());
            alertService.clear();
        }

    }

    @AfterEach
    public void after() throws IOException {

        if(!isExternalRedis) {
            redisServer.stop();
        } else {
            // Clear alerts from the cache.
            final AlertService alertService = new RedisAlertService(getConfiguration());
            alertService.clear();
        }

    }

    @Test
    public void generate1() throws IOException {

        final AlertService alertService = new RedisAlertService(getConfiguration());

        alertService.generateAlert("fp", "id", "context", "docid", FilterType.NER_ENTITY);

        final List<Alert> alerts = alertService.getAlerts();

        Assertions.assertEquals(1, alerts.size());

    }

    @Test
    public void remove1() throws IOException {

        final AlertService alertService = new RedisAlertService(getConfiguration());

        alertService.generateAlert("fp", "id", "context", "docid", FilterType.NER_ENTITY);

        List<Alert> alerts = alertService.getAlerts();

        Assertions.assertEquals(1, alerts.size());

        final String alertId = alerts.get(0).getId();

        LOGGER.info("Removing alert ID {}", alertId);

        alertService.delete(alertId);

        alerts = alertService.getAlerts();

        for(final Alert alert : alerts) {
            LOGGER.info(alert.toString());
        }

        Assertions.assertEquals(0, alerts.size());

    }

    @Test
    public void clear1() throws IOException {

        final AlertService alertService = new RedisAlertService(getConfiguration());

        alertService.generateAlert("fp", "id", "context", "docid", FilterType.NER_ENTITY);

        List<Alert> alerts = alertService.getAlerts();

        Assertions.assertEquals(1, alerts.size());

        alertService.clear();

        alerts = alertService.getAlerts();

        for(final Alert alert : alerts) {
            LOGGER.info(alert.toString());
        }

        Assertions.assertEquals(0, alerts.size());

    }

}
