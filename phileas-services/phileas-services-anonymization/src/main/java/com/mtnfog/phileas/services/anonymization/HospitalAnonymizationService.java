package com.mtnfog.phileas.services.anonymization;

import com.github.javafaker.Faker;
import com.mtnfog.phileas.model.services.AnonymizationCacheService;

public class HospitalAnonymizationService extends AbstractAnonymizationService {

    private transient Faker faker;

    public HospitalAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);
        this.faker = new Faker();
    }

    @Override
    public String anonymize(String token) {

        return faker.address().cityName() + " General Hospital";

    }

}