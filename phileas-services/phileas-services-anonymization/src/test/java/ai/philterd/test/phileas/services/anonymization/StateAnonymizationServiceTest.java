package ai.philterd.test.phileas.services.anonymization;

import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.services.anonymization.StateAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StateAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(StateAnonymizationServiceTest.class);

    @Test
    public void anonymize1() {

        AnonymizationService anonymizationService = new StateAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "abcd1234";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("State: " + replacement);
        Assertions.assertNotNull(replacement);
        Assertions.assertTrue(replacement.length() > 0);

    }

}
