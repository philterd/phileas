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
package ai.philterd.phileas.services.strategies;

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Replacement;
import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.services.anonymization.AnonymizationService;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.utils.Encryption;
import ai.philterd.phileas.utils.FormatPreservingEncryptionException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Base for strategies that use the standard replacement handling (redact, replace, encrypt, anonymize). */
public abstract class StandardFilterStrategy extends AbstractFilterStrategy {

    private static final Logger LOGGER = LogManager.getLogger(StandardFilterStrategy.class);

    public Replacement getStandardReplacement(ContextService contextService, String label, String token,
                                      Crypto crypto, FPE fpe,
                                      AnonymizationService anonymizationService,
                                      FilterType filterType) throws Exception {

        String replacement;
        String salt = "";

        // The strategy that actually produces the replacement. For MAP_REPLACE this becomes the
        // fallback strategy when the token is absent from the lookup table and no generator produces
        // a value; for every other strategy it is simply the strategy itself.
        String effectiveStrategy = strategy;

        final boolean mapReplaceContextScope =
                Strings.CI.equals(strategy, MAP_REPLACE) && Strings.CI.equals(replacementScope, REPLACEMENT_SCOPE_CONTEXT);

        if(Strings.CI.equals(strategy, MAP_REPLACE)) {

            // Resolve the lookup-table or generator replacement. In CONTEXT scope this reuses the same
            // context-scoped caching as RANDOM_REPLACE (computeReplacementIfAbsent), so a repeated value
            // yields the same output and the generator is invoked at most once for it. The generator's
            // PII re-scan runs against a throwaway context, so it never mutates this context and is safe
            // to run inside computeReplacementIfAbsent. A miss (no mapping and no accepted generator
            // output) returns null and falls through to the fallback strategy.
            final String resolved = mapReplaceContextScope
                    ? contextService.computeReplacementIfAbsent(token, filterType.getType(), () -> resolveMapReplacement(token, label))
                    : resolveMapReplacement(token, label);

            if(resolved != null) {
                return new Replacement(resolved, salt);
            }

            // Fallback. The fallback enum never includes MAP_REPLACE, but guard against recursion
            // anyway so a hand-written policy can never loop.
            effectiveStrategy = getValueOrDefault(fallbackStrategy, REDACT);
            if(Strings.CI.equals(effectiveStrategy, MAP_REPLACE)) {
                effectiveStrategy = REDACT;
            }

        }

        if(Strings.CI.equals(effectiveStrategy, REDACT)) {

            replacement = getRedactedToken(token, label, filterType);

        } else if(Strings.CI.equals(effectiveStrategy, MASK)) {

            int characters = token.length();

            if(maskLength != null && !maskLength.equals("null") && !Strings.CI.equals(maskLength, AbstractFilterStrategy.SAME)) {
                characters = Integer.parseInt(maskLength);
            }

            if(characters < 1) {
                characters = 5;
            }

            replacement = maskCharacter.repeat(characters);

        } else if(Strings.CI.equals(effectiveStrategy, TRUNCATE)) {

            int leaveCharacters = getValueOrDefault(truncateLeaveCharacters, 4);

            if (leaveCharacters < 1) {
                leaveCharacters = 1;
            }

            if(Strings.CI.equals(truncateDirection, LEADING)) {
                replacement = token.substring(0, leaveCharacters) + StringUtils.repeat(truncateCharacter, token.length() - leaveCharacters);
            } else {
                replacement = StringUtils.repeat(truncateCharacter, token.length() - leaveCharacters) + token.substring(token.length() - leaveCharacters);
            }

        } else if(Strings.CI.equals(effectiveStrategy, RANDOM_REPLACE)) {

            AnonymizationService as = anonymizationService;
            if (this.anonymizationService != null) {
                as = this.anonymizationService;
            }

            replacement = getAnonymizedToken(contextService, replacementScope, token, as, filterType.getType());

        } else if(Strings.CI.equals(effectiveStrategy, STATIC_REPLACE)) {

            replacement = staticReplacement;

        } else if(Strings.CI.equals(effectiveStrategy, CRYPTO_REPLACE)) {

            replacement = "{{" + Encryption.encrypt(token, crypto) + "}}";

        } else if(Strings.CI.equals(effectiveStrategy, FPE_ENCRYPT_REPLACE)) {

            try {
                replacement = Encryption.formatPreservingEncrypt(fpe, token);
            } catch (final FormatPreservingEncryptionException e) {
                // This token cannot be format-preserving encrypted (for example, its content is
                // outside FF3's supported length range). Fall back to redaction so the token is
                // still redacted - one such token must not abort redaction of the whole document,
                // and the original value must never be emitted. The token is not logged.
                LOGGER.warn("Could not format-preserving encrypt a {} value; falling back to redaction. Reason: {}",
                        filterType.getType(), e.getMessage());
                replacement = getRedactedToken(token, label, filterType);
            }

        } else if(Strings.CI.equals(effectiveStrategy, LAST_4)) {

            replacement = token.substring(token.length() - 4);

        } else if(Strings.CI.equals(effectiveStrategy, HASH_SHA256_REPLACE)) {

            if(isSalt()) {
                salt = RandomStringUtils.secure().nextAlphanumeric(16);
            }

            replacement = DigestUtils.sha256Hex(token + salt);

        } else if(Strings.CI.equals(effectiveStrategy, ABBREVIATE)) {

            // Reduce the detected value to the uppercase initials of its words, e.g. "John Smith" (or
            // "john smith") becomes "JS". Shared with PhEyeFilterStrategy via the base class so every
            // filter type abbreviates identically.
            replacement = abbreviate(token);

        } else {

            // Default to redaction.
            replacement = getRedactedToken(token, label, filterType);

        }

        // Cache a MAP_REPLACE fallback result in CONTEXT scope so the generator is not retried for a
        // repeated value that is not in the lookup table. (A map hit or accepted generator value was
        // already cached above by computeReplacementIfAbsent; only the fallback path reaches here.)
        if(mapReplaceContextScope) {
            contextService.putReplacement(token, replacement, filterType.getType());
        }

        return new Replacement(replacement, salt);

    }

