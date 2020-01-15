package com.mtnfog.phileas.services.cache;

import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

/**
 * Factory methods for getting an {@link AnonymizationCacheService}.
 */
public class AnonymizationCacheServiceFactory {

    private static final Logger LOGGER = LogManager.getLogger(AnonymizationCacheServiceFactory.class);

    private AnonymizationCacheServiceFactory() {
        // This is a utility class.
    }

    /**
     * Gets a configured {@link AnonymizationCacheService}.
     * @param properties Philter configuration {@link Properties}.
     * @return a configured {@link AnonymizationCacheService}.
     */
    public static AnonymizationCacheService getAnonymizationCacheService(Properties properties) {

        AnonymizationCacheService anonymizationCacheService = null;

        if(StringUtils.equalsIgnoreCase(properties.getProperty("anonymization.cache.service.redis.enabled"), "true")) {

            LOGGER.info("Configuring connection to Redis for anonymization cache service.");

            final String host = properties.getProperty("anonymization.cache.service.redis.host");
            final int port = Integer.parseInt(properties.getProperty("anonymization.cache.service.redis.port", "6379"));
            final String authToken = properties.getProperty("anonymization.cache.service.redis.auth.token", "");
            final String trustStore = properties.getProperty("anonymization.cache.service.redis.truststore", "");

            try {

                if(StringUtils.isEmpty(trustStore)) {
                    anonymizationCacheService = new RedisAnonymizationCacheService(host, port, authToken);
                } else {
                    LOGGER.info("Configuring redis client with truststore {}", trustStore);
                    anonymizationCacheService = new RedisAnonymizationCacheService(host, port, authToken, trustStore);
                }

            } catch (Exception ex) {

                LOGGER.error("Unable to initialize the Redis cache client. A local anonymization cache service will be used instead.", ex);
                anonymizationCacheService = new LocalAnonymizationCacheService();

            }

        } else {

            LOGGER.info("Using local anonymization cache service.");
            anonymizationCacheService = new LocalAnonymizationCacheService();

        }

        return anonymizationCacheService;

    }

}
