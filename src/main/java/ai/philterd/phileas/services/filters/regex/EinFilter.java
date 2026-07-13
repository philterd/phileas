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
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Detects U.S. Employer Identification Numbers (EIN), the federal tax ID with canonical form
 * {@code NN-NNNNNNN} (two digits, hyphen, seven digits).
 *
 * <p>Only the hyphenated form is matched. A bare nine-digit run is deliberately not claimed because
 * it is ambiguous with SSN and other identifiers; the hyphen after the second digit is what
 * distinguishes an EIN from an SSN (whose canonical form hyphenates after the third and fifth
 * digits). This keeps EIN from competing with SSN for bare nine-digit runs during span
 * disambiguation.</p>
 */
public class EinFilter extends RegexFilter {

    /**
     * The two-digit prefixes the IRS currently issues (the campus/online assignment prefixes).
     * Source: IRS "How EINs are Assigned and Valid EIN Prefixes".
     */
    private static final Set<String> VALID_PREFIXES = Set.of(
            "01", "02", "03", "04", "05", "06",
            "10", "11", "12", "13", "14", "15", "16",
            "20", "21", "22", "23", "24", "25", "26", "27",
            "30", "31", "32", "33", "34", "35", "36", "37", "38", "39",
            "40", "41", "42", "43", "44", "45", "46", "47", "48",
            "50", "51", "52", "53", "54", "55", "56", "57", "58", "59",
            "60", "61", "62", "63", "64", "65", "66", "67", "68",
            "71", "72", "73", "74", "75", "76", "77",
            "80", "81", "82", "83", "84", "85", "86", "87", "88",
            "90", "91", "92", "93", "94", "95", "98", "99");

    private final boolean onlyValidPrefixes;

    public EinFilter(final FilterConfiguration filterConfiguration) {
        this(filterConfiguration, false);
    }

    public EinFilter(final FilterConfiguration filterConfiguration, final boolean onlyValidPrefixes) {
        super(FilterType.EIN, filterConfiguration);

        this.onlyValidPrefixes = onlyValidPrefixes;

        // Canonical EIN form NN-NNNNNNN. This is the same shape SSN historically labels as a TIN;
        // EIN is given a higher confidence so it wins span disambiguation for its canonical form
        // when both filters are enabled.
        final Pattern einPattern = Pattern.compile("\\b\\d{2}-\\d{7}\\b");
        final FilterPattern ein1 = new FilterPattern.FilterPatternBuilder(einPattern, 0.95).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("ein");
        this.contextualTerms.add("fein");
        this.contextualTerms.add("employer");
        this.contextualTerms.add("tax");

        this.analyzer = new Analyzer(contextualTerms, ein1);

    }

    @Override
    public Filtered filter(ContextService contextService, Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(contextService, policy, analyzer, input, context);

        if(onlyValidPrefixes) {
            // Keep only EINs whose two-digit prefix is one the IRS issues.
            spans.removeIf(span -> !VALID_PREFIXES.contains(span.getText().substring(0, 2)));
        }

        return new Filtered(context, spans);

    }

}
