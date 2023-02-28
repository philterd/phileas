package io.philterd.test.phileas.services.anonymization;

import io.philterd.phileas.model.services.AnonymizationService;
import io.philterd.phileas.services.anonymization.StateAbbreviationAnonymizationService;
import io.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StateAbbreviationAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(StateAbbreviationAnonymizationServiceTest.class);

    @Test
    public void anonymize1() {

        AnonymizationService anonymizationService = new StateAbbreviationAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "AK";
        final String replacement = anonymizationService.anonymize(token);

        Assertions.assertNotNull(replacement);
        Assertions.assertNotEquals(token, replacement);

    }

}
