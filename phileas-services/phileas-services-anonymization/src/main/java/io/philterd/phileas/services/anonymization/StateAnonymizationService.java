package io.philterd.phileas.services.anonymization;

import com.github.javafaker.Faker;
import io.philterd.phileas.model.services.AnonymizationCacheService;

public class StateAnonymizationService extends AbstractAnonymizationService {

    private transient Faker faker;

    public StateAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);
        this.faker = new Faker();
    }

    @Override
    public String anonymize(String token) {

        return faker.address().state();

    }

}
