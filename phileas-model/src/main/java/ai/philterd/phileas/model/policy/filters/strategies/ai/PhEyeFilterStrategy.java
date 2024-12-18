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
package ai.philterd.phileas.model.policy.filters.strategies.ai;

import ai.philterd.phileas.model.conditions.ParsedCondition;
import ai.philterd.phileas.model.conditions.ParserListener;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.policy.Crypto;
import ai.philterd.phileas.model.policy.FPE;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.policy.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.model.utils.Encryption;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class PhEyeFilterStrategy extends AbstractFilterStrategy {

    private static final Logger LOGGER = LogManager.getLogger(PhEyeFilterStrategy.class);

    private static final FilterType filterType = FilterType.PERSON;

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    @Override
    public boolean evaluateCondition(Policy policy, String context, String documentId, String token, String[] window, String condition, double confidence, Map<String, String> attributes) {

        final List<ParsedCondition> parsedConditions = ParserListener.getTerminals(condition);

        boolean conditionsSatisfied = false;

        for(ParsedCondition parsedCondition : parsedConditions) {

            if(StringUtils.equalsIgnoreCase(TOKEN, parsedCondition.getField())) {

                conditionsSatisfied = evaluateTokenCondition(parsedCondition, token, window);

            } else if(StringUtils.equalsIgnoreCase(CLASSIFICATION, parsedCondition.getField()) || StringUtils.equalsIgnoreCase(TYPE, parsedCondition.getField())) {

                final String entityType = attributes.getOrDefault("classification", "");

                if(parsedCondition.getOperator().equalsIgnoreCase("==")) {
                    conditionsSatisfied = StringUtils.equalsIgnoreCase(entityType, parsedCondition.getValue());
                } else if(parsedCondition.getOperator().equalsIgnoreCase("!=")) {
                    conditionsSatisfied = !StringUtils.equalsIgnoreCase(entityType, parsedCondition.getValue());
                } else {
                    // Invalid operator.
                    LOGGER.warn("Invalid comparator on NER filter strategy condition: {}", condition);
                }

                break;

            } else if(StringUtils.equalsIgnoreCase(CONFIDENCE, parsedCondition.getField())) {

                final double threshold = Double.parseDouble(parsedCondition.getValue());

                conditionsSatisfied = switch (parsedCondition.getOperator()) {
                    case GREATER_THAN -> (confidence > threshold);
                    case LESS_THAN -> (confidence < threshold);
                    case GREATER_THAN_EQUALS -> (confidence >= threshold);
                    case LESS_THAN_EQUALS -> (confidence <= threshold);
                    case EQUALS -> (confidence == threshold);
                    case NOT_EQUALS -> (confidence != threshold);
                    default -> conditionsSatisfied;
                };

            } else if(StringUtils.equalsIgnoreCase(CONTEXT, parsedCondition.getField())) {

                final String conditionContext = parsedCondition.getValue();

                conditionsSatisfied = switch (parsedCondition.getOperator()) {
                    case EQUALS -> (StringUtils.equalsIgnoreCase("\"" + context + "\"", conditionContext));
                    case NOT_EQUALS -> !(StringUtils.equalsIgnoreCase("\"" + context + "\"", conditionContext));
                    default -> conditionsSatisfied;
                };

            } else if(StringUtils.equalsIgnoreCase(SENTIMENT, parsedCondition.getField())) {

                // If there is no sentiment attribute, the condition is automatically not satisfied.
                if(attributes.containsKey("sentiment")) {

                    final int documentSentiment = Integer.parseInt(attributes.get("sentiment"));
                    final int sentimentCondition = Integer.parseInt(parsedCondition.getValue());

                    conditionsSatisfied = switch (parsedCondition.getOperator()) {
                        case GREATER_THAN -> (documentSentiment > sentimentCondition);
                        case LESS_THAN -> (documentSentiment < sentimentCondition);
                        case GREATER_THAN_EQUALS -> (documentSentiment >= sentimentCondition);
                        case LESS_THAN_EQUALS -> (documentSentiment <= sentimentCondition);
                        case EQUALS -> (documentSentiment == sentimentCondition);
                        case NOT_EQUALS -> (documentSentiment != sentimentCondition);
                        default -> conditionsSatisfied;
                    };

                } else {
                    conditionsSatisfied = false;
                }

            }

            // Short-circuit if we have a failure.
            if(!conditionsSatisfied) break;

        }

        return conditionsSatisfied;

    }

    @Override
    public Replacement getReplacement(String label, String context, String documentId, String token, String[] window, Crypto crypto, FPE fpe, AnonymizationService anonymizationService, FilterPattern filterPattern) throws Exception {

        String replacement = null;
        String salt = "";

        if(StringUtils.equalsIgnoreCase(strategy, REDACT)) {

            replacement = getRedactedToken(token, label, filterType);

        } else if(StringUtils.equalsIgnoreCase(strategy, MASK)) {

            int characters = token.length();

            if(!StringUtils.equalsIgnoreCase(maskLength, AbstractFilterStrategy.SAME)) {
                characters = Integer.parseInt(maskLength);
            }

            if(characters < 1) {
                characters = 5;
            }

            replacement = maskCharacter.repeat(characters);

        } else if(StringUtils.equalsIgnoreCase(strategy, TRUNCATE)) {

            int truncateLength = getValueOrDefault(truncateDigits, 4);

            if (truncateLength < 1) {
                truncateLength = 1;
            }

            if(StringUtils.equalsIgnoreCase(truncateDirection, LEADING)) {
                replacement = token.substring(0, truncateLength) + StringUtils.repeat(truncateCharacter, token.length() - truncateLength);
            } else {
                replacement = StringUtils.repeat(truncateCharacter, token.length() - truncateLength) + token.substring(token.length() - truncateLength);
            }

        } else if(StringUtils.equalsIgnoreCase(strategy, RANDOM_REPLACE)) {

            // Default to document scope.
            String scope = REPLACEMENT_SCOPE_DOCUMENT;

            if (StringUtils.equalsIgnoreCase(replacementScope, REPLACEMENT_SCOPE_CONTEXT)) {
                scope = REPLACEMENT_SCOPE_CONTEXT;
            }

            replacement = getAnonymizedToken(scope, token, anonymizationService);

        } else if(StringUtils.equalsIgnoreCase(strategy, STATIC_REPLACE)) {

            replacement = staticReplacement;

        } else if(StringUtils.equalsIgnoreCase(strategy, CRYPTO_REPLACE)) {

            replacement = "{{" + Encryption.encrypt(token, crypto) + "}}";

        } else if(StringUtils.equalsIgnoreCase(strategy, FPE_ENCRYPT_REPLACE)) {

            replacement = Encryption.formatPreservingEncrypt(fpe, token);

        } else if(StringUtils.equalsIgnoreCase(strategy, HASH_SHA256_REPLACE)) {

            if (isSalt()) {
                salt = RandomStringUtils.randomAlphanumeric(16);
            }

            replacement = DigestUtils.sha256Hex(token + salt);

        } else if(StringUtils.equalsIgnoreCase(strategy, ABBREVIATE)) {

            // TODO: Make PER a constant somewhere.
            // Philter-NER is only returning PER entities at this point.
            if(StringUtils.equalsIgnoreCase(label, "PER")) {
                replacement = WordUtils.initials(token, null);
            }

        } else {

            // Default to redaction.
            replacement = getRedactedToken(token, label, filterType);

        }

        return new Replacement(replacement, salt);

    }

}
