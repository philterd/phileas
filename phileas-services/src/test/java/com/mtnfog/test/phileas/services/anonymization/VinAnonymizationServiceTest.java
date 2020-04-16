package com.mtnfog.test.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.VinAnonymizationService;
import com.mtnfog.phileas.services.cache.anonymization.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class VinAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(VinAnonymizationServiceTest.class);

    @Test
    public void anonymize() {

        AnonymizationService anonymizationService = new VinAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "11111111111111111";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("VIN replacement: " + replacement);

        Assert.assertNotNull(replacement);
        Assert.assertEquals(17, replacement.length());

    }

}
