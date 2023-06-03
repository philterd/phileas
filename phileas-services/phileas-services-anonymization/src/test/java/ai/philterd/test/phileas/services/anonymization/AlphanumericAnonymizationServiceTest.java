package ai.philterd.test.phileas.services.anonymization;

import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.services.anonymization.AlphanumericAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AlphanumericAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(AlphanumericAnonymizationServiceTest.class);

    @Test
    public void anonymize1() {

        AnonymizationService anonymizationService = new AlphanumericAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "abcd1234";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Alphanumeric: " + replacement);
        Assertions.assertEquals(token.length(), replacement.length());

    }

    @Test
    public void anonymize2() {

        AnonymizationService anonymizationService = new AlphanumericAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "April 1, 2019";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Alphanumeric: " + replacement);
        Assertions.assertEquals(token.length(), replacement.length());

    }

}
