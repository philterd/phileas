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

public class IpAddressFilter extends RegexFilter {

    public IpAddressFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.IP_ADDRESS, filterConfiguration);

        // The per-octet alternation already carried most of this complexity before the (?<!\d)/(?!\d)
        // boundaries added here for issues #335 and #336; splitting octet validation out of the regex
        // and into post-match Java code would be a larger, separate change.
        @SuppressWarnings("java:S5843")
        final Pattern ipv4Pattern = Pattern.compile("(?<!\\d)([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])(?!\\d)");

        final FilterPattern ipv4 = new FilterPattern.FilterPatternBuilder(ipv4Pattern, 0.90).build();

        // One pattern for every IPv6 form. The alternation came from Dynatrace's InetAddressValidator
        // (Apache 2.0), which is used anchored; unanchored it needs the boundary in Ipv6Patterns to
        // stop a compressed address matching only as far as its "::". See issues #351 and #354.
        final Pattern ipv6Pattern = Pattern.compile(Ipv6Patterns.ADDRESS, Pattern.CASE_INSENSITIVE);

        final FilterPattern ipv6 = new FilterPattern.FilterPatternBuilder(ipv6Pattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("ipv4");
        this.contextualTerms.add("ipv6");
        this.contextualTerms.add("ip");
        this.contextualTerms.add("ip address");

        this.analyzer = new Analyzer(contextualTerms, ipv4, ipv6);

    }

    @Override
    public Filtered filter(ContextService contextService, Policy policy, String context, int piece, String input) throws Exception {

        // The IPv4 pattern also matches the dotted quad inside an IPv4-mapped address, so resolve
        // overlaps here rather than leaving two spans for one address. See issue #354.
        final List<Span> spans = Span.dropOverlappingSpans(findSpans(contextService, policy, analyzer, input, context));

        return new Filtered(context, spans);

    }

}
