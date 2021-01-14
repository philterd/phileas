package com.mtnfog.test.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.AgeAnonymizationService;
import com.mtnfog.phileas.services.anonymization.StreetAddressAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StreetAddressAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(StreetAddressAnonymizationServiceTest.class);

    @Test
    public void anonymize1() {

        AnonymizationService anonymizationService = new StreetAddressAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "100 Main St";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Street Address: " + replacement);
        Assertions.assertNotEquals(token, replacement);

    }

    @Test
    public void anonymize2() {

        AnonymizationService anonymizationService = new StreetAddressAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "1000 Main St";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Street Address: " + replacement);
        Assertions.assertNotEquals(token, replacement);

    }

}
