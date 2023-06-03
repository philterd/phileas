package ai.philterd.test.phileas.services.anonymization;

import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.services.anonymization.MacAddressAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class MacAddressAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(MacAddressAnonymizationServiceTest.class);

    @Test
    public void anonymize1() {

        AnonymizationService anonymizationService = new MacAddressAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "00-14-22-04-25-37";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("MAC address replacement: " + replacement);

    }

}
