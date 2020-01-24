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

    // https://www.regexpal.com/93652: This regex will find things like test.link where it might just be two sentences without a space between them.
    private static final Pattern URL_WITH_OPTIONAL_PROTOCOL_REGEX = Pattern.compile("(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_WITH_PROTOCOL_REGEX = Pattern.compile("(www\\.|http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?", Pattern.CASE_INSENSITIVE);

    private boolean requireHttpWwwPrefix;

    public UrlFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, boolean requireHttpWwwPrefix, Set<String> ignored) {
        super(FilterType.URL, strategies, anonymizationService, ignored);
        this.requireHttpWwwPrefix = requireHttpWwwPrefix;
    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws IOException {

        if(requireHttpWwwPrefix) {

            return findSpans(filterProfile, URL_WITH_PROTOCOL_REGEX, input, context, documentId);

        } else {

            return findSpans(filterProfile, URL_WITH_OPTIONAL_PROTOCOL_REGEX, input, context, documentId);

        }

    }

}
