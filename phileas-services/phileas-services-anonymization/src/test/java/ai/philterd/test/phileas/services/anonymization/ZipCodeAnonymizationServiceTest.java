package ai.philterd.test.phileas.services.anonymization;

import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.services.anonymization.ZipCodeAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class ZipCodeAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(ZipCodeAnonymizationServiceTest.class);

    @Test
    public void anonymize() {

        AnonymizationService anonymizationService = new ZipCodeAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "90210";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Zip code replacement: " + replacement);

    }

}
