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

/**
 * Detects U.S. Individual Taxpayer Identification Numbers (ITIN): nine digits beginning with 9, in the
 * same forms the SSN filter accepts (hyphenated {@code 9XX-XX-XXXX}, single-space separated, or
 * unformatted). The SSN filter never matches a value beginning with 9, so the two do not overlap.
 *
 * <p>Adoption Taxpayer Identification Numbers ({@code 9XX-93-XXXX}) are detected as ITINs unless
 * {@code onlyValidRanges} is set.</p>
 */
public class ItinFilter extends RegexFilter {

    private final boolean onlyValidRanges;

    public ItinFilter(final FilterConfiguration filterConfiguration) {
        this(filterConfiguration, false);
    }

    public ItinFilter(final FilterConfiguration filterConfiguration, final boolean onlyValidRanges) {
        super(FilterType.ITIN, filterConfiguration);

        this.onlyValidRanges = onlyValidRanges;

        // The same separators as the SSN filter: a hyphen (or a substitute for one), optionally
        // wrapped across a line break after it, one horizontal space, or nothing, the same for both.
        // A match may not begin or end partway through a longer run of digits.
        final Pattern itinPattern = Pattern.compile("(?<!\\w)" + IdentifierSeparators.join("9\\d{2}", "\\d{2}", "\\d{4}") + "(?!\\w)");
        final FilterPattern itin1 = new FilterPattern.FilterPatternBuilder(itinPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("itin");
        this.contextualTerms.add("tin");
        this.contextualTerms.add("taxpayer");
        this.contextualTerms.add("tax");

        this.analyzer = new Analyzer(contextualTerms, itin1);

    }

    @Override
    public Filtered filter(ContextService contextService, Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(contextService, policy, analyzer, input, context);

        if(onlyValidRanges) {
            spans.removeIf(span -> !isValidRange(span.getText()));
        }

        return new Filtered(context, spans);

    }

    /**
     * Whether the fourth and fifth digits of an ITIN fall in the ranges the IRS issues: 50 to 65,
     * 70 to 88, 90 to 92, and 94 to 99 (IRS Publication 4757). 93 is used for ATINs.
     * @param text The matched text, in any accepted form.
     * @return {@code true} if the digits are in an issued range.
     */
    public static boolean isValidRange(final String text) {

        final String digits = text.replaceAll("\\D", "");

        if(digits.length() != 9) {
            return false;
        }

        final int group = Integer.parseInt(digits.substring(3, 5));

        return (group >= 50 && group <= 65)
                || (group >= 70 && group <= 88)
                || (group >= 90 && group <= 92)
                || (group >= 94 && group <= 99);

    }

}
