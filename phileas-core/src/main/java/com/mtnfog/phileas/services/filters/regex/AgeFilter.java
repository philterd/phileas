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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class AgeFilter extends RegexFilter {

    public AgeFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.AGE, strategies, anonymizationService, alertService, ignored, crypto, windowSize);

        final Pattern agePattern1 = Pattern.compile("\\b[0-9.]+[\\s]*(years|yrs)(\\s)*(old)?\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern age1 = new FilterPattern.FilterPatternBuilder(agePattern1, 0.90).build();

        final Pattern agePattern2 = Pattern.compile("\\b(age)(d)?(\\s)*[0-9.]+\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern age2 = new FilterPattern.FilterPatternBuilder(agePattern2, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("age");
        this.contextualTerms.add("years");

        this.analyzer = new Analyzer(contextualTerms, age1, age2);

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        return findSpans(filterProfile, analyzer, input, context, documentId);

    }

}
