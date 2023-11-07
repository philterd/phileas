/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
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

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.rules.regex.RegexFilter;
import ai.philterd.phileas.model.objects.Analyzer;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.Policy;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AgeFilter extends RegexFilter {

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

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("age");
        this.contextualTerms.add("years");

        this.analyzer = new Analyzer(contextualTerms, age1, age2, age3, age4);

    }

    @Override
    public FilterResult filter(Policy policy, String context, String documentId, int piece, String input, Map<String, String> attributes) throws Exception {

        final List<Span> spans = findSpans(policy, analyzer, input, context, documentId, attributes);

        final List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        return new FilterResult(context, documentId, nonOverlappingSpans);

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
                    || span.getText().contains("aged")
                    || span.getText().contains("old")
                    || span.getText().contains("y/o")
                    || span.getText().contains("yo")) {

                postFilteredSpans.add(span);

            }

        }

        return postFilteredSpans;

    }

}
