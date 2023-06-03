package ai.philterd.phileas.services.anonymization;

import com.github.javafaker.Faker;
import ai.philterd.phileas.model.services.AnonymizationCacheService;

public class LocationsAnonymizationService extends AbstractAnonymizationService {

    private transient Faker faker;

    public LocationsAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);
        this.faker = new Faker();
    }

    @Override
    public String anonymize(String token) {

        return faker.address().cityName();

    }

}
