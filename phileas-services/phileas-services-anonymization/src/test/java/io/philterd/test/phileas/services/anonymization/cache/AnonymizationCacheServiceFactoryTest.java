package io.philterd.test.phileas.services.anonymization.cache;

import io.philterd.phileas.configuration.PhileasConfiguration;
import io.philterd.phileas.model.services.AnonymizationCacheService;
import io.philterd.phileas.services.anonymization.cache.AnonymizationCacheServiceFactory;
import io.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

public class AnonymizationCacheServiceFactoryTest {

    @Test
    public void useLocalAsDefault() throws Exception {

        final Properties properties = new Properties();

        final AnonymizationCacheService anonymizationCacheService = AnonymizationCacheServiceFactory.getAnonymizationCacheService(getConfiguration());

        Assertions.assertTrue(anonymizationCacheService instanceof LocalAnonymizationCacheService);

    }

    private PhileasConfiguration getConfiguration() {

        final Properties properties = new Properties();

        return ConfigFactory.create(PhileasConfiguration.class, properties);

    }

}
