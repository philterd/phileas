package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StateAbbreviationFilter extends RegexFilter implements Serializable {

    public static List<String> STATES = new LinkedList<>();

    static {

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

    public StateAbbreviationFilter(AnonymizationService anonymizationService) {
        super(FilterType.STATE_ABBREVIATION, anonymizationService);

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws IOException {

        final List<Span> spans = new LinkedList<>();

        for(final String state : STATES) {

            final Pattern p = Pattern.compile("(?i)\\b" + state + "\\b");
            final Matcher m = p.matcher(input);

            while(m.find()) {

                final String replacement = getReplacement(filterProfile, context, documentId, input, Collections.emptyMap());
                final Span span = Span.make(m.start(), m.end(), FilterType.STATE_ABBREVIATION, context, documentId, 1.0, replacement);

                spans.add(span);

            }

        }

        return spans;

    }

}
