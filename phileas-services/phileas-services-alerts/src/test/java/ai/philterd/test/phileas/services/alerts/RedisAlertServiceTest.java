/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.test.phileas.services.alerts;

import ai.philterd.phileas.model.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Alert;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.alerts.RedisAlertService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class RedisAlertServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(RedisAlertServiceTest.class);

    private RedisServer redisServer;
    private static boolean isExternalRedis = false;

    private PhileasConfiguration getConfiguration() throws IOException {

        final Properties properties = new Properties();

        final String redisHost = System.getenv("PHILTER_REDIS_HOST");
        final String redisPort = System.getenv("PHILTER_REDIS_PORT");
        final String redisSsl = System.getenv("PHILTER_REDIS_SSL");
        final String redisToken = System.getenv("PHILTER_REDIS_AUTH_TOKEN");
        final String redisClustered = System.getenv("PHILTER_REDIS_CLUSTERED");

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

        return new PhileasConfiguration(properties);

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
            redisServer = new RedisServer(31000);
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

        alertService.generateAlert("fp", "id", "context", "docid", FilterType.AGE);

        final List<Alert> alerts = alertService.getAlerts();

        Assertions.assertEquals(1, alerts.size());

    }

    @Test
    public void remove1() throws IOException {

        final AlertService alertService = new RedisAlertService(getConfiguration());

        alertService.generateAlert("fp", "id", "context", "docid", FilterType.AGE);

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

        alertService.generateAlert("fp", "id", "context", "docid", FilterType.AGE);

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
