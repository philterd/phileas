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

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class CurrencyFilter extends RegexFilter {

    public CurrencyFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.CURRENCY, filterConfiguration);

        // Pattern 1: Prefix Symbol (e.g., €1,450.00, € 1.500,00, $35.53, £250.00, ¥5000, ₹500, $3,450.75 USD)
        final Pattern currencyPattern1 = Pattern.compile("[$\\u20AC\\u00A3\\u00A5\\u20B9]\\s*[0-9.,]*[0-9](?:\\s*\\b(?:USD|EUR|GBP|JPY|CAD|AUD|INR)\\b)?", Pattern.CASE_INSENSITIVE);
        final FilterPattern currency1 = new FilterPattern.FilterPatternBuilder(currencyPattern1, 0.80).build();

        // Pattern 2: Suffix Symbol (e.g., 1.450,00 €, 250,50 £, 5000 ¥, 500 ₹)
        final Pattern currencyPattern2 = Pattern.compile("(?<![$\\u20AC\\u00A3\\u00A5\\u20B9])[0-9.,]*[0-9]\\s*[$\\u20AC\\u00A3\\u00A5\\u20B9](?!\\d)", Pattern.CASE_INSENSITIVE);
        final FilterPattern currency2 = new FilterPattern.FilterPatternBuilder(currencyPattern2, 0.80).build();

        // Pattern 3: Prefix ISO Code (e.g., GBP 250.00, EUR 500.00, CAD 150.00, AUD 75.50, JPY 10000, USD 1,234.56, INR 2,500)
        final Pattern currencyPattern3 = Pattern.compile("\\b(?:USD|EUR|GBP|JPY|CAD|AUD|INR)\\b\\s*[0-9.,]*[0-9]", Pattern.CASE_INSENSITIVE);
        final FilterPattern currency3 = new FilterPattern.FilterPatternBuilder(currencyPattern3, 0.80).build();

        // Pattern 4: Suffix ISO Code (e.g., 500 EUR, 250 GBP, 150.00 CAD, 75.50 AUD, 10000 JPY, 1234.56 USD, 2500.00 INR)
        final Pattern currencyPattern4 = Pattern.compile("[0-9.,]*[0-9]\\s*\\b(?:USD|EUR|GBP|JPY|CAD|AUD|INR)\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern currency4 = new FilterPattern.FilterPatternBuilder(currencyPattern4, 0.80).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("dollars");
        this.contextualTerms.add("amount");
        this.contextualTerms.add("euros");
        this.contextualTerms.add("pounds");
        this.contextualTerms.add("yen");
        this.contextualTerms.add("rupees");
        this.contextualTerms.add("currency");
        this.contextualTerms.add("price");
        this.contextualTerms.add("cost");
        this.contextualTerms.add("fee");
        this.contextualTerms.add("balance");

        this.analyzer = new Analyzer(contextualTerms, currency1, currency2, currency3, currency4);

    }

    @Override
    public Filtered filter(ContextService contextService, Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(contextService, policy, analyzer, input, context);

        final List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        return new Filtered(context, nonOverlappingSpans);

    }

}
