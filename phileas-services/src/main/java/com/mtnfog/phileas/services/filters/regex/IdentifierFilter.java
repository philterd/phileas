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

    private static final Pattern ID_REGEX = Pattern.compile("(\\d+[A-Za-z_-]+|[A-Za-z_-]+\\d+)[A-Za-z_\\-\\d]*", Pattern.CASE_INSENSITIVE);

    public IdentifierFilter(AnonymizationService anonymizationService) {
        super(FilterType.IDENTIFIER, anonymizationService);
    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws IOException {

        return findSpans(filterProfile, ID_REGEX, input, context, documentId);

    }

}
