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
 * Detects U.S. National Provider Identifiers (NPI): ten digits beginning with 1 or 2, kept only when
 * the last digit is the NPI check digit.
 */
public class NpiFilter extends RegexFilter {

    public NpiFilter(final FilterConfiguration filterConfiguration) {
        super(FilterType.NPI, filterConfiguration);

        final Pattern npiPattern = Pattern.compile("(?<!\\w)[12]\\d{9}(?!\\w)");
        final FilterPattern npi1 = new FilterPattern.FilterPatternBuilder(npiPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("npi");
        this.contextualTerms.add("provider");

        this.analyzer = new Analyzer(contextualTerms, npi1);

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
     * Whether a value is an NPI: ten digits beginning with 1 or 2 whose last digit is the Luhn check
     * digit of the number with the prefix 80840, as CMS specifies.
     * @param value The value.
     * @return {@code true} if the value is an NPI.
     */
    public static boolean isValid(final String value) {

        if(value == null || !value.matches("[12]\\d{9}")) {
            return false;
        }

        return luhnValid("80840" + value);

    }

    /**
     * The NPI check digit for the first nine digits of an NPI.
     * @param firstNineDigits The first nine digits.
     * @return The check digit.
     */
    public static int checkDigit(final String firstNineDigits) {

        for(int digit = 0; digit <= 9; digit++) {
            if(luhnValid("80840" + firstNineDigits + digit)) {
                return digit;
            }
        }

        throw new IllegalArgumentException("Not a run of nine digits.");

    }

    private static boolean luhnValid(final String digits) {

        int sum = 0;

        for(int i = 0; i < digits.length(); i++) {
            int digit = digits.charAt(digits.length() - 1 - i) - '0';
            if(i % 2 == 1) {
                digit *= 2;
                if(digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }

        return sum % 10 == 0;

    }

}
