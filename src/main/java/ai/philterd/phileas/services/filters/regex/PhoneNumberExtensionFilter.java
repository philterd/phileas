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

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.rules.regex.RegexFilter;
import ai.philterd.phileas.model.objects.Analyzer;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.policy.Policy;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class PhoneNumberExtensionFilter extends RegexFilter {

    public PhoneNumberExtensionFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.PHONE_NUMBER_EXTENSION, filterConfiguration);

        final Pattern phoneNumberExtendionPattern = Pattern.compile("\\bx[0-9]+\\b");
        final FilterPattern phoneExtension1 = new FilterPattern.FilterPatternBuilder(phoneNumberExtendionPattern, 0.75).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("phone");
        this.contextualTerms.add("extension");
        this.contextualTerms.add("ext");

        this.analyzer = new Analyzer(contextualTerms, phoneExtension1);

    }

    @Override
    public FilterResult filter(Policy policy, String contextName, String documentId, int piece, String input, Map<String, String> attributes) throws Exception {

        final List<Span> spans = findSpans(policy, analyzer, input, contextName, documentId, attributes);

        return new FilterResult(contextName, documentId, spans);

    }

}
