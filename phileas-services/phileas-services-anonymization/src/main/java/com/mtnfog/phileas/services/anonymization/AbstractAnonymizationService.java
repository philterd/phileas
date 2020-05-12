package com.mtnfog.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import com.mtnfog.phileas.model.services.AnonymizationService;

public abstract class AbstractAnonymizationService implements AnonymizationService {

    private AnonymizationCacheService anonymizationCacheService;

    public AbstractAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        this.anonymizationCacheService = anonymizationCacheService;
    }

    @Override
    public AnonymizationCacheService getAnonymizationCacheService() {
        return anonymizationCacheService;
    }

}
