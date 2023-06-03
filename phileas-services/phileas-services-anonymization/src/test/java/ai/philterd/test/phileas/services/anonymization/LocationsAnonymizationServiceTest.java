package ai.philterd.test.phileas.services.anonymization;

import ai.philterd.phileas.services.anonymization.LocationsAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class LocationsAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(IpAddressAnonymizationServiceTest.class);

    @Test
    public void anonymizeLocation1() {

        LocationsAnonymizationService anonymizationService = new LocationsAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "Morgantown";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Location replacement: " + replacement);

    }

}
