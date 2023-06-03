package ai.philterd.test.phileas.services.anonymization;

import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.services.anonymization.AgeAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AgeAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(AgeAnonymizationServiceTest.class);

    @Test
    public void anonymize1() {

        AnonymizationService anonymizationService = new AgeAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "3.5yrs";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Age: " + replacement);
        Assertions.assertEquals(token.length(), replacement.length());

    }

    @Test
    public void anonymize2() {

        AnonymizationService anonymizationService = new AgeAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "18 years old";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Age: " + replacement);
        Assertions.assertEquals(token.length(), replacement.length());

    }

}
