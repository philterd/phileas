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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class BitcoinAddressFilter extends RegexFilter implements Serializable {

    public BitcoinAddressFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.BITCOIN_ADDRESS, strategies, anonymizationService, alertService, ignored, crypto, windowSize);

        final Pattern BITCOIN_ADDRESS_REGEX = Pattern.compile("\\b[13][a-km-zA-HJ-NP-Z1-9]{25,34}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern bitcoin1 = new FilterPattern(BITCOIN_ADDRESS_REGEX, 0.90);

        this.contextualTerms = new HashSet<>(){{
            add("bitcoin");
            add("wallet");
            add("btc");
            add("crypto");
        }};

        this.analyzer = new Analyzer(contextualTerms, bitcoin1);

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        return findSpans(filterProfile, analyzer, input, context, documentId);

    }

}