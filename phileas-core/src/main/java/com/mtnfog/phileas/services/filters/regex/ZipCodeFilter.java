package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
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

public class ZipCodeFilter extends RegexFilter {

    public ZipCodeFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, List<IgnoredPattern> ignoredPatterns, Crypto crypto, int windowSize) {
        super(FilterType.ZIP_CODE, strategies, anonymizationService, alertService, ignored, ignoredPatterns, crypto, windowSize);

        final Pattern zipCodePattern = Pattern.compile("\\b[0-9]{5}(?:-[0-9]{4})?\\b");
        final FilterPattern zipCode1 = new FilterPattern.FilterPatternBuilder(zipCodePattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("zip");
        this.contextualTerms.add("zipcode");
        this.contextualTerms.add("postal");

        this.analyzer = new Analyzer(contextualTerms, zipCode1);

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        return findSpans(filterProfile, analyzer, input, context, documentId);

    }

}
