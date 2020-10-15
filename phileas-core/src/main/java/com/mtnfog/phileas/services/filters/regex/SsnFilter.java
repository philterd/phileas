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

public class SsnFilter extends RegexFilter {

    public SsnFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Set<String> ignoredFiles, List<IgnoredPattern> ignoredPatterns, Crypto crypto, int windowSize) {
        super(FilterType.SSN, strategies, anonymizationService, alertService, ignored, ignoredFiles, ignoredPatterns, crypto, windowSize);

        final Pattern ssnPattern = Pattern.compile("\\b(?!000|666)[0-8][0-9]{2}[- ]?(?!00)[0-9]{2}[- ]?(?!0000)[0-9]{4}\\b");
        final FilterPattern ssn1 = new FilterPattern.FilterPatternBuilder(ssnPattern, 0.90).build();

        final Pattern tinPattern = Pattern.compile("\\b\\d{2}-\\d{7}\\b");
        final FilterPattern tin1 = new FilterPattern.FilterPatternBuilder(tinPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("ssn");
        this.contextualTerms.add("tin");
        this.contextualTerms.add("social");
        this.contextualTerms.add("ssid");

        this.analyzer = new Analyzer(contextualTerms, ssn1, tin1);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        return new FilterResult(context, documentId, spans);

    }

}
