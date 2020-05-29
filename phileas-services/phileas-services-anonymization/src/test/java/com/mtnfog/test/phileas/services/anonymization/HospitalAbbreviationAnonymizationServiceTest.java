package com.mtnfog.test.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.HospitalAbbreviationAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HospitalAbbreviationAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(HospitalAbbreviationAnonymizationServiceTest.class);

    @Test
    public void anonymize() {

        AnonymizationService anonymizationService = new HospitalAbbreviationAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "Plateau Medical Center";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Hospital abbreviation: " + replacement);

        Assertions.assertTrue(StringUtils.isAllUpperCase(replacement));
        Assertions.assertEquals(-1, replacement.indexOf(" "));

    }

}
