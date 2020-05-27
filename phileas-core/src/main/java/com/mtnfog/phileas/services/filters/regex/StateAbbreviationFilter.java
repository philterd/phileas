package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StateAbbreviationFilter extends RegexFilter implements Serializable {

    private static final List<String> STATES = new LinkedList<>() {{

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

    public StateAbbreviationFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.STATE_ABBREVIATION, strategies, anonymizationService, alertService, ignored, crypto, windowSize);

        final List<FilterPattern> filterPatterns = new LinkedList<>();

        for(final String state : STATES) {

            final Pattern STATE_REGEX = Pattern.compile("(?i)\\b" + state + "\\b");
            filterPatterns.add(new FilterPattern(STATE_REGEX, 0.25));

        }

        // No contextual terms.
        this.analyzer = new Analyzer(filterPatterns);

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        final List<Span> spans = new LinkedList<>();

        for(final String state : STATES) {

            final Pattern p = Pattern.compile("(?i)\\b" + state + "\\b");
            final Matcher m = p.matcher(input);

            while(m.find()) {

                final String[] window = getWindow(input, m.start(), m.end());
                final String token = m.group();
                final String replacement = getReplacement(filterProfile.getName(), label, context, documentId, token, Collections.emptyMap());
                final boolean isIgnored = ignored.contains(token);
                final Span span = Span.make(m.start(), m.end(), FilterType.STATE_ABBREVIATION, context, documentId, 1.0, token, replacement, isIgnored, window);

                spans.add(span);

            }

        }

        return spans;

    }

    public static List<String> getStates() {
        return STATES;
    }

}
