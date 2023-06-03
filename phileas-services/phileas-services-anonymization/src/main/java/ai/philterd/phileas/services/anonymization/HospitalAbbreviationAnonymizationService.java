package ai.philterd.phileas.services.anonymization;

import com.github.javafaker.Faker;
import ai.philterd.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.text.WordUtils;

public class HospitalAbbreviationAnonymizationService extends AbstractAnonymizationService {

    private transient Faker faker;

    public HospitalAbbreviationAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);
        this.faker = new Faker();
    }

    @Override
    public String anonymize(String token) {

        final String hopspitalName = faker.address().cityName() + " General Hospital";

        return WordUtils.initials(hopspitalName);

    }

}