    /**
     * Resolves a MAP_REPLACE replacement from the lookup table or the generator. Returns the mapped
     * value if the token is in the table; otherwise the generator's output if one is configured and it
     * produces an accepted value; otherwise {@code null} to signal that the caller should apply the
     * fallback strategy.
     * @param token The detected value.
     * @param label The entity label of the detected value.
     * @return The resolved replacement, or {@code null} if neither the table nor the generator yields one.
     */
    private String resolveMapReplacement(final String token, final String label) {

        final String mapped = lookupMapping(token);
        if(mapped != null) {
            return mapped;
        }

        // Skip the generator while re-scanning a previously generated value: the re-scan runs the
        // filter pipeline over a candidate, and invoking a generator there would recurse.
        if(replacementGenerator != null && !isRescanning()) {
            try {
                final String generated = replacementGenerator.generate(token, label);
                if(isAcceptableGeneratedValue(token, generated)) {
                    return generated;
                }
            } catch (final Exception e) {
                // The token is not logged.
                LOGGER.warn("Generator '{}' failed for a {} value; falling back to {}. Reason: {}",
                        generator, getFilterType().getType(), fallbackStrategy, e.getMessage());
            }
        }

        return null;

    }

    /**
     * Validates a generated replacement before it is used. A value is rejected (and the strategy falls
     * back) when it is blank, equals the original token after case normalization, or is found by the
     * re-scan to contain reintroduced PII. This ensures the generator can never leave the value
     * effectively unredacted or emit new sensitive information.
     * @param token The original detected value.
     * @param generated The generator's output.
     * @return {@code true} if the generated value is acceptable; otherwise {@code false}.
     */
    private boolean isAcceptableGeneratedValue(final String token, final String generated) {

        if(StringUtils.isBlank(generated)) {
            return false;
        }

        // Reject a replacement that is just the original value again (case-insensitively), which would
        // leave the token effectively unredacted.
        if(Strings.CI.equals(generated.trim(), token.trim())) {
            LOGGER.warn("Generator '{}' returned the original value for a {} value; falling back to {}.",
                    generator, getFilterType().getType(), fallbackStrategy);
            return false;
        }

        // Re-scan the generated value to confirm the generator did not reintroduce PII.
        if(replacementValidator != null && replacementValidator.containsPii(generated)) {
            LOGGER.warn("Generator '{}' produced a {} replacement containing detectable PII; falling back to {}.",
                    generator, getFilterType().getType(), fallbackStrategy);
            return false;
        }

        return true;

    }

}