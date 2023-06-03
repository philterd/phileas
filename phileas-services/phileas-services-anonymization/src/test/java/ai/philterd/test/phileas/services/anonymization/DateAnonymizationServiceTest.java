package ai.philterd.test.phileas.services.anonymization;

import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.services.anonymization.DateAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class DateAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(DateAnonymizationServiceTest.class);

    @Test
    public void anonymize1() {

        AnonymizationService anonymizationService = new DateAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "11-18-2018";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Date: " + replacement);

    }

    @Test
    public void anonymize2() {

        AnonymizationService anonymizationService = new DateAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "April 1, 2019";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Date: " + replacement);

    }

}
