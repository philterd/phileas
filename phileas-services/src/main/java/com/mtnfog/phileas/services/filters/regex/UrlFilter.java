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
import java.util.Set;
import java.util.regex.Pattern;

public class UrlFilter extends RegexFilter implements Serializable {

    // https://www.regexpal.com/93652
    private static final Pattern URL_REGEX = Pattern.compile("(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?", Pattern.CASE_INSENSITIVE);

    public UrlFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, Set<String> ignored) {
        super(FilterType.URL, strategies, anonymizationService, ignored);
    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws IOException {

        return findSpans(filterProfile, URL_REGEX, input, context, documentId);

    }

}
