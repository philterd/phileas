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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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

    public AgeFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.AGE, filterConfiguration);

        final Pattern agePattern1 = Pattern.compile("\\b[0-9.]+[\\s]*(year|years|yrs|yr|yo)(.?)(\\s)*(old)?\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern age1 = new FilterPattern.FilterPatternBuilder(agePattern1, 0.90).build();

        final Pattern agePattern2 = Pattern.compile("\\b(age)(d)?(\\s)*[0-9.]+\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern age2 = new FilterPattern.FilterPatternBuilder(agePattern2, 0.90).build();

        final Pattern agePattern3 = Pattern.compile("\\b[0-9.]+[-]*(year|years|yrs|yr|yo)(.?)(-)*(old)?\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern age3 = new FilterPattern.FilterPatternBuilder(agePattern3, 0.90).build();

        // 61 y/o
        final Pattern agePattern4 = Pattern.compile("\\b([0-9]{1,3}) (y\\/o)\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern age4 = new FilterPattern.FilterPatternBuilder(agePattern4, 0.90).build();

        // Spelled-out ages, e.g. "thirty-five years old", "thirty-five-year-old", "five yo".
        final Pattern agePattern5 = Pattern.compile("\\b(" + NUMBER_WORD + ")[\\s-]*(year|years|yrs|yr|yo)(.?)[\\s-]*(old)?\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern age5 = new FilterPattern.FilterPatternBuilder(agePattern5, 0.90).build();

        // Spelled-out ages, e.g. "age thirty-five", "aged thirty-five".
        final Pattern agePattern6 = Pattern.compile("\\b(age)(d)?(\\s)*(" + NUMBER_WORD + ")\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern age6 = new FilterPattern.FilterPatternBuilder(agePattern6, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("age");
        this.contextualTerms.add("years");

        this.analyzer = new Analyzer(contextualTerms, age1, age2, age3, age4, age5, age6);

    }

    @Override
    public Filtered filter(Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(policy, analyzer, input, context);

        final List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        return new Filtered(context, nonOverlappingSpans);

    }

    @Override
    public List<Span> postFilter(List<Span> spans) {

        final List<Span> postFilteredSpans = new LinkedList<>();

        for(final Span span : spans) {

            final List<String> window = Arrays.stream(span.getWindow()).map(String::toLowerCase).toList();

            // Determine between a date and an age.
            // Does it contain 'age' or 'old' or 'yo'? If not, drop it.
            // TODO: Should this list be exposed to the user and customizable?
            if(window.contains("age")
                    || span.getText().toLowerCase().contains("age")
                    || span.getText().contains("old")
                    || span.getText().contains("y/o")
                    || span.getText().contains("yo")) {

                postFilteredSpans.add(span);

            }

        }

        return postFilteredSpans;

    }

}
