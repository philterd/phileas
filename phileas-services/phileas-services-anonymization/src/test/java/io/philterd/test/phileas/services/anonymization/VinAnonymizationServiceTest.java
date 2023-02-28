package io.philterd.test.phileas.services.anonymization;

import io.philterd.phileas.model.services.AnonymizationService;
import io.philterd.phileas.services.anonymization.VinAnonymizationService;
import io.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VinAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(VinAnonymizationServiceTest.class);

    @Test
    public void anonymize() {

        AnonymizationService anonymizationService = new VinAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "11111111111111111";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("VIN replacement: " + replacement);

        Assertions.assertNotNull(replacement);
        Assertions.assertEquals(17, replacement.length());

    }

}
