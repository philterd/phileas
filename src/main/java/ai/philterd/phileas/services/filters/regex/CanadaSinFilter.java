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
import ai.philterd.phileas.services.validators.LuhnValidator;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects Canadian Social Insurance Numbers (SIN): nine digits, unformatted or in three groups of
 * three separated by a hyphen or a single space, such as {@code 046-454-286}. Every match must pass
 * the Luhn check. SINs beginning with 9 (issued to temporary residents) are detected like any other.
 *
 * <p>CRA business numbers also pass the Luhn check on their nine-digit root, so they are detected as
 * SINs unless {@code onlyValidPrefixes} is set, which drops values beginning with 0 or 8.</p>
 */
public class CanadaSinFilter extends RegexFilter {

    private final boolean onlyValidPrefixes;

    public CanadaSinFilter(final FilterConfiguration filterConfiguration) {
        this(filterConfiguration, false);
    }

    public CanadaSinFilter(final FilterConfiguration filterConfiguration, final boolean onlyValidPrefixes) {
        super(FilterType.CANADA_SIN, filterConfiguration);

        this.onlyValidPrefixes = onlyValidPrefixes;

        // The same separators as the SSN filter: a hyphen (or a substitute for one), optionally
        // wrapped across a line break after it, or one horizontal space.
        final String hyphenCharacters = "-\\u00AD\\u2010-\\u2015\\u2212\\uFE58\\uFE63\\uFF0D";
        final String wrap = "[" + hyphenCharacters + "]\\h*+(?:\\R\\h*+)?+";
        final String separator = "(?:" + wrap + "|\\h)?";

        // A match may not begin or end partway through a longer run of digits.
        final Pattern sinPattern = Pattern.compile("(?<!\\w)\\d{3}" + separator + "\\d{3}" + separator + "\\d{3}(?!\\w)");
        final FilterPattern sin1 = new FilterPattern.FilterPatternBuilder(sinPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("sin");
        this.contextualTerms.add("social insurance");
        this.contextualTerms.add("nas");
        this.contextualTerms.add("assurance sociale");

        this.analyzer = new Analyzer(contextualTerms, sin1);

    }

    @Override
    public Filtered filter(ContextService contextService, Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(contextService, policy, analyzer, input, context);

        spans.removeIf(span -> !LuhnValidator.isValid(span.getText()));

        if(onlyValidPrefixes) {
            spans.removeIf(span -> !isValidPrefix(span.getText()));
        }

        return new Filtered(context, spans);

    }

    /**
     * Whether a SIN begins with a digit issued to individuals: 1 to 7, or 9 for temporary
     * residents. 0 is not issued and 8 is used for CRA business numbers.
     * @param text The matched text, in any accepted form.
     * @return {@code true} if the first digit is issued to individuals.
     */
    public static boolean isValidPrefix(final String text) {

        final char first = text.charAt(0);

        return first != '0' && first != '8';

    }

}
