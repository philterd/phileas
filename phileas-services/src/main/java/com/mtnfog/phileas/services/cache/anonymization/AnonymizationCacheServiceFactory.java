package com.mtnfog.phileas.services.cache.anonymization;

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
    public static AnonymizationCacheService getAnonymizationCacheService(Properties properties) throws Exception {

        final String redisEnabled = properties.getProperty("cache.redis.enabled", "false");

        if(StringUtils.equalsIgnoreCase(redisEnabled, "true")) {

            LOGGER.info("Initializing Redis anonymization cache service.");

            return new RedisAnonymizationCacheService(properties);

        } else {

            LOGGER.info("Initializing local anonymization cache service.");

            return new LocalAnonymizationCacheService();

        }

    }

}
