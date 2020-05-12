package com.mtnfog.test.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.AgeAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class AgeAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(AgeAnonymizationServiceTest.class);

    @Test
    public void anonymize1() {

        AnonymizationService anonymizationService = new AgeAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "3.5yrs";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Age: " + replacement);
        Assert.assertEquals(token.length(), replacement.length());

    }

    @Test
    public void anonymize2() {

        AnonymizationService anonymizationService = new AgeAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "18 years old";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Age: " + replacement);
        Assert.assertEquals(token.length(), replacement.length());

    }

}
