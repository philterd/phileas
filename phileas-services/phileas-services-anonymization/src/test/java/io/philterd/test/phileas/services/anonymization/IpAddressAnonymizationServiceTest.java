package io.philterd.test.phileas.services.anonymization;

import io.philterd.phileas.model.services.AnonymizationService;
import io.philterd.phileas.services.anonymization.IpAddressAnonymizationService;
import io.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class IpAddressAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(IpAddressAnonymizationServiceTest.class);

    @Test
    public void anonymizeIPv4() {

        AnonymizationService anonymizationService = new IpAddressAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "192.168.1.1";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("IP replacement: " + replacement);

    }

    @Test
    public void anonymizeIPv6() {

        AnonymizationService anonymizationService = new IpAddressAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "2001:0db8:85a3:08d3:1319:8a2e:0370:7344";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("IP replacement: " + replacement);

    }

}
