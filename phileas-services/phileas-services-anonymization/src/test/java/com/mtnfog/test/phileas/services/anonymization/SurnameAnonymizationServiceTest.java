package com.mtnfog.test.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.SurnameAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SurnameAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(SurnameAnonymizationServiceTest.class);

    @Test
    public void anonymize1() {

        AnonymizationService anonymizationService = new SurnameAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "abcd1234";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Surname: " + replacement);
        Assertions.assertNotNull(replacement);
        Assertions.assertTrue(replacement.length() > 0);

    }

    @Test
    public void anonymize2() {

        AnonymizationService anonymizationService = new SurnameAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "April 1, 2019";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Surname: " + replacement);
        Assertions.assertNotNull(replacement);
        Assertions.assertTrue(replacement.length() > 0);

    }

}
