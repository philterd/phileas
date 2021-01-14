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

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StreetAddressFilter extends RegexFilter {

    public StreetAddressFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Set<String> ignoredFiles, List<IgnoredPattern> ignoredPatterns, Crypto crypto, int windowSize) {
        super(FilterType.STREET_ADDRESS, strategies, anonymizationService, alertService, ignored, ignoredFiles, ignoredPatterns, crypto, windowSize);

        final Pattern addressPattern = Pattern.compile("\\b\\d{1,6} +.{2,25}\\b(avenue|ave|court|ct|street|st|drive|dr|lane|ln|road|rd|blvd|plaza|parkway|pkwy)[.,]?(.{0,25} +\\b\\d{5}\\b)?", Pattern.CASE_INSENSITIVE);
        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(addressPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("address");
        this.contextualTerms.add("location");

        this.analyzer = new Analyzer(contextualTerms, filterPattern);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        return new FilterResult(context, documentId, spans);

    }

}
