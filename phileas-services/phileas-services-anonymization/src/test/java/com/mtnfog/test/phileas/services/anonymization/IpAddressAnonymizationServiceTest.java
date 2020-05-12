package com.mtnfog.test.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.IpAddressAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

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
