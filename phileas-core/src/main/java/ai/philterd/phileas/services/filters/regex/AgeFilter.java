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
import ai.philterd.phileas.services.strategies.rules.AgeInYears;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.regex.Pattern;

public class AgeFilter extends RegexFilter {

    // Spelled-out number words covering a realistic human age range (0 to ~119): the single words
    // zero-nineteen, a tens word optionally followed by a ones word (e.g. "thirty-five", "forty one"),
    // and an optional "(one|a) hundred (and)?" prefix for ages of one hundred and above.
    private static final String ONES = "one|two|three|four|five|six|seven|eight|nine";
    private static final String TEENS = "ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen";
    private static final String TENS = "twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety";
    private static final String ONE_TO_NINETY_NINE =
            "(?:(?:" + TENS + ")(?:[\\s-](?:" + ONES + "))?|" + TEENS + "|" + ONES + "|zero)";
    private static final String NUMBER_WORD =
            "(?:(?:(?:one|a)\\s+)?hundred(?:\\s+(?:and\\s+)?" + ONE_TO_NINETY_NINE + ")?|" + ONE_TO_NINETY_NINE + ")";

    // An "age" keyword, as "age", "aged", or "age of" (e.g. "at the age of 39").
    private static final String AGE_KEYWORD = "\\b(age)(d|\\s+of)?";

    // The units of an age. A unit is not followed by a hyphen and a letter, as in "yo-yo", unless the word is
    // "old", as in "47 year-old".
    private static final String UNIT = "(year|years|yrs|yr|yo)(?!-(?!old\\b)[a-z])";

    // The oldest age a number before a unit can be. Well above the longest recorded lifespan, so no real age is
    // dropped, while a year or the age of an object, as in "the 1990 yo-yo" or "150 years old", is not an age.
    static final double MAX_AGE = 125;

    // Optional separator between the keyword and its value, e.g. "Age: 47", "Age - 47".
    // Whitespace sits inside the group to avoid ambiguous backtracking.
    private static final String AGE_SEPARATOR = "\\s*(?:[:=-]\\s*)?";

    // A number after an age keyword.
    private static final String NUMBER = "[\\d.]+";

    // Ends a keyword age. It does not end right before a hyphen or a slash and a digit, so a date such as
    // "age 25-03-2024" or "Age: 12/25/2020" is not cut into a partial age.
    private static final String KEYWORD_END = "\\b(?![-/]\\d)";

    // An age up to MAX_AGE.
    private static final String BOUNDED_AGE = "(?:1[01]\\d|12[0-5]|0?\\d?\\d)(?:\\.\\d+)?";

    // The units an age in months, weeks, or days is written with, as AgeInYears reads them.
    private static final String SMALL_UNIT = "(?:months?|mos?|mths?|weeks?|wks?|days?)";

    // An age after the first in a run or range. It is read only up to MAX_AGE, so a date such as
    // "aged 12-25-2020" is not read as a run of ages, unless a month, week, or day unit follows it, as in
    // "aged 6 to 130 months", where a larger number is still a real age. It is not followed by a slash and a digit,
    // or by a hyphen and a digit unless another age follows the hyphen, as in "ages 3-5-7", so it is matched whole
    // or not at all.
    private static final String NEXT_AGE = "(?:" + BOUNDED_AGE + "\\b(?!/\\d)(?!-(?!" + BOUNDED_AGE + "\\b)\\d)"
            + "|\\d+(?:\\.\\d+)?(?=\\s*" + SMALL_UNIT + "\\b))";

    // Joins two ages with a hyphen or a word: "39-41", "39 to 41", "39 and 41", "39 or 41".
    private static final String WORD_JOIN = "(?:\\s*-\\s*|,?\\s+(?:and|or|to)\\s+)";

    // After the singular "age" or "age of": one age, or a range joined by a hyphen or "to", as in "age 18-65".
    // A comma or "and" does not continue it, so "age 45, 3 visits" is one age.
    private static final String AGE_OR_RANGE = NUMBER + "(?:(?:\\s*-\\s*|\\s+to\\s+)" + NEXT_AGE + ")?";

    // After "aged", "between", or "from": ages joined by a hyphen or a word, where a comma joins only inside a list
    // that a word or a hyphen closes, so "aged 3, 5 and 7" is read whole and "aged 39, 2 children" is one age.
    // The repetitions are possessive: each joined age is matched whole, so nothing is given back, and a long run
    // does not overflow the stack.
    private static final String AGE_RUN =
            NUMBER + "(?:(?:\\s*,\\s*" + NEXT_AGE + ")*+" + WORD_JOIN + NEXT_AGE + ")*+";

    // After the plural "ages" or "ages of": ages joined by commas, words, or hyphens, as in "Ages: 39, 41".
    private static final String AGE_SERIES = NUMBER + "(?:(?:\\s*,\\s*|" + WORD_JOIN + ")" + NEXT_AGE + ")*+";

    private final Set<FilterPattern> unitPatterns;

