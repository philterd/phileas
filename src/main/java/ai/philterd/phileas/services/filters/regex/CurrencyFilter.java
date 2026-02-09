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

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class CurrencyFilter extends RegexFilter {

    public CurrencyFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.CURRENCY, filterConfiguration);

        // See https://stackoverflow.com/a/14174261/1428388
        final Pattern currencyPattern1 = Pattern.compile("\\$\\d*(?:.(\\d+))?", Pattern.CASE_INSENSITIVE);
        final FilterPattern currency1 = new FilterPattern.FilterPatternBuilder(currencyPattern1, 0.80).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("dollars");
        this.contextualTerms.add("amount");

        this.analyzer = new Analyzer(contextualTerms, currency1);

    }

    @Override
    public Filtered filter(Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(policy, analyzer, input, context);

        final List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        return new Filtered(context, nonOverlappingSpans);

    }

}
