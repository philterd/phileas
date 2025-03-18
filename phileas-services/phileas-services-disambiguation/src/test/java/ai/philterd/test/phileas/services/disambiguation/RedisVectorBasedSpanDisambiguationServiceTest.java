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
package ai.philterd.test.phileas.services.disambiguation;

import ai.philterd.phileas.model.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.services.disambiguation.VectorBasedSpanDisambiguationService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Disabled
public class RedisVectorBasedSpanDisambiguationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(RedisVectorBasedSpanDisambiguationServiceTest.class);

    private RedisServer redisServer;

    @BeforeEach
    public void before() throws IOException {

        redisServer = new RedisServer(31000);
        redisServer.start();

    }

    @AfterEach
    public void after() throws IOException {

        redisServer.stop();

    }

    @Test
    public void disambiguateWithRedis1() throws IOException {

        final Properties properties = getProperties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final String context = "c";

        final VectorBasedSpanDisambiguationService vectorBasedSpanDisambiguationService = new VectorBasedSpanDisambiguationService(phileasConfiguration);

        final Span span1 = Span.make(0, 4, FilterType.SSN, context, "d", 0.00, "123-45-6789", "000-00-0000",  "", false, true, new String[]{"ssn", "was", "he", "id"}, 0);
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span1);

        final Span span = Span.make(0, 4, FilterType.SSN, context, "d", 0.00, "123-45-6789", "000-00-0000",  "", false, true, new String[]{"ssn", "asdf", "he", "was"}, 0);
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span);

        final Span span2 = Span.make(0, 4, FilterType.PHONE_NUMBER, "c", "d", 0.00, "123-45-6789", "000-00-0000",  "", false, true, new String[]{"phone", "number", "she", "had"}, 0);
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span2);

        final List<FilterType> filterTypes = Arrays.asList(span1.getFilterType(), span2.getFilterType());

        final Span ambiguousSpan = Span.make(0, 4, FilterType.PHONE_NUMBER, "c", "d", 0.00, "123-45-6789", "000-00-0000",  "", false, true, new String[]{"phone", "number", "called", "is"}, 0);
        final FilterType filterType = vectorBasedSpanDisambiguationService.disambiguate(context, filterTypes, ambiguousSpan);

        Assertions.assertEquals(FilterType.PHONE_NUMBER, filterType);

    }

    private Properties getProperties() {

        final Properties properties = new Properties();

        properties.setProperty("span.disambiguation.enabled", "true");
        properties.setProperty("span.disambiguation.ignore.stopwords", "false");
        properties.setProperty("span.disambiguation.vector.size", "32");

        properties.setProperty("cache.redis.enabled", "true");

        final String redisHost = System.getenv("PHILTER_REDIS_HOST");
        final String redisPort = System.getenv("PHILTER_REDIS_PORT");
        final String redisSsl = System.getenv("PHILTER_REDIS_SSL");
        final String redisToken = System.getenv("PHILTER_REDIS_AUTH_TOKEN");
        final String redisClustered = System.getenv("PHILTER_REDIS_CLUSTERED");

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

        return properties;

    }

}
