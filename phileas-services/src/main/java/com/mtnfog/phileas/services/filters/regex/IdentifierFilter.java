package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.IdentifierFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.regex.Pattern;

public class IdentifierFilter extends RegexFilter implements Serializable {

    private Pattern pattern;
    private String name;

    public IdentifierFilter(String name, String pattern, boolean caseSensitive, List<IdentifierFilterStrategy> strategies, AnonymizationService anonymizationService) {
        super(FilterType.IDENTIFIER, strategies, anonymizationService);
        this.name = name;

        if(caseSensitive) {
            this.pattern = Pattern.compile(pattern);
        } else {
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        }

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws IOException {

        return findSpans(filterProfile, pattern, input, context, documentId);

    }

    public String getName() {
        return name;
    }

}
