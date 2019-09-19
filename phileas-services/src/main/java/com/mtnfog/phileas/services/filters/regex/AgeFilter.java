package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.regex.Pattern;

public class AgeFilter extends RegexFilter implements Serializable {

    private static final Pattern AGE_REGEX_1 = Pattern.compile("\\b[0-9.]+[\\s]*(years|yrs)(\\s)*(old)?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern AGE_REGEX_2 = Pattern.compile("\\b(age)(d)?(\\s)*[0-9.]+\\b", Pattern.CASE_INSENSITIVE);

    public AgeFilter(AnonymizationService anonymizationService) {
        super(FilterType.AGE, anonymizationService);
    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws IOException {

        final List<Span> spans = findSpans(filterProfile, AGE_REGEX_1, input, context, documentId);
        spans.addAll(findSpans(filterProfile, AGE_REGEX_2, input, context, documentId));

        return spans;

    }

}
