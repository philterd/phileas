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
import ai.philterd.phileas.services.Analyzer;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.policy.Policy;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class StreetAddressFilter extends RegexFilter {

    public StreetAddressFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.STREET_ADDRESS, filterConfiguration);

        final Pattern addressPattern = Pattern.compile("(?i)\\b\\d{1,6} +.*?\\b(avenue|ave|av e|cir|court|ct|street|st|drive|dr|lane|ln|road|rd|blvd|boulevard|plaza|parkway|pkwy)[.]?(([,\\s]+)?\\b(suite|ste|apt|apartment)[\\s]+\\d{1,6})?", Pattern.CASE_INSENSITIVE);
        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(addressPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("address");
        this.contextualTerms.add("location");

        this.analyzer = new Analyzer(contextualTerms, filterPattern);

    }

    @Override
    public FilterResult filter(Policy policy, String context, int piece, String input, Map<String, String> attributes) throws Exception {

        final List<Span> spans = findSpans(policy, analyzer, input, context, attributes);

        return new FilterResult(context, spans);

    }

}
