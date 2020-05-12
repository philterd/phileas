package com.mtnfog.test.phileas.services.anonymization.cache;

import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import com.mtnfog.phileas.services.anonymization.cache.AnonymizationCacheServiceFactory;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.aeonbits.owner.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class AnonymizationCacheServiceFactoryTest {

    @Test
    public void useLocalAsDefault() throws Exception {

        final Properties properties = new Properties();

        final AnonymizationCacheService anonymizationCacheService = AnonymizationCacheServiceFactory.getAnonymizationCacheService(getConfiguration());

        Assert.assertTrue(anonymizationCacheService instanceof LocalAnonymizationCacheService);

    }

    private PhileasConfiguration getConfiguration() {

        final Properties properties = new Properties();

        return ConfigFactory.create(PhileasConfiguration.class, properties);

    }

}
