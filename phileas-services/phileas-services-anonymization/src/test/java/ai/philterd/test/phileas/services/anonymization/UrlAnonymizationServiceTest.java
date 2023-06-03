package ai.philterd.test.phileas.services.anonymization;

import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.services.anonymization.UrlAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class UrlAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(UrlAnonymizationServiceTest.class);

    @Test
    public void anonymize() {

        AnonymizationService anonymizationService = new UrlAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "http://www.cnn.com";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("URL replacement: " + replacement);

    }

}