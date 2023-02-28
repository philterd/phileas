package io.philterd.test.phileas.services.anonymization;

import io.philterd.phileas.model.services.AnonymizationService;
import io.philterd.phileas.services.anonymization.EmailAddressAnonymizationService;
import io.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

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
