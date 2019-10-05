package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StateAbbreviationFilter extends RegexFilter implements Serializable {

    private static final List<String> STATES = new LinkedList<String>() {{

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

    public StateAbbreviationFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService) {
        super(FilterType.STATE_ABBREVIATION, strategies, anonymizationService);

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws IOException {

        final List<Span> spans = new LinkedList<>();

        for(final String state : STATES) {

            final Pattern p = Pattern.compile("(?i)\\b" + state + "\\b");
            final Matcher m = p.matcher(input);

            while(m.find()) {

                final String replacement = getReplacement(context, documentId, input, Collections.emptyMap());
                final Span span = Span.make(m.start(), m.end(), FilterType.STATE_ABBREVIATION, context, documentId, 1.0, replacement);

                spans.add(span);

            }

        }

        return spans;

    }

    public static List<String> getStates() {
        return STATES;
    }

}
