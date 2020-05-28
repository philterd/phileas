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

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class SectionFilter extends RegexFilter {

    public SectionFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, String startPattern, String endPattern, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.SECTION, strategies, anonymizationService, alertService, ignored, crypto, windowSize);

        final Pattern pattern = Pattern.compile(startPattern + "(.*?)" + endPattern);
        final FilterPattern sectionPattern1 = new FilterPattern.FilterPatternBuilder(pattern, 0.90).build();

        // There are no contextual terms for a section.
        this.analyzer = new Analyzer(sectionPattern1);

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        return findSpans(filterProfile, analyzer, input, context, documentId);

    }

}
