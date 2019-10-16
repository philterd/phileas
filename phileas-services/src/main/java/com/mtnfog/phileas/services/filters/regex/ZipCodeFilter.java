package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.regex.Pattern;

public class ZipCodeFilter extends RegexFilter implements Serializable {

    private static final Pattern ZIP_CODE_REGEX = Pattern.compile("\\b[0-9]{5}(?:-[0-9]{4})?\\b");

    public ZipCodeFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService) {
        super(FilterType.ZIP_CODE, strategies, anonymizationService);
    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws IOException {

        final List<Span> spans = findSpans(filterProfile, ZIP_CODE_REGEX, input, context, documentId);
        System.out.println("Zip code spans: " + spans.size());
        System.out.println(spans.get(0).getText(input));
        return spans;

    }

}
