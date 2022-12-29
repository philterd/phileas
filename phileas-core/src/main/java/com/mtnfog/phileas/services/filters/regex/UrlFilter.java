package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.FilterConfiguration;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class UrlFilter extends RegexFilter {

    public UrlFilter(FilterConfiguration filterConfiguration, boolean requireHttpWwwPrefix) {
        super(FilterType.URL, filterConfiguration);

        // https://www.regexpal.com/93652: This regex will find things like test.link where it might just be two sentences without a space between them.
        // These two patterns do NOT consider IP addresses instead of domain names.
        final Pattern urlWithOptionalProtocolPattern = Pattern.compile("(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?", Pattern.CASE_INSENSITIVE);
        final FilterPattern url1 = new FilterPattern.FilterPatternBuilder(urlWithOptionalProtocolPattern, 0.10).build();

        final Pattern urlWithProtocolPattern = Pattern.compile("(www\\.|http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?", Pattern.CASE_INSENSITIVE);
        final FilterPattern url2 = new FilterPattern.FilterPatternBuilder(urlWithProtocolPattern, 0.80).build();

        // These two patterns only consider IP addresses.
        final Pattern urlIpv4AddressPattern = Pattern.compile("(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(:[0-9]{1,5})?(\\/.*)?", Pattern.CASE_INSENSITIVE);
        final FilterPattern url3 = new FilterPattern.FilterPatternBuilder(urlIpv4AddressPattern, 0.80).build();

        final Pattern urlIpv6AddressPattern = Pattern.compile("(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))(:[0-9]{1,5})?(\\/.*)?", Pattern.CASE_INSENSITIVE);
        final FilterPattern url4 = new FilterPattern.FilterPatternBuilder(urlIpv6AddressPattern, 0.80).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("web");
        this.contextualTerms.add("webpage");
        this.contextualTerms.add("website");
        this.contextualTerms.add("url");
        this.contextualTerms.add("uri");
        this.contextualTerms.add("address");

        if(requireHttpWwwPrefix) {
            this.analyzer = new Analyzer(contextualTerms, url2, url3, url4);
        } else {
            this.analyzer = new Analyzer(contextualTerms, url1, url3, url4);
        }

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        return new FilterResult(context, documentId, spans);

    }

}
