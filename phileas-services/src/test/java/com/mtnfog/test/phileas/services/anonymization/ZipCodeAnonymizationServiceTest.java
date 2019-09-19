package com.mtnfog.test.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.ZipCodeAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

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
