package io.philterd.test.phileas.services.anonymization;

import io.philterd.phileas.model.services.AnonymizationService;
import io.philterd.phileas.services.anonymization.CreditCardAnonymizationService;
import io.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CreditCardAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(CreditCardAnonymizationServiceTest.class);

    @Test
    public void anonymize1() {

        AnonymizationService anonymizationService = new CreditCardAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "abcd1234";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Credit Card: " + replacement);
        Assertions.assertNotNull(replacement);
        Assertions.assertTrue(replacement.length() == 16);

    }

    @Test
    public void anonymize2() {

        AnonymizationService anonymizationService = new CreditCardAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "April 1, 2019";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Credit Card: " + replacement);
        Assertions.assertNotNull(replacement);
        Assertions.assertTrue(replacement.length() == 16);

    }

}
