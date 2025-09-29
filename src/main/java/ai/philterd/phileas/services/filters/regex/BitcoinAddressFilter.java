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

public class BitcoinAddressFilter extends RegexFilter {

    public BitcoinAddressFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.BITCOIN_ADDRESS, filterConfiguration);

        final Pattern bitcoinPattern = Pattern.compile("\\b[13][a-km-zA-HJ-NP-Z1-9]{25,34}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern bitcoin1 = new FilterPattern.FilterPatternBuilder(bitcoinPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("bitcoin");
        this.contextualTerms.add("wallet");
        this.contextualTerms.add("btc");
        this.contextualTerms.add("crypto");

        this.analyzer = new Analyzer(contextualTerms, bitcoin1);

    }

    @Override
    public FilterResult filter(Policy policy, String contextName, Map<String, String> context, String documentId, int piece, String input, Map<String, String> attributes) throws Exception {

        final List<Span> spans = findSpans(policy, analyzer, input, contextName, context, documentId, attributes);

        return new FilterResult(contextName, documentId, spans);

    }

}