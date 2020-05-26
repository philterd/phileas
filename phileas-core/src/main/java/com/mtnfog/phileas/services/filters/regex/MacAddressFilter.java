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
import java.util.*;
import java.util.regex.Pattern;

public class MacAddressFilter extends RegexFilter implements Serializable {

    public MacAddressFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.MAC_ADDRESS, strategies, anonymizationService, alertService, ignored, crypto, windowSize);

        final Pattern MAC_ADDRESS_PATTERN = Pattern.compile("\\b([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})\\b");
        final FilterPattern macAddress1 = new FilterPattern(MAC_ADDRESS_PATTERN, 0.90);

        this.contextualTerms = new HashSet<>(){{
            add("mac");
            add("network");
        }};

        this.analyzer = new Analyzer(contextualTerms, macAddress1);

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        return findSpans(filterProfile, analyzer, input, context, documentId);

    }

}
