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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class IdentifierFilter extends RegexFilter {

    public IdentifierFilter(FilterConfiguration filterConfiguration, String classification, String regex, boolean caseSensitive, int groupNumber) {
        super(FilterType.IDENTIFIER, filterConfiguration);

        final Pattern pattern;

        if(caseSensitive) {
            pattern = Pattern.compile(regex);
        } else {
            pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        }

        // TODO: Expose initialConfidence via the policy.
        // TODO: Expose the contextual terms via the policy.
        final FilterPattern id1 = new FilterPattern.FilterPatternBuilder(pattern, 0.90)
                .withClassification(classification)
                .withGroupNumber(groupNumber)
                .build();

        // There are no contextual terms because we don't know what they would be.
        // TODO: Let the user set a list of contextual terms?
        this.contextualTerms = new HashSet<>();
        this.analyzer = new Analyzer(contextualTerms, id1);

    }

    @Override
    public FilterResult filter(Policy policy, String context, String documentId, int piece, String input, Map<String, String> attributes) throws Exception {

        final List<Span> spans = findSpans(policy, analyzer, input, context, documentId, attributes);

        return new FilterResult(context, documentId, spans);

    }

}
