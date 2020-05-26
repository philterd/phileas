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

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

public class SsnFilter extends RegexFilter implements Serializable {

    public SsnFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.SSN, strategies, anonymizationService, alertService, ignored, crypto, windowSize);

        final Pattern SSN_REGEX = Pattern.compile("\\b(?!000|666)[0-8][0-9]{2}[- ]?(?!00)[0-9]{2}[- ]?(?!0000)[0-9]{4}\\b");
        final FilterPattern ssn1 = new FilterPattern(SSN_REGEX, 0.90);

        final Pattern TIN_REGEX = Pattern.compile("\\b\\d{2}-\\d{7}\\b");
        final FilterPattern tin1 = new FilterPattern(TIN_REGEX, 0.90);

        this.contextualTerms = new HashSet<>(){{
            add("ssn");
            add("tin");
            add("social");
            add("ssid");
        }};

        this.analyzer = new Analyzer(contextualTerms, ssn1, tin1);

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        return findSpans(filterProfile, analyzer, input, context, documentId);

    }

}
