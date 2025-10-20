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

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.rules.regex.RegexFilter;
import ai.philterd.phileas.services.Analyzer;
import ai.philterd.phileas.model.filtering.FilterPattern;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class SectionFilter extends RegexFilter {

    public SectionFilter(FilterConfiguration filterConfiguration, String startPattern, String endPattern) {
        super(FilterType.SECTION, filterConfiguration);

        final Pattern pattern = Pattern.compile(startPattern + "(.*?)" + endPattern);
        final FilterPattern sectionPattern1 = new FilterPattern.FilterPatternBuilder(pattern, 0.90).build();

        // There are no contextual terms because it doesn't make sense to have them for a section.
        this.contextualTerms = new HashSet<>();

        this.analyzer = new Analyzer(sectionPattern1);

    }

    @Override
    public Filtered filter(Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(policy, analyzer, input, context);

        return new Filtered(context, spans);

    }

}
