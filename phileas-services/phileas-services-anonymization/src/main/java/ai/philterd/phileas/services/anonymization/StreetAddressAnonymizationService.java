package ai.philterd.phileas.services.anonymization;

import com.github.javafaker.Faker;
import ai.philterd.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.lang3.StringUtils;

import java.util.Random;

public class StreetAddressAnonymizationService extends AbstractAnonymizationService {

    private transient Faker faker;

    public StreetAddressAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);
        this.faker = new Faker();
    }

    @Override
    public String anonymize(String token) {

        return faker.address().streetAddress();

    }

}
