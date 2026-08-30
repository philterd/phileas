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
import ai.philterd.phileas.services.context.ContextService;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class UrlFilter extends RegexFilter {

    public UrlFilter(FilterConfiguration filterConfiguration, boolean requireHttpWwwPrefix) {
        super(FilterType.URL, filterConfiguration);

        // https://www.regexpal.com/93652: This regex will find things like test.link where it might just be two sentences without a space between them.
        // These two patterns do NOT consider IP addresses instead of domain names.
        // The trailing path group stops at whitespace so it can't run into the next sentence, and
        // excludes a period only when it's followed by whitespace (a sentence-ending period), so a
        // period inside the path/host, or one at the very end of the input, still matches.
        // This pattern's alternation-heavy structure predates this fix; splitting it into smaller
        // composable patterns would be a separate, larger restructuring of this class. The trailing
        // path group's own repetition, (?:[^\s.]|\.(?!\s))*, is linear despite the S5998 warning: its
        // two branches never match the same character (one excludes '.', the other matches only '.'),
        // so there is no ambiguity for the engine to backtrack over.
        @SuppressWarnings({"java:S5843", "java:S5998"})
        final Pattern urlWithOptionalProtocolPattern = Pattern.compile("(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?[a-z\\d]+([\\-\\.]{1}[a-z\\d]+)*\\.[a-z]{2,5}(:[\\d]{1,5})?(\\/(?:[^\\s.]|\\.(?!\\s))*)?", Pattern.CASE_INSENSITIVE);
        final FilterPattern url1 = new FilterPattern.FilterPatternBuilder(urlWithOptionalProtocolPattern, 0.10).build();

        // Same pre-existing alternation-heavy structure as urlWithOptionalProtocolPattern above.
        @SuppressWarnings({"java:S5843", "java:S5998"})
        final Pattern urlWithProtocolPattern = Pattern.compile("(www\\.|http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)[a-z\\d]+([\\-\\.]{1}[a-z\\d]+)*\\.[a-z]{2,5}(:[\\d]{1,5})?(\\/(?:[^\\s.]|\\.(?!\\s))*)?", Pattern.CASE_INSENSITIVE);
        final FilterPattern url2 = new FilterPattern.FilterPatternBuilder(urlWithProtocolPattern, 0.80).build();

        // These two patterns only consider IP addresses.
        // Same pre-existing alternation-heavy structure as urlWithOptionalProtocolPattern above.
        @SuppressWarnings({"java:S5843", "java:S5998"})
        final Pattern urlIpv4AddressPattern = Pattern.compile("(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?(?:[\\d]{1,3}\\.){3}[\\d]{1,3}(:[\\d]{1,5})?(\\/(?:[^\\s.]|\\.(?!\\s))*)?", Pattern.CASE_INSENSITIVE);
        final FilterPattern url3 = new FilterPattern.FilterPatternBuilder(urlIpv4AddressPattern, 0.80).build();

        // Ported wholesale from the Dynatrace source cited above; splitting it into a Pattern per
        // compression form or replacing it with a non-regex validator would be a separate, much
        // larger effort than this fix, and risks regressing IPv6 forms this file has no test for.
        @SuppressWarnings({"java:S5843", "java:S5998"})
        final Pattern urlIpv6AddressPattern = Pattern.compile("(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?(([\\da-f]{1,4}:){7}[\\da-f]{1,4}|([\\da-f]{1,4}:){1,7}:|([\\da-f]{1,4}:){1,6}:[\\da-f]{1,4}|([\\da-f]{1,4}:){1,5}(:[\\da-f]{1,4}){1,2}|([\\da-f]{1,4}:){1,4}(:[\\da-f]{1,4}){1,3}|([\\da-f]{1,4}:){1,3}(:[\\da-f]{1,4}){1,4}|([\\da-f]{1,4}:){1,2}(:[\\da-f]{1,4}){1,5}|[\\da-f]{1,4}:((:[\\da-f]{1,4}){1,6})|:((:[\\da-f]{1,4}){1,7}|:)|fe80:(:[\\da-f]{0,4}){0,4}%[\\da-z]+|::(ffff(:0{1,4})?:)?((25[0-5]|(2[0-4]|1?[\\d])?[\\d])\\.){3}(25[0-5]|(2[0-4]|1?[\\d])?[\\d])|([\\da-f]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?[\\d])?[\\d])\\.){3}(25[0-5]|(2[0-4]|1?[\\d])?[\\d]))(:[\\d]{1,5})?(\\/(?:[^\\s.]|\\.(?!\\s))*)?", Pattern.CASE_INSENSITIVE);
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
    public Filtered filter(ContextService contextService, Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(contextService, policy, analyzer, input, context);

        return new Filtered(context, spans);

    }

}
