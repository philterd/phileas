package com.mtnfog.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.lang3.RandomStringUtils;

public class UrlAnonymizationService extends AbstractAnonymizationService {

    public UrlAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);
    }

    @Override
    public String anonymize(String token) {

        return "http://" + RandomStringUtils.randomAlphanumeric(10).toLowerCase() + ".com";

    }

}
