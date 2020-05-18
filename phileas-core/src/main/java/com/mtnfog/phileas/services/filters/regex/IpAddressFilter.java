package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class IpAddressFilter extends RegexFilter implements Serializable {

    public IpAddressFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.IP_ADDRESS, strategies, anonymizationService, ignored, crypto, windowSize);

        final Pattern IPV4_PATTERN = Pattern.compile("([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])");

        final FilterPattern ipv4 = new FilterPattern(IPV4_PATTERN, 0.90);

        // IPv6 patterns taken from https://github.com/Dynatrace/openkit-java
        // https://github.com/Dynatrace/openkit-java/blob/master/src/main/java/com/dynatrace/openkit/core/util/InetAddressValidator.java
        // At commit 1d7118913bf2ea6befc1522724eed3ef6378b9d1
        // Apache License, version 2.0: https://github.com/Dynatrace/openkit-java/blob/master/LICENSE

        final Pattern IPV6_STD_PATTERN =
                Pattern.compile(
                        ""                           // start of string
                                + "(?:[0-9a-fA-F]{1,4}:){7}"    // 7 blocks of a 1 to 4 digit hex number followed by double colon ':'
                                + "[0-9a-fA-F]{1,4}"            // one more block of a 1 to 4 digit hex number
                                + "");                          // end of string

        final FilterPattern ipv61 = new FilterPattern(IPV6_STD_PATTERN, 0.90);

        final Pattern IPV6_HEX_COMPRESSED_PATTERN =
                Pattern.compile(
                        ""                             // start of string
                                + "("                             // 1st group
                                + "(?:[0-9A-Fa-f]{1,4}"           // at least one block of a 1 to 4 digit hex number
                                + "(?::[0-9A-Fa-f]{1,4})*)?"      // optional further blocks, any number
                                + ")"
                                + "::"                            // in the middle of the expression the two occurences of ':' are neccessary
                                + "("                             // 2nd group
                                + "(?:[0-9A-Fa-f]{1,4}"           // at least one block of a 1 to 4 digit hex number
                                + "(?::[0-9A-Fa-f]{1,4})*)?"      // optional further blocks, any number
                                + ")"
                                + "");                           // end of string

        final FilterPattern ipv62 = new FilterPattern(IPV6_HEX_COMPRESSED_PATTERN, 0.90);

        //this regex checks the ipv6 uncompressed part of a ipv6 mixed address
        final Pattern IPV6_MIXED_COMPRESSED_REGEX =
                Pattern.compile(""                                               // start of string
                        + "("                                               // 1st group
                        + "(?:[0-9A-Fa-f]{1,4}"                             // at least one block of a 1 to 4 digit hex number
                        + "(?::[0-9A-Fa-f]{1,4})*)?"                        // optional further blocks, any number
                        + ")"
                        + "::"                                              // in the middle of the expression the two occurences of ':' are neccessary
                        + "("                                               // 2nd group
                        + "(?:[0-9A-Fa-f]{1,4}:"                            // at least one block of a 1 to 4 digit hex number followed by a ':' character
                        + "(?:[0-9A-Fa-f]{1,4}:)*)?"                        // optional further blocks, any number, all succeeded by ':' character
                        + ")"
                        + "");                                             // end of string

        final FilterPattern ipv63 = new FilterPattern(IPV6_MIXED_COMPRESSED_REGEX, 0.90);

        //this regex checks the ipv6 uncompressed part of a ipv6 mixed address
        final Pattern IPV6_MIXED_UNCOMPRESSED_REGEX =
                Pattern.compile(""  // start of string
                        + "(?:[0-9a-fA-F]{1,4}:){6}"                             // 6 blocks of a 1 to 4 digit hex number followed by double colon ':'
                        + "" );                                                 // end of string

        final FilterPattern ipv64 = new FilterPattern(IPV6_MIXED_UNCOMPRESSED_REGEX, 0.90);

        this.contextualTerms = new HashSet<>(){{
            add("ipv4");
            add("ipv6");
            add("ip");
            add("ip address");
        }};

        this.analyzer = new Analyzer(contextualTerms, ipv4, ipv61, ipv62, ipv63, ipv64);

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        return findSpans(filterProfile, analyzer, input, context, documentId);

    }

}
