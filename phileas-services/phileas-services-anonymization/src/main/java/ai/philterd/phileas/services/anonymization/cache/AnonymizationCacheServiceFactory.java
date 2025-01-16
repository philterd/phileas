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
package ai.philterd.phileas.services.anonymization.cache;

import ai.philterd.phileas.model.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.services.AnonymizationCacheService;
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
