package com.mtnfog.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.lang3.RandomStringUtils;

public class BitcoinAddressAnonymizationService extends AbstractAnonymizationService {

    public BitcoinAddressAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);
    }

    @Override
    public String anonymize(String token) {

        // TODO: Generate valid Bitcoin address?
        return RandomStringUtils.randomAlphanumeric(32);

    }

}
