package com.mtnfog.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.lang3.RandomStringUtils;

public class EmailAddressAnonymizationService extends AbstractAnonymizationService {

    public EmailAddressAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);
    }

    @Override
    public String anonymize(String token) {

        return RandomStringUtils.randomAlphanumeric(10) + "@fake.com";

    }

}
