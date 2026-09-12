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

public class SsnFilter extends RegexFilter {

    public SsnFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.SSN, filterConfiguration);

        // Substitutes for the ASCII hyphen: the soft hyphen, the U+2010 dash block (including the
        // non-breaking hyphen), the minus sign, and the small and fullwidth forms.
        final String hyphenCharacters = "-\\u00AD\\u2010-\\u2015\\u2212\\uFE58\\uFE63\\uFF0D";

        // A hyphen the identifier may be wrapped across. Requiring the hyphen before the line break
        // keeps three numbers on three lines from matching. Whitespace is never part of a digit
        // group, so the runs are possessive.
        final String wrap = "[" + hyphenCharacters + "]\\h*+(?:\\R\\h*+)?+";

        // Between the groups of an SSN: a hyphen, wrapped or not, or one horizontal space.
        final String separator = "(?:" + wrap + "|\\h)?";

        final String ssn = "(?!000|666)[0-8]\\d{2}" + separator + "(?!00)\\d{2}" + separator + "(?!0000)\\d{4}";

        // A match may not begin or end partway through a longer run of digits: that is what let a
        // fragment straddling two unseparated SSNs match while neither SSN did. Repeating the SSN
        // between the boundaries keeps SSNs written with nothing between them redacted, as a single
        // span covering the run, while a run that does not divide evenly into SSNs (an account
        // number, say) still matches nothing. The repetition is bounded because the engine recurses
        // once per iteration. See issue #343.
        final Pattern ssnPattern = Pattern.compile("(?<!\\w)(?:" + ssn + "){1,8}(?!\\w)");
        final FilterPattern ssn1 = new FilterPattern.FilterPatternBuilder(ssnPattern, 0.90).build();

        // A TIN gets the same boundaries, and a hyphen counts as part of a longer token: without
        // that, "45-6789123" out of "123-45-6789123-45-6789" was a TIN.
        final Pattern tinPattern = Pattern.compile("(?<![\\w" + hyphenCharacters + "])\\d{2}" + wrap
                + "\\d{7}(?![\\w" + hyphenCharacters + "])");
        final FilterPattern tin1 = new FilterPattern.FilterPatternBuilder(tinPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("ssn");
        this.contextualTerms.add("tin");
        this.contextualTerms.add("social");
        this.contextualTerms.add("ssid");

        this.analyzer = new Analyzer(contextualTerms, ssn1, tin1);

    }

    @Override
    public Filtered filter(ContextService contextService, Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(contextService, policy, analyzer, input, context);

        return new Filtered(context, spans);

    }

}
