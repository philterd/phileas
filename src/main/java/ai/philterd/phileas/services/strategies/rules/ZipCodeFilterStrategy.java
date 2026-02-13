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
package ai.philterd.phileas.services.strategies.rules;

import ai.philterd.phileas.model.conditions.ParsedCondition;
import ai.philterd.phileas.model.conditions.ParserListener;
import ai.philterd.phileas.model.filtering.FilterPattern;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Replacement;
import ai.philterd.phileas.model.metadata.zipcode.ZipCodeMetadataRequest;
import ai.philterd.phileas.model.metadata.zipcode.ZipCodeMetadataResult;
import ai.philterd.phileas.model.metadata.zipcode.ZipCodeMetadataService;
import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.anonymization.AnonymizationService;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.utils.Encryption;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

public class ZipCodeFilterStrategy extends AbstractFilterStrategy {

    private static final Logger LOGGER = LogManager.getLogger(ZipCodeFilterStrategy.class);

    public static final String TRUNCATE = "truncate";
    public static final String POPULATION = "population";
    public static final String ZERO_LEADING = "zero_leading";

    private final FilterType filterType = FilterType.ZIP_CODE;

    private final transient ZipCodeMetadataService zipCodeMetadataService;

    public ZipCodeFilterStrategy() throws IOException {
        this.zipCodeMetadataService = new ZipCodeMetadataService();
    }

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    @Override
    public boolean evaluateCondition(Policy policy, String context, String token, String[] window, String condition, double confidence) {

        boolean conditionsSatisfied = false;

        final List<ParsedCondition> parsedConditions = ParserListener.getTerminals(condition);

        for(ParsedCondition parsedCondition : parsedConditions) {

            if(Strings.CI.equals(POPULATION, parsedCondition.getField())) {

                final int value = Integer.parseInt(parsedCondition.getValue());

                final ZipCodeMetadataResult response = zipCodeMetadataService.getMetadata(new ZipCodeMetadataRequest(token));

                if(response.isExists()) {

                    final long populationForZipCode = response.getPopulation();

                    if (Strings.CI.equals(POPULATION, parsedCondition.getField())) {

                        conditionsSatisfied = switch (parsedCondition.getOperator()) {
                            case GREATER_THAN -> (populationForZipCode > value);
                            case LESS_THAN -> (populationForZipCode < value);
                            case GREATER_THAN_EQUALS -> (populationForZipCode >= value);
                            case LESS_THAN_EQUALS -> (populationForZipCode <= value);
                            case EQUALS -> (populationForZipCode == value);
                            case NOT_EQUALS -> (populationForZipCode != value);
                            default -> conditionsSatisfied;
                        };

                    }

                } else {

                    // The zip code did not exist.
                    conditionsSatisfied = false;

                }

            } else if(Strings.CI.equals(TOKEN, parsedCondition.getField())) {

                conditionsSatisfied = evaluateTokenCondition(parsedCondition, token, window);

            } else if(Strings.CI.equals(CONTEXT, parsedCondition.getField())) {

                final String conditionContext = parsedCondition.getValue();

                conditionsSatisfied = switch (parsedCondition.getOperator()) {
                    case EQUALS -> (Strings.CI.equals("\"" + context + "\"", conditionContext));
                    case NOT_EQUALS -> !(Strings.CI.equals("\"" + context + "\"", conditionContext));
                    default -> conditionsSatisfied;
                };

            } else if(Strings.CI.equals(CONFIDENCE, parsedCondition.getField())) {

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

            }
            LOGGER.debug("Condition for [{}] satisfied: {}", condition, conditionsSatisfied);

            // Short-circuit if we have a failure.
            if(!conditionsSatisfied) break;

        }

        return conditionsSatisfied;

    }

    @Override
    public Replacement getReplacement(String label, String context, String token, String[] window, Crypto crypto, FPE fpe, AnonymizationService anonymizationService, FilterPattern filterPattern) throws Exception {

        String replacement;
        String salt = "";

        if(Strings.CI.equals(strategy, REDACT)) {

            replacement = getRedactedToken(token, label, filterType);

        } else if(Strings.CI.equals(strategy, MASK)) {

            int characters = token.length();

            if(!Strings.CI.equals(maskLength, AbstractFilterStrategy.SAME)) {
                characters = Integer.parseInt(maskLength);
            }

            if (characters < 1) {
                characters = 5;
            }

            replacement = maskCharacter.repeat(characters);

        } else if(Strings.CI.equals(strategy, RANDOM_REPLACE)) {

            AnonymizationService as = anonymizationService;
            if (this.anonymizationService != null) {
                as = this.anonymizationService;
            }

            replacement = getAnonymizedToken(replacementScope, token, as, filterType.getType());

        } else if(Strings.CI.equals(strategy, STATIC_REPLACE)) {

            replacement = staticReplacement;

        } else if(Strings.CI.equals(strategy, TRUNCATE)) {

            int leaveCharacters = getValueOrDefault(truncateLeaveCharacters, 4);

            if (leaveCharacters < 1) {
                leaveCharacters = 1;
            }

            if(Strings.CI.equals(truncateDirection, LEADING)) {
                replacement = token.substring(0, leaveCharacters) + StringUtils.repeat(truncateCharacter, Math.min(token.length() - leaveCharacters, 5 - leaveCharacters));
            } else {
                replacement = StringUtils.repeat(truncateCharacter, Math.min(token.length() - leaveCharacters, 5 - leaveCharacters)) + token.substring(Math.min(token.length() - leaveCharacters, 5 - leaveCharacters), 5);
            }


        } else if(Strings.CI.equals(strategy, ZERO_LEADING)) {

            replacement = "000" + token.subSequence(3, 5);

        } else if(Strings.CI.equals(strategy, CRYPTO_REPLACE)) {

            replacement = "{{" + Encryption.encrypt(token, crypto) + "}}";

        } else if(Strings.CI.equals(strategy, HASH_SHA256_REPLACE)) {

            if(isSalt()) {
                salt = RandomStringUtils.secure().nextAlphanumeric(16);
            }

            replacement = DigestUtils.sha256Hex(token + salt);

        } else {

            // Default to redaction.
            replacement = getRedactedToken(token, label, filterType);

        }

        return new Replacement(replacement, salt);

    }

    public void setTruncateDigits(Integer truncateDigits) {
        setTruncateLeaveCharacters(truncateDigits);
    }

    public void setTruncateLeaveCharacters(Integer truncateLeaveCharacters) {

        // Make sure it is a valid value.
        if(truncateLeaveCharacters >= 1 && truncateLeaveCharacters <= 4) {
            this.truncateLeaveCharacters = truncateLeaveCharacters;
        } else {
            throw new IllegalArgumentException("Truncate length must be between 1 and 4, inclusive.");
        }

    }

}