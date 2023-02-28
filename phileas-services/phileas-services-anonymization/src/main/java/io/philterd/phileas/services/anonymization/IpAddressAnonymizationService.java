package io.philterd.phileas.services.anonymization;

import io.philterd.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.lang3.RandomStringUtils;

public class IpAddressAnonymizationService extends AbstractAnonymizationService {

    public IpAddressAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);
    }

    @Override
    public String anonymize(String token) {

        if(token.contains(":")) {

            // IPv6

            return RandomStringUtils.randomNumeric(4) + ":"
                    + RandomStringUtils.randomNumeric(4) + ":"
                    + RandomStringUtils.randomNumeric(4) + ":"
                    + RandomStringUtils.randomNumeric(4) + ":"
                    + RandomStringUtils.randomNumeric(4) + ":"
                    + RandomStringUtils.randomNumeric(4) + ":"
                    + RandomStringUtils.randomNumeric(4) + ":"
                    + RandomStringUtils.randomNumeric(4);

        } else {

            // IPv4

            return RandomStringUtils.randomNumeric(3) + "."
                    + RandomStringUtils.randomNumeric(3) + "."
                    + RandomStringUtils.randomNumeric(3) + "."
                    + RandomStringUtils.randomNumeric(3);

        }

    }

}
