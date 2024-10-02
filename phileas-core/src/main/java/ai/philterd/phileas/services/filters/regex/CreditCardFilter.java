/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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
import ai.philterd.phileas.model.objects.ConfidenceModifier;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.Policy;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class CreditCardFilter extends RegexFilter {

    private final boolean onlyValidCreditCardNumbers;
    private final LuhnCheckDigit luhnCheckDigit;
    private final boolean ignoreWhenInUnixTimestamp;

    private final String UNIX_TIMESTAMP_REGEX = "1[5-8][0-9]{11}";

    public CreditCardFilter(FilterConfiguration filterConfiguration, boolean onlyValidCreditCardNumbers,
                            boolean ignoreWhenInUnixTimestamp) {

        super(FilterType.CREDIT_CARD, filterConfiguration);

        this.onlyValidCreditCardNumbers = onlyValidCreditCardNumbers;
        this.luhnCheckDigit = new LuhnCheckDigit();
        this.ignoreWhenInUnixTimestamp = ignoreWhenInUnixTimestamp;

        // Modify the confidence based on the characters around the span.
        final List<ConfidenceModifier> confidenceModifiers = List.of(
                new ConfidenceModifier(0.6, ConfidenceModifier.ConfidenceCondition.CHARACTER_SEQUENCE_BEFORE, "-"),
                new ConfidenceModifier(0.6, ConfidenceModifier.ConfidenceCondition.CHARACTER_SEQUENCE_AFTER, "-"),
                new ConfidenceModifier(0.5, ConfidenceModifier.ConfidenceCondition.CHARACTER_SEQUENCE_SURROUNDING, "-"));

        // See http://regular-expressions.info/creditcard.html
        final Pattern creditCard = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern creditCardPattern = new FilterPattern.FilterPatternBuilder(creditCard, 0.90)
                .withConfidenceModifiers(confidenceModifiers).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("credit");
        this.contextualTerms.add("card");
        this.contextualTerms.add("american express");
        this.contextualTerms.add("amex");
        this.contextualTerms.add("discover");
        this.contextualTerms.add("jcb");
        this.contextualTerms.add("diners");

        this.analyzer = new Analyzer(contextualTerms, creditCardPattern);

    }

    @Override
    public FilterResult filter(Policy policy, String context, String documentId, int piece, String input, Map<String, String> attributes) throws Exception {

        final List<Span> spans = findSpans(policy, analyzer, input, context, documentId, attributes);

        for (final Iterator<Span> iterator = spans.iterator(); iterator.hasNext();) {

            final Span span = iterator.next();

            if (ignoreWhenInUnixTimestamp && span.getText().matches(UNIX_TIMESTAMP_REGEX)) {
                spans.remove(span);
            }

            if (onlyValidCreditCardNumbers) {

                final String creditCardNumber = input.substring(span.getCharacterStart(), span.getCharacterEnd())
                        .replaceAll(" ", "")
                        .replaceAll("-", "");

                if (!luhnCheckDigit.isValid(creditCardNumber)) {
                    spans.remove(span);
                }

            }

        }

        return new FilterResult(context, documentId, spans);

    }

}