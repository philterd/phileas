/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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

/**
 * The IPv6 address expression shared by the filters that detect addresses. Compile it with
 * {@link java.util.regex.Pattern#CASE_INSENSITIVE}.
 */
final class Ipv6Patterns {

    /**
     * Every IPv6 form: expanded, expanded mixed (six hextets and a dotted quad), compressed,
     * IPv4-mapped, and link-local with a zone.
     */
    static final String ALTERNATIVES = "(([\\da-f]{1,4}:){7}[\\da-f]{1,4}|([\\da-f]{1,4}:){6}((25[0-5]|(2[0-4]|1?[\\d])?[\\d])\\.){3}(25[0-5]|(2[0-4]|1?[\\d])?[\\d])|([\\da-f]{1,4}:){1,7}:|([\\da-f]{1,4}:){1,6}:[\\da-f]{1,4}|([\\da-f]{1,4}:){1,5}(:[\\da-f]{1,4}){1,2}|([\\da-f]{1,4}:){1,4}(:[\\da-f]{1,4}){1,3}|([\\da-f]{1,4}:){1,3}(:[\\da-f]{1,4}){1,4}|([\\da-f]{1,4}:){1,2}(:[\\da-f]{1,4}){1,5}|[\\da-f]{1,4}:((:[\\da-f]{1,4}){1,6})|:((:[\\da-f]{1,4}){1,7}|:)|fe80:(:[\\da-f]{0,4}){0,4}%[\\da-z]+|::(ffff(:0{1,4})?:)?((25[0-5]|(2[0-4]|1?[\\d])?[\\d])\\.){3}(25[0-5]|(2[0-4]|1?[\\d])?[\\d])|([\\da-f]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?[\\d])?[\\d])\\.){3}(25[0-5]|(2[0-4]|1?[\\d])?[\\d]))";

    /**
     * An optional zone identifier, e.g. "fe80::1%eth0", or "%25eth0" percent-encoded for a URI.
     */
    static final String ZONE = "(?:%[\\da-z]+)?";

    /**
     * The alternation is ordered and java.util.regex takes the first alternative that matches, not
     * the longest, so a compressed address matched only as far as its "::". This rejects a match
     * that stopped inside an address, so the engine backtracks into one that consumes all of it: not
     * a bare hex digit ("FE80::" out of "FE80::1"), not a further hextet, and not a further IPv4
     * octet. A period not followed by a digit still ends the match, so a trailing sentence period is
     * left out rather than blocking the match. See issue #351.
     */
    static final String BOUNDARY = "(?![\\da-f])(?!:[\\da-f])(?!\\.\\d)";

    /**
     * A complete address: any form, an optional zone, and the boundary that keeps a match from
     * stopping partway through.
     */
    static final String ADDRESS = ALTERNATIVES + ZONE + BOUNDARY;

    private Ipv6Patterns() {

    }

}
