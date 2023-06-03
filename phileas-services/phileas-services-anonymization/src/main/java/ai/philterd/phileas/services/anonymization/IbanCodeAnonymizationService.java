package ai.philterd.phileas.services.anonymization;

import ai.philterd.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.lang3.RandomStringUtils;

public class IbanCodeAnonymizationService extends AbstractAnonymizationService {

    public IbanCodeAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);
    }

    @Override
    public String anonymize(String token) {

        return RandomStringUtils.randomAlphanumeric(34);

    }

}
