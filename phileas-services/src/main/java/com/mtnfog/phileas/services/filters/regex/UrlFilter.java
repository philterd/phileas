package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class UrlFilter extends RegexFilter implements Serializable {

    // https://www.regexpal.com/93652: This regex will find things like test.link where it might just be two sentences without a space between them.
    // These two patterns do NOT consider IP addresses instead of domain names.
    private static final Pattern URL_WITH_OPTIONAL_PROTOCOL_REGEX = Pattern.compile("(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_WITH_PROTOCOL_REGEX = Pattern.compile("(www\\.|http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?", Pattern.CASE_INSENSITIVE);

    // These two patterns only consider IP addresses.
    private static final Pattern URL_IPV4_ADDRESS_REGEX = Pattern.compile("(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(:[0-9]{1,5})?(\\/.*)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_IPV6_ADDRESS_REGEX = Pattern.compile("(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))(:[0-9]{1,5})?(\\/.*)?", Pattern.CASE_INSENSITIVE);

    private boolean requireHttpWwwPrefix;

    public UrlFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, boolean requireHttpWwwPrefix, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.URL, strategies, anonymizationService, ignored, crypto, windowSize);
        this.requireHttpWwwPrefix = requireHttpWwwPrefix;
    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        List<Span> spans = new LinkedList<>();

        if(requireHttpWwwPrefix) {

            spans.addAll(findSpans(filterProfile, URL_WITH_PROTOCOL_REGEX, input, context, documentId));

        } else {

            spans.addAll(findSpans(filterProfile, URL_WITH_OPTIONAL_PROTOCOL_REGEX, input, context, documentId));

        }

        spans.addAll(findSpans(filterProfile, URL_IPV4_ADDRESS_REGEX, input, context, documentId));
        spans.addAll(findSpans(filterProfile, URL_IPV6_ADDRESS_REGEX, input, context, documentId));

        spans = Span.dropOverlappingSpans(spans);

        return spans;

    }

}
