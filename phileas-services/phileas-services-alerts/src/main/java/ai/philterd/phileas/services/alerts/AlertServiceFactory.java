package ai.philterd.phileas.services.alerts;

import ai.philterd.phileas.configuration.PhileasConfiguration;
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
