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
package ai.philterd.phileas.model.policy.filters.strategies;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.policy.Crypto;
import ai.philterd.phileas.model.policy.FPE;
import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.model.utils.Encryption;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class StandardFilterStrategy extends AbstractFilterStrategy {

    public Replacement getStandardReplacement(String label, String context, String documentId, String token,
                                      String[] window, Crypto crypto, FPE fpe,
                                      AnonymizationService anonymizationService,
                                      FilterPattern filterPattern, FilterType filterType) throws Exception {

        String replacement;
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

        } else if(StringUtils.equalsIgnoreCase(strategy, LAST_4)) {

            replacement = token.substring(token.length() - 4);

        } else if(StringUtils.equalsIgnoreCase(strategy, HASH_SHA256_REPLACE)) {

            if(isSalt()) {
                salt = RandomStringUtils.randomAlphanumeric(16);
            }

            replacement = DigestUtils.sha256Hex(token + salt);

        } else {

            // Default to redaction.
            replacement = getRedactedToken(token, label, filterType);

        }

        return new Replacement(replacement, salt);

    }

}