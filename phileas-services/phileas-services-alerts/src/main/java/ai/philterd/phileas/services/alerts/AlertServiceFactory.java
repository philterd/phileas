/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services.alerts;

import ai.philterd.phileas.model.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.services.AlertService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class AlertServiceFactory {

    private static final Logger LOGGER = LogManager.getLogger(AlertServiceFactory.class);

    private AlertServiceFactory() {
        // Use the static methods.
    }

    public static AlertService getAlertService(PhileasConfiguration phileasConfiguration) throws IOException {

        final boolean redisEnabled = phileasConfiguration.cacheRedisEnabled();

        if(redisEnabled) {

            LOGGER.info("Initializing Redis anonymization cache service.");

            return new RedisAlertService(phileasConfiguration);

        } else {

            LOGGER.info("Initializing local anonymization cache service.");

            return new LocalAlertService();

        }

    }

}
