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
package ai.philterd.phileas.services.strategies.ai;

import ai.philterd.phileas.model.conditions.ParsedCondition;
import ai.philterd.phileas.model.conditions.ParserListener;
import ai.philterd.phileas.model.filtering.FilterPattern;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Replacement;
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
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class PhEyeFilterStrategy extends AbstractFilterStrategy {

    private static final Logger LOGGER = LogManager.getLogger(PhEyeFilterStrategy.class);

    private final FilterType filterType = FilterType.PERSON;

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    @Override
    public boolean evaluateCondition(Policy policy, String context, String token, String[] window, String condition, double confidence) {

        final List<ParsedCondition> parsedConditions = ParserListener.getTerminals(condition);

        boolean conditionsSatisfied = false;

        for(ParsedCondition parsedCondition : parsedConditions) {

            if(Strings.CI.equals(TOKEN, parsedCondition.getField())) {

                conditionsSatisfied = evaluateTokenCondition(parsedCondition, token, window);

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

            } else if(Strings.CI.equals(CONTEXT, parsedCondition.getField())) {

                final String conditionContext = parsedCondition.getValue();

                conditionsSatisfied = switch (parsedCondition.getOperator()) {
                    case EQUALS -> (Strings.CI.equals("\"" + context + "\"", conditionContext));
                    case NOT_EQUALS -> !(Strings.CI.equals("\"" + context + "\"", conditionContext));
                    default -> conditionsSatisfied;
                };

            }

            // Short-circuit if we have a failure.
            if(!conditionsSatisfied) break;

        }

        return conditionsSatisfied;

    }

    @Override
    public Replacement getReplacement(String label, String context, String token, String[] window, Crypto crypto, FPE fpe, AnonymizationService anonymizationService, FilterPattern filterPattern) throws Exception {

        String replacement = null;
        String salt = "";

        if(Strings.CI.equals(strategy, REDACT)) {

            replacement = getRedactedToken(token, label, filterType);

        } else if(Strings.CI.equals(strategy, MASK)) {

            int characters = token.length();

            if(!Strings.CI.equals(maskLength, AbstractFilterStrategy.SAME)) {
                characters = Integer.parseInt(maskLength);
            }

            if(characters < 1) {
                characters = 5;
            }

            replacement = maskCharacter.repeat(characters);

        } else if(Strings.CI.equals(strategy, TRUNCATE)) {

            int leaveCharacters = getValueOrDefault(truncateLeaveCharacters, 4);

            if (leaveCharacters < 1) {
                leaveCharacters = 1;
            }

            if(Strings.CI.equals(truncateDirection, LEADING)) {
                replacement = token.substring(0, leaveCharacters) + StringUtils.repeat(truncateCharacter, token.length() - leaveCharacters);
            } else {
                replacement = StringUtils.repeat(truncateCharacter, token.length() - leaveCharacters) + token.substring(token.length() - leaveCharacters);
            }

        } else if(Strings.CI.equals(strategy, RANDOM_REPLACE)) {

            AnonymizationService as = anonymizationService;
            if (this.anonymizationService != null) {
                as = this.anonymizationService;
            }

            replacement = getAnonymizedToken(replacementScope, token, as, filterType.getType());

        } else if(Strings.CI.equals(strategy, STATIC_REPLACE)) {

            replacement = staticReplacement;

        } else if(Strings.CI.equals(strategy, CRYPTO_REPLACE)) {

            replacement = "{{" + Encryption.encrypt(token, crypto) + "}}";

        } else if(Strings.CI.equals(strategy, FPE_ENCRYPT_REPLACE)) {

            replacement = Encryption.formatPreservingEncrypt(fpe, token);

        } else if(Strings.CI.equals(strategy, HASH_SHA256_REPLACE)) {

            if (isSalt()) {
                salt = RandomStringUtils.secure().nextAlphanumeric(16);
            }

            replacement = DigestUtils.sha256Hex(token + salt);

        } else if(Strings.CI.equals(strategy, ABBREVIATE)) {

            // TODO: Make PER a constant somewhere.
            // Philter-NER is only returning PER entities at this point.
            if(Strings.CI.equals(label, "PER")) {
                replacement = WordUtils.initials(token, null);
            }

        } else {

            // Default to redaction.
            replacement = getRedactedToken(token, label, filterType);

        }

        return new Replacement(replacement, salt);

    }

}
