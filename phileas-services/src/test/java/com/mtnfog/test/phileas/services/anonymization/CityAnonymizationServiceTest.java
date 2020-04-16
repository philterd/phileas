package com.mtnfog.test.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.CityAnonymizationService;
import com.mtnfog.phileas.services.cache.anonymization.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class CityAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(CityAnonymizationServiceTest.class);

    @Test
    public void anonymize1() {

        AnonymizationService anonymizationService = new CityAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "abcd1234";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("City: " + replacement);
        Assert.assertNotNull(replacement);
        Assert.assertTrue(replacement.length() > 0);

    }

    @Test
    public void anonymize2() {

        AnonymizationService anonymizationService = new CityAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "April 1, 2019";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("City: " + replacement);
        Assert.assertNotNull(replacement);
        Assert.assertTrue(replacement.length() > 0);

    }

}
