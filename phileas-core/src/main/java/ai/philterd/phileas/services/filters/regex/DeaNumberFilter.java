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
 * Detects DEA registration numbers: two capital letters, or a capital letter and 9, followed by seven
 * digits, kept only when the last digit is the check digit.
 */
public class DeaNumberFilter extends RegexFilter {

    public DeaNumberFilter(final FilterConfiguration filterConfiguration) {
        super(FilterType.DEA_NUMBER, filterConfiguration);

        final Pattern deaPattern = Pattern.compile("(?<!\\w)[A-Z][A-Z9]\\d{7}(?!\\w)");
        final FilterPattern dea1 = new FilterPattern.FilterPatternBuilder(deaPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("dea");
        this.contextualTerms.add("registration");

        this.analyzer = new Analyzer(contextualTerms, dea1);

    }

    @Override
    public Filtered filter(ContextService contextService, Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(contextService, policy, analyzer, input, context);

        return new Filtered(context, spans);

    }

    @Override
    protected boolean accepts(final String input, final int characterStart, final int characterEnd,
                              final FilterPattern filterPattern) {
        return isValid(input.substring(characterStart, characterEnd));
    }

    /**
     * Whether a value is a DEA number: the last digit of the sum of the first, third, and fifth digits
     * plus twice the sum of the second, fourth, and sixth digits equals the seventh digit.
     * @param value The value.
     * @return {@code true} if the value is a DEA number.
     */
    public static boolean isValid(final String value) {

        if(value == null || !value.matches("[A-Z][A-Z9]\\d{7}")) {
            return false;
        }

        return checkDigit(value.substring(2, 8)) == value.charAt(8) - '0';

    }

    /**
     * The check digit for the first six digits of a DEA number.
     * @param sixDigits The six digits after the two-character prefix.
     * @return The check digit.
     */
    public static int checkDigit(final String sixDigits) {

        final int odd = (sixDigits.charAt(0) - '0') + (sixDigits.charAt(2) - '0') + (sixDigits.charAt(4) - '0');
        final int even = (sixDigits.charAt(1) - '0') + (sixDigits.charAt(3) - '0') + (sixDigits.charAt(5) - '0');

        return (odd + 2 * even) % 10;

    }

}
