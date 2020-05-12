package com.mtnfog.test.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.StateAbbreviationAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class StateAbbreviationAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(StateAbbreviationAnonymizationServiceTest.class);

    @Test
    public void anonymize1() {

        AnonymizationService anonymizationService = new StateAbbreviationAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "AK";
        final String replacement = anonymizationService.anonymize(token);

        Assert.assertNotNull(replacement);
        Assert.assertTrue(StateAbbreviationAnonymizationService.STATES.contains(replacement));
        Assert.assertNotEquals(token, replacement);

    }

}
