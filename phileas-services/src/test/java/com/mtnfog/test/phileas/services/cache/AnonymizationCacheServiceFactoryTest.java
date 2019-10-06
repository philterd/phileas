package com.mtnfog.test.phileas.services.cache;

import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import com.mtnfog.phileas.services.cache.AnonymizationCacheServiceFactory;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.cache.RedisAnonymizationCacheService;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class AnonymizationCacheServiceFactoryTest {

    @Test
    public void useLocalAsDefault() {

        final Properties properties = new Properties();

        final AnonymizationCacheService anonymizationCacheService = AnonymizationCacheServiceFactory.getAnonymizationCacheService(properties);

        Assert.assertTrue(anonymizationCacheService instanceof LocalAnonymizationCacheService);

    }

    @Test
    public void useRedis() {

        final Properties properties = new Properties();

        properties.setProperty("anonymization.cache.service", "redis");
        properties.setProperty("anonymization.cache.service.host", "localhost");
        properties.setProperty("anonymization.cache.service.host", "6379");
        properties.setProperty("anonymization.cache.service.ssl", "true");

        final AnonymizationCacheService anonymizationCacheService = AnonymizationCacheServiceFactory.getAnonymizationCacheService(properties);

        Assert.assertTrue(anonymizationCacheService instanceof RedisAnonymizationCacheService);

    }

}
