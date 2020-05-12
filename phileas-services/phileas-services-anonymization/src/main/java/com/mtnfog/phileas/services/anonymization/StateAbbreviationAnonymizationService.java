package com.mtnfog.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;

public class StateAbbreviationAnonymizationService extends AbstractAnonymizationService {

    // TODO: Don't duplicate this from StateFilter.
    public static final List<String> STATES = new LinkedList<>() {{

        add("AL");
        add("AK");
        add("AZ");
        add("AR");
        add("CA");
        add("CO");
        add("CT");
        add("DE");
        add("FL");
        add("GA");
        add("HI");
        add("ID");
        add("IL");
        add("IN");
        add("IA");
        add("KS");
        add("KY");
        add("LA");
        add("ME");
        add("MD");
        add("MA");
        add("MI");
        add("MN");
        add("MS");
        add("MO");
        add("MT");
        add("NE");
        add("NV");
        add("NH");
        add("NJ");
        add("NM");
        add("NY");
        add("NC");
        add("ND");
        add("OH");
        add("OK");
        add("OR");
        add("PA");
        add("RI");
        add("SC");
        add("SD");
        add("TN");
        add("TX");
        add("UT");
        add("VT");
        add("VA");
        add("WA");
        add("WV");
        add("WI");
        add("WY");

    }};

    public StateAbbreviationAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);
    }

    @Override
    public String anonymize(String token) {

        final String anonymized = STATES.stream()
                .skip((int) (STATES.size() * Math.random()))
                .findFirst().orElse("AK");

        // Make sure the anonymized and the token aren't the same.
        if(StringUtils.equalsIgnoreCase(token, anonymized)) {
            return anonymize(token);
        }

        return anonymized;

    }

}
