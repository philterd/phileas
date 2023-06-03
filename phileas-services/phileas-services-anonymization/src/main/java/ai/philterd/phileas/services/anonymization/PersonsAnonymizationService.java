package ai.philterd.phileas.services.anonymization;

import com.github.javafaker.Faker;
import ai.philterd.phileas.model.services.AnonymizationCacheService;

public class PersonsAnonymizationService extends AbstractAnonymizationService {

    private transient Faker faker;

    public PersonsAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);
        this.faker = new Faker();
    }

    @Override
    public String anonymize(String token) {

        return faker.name().firstName() + " " + faker.name().lastName();

    }

}
