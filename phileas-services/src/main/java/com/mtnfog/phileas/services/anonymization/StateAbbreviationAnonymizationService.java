package com.mtnfog.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.StateAbbreviationFilter;
import org.apache.commons.lang3.StringUtils;

public class StateAbbreviationAnonymizationService extends AbstractAnonymizationService {

    public StateAbbreviationAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);
    }

    @Override
    public String anonymize(String token) {

        final String anonymized = StateAbbreviationFilter.STATES.stream()
                .skip((int) (StateAbbreviationFilter.STATES.size() * Math.random()))
                .findFirst().orElse("AK");

        // Make sure the anonymized and the token aren't the same.
        if(StringUtils.equalsIgnoreCase(token, anonymized)) {
            return anonymize(token);
        }

        return anonymized;

    }

}
