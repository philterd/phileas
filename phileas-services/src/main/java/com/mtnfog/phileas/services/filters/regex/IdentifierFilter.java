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

public class IdentifierFilter extends RegexFilter implements Serializable {

    private Pattern pattern;

    public IdentifierFilter(String pattern, AnonymizationService anonymizationService) {
        super(FilterType.IDENTIFIER, anonymizationService);
        this.pattern = Pattern.compile(pattern);
    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws IOException {

        return findSpans(filterProfile, pattern, input, context, documentId);

    }

}
