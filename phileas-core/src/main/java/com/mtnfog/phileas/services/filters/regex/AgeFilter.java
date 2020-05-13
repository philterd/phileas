package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class AgeFilter extends RegexFilter implements Serializable {

    private static final Pattern AGE_REGEX_1 = Pattern.compile("\\b[0-9.]+[\\s]*(years|yrs)(\\s)*(old)?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern AGE_REGEX_2 = Pattern.compile("\\b(age)(d)?(\\s)*[0-9.]+\\b", Pattern.CASE_INSENSITIVE);

    public AgeFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.AGE, strategies, anonymizationService, ignored, crypto, windowSize);
    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, AGE_REGEX_1, input, context, documentId);
        spans.addAll(findSpans(filterProfile, AGE_REGEX_2, input, context, documentId));

        return spans;

    }

}