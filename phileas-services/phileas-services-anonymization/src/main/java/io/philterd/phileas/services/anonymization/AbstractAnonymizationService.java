package io.philterd.phileas.services.anonymization;

import io.philterd.phileas.model.services.AnonymizationCacheService;
import io.philterd.phileas.model.services.AnonymizationService;

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
