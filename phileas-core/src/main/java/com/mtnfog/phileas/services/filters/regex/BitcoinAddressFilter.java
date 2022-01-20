package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.FilterConfiguration;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.IgnoredPattern;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class BitcoinAddressFilter extends RegexFilter {

    public BitcoinAddressFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.BITCOIN_ADDRESS, filterConfiguration);

        final Pattern bitcoinPattern = Pattern.compile("\\b[13][a-km-zA-HJ-NP-Z1-9]{25,34}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern bitcoin1 = new FilterPattern.FilterPatternBuilder(bitcoinPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("bitcoin");
        this.contextualTerms.add("wallet");
        this.contextualTerms.add("btc");
        this.contextualTerms.add("crypto");

        this.analyzer = new Analyzer(contextualTerms, bitcoin1);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        return new FilterResult(context, documentId, spans);

    }

}