    public AgeFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.AGE, filterConfiguration);

        final Pattern agePattern1 = Pattern.compile("\\b[0-9.]+[\\s]*" + UNIT + "(\\.?)(?:[\\s-]*old)?\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern age1 = new FilterPattern.FilterPatternBuilder(agePattern1, 0.90).build();

        // "age 47", "Age: 47", "age of 39", "age 18-65", "age 39 to 41".
        final Pattern agePattern2 = Pattern.compile("\\bage(?:\\s+of)?" + AGE_SEPARATOR + AGE_OR_RANGE + KEYWORD_END, Pattern.CASE_INSENSITIVE);
        final FilterPattern age2 = new FilterPattern.FilterPatternBuilder(agePattern2, 0.90).build();

        // "aged 65", "aged 39 to 41", "aged 3, 5 and 7".
        final Pattern agePattern7 = Pattern.compile("\\baged" + AGE_SEPARATOR + AGE_RUN + KEYWORD_END, Pattern.CASE_INSENSITIVE);
        final FilterPattern age7 = new FilterPattern.FilterPatternBuilder(agePattern7, 0.90).build();

        // "ages 39-41", "Ages: 39, 41", "the ages of 39, 41, and 44".
        final Pattern agePattern8 = Pattern.compile("\\bages(?:\\s+of)?" + AGE_SEPARATOR + AGE_SERIES + KEYWORD_END, Pattern.CASE_INSENSITIVE);
        final FilterPattern age8 = new FilterPattern.FilterPatternBuilder(agePattern8, 0.90).build();

        // "age between 18 and 65", "Age: between 18 and 65", "ages of between 18 and 65", "aged from 39 to 41".
        // The keyword is separated from "between" or "from" by whitespace or a separator, so "agebetween" is not read.
        final Pattern agePattern9 = Pattern.compile("\\bage[ds]?(?:\\s+of)?(?:\\s*[:=-]\\s*|\\s+)(?:between|from)\\s+" + AGE_RUN + KEYWORD_END, Pattern.CASE_INSENSITIVE);
        final FilterPattern age9 = new FilterPattern.FilterPatternBuilder(agePattern9, 0.90).build();

        final Pattern agePattern3 = Pattern.compile("\\b[0-9.]+[-]*" + UNIT + "(\\.?)(?:-*old)?\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern age3 = new FilterPattern.FilterPatternBuilder(agePattern3, 0.90).build();

        // 61 y/o
        final Pattern agePattern4 = Pattern.compile("\\b([0-9]{1,3}) (y\\/o)\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern age4 = new FilterPattern.FilterPatternBuilder(agePattern4, 0.90).build();

        // Spelled-out ages, e.g. "thirty-five years old", "thirty-five-year-old", "five yo".
        final Pattern agePattern5 = Pattern.compile("\\b(" + NUMBER_WORD + ")[\\s-]*" + UNIT + "(\\.?)(?:[\\s-]*old)?\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern age5 = new FilterPattern.FilterPatternBuilder(agePattern5, 0.90).build();

        // Spelled-out ages, e.g. "age thirty-five", "aged thirty-five", "age of thirty-five".
        final Pattern agePattern6 = Pattern.compile(AGE_KEYWORD + AGE_SEPARATOR + "(" + NUMBER_WORD + ")\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern age6 = new FilterPattern.FilterPatternBuilder(agePattern6, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("age");
        this.contextualTerms.add("years");

        this.analyzer = new Analyzer(contextualTerms, age1, age2, age3, age4, age5, age6, age7, age8, age9);

        // The patterns whose number is followed by a unit. An age after the "age" keyword is not bounded here,
        // since a unit after it, as in "aged 1100 months", can make a large number a real age.
        this.unitPatterns = Set.of(age1, age3, age4, age5);

    }

    @Override
    public Filtered filter(ContextService contextService, Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(contextService, policy, analyzer, input, context);

        final List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        return new Filtered(context, nonOverlappingSpans);

    }

    /**
     * The strategies read the unit of a keyword age, such as {@code aged 18 months}, from the word after the
     * span, so their window starts at the span. With the full window, an earlier occurrence of the same words
     * could be read instead.
     */
    @Override
    protected String[] getStrategyWindow(final String input, final int characterStart, final int characterEnd,
                                         final String[] window) {
        return getWindowFromToken(input, characterStart, characterEnd);
    }

    /**
     * A number before a unit is an age only up to {@link #MAX_AGE}, so "the 1990 yo-yo" or "150 years old" is not
     * read as an age.
     */
    @Override
    protected boolean accepts(final String input, final int characterStart, final int characterEnd,
                              final FilterPattern filterPattern) {

        if(!unitPatterns.contains(filterPattern)) {
            return true;
        }

        final OptionalDouble age = AgeInYears.of(input.substring(characterStart, characterEnd), null);

        return age.isEmpty() || age.getAsDouble() <= MAX_AGE;

    }

    @Override
    public List<Span> postFilter(List<Span> spans) {

        final List<Span> postFilteredSpans = new LinkedList<>();

        for(final Span span : spans) {

            final List<String> window = Arrays.stream(span.getWindow()).map(String::toLowerCase).toList();

            // Compared in lower case, so "93 YEARS OLD" and "61 Y/O" are kept like "93 years old".
            final String text = span.getText().toLowerCase(Locale.ROOT);

            // Determine between a date and an age.
            // Does it contain 'age' or 'old' or 'yo'? If not, drop it.
            // TODO: Should this list be exposed to the user and customizable?
            if(window.contains("age")
                    || text.contains("age")
                    || text.contains("old")
                    || text.contains("y/o")
                    || text.contains("yo")) {

                postFilteredSpans.add(span);

            }

        }

        return postFilteredSpans;

    }

}
