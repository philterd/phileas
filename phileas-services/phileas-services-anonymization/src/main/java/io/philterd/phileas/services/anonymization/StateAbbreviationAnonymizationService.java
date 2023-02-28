package io.philterd.phileas.services.anonymization;

import io.philterd.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;

public class StateAbbreviationAnonymizationService extends AbstractAnonymizationService {

    private final List<String> STATES;

    public StateAbbreviationAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);

        this.STATES = new LinkedList<>();

        // TODO: Don't duplicate this from StateFilter.
        STATES.add("AL");
        STATES.add("AK");
        STATES.add("AZ");
        STATES.add("AR");
        STATES.add("CA");
        STATES.add("CO");
        STATES.add("CT");
        STATES.add("DE");
        STATES.add("FL");
        STATES.add("GA");
        STATES.add("HI");
        STATES.add("ID");
        STATES.add("IL");
        STATES.add("IN");
        STATES.add("IA");
        STATES.add("KS");
        STATES.add("KY");
        STATES.add("LA");
        STATES.add("ME");
        STATES.add("MD");
        STATES.add("MA");
        STATES.add("MI");
        STATES.add("MN");
        STATES.add("MS");
        STATES.add("MO");
        STATES.add("MT");
        STATES.add("NE");
        STATES.add("NV");
        STATES.add("NH");
        STATES.add("NJ");
        STATES.add("NM");
        STATES.add("NY");
        STATES.add("NC");
        STATES.add("ND");
        STATES.add("OH");
        STATES.add("OK");
        STATES.add("OR");
        STATES.add("PA");
        STATES.add("RI");
        STATES.add("SC");
        STATES.add("SD");
        STATES.add("TN");
        STATES.add("TX");
        STATES.add("UT");
        STATES.add("VT");
        STATES.add("VA");
        STATES.add("WA");
        STATES.add("WV");
        STATES.add("WI");
        STATES.add("WY");

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
