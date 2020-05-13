package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.IdentifierFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class IdentifierFilter extends RegexFilter implements Serializable {

    private Pattern pattern;

    public IdentifierFilter(String label, String pattern, boolean caseSensitive, List<IdentifierFilterStrategy> strategies, AnonymizationService anonymizationService, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.IDENTIFIER, strategies, anonymizationService, ignored, crypto, windowSize);
        this.label = label;

        if(caseSensitive) {
            this.pattern = Pattern.compile(pattern);
        } else {
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        }

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        return findSpans(filterProfile, pattern, input, context, documentId);

    }

}