package com.mtnfog.phileas.services.anonymization.cache;

import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

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
     * @param phileasConfiguration Philter configuration {@link PhileasConfiguration}.
     * @return a configured {@link AnonymizationCacheService}.
     */
    public static AnonymizationCacheService getAnonymizationCacheService(PhileasConfiguration phileasConfiguration) throws IOException {

        final boolean redisEnabled = phileasConfiguration.cacheRedisEnabled();

        if(redisEnabled) {

            LOGGER.info("Initializing Redis anonymization cache service.");

            return new RedisAnonymizationCacheService(phileasConfiguration);

        } else {

            LOGGER.info("Initializing local anonymization cache service.");

            return new LocalAnonymizationCacheService();

        }

    }

}
