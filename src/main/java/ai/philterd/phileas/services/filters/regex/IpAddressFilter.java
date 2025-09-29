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

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.rules.regex.RegexFilter;
import ai.philterd.phileas.model.objects.Analyzer;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.policy.Policy;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class IpAddressFilter extends RegexFilter {

    public IpAddressFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.IP_ADDRESS, filterConfiguration);

        final Pattern ipv4Pattern = Pattern.compile("([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])");

        final FilterPattern ipv4 = new FilterPattern.FilterPatternBuilder(ipv4Pattern, 0.90).build();

        // IPv6 patterns taken from https://github.com/Dynatrace/openkit-java
        // https://github.com/Dynatrace/openkit-java/blob/master/src/main/java/com/dynatrace/openkit/core/util/InetAddressValidator.java
        // At commit 1d7118913bf2ea6befc1522724eed3ef6378b9d1
        // Apache License, version 2.0: https://github.com/Dynatrace/openkit-java/blob/master/LICENSE

        final Pattern ipv6StdPattern =
                Pattern.compile(
                        ""                           // start of string
                                + "(?:[\\da-fA-F]{1,4}:){7}"    // 7 blocks of a 1 to 4 digit hex number followed by double colon ':'
                                + "[\\da-fA-F]{1,4}"            // one more block of a 1 to 4 digit hex number
                                + "");                          // end of string

        final FilterPattern ipv61 = new FilterPattern.FilterPatternBuilder(ipv6StdPattern, 0.90).build();

        final Pattern ipv6HexCompressedPattern =
                Pattern.compile(
                        ""                             // start of string
                                + "("                             // 1st group
                                + "(?:[\\dA-Fa-f]{1,4}"           // at least one block of a 1 to 4 digit hex number
                                + "(?::[\\dA-Fa-f]{1,4})*)?"      // optional further blocks, any number
                                + ")"
                                + "::"                            // in the middle of the expression the two occurences of ':' are neccessary
                                + "("                             // 2nd group
                                + "(?:[\\dA-Fa-f]{1,4}"           // at least one block of a 1 to 4 digit hex number
                                + "(?::[\\dA-Fa-f]{1,4})*)?"      // optional further blocks, any number
                                + ")"
                                + "");                           // end of string

        final FilterPattern ipv62 = new FilterPattern.FilterPatternBuilder(ipv6HexCompressedPattern, 0.90).build();

        //this regex checks the ipv6 uncompressed part of a ipv6 mixed address
        final Pattern ipv6MixedCompressedPattern =
                Pattern.compile(""                                               // start of string
                        + "("                                               // 1st group
                        + "(?:[\\dA-Fa-f]{1,4}"                             // at least one block of a 1 to 4 digit hex number
                        + "(?::[\\dA-Fa-f]{1,4})*)?"                        // optional further blocks, any number
                        + ")"
                        + "::"                                              // in the middle of the expression the two occurences of ':' are neccessary
                        + "("                                               // 2nd group
                        + "(?:[\\dA-Fa-f]{1,4}:"                            // at least one block of a 1 to 4 digit hex number followed by a ':' character
                        + "(?:[\\dA-Fa-f]{1,4}:)*)?"                        // optional further blocks, any number, all succeeded by ':' character
                        + ")"
                        + "");                                             // end of string

        final FilterPattern ipv63 = new FilterPattern.FilterPatternBuilder(ipv6MixedCompressedPattern, 0.90).build();

        //this regex checks the ipv6 uncompressed part of a ipv6 mixed address
        final Pattern IPV6_MIXED_UNCOMPRESSED_REGEX =
                Pattern.compile(""  // start of string
                        + "(?:[\\da-fA-F]{1,4}:){6}"                             // 6 blocks of a 1 to 4 digit hex number followed by double colon ':'
                        + "" );                                                 // end of string

        final FilterPattern ipv64 = new FilterPattern.FilterPatternBuilder(IPV6_MIXED_UNCOMPRESSED_REGEX, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("ipv4");
        this.contextualTerms.add("ipv6");
        this.contextualTerms.add("ip");
        this.contextualTerms.add("ip address");

        this.analyzer = new Analyzer(contextualTerms, ipv4, ipv61, ipv62, ipv63, ipv64);

    }

    @Override
    public FilterResult filter(Policy policy, String contextName, String documentId, int piece, String input, Map<String, String> attributes) throws Exception {

        final List<Span> spans = findSpans(policy, analyzer, input, contextName, documentId, attributes);

        return new FilterResult(contextName, documentId, spans);

    }

}
