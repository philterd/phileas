package com.mtnfog.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.lang3.StringUtils;

import java.util.Random;

public class StreetAddressAnonymizationService extends AbstractAnonymizationService {

    private Random random;

    public StreetAddressAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);
        this.random = new Random();
    }

    @Override
    public String anonymize(String token) {

        // TODO: Provide street address anonymization.

        final String anonymized = token;

        return anonymized;

    }

}
