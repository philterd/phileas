package com.mtnfog.test.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.HospitalAbbreviationAnonymizationService;
import com.mtnfog.phileas.services.cache.anonymization.LocalAnonymizationCacheService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class HospitalAbbreviationAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(HospitalAbbreviationAnonymizationServiceTest.class);

    @Test
    public void anonymize() {

        AnonymizationService anonymizationService = new HospitalAbbreviationAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "Plateau Medical Center";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Hospital abbreviation: " + replacement);

        Assert.assertTrue(StringUtils.isAllUpperCase(replacement));
        Assert.assertEquals(-1, replacement.indexOf(" "));

    }

}
