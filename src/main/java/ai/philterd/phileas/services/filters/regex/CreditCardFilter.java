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
import ai.philterd.phileas.model.filtering.ConfidenceModifier;
import ai.philterd.phileas.model.filtering.FilterPattern;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.Analyzer;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class CreditCardFilter extends RegexFilter {

    private final boolean onlyValidCreditCardNumbers;
    private final LuhnCheckDigit luhnCheckDigit;
    private final boolean ignoreWhenInUnixTimestamp;
    protected static final Logger LOGGER = LogManager.getLogger(CreditCardFilter.class);

    private final String UNIX_TIMESTAMP_REGEX = "1[5-8][0-9]{11}";

    final Pattern detailedSearch = Pattern.compile("""
                (
                    4[0-9]{12}(?:[0-9]{3})?             # Visa
                    |  (?:5[1-5][0-9]{2}                # MasterCard
                        | 222[1-9]|22[3-9][0-9]|2[3-6][0-9]{2}|27[01][0-9]|2720)[0-9]{12}
                    |  3[47][0-9]{13}                   # American Express
                    |  3(?:0[0-5]|[68][0-9])[0-9]{11}   # Diners Club
                    |  6(?:011|5[0-9]{2})[0-9]{12}      # Discover
                    |  (?:2131|1800|35\\d{3})\\d{11}    # JCB
                )
            """, Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);

    public CreditCardFilter(FilterConfiguration filterConfiguration, boolean onlyValidCreditCardNumbers,
                            boolean ignoreWhenInUnixTimestamp, boolean onlyWordBoundaries) {
        super(FilterType.CREDIT_CARD, filterConfiguration);

        this.onlyValidCreditCardNumbers = onlyValidCreditCardNumbers;
        this.luhnCheckDigit = new LuhnCheckDigit();
        this.ignoreWhenInUnixTimestamp = ignoreWhenInUnixTimestamp;

        // Modify the confidence based on the characters around the span.
        final List<ConfidenceModifier> confidenceModifiers = new ArrayList<>(List.of(
                new ConfidenceModifier(0.6, ConfidenceModifier.ConfidenceCondition.CHARACTER_SEQUENCE_BEFORE, "-"),
                new ConfidenceModifier(0.6, ConfidenceModifier.ConfidenceCondition.CHARACTER_SEQUENCE_AFTER, "-"),
                new ConfidenceModifier(0.5, ConfidenceModifier.ConfidenceCondition.CHARACTER_SEQUENCE_SURROUNDING, "-")));

        // See http://regular-expressions.info/creditcard.html
        final String genericCards = "(?:\\d[ -]*?){13,16}";

        double initialConfidence;
        Pattern creditCard;
        FilterPattern creditCardPattern;

        if (onlyWordBoundaries) {
            creditCard = Pattern.compile("\\b" + genericCards + "\\b", Pattern.CASE_INSENSITIVE);
            initialConfidence = 0.9;
            creditCardPattern = new FilterPattern.FilterPatternBuilder(creditCard, initialConfidence)
                    .withConfidenceModifiers(confidenceModifiers).build();
        } else {
            creditCard = Pattern.compile("(?=(" + genericCards + "))", Pattern.CASE_INSENSITIVE);
            initialConfidence = 0.7;
            confidenceModifiers.add(
                    new ConfidenceModifier(
                            ConfidenceModifier.ConfidenceCondition.CHARACTER_REGEX_SURROUNDING,
                            0.2, Pattern.compile("[\\s\\-]"))
            );
            creditCardPattern = new FilterPattern.FilterPatternBuilder(creditCard, initialConfidence)
                    .withConfidenceModifiers(confidenceModifiers).withGroupNumber(1).build();
        }


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
    public Filtered filter(Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(policy, analyzer, input, context);

        LOGGER.debug("Found {} spans", spans.size());
        for (final Iterator<Span> iterator = spans.iterator(); iterator.hasNext(); ) {

            final Span span = iterator.next();

            if (ignoreWhenInUnixTimestamp && span.getText().matches(UNIX_TIMESTAMP_REGEX)) {
                LOGGER.debug("Ignoring unix timestamp");
                iterator.remove();
            }

            if (onlyValidCreditCardNumbers) {
                final String creditCardNumber = input.substring(span.getCharacterStart(), span.getCharacterEnd())
                        .replaceAll(" ", "")
                        .replaceAll("-", "");

                if (!detailedSearch.matcher(creditCardNumber).matches() || !luhnCheckDigit.isValid(creditCardNumber)) {
                    LOGGER.debug("Ignoring a number that doesn't quite fit the credit card number patterns or LUHN");
                    iterator.remove();
                }

            }

        }

        return new Filtered(context, spans);

    }

}