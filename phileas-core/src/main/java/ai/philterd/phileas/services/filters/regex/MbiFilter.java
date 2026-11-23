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
 * Detects Medicare Beneficiary Identifiers (MBI) in the format CMS publishes: eleven characters, a digit
 * from 1 to 9, then letters in the 2nd, 5th, 8th, and 9th positions, a letter or digit in the 3rd and
 * 6th, and digits in the rest, never using S, L, O, I, B, or Z. Written unformatted or in the card's
 * grouping ({@code 1EG4-TE5-MK73}). The MBI has no check digit.
 */
public class MbiFilter extends RegexFilter {

    /** The letters an MBI uses: A to Z without S, L, O, I, B, and Z. */
    public static final String LETTERS = "ACDEFGHJKMNPQRTUVWXY";

    private static final String LETTER = "[" + LETTERS + "]";
    private static final String LETTER_OR_DIGIT = "[" + LETTERS + "0-9]";

    /** An MBI, with the hyphens of the card's grouping or none. */
    private static final String MBI = "[1-9]" + LETTER + LETTER_OR_DIGIT + "\\d(-?)" + LETTER + LETTER_OR_DIGIT
            + "\\d\\1" + LETTER + LETTER + "\\d{2}";

    private static final Pattern MBI_VALUE = Pattern.compile(MBI);

    public MbiFilter(final FilterConfiguration filterConfiguration) {
        super(FilterType.MBI, filterConfiguration);

        final Pattern mbiPattern = Pattern.compile("(?<![\\w-])" + MBI + "(?![\\w-])");
        final FilterPattern mbi1 = new FilterPattern.FilterPatternBuilder(mbiPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("mbi");
        this.contextualTerms.add("medicare");
        this.contextualTerms.add("beneficiary");

        this.analyzer = new Analyzer(contextualTerms, mbi1);

    }

    @Override
    public Filtered filter(ContextService contextService, Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(contextService, policy, analyzer, input, context);

        return new Filtered(context, spans);

    }

    /**
     * Whether a value is in the MBI format, unformatted or in the card's grouping.
     * @param value The value.
     * @return {@code true} if the value is an MBI.
     */
    public static boolean isValid(final String value) {
        return value != null && MBI_VALUE.matcher(value).matches();
    }

}
