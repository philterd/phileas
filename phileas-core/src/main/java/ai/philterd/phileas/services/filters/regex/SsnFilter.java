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

public class SsnFilter extends RegexFilter {

    public SsnFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.SSN, filterConfiguration);

        final Pattern ssnPattern = Pattern.compile("\\b(?!000|666)[0-8][0-9]{2}[- ]?(?!00)[0-9]{2}[- ]?(?!0000)[0-9]{4}\\b");
        final FilterPattern ssn1 = new FilterPattern.FilterPatternBuilder(ssnPattern, 0.90).build();

        final Pattern tinPattern = Pattern.compile("\\b\\d{2}-\\d{7}\\b");
        final FilterPattern tin1 = new FilterPattern.FilterPatternBuilder(tinPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("ssn");
        this.contextualTerms.add("tin");
        this.contextualTerms.add("social");
        this.contextualTerms.add("ssid");

        this.analyzer = new Analyzer(contextualTerms, ssn1, tin1);

    }

    @Override
    public FilterResult filter(Policy policy, String context, String documentId, int piece, String input, Map<String, String> attributes) throws Exception {

        final List<Span> spans = findSpans(policy, analyzer, input, context, documentId, attributes);

        return new FilterResult(context, documentId, spans);

    }

}
