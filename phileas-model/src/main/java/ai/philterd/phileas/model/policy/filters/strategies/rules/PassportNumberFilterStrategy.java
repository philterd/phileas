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
package ai.philterd.phileas.model.policy.filters.strategies.rules;

import ai.philterd.phileas.model.conditions.ParsedCondition;
import ai.philterd.phileas.model.conditions.ParserListener;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.policy.Crypto;
import ai.philterd.phileas.model.policy.FPE;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.policy.filters.strategies.StandardFilterStrategy;
import ai.philterd.phileas.model.services.AnonymizationService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PassportNumberFilterStrategy extends StandardFilterStrategy {

    private static final Logger LOGGER = LogManager.getLogger(PassportNumberFilterStrategy.class);

    private static final FilterType filterType = FilterType.PASSPORT_NUMBER;

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    @Override
    public boolean evaluateCondition(Policy policy, String context, String documentId, String token, String[] window, String condition, double confidence, Map<String, String> attributes) {

        boolean conditionsSatisfied = false;

        final List<ParsedCondition> parsedConditions = ParserListener.getTerminals(condition);

        for(ParsedCondition parsedCondition : parsedConditions) {

            if(StringUtils.equalsIgnoreCase(TOKEN, parsedCondition.getField())) {

                conditionsSatisfied = evaluateTokenCondition(parsedCondition, token, window);

            } else if(StringUtils.equalsIgnoreCase(CONTEXT, parsedCondition.getField())) {

                final String conditionContext = parsedCondition.getValue();

                conditionsSatisfied = switch (parsedCondition.getOperator()) {
                    case "==" -> (StringUtils.equalsIgnoreCase("\"" + context + "\"", conditionContext));
                    case "!=" -> !(StringUtils.equalsIgnoreCase("\"" + context + "\"", conditionContext));
                    default -> conditionsSatisfied;
                };

            } else if(StringUtils.equalsIgnoreCase(CONFIDENCE, parsedCondition.getField())) {

                final double threshold = Double.parseDouble(parsedCondition.getValue());

                conditionsSatisfied = switch (parsedCondition.getOperator()) {
                    case ">" -> (confidence > threshold);
                    case "<" -> (confidence < threshold);
                    case ">=" -> (confidence >= threshold);
                    case "<=" -> (confidence <= threshold);
                    case "==" -> (confidence == threshold);
                    case "!=" -> (confidence != threshold);
                    default -> conditionsSatisfied;
                };

            } else if(StringUtils.equalsIgnoreCase(CLASSIFICATION, parsedCondition.getField())) {

                final String conditionClassification = parsedCondition.getValue();

                conditionsSatisfied = switch (parsedCondition.getOperator()) {
                    case "==" -> (StringUtils.equalsIgnoreCase("\"" + attributes.getOrDefault("classification", "") + "\"", conditionClassification));
                    case "!=" -> !(StringUtils.equalsIgnoreCase("\"" + attributes.getOrDefault("classification", "") + "\"", conditionClassification));
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

            LOGGER.debug("Condition for [{}] satisfied: {}", condition, conditionsSatisfied);

            // Short-circuit if we have a failure.
            if(!conditionsSatisfied) break;

        }

        return conditionsSatisfied;

    }

    @Override
    public Replacement getReplacement(String label, String context, String documentId, String token, String[] window, Crypto crypto, FPE fpe, AnonymizationService anonymizationService, FilterPattern filterPattern) throws Exception {

        return getStandardReplacement(label, context, documentId, token, window, crypto, fpe, anonymizationService, filterPattern, filterType);

    }

}
