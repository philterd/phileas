package ai.philterd.test.phileas.services.anonymization;

import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.services.anonymization.HospitalAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class HospitalAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(HospitalAnonymizationServiceTest.class);

    @Test
    public void anonymize() {

        AnonymizationService anonymizationService = new HospitalAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "Plateau Medical Center";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Hospital: " + replacement);
        
    }

}
