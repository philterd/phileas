/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.services.filters.regex;

import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.rules.regex.RegexFilter;
import ai.philterd.phileas.model.filtering.FilterPattern;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.Analyzer;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class UrlFilter extends RegexFilter {

    public UrlFilter(FilterConfiguration filterConfiguration, boolean requireHttpWwwPrefix) {
        super(FilterType.URL, filterConfiguration);

        // https://www.regexpal.com/93652: This regex will find things like test.link where it might just be two sentences without a space between them.
        // These two patterns do NOT consider IP addresses instead of domain names.
        final Pattern urlWithOptionalProtocolPattern = Pattern.compile("(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?[a-z\\d]+([\\-\\.]{1}[a-z\\d]+)*\\.[a-z]{2,5}(:[\\d]{1,5})?(\\/.*)?", Pattern.CASE_INSENSITIVE);
        final FilterPattern url1 = new FilterPattern.FilterPatternBuilder(urlWithOptionalProtocolPattern, 0.10).build();

        final Pattern urlWithProtocolPattern = Pattern.compile("(www\\.|http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)[a-z\\d]+([\\-\\.]{1}[a-z\\d]+)*\\.[a-z]{2,5}(:[\\d]{1,5})?(\\/.*)?", Pattern.CASE_INSENSITIVE);
        final FilterPattern url2 = new FilterPattern.FilterPatternBuilder(urlWithProtocolPattern, 0.80).build();

        // These two patterns only consider IP addresses.
        final Pattern urlIpv4AddressPattern = Pattern.compile("(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?(?:[\\d]{1,3}\\.){3}[\\d]{1,3}(:[\\d]{1,5})?(\\/.*)?", Pattern.CASE_INSENSITIVE);
        final FilterPattern url3 = new FilterPattern.FilterPatternBuilder(urlIpv4AddressPattern, 0.80).build();

        final Pattern urlIpv6AddressPattern = Pattern.compile("(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?(([\\da-fA-F]{1,4}:){7,7}[\\da-fA-F]{1,4}|([\\da-fA-F]{1,4}:){1,7}:|([\\da-fA-F]{1,4}:){1,6}:[\\da-fA-F]{1,4}|([\\da-fA-F]{1,4}:){1,5}(:[\\da-fA-F]{1,4}){1,2}|([\\da-fA-F]{1,4}:){1,4}(:[\\da-fA-F]{1,4}){1,3}|([\\da-fA-F]{1,4}:){1,3}(:[\\da-fA-F]{1,4}){1,4}|([\\da-fA-F]{1,4}:){1,2}(:[\\da-fA-F]{1,4}){1,5}|[\\da-fA-F]{1,4}:((:[\\da-fA-F]{1,4}){1,6})|:((:[\\da-fA-F]{1,4}){1,7}|:)|fe80:(:[\\da-fA-F]{0,4}){0,4}%[\\da-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[\\d]){0,1}[\\d])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[\\d]){0,1}[\\d])|([\\da-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[\\d]){0,1}[\\d])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[\\d]){0,1}[\\d]))(:[\\d]{1,5})?(\\/.*)?", Pattern.CASE_INSENSITIVE);
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
    public Filtered filter(Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(policy, analyzer, input, context);

        return new Filtered(context, spans);

    }

}
