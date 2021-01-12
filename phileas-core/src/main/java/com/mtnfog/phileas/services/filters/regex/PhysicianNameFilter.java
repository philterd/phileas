package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
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

public class PhysicianNameFilter extends RegexFilter {

    public PhysicianNameFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Set<String> ignoredFiles, List<IgnoredPattern> ignoredPatterns, Crypto crypto, int windowSize) {
        super(FilterType.PHYSICIAN_NAME, strategies, anonymizationService, alertService, ignored, ignoredFiles, ignoredPatterns, crypto, windowSize);

        // \b[A-Z][A-Za-z'\s+]+,[A-Z][A-Za-z'\s+]+,(MD|PhD)\b

        // TODO: Set the pattern here.
        final Pattern bitcoinPattern = Pattern.compile("\\b[13][a-km-zA-HJ-NP-Z1-9]{25,34}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern bitcoin1 = new FilterPattern.FilterPatternBuilder(bitcoinPattern, 0.90).build();

        // TODO: Set the contextual terms.
        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("bitcoin");
        this.contextualTerms.add("wallet");
        this.contextualTerms.add("btc");
        this.contextualTerms.add("crypto");

        this.analyzer = new Analyzer(contextualTerms, bitcoin1);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        return new FilterResult(context, documentId, spans);

    }

}