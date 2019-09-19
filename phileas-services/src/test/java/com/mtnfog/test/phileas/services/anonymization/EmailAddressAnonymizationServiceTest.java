package com.mtnfog.test.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.EmailAddressAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

public class EmailAddressAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(EmailAddressAnonymizationServiceTest.class);

    @Test
    public void anonymize() {

        AnonymizationService anonymizationService = new EmailAddressAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "me@testemail.com";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Email replacement: " + replacement);

    }

}
