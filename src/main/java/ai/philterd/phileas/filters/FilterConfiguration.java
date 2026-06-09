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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.policy.IgnoredPattern;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.CRYPTO_REPLACE;
import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.FPE_ENCRYPT_REPLACE;

public class FilterConfiguration {

    private final List<? extends AbstractFilterStrategy> strategies;
    private final ContextService contextService;
    private final Random random;
    private final Set<String> ignored;
    private final Set<String> ignoredFiles;
    private final List<IgnoredPattern> ignoredPatterns;
    private final Crypto crypto;
    private final FPE fpe;
    private final int windowSize;
    private final int priority;
    private final long regexTimeoutMs;

    private FilterConfiguration(
            final List<? extends AbstractFilterStrategy> strategies,
            final ContextService contextService,
            final Random random,
            final Set<String> ignored,
            final Set<String> ignoredFiles,
            final List<IgnoredPattern> ignoredPatterns,
            final Crypto crypto,
            final FPE fpe,
            final int windowSize,
            final int priority,
            final long regexTimeoutMs
    ) {

        this.strategies = strategies;
        this.contextService = contextService;
        this.random = random;
        this.ignored = ignored;
        this.ignoredFiles = ignoredFiles;
        this.ignoredPatterns = ignoredPatterns;
        this.crypto = crypto;
        this.fpe = fpe;
        this.windowSize = windowSize;
        this.priority = priority;
        this.regexTimeoutMs = regexTimeoutMs;

    }

    public static class FilterConfigurationBuilder {

        private List<? extends AbstractFilterStrategy> strategies;
        private ContextService contextService;
        private Random random;
        private Set<String> ignored;
        private Set<String> ignoredFiles;
        private List<IgnoredPattern> ignoredPatterns;
        private Crypto crypto;
        private FPE fpe;
        private int windowSize;
        private int priority;
        // Defaults so existing builders are protected without changes; a value <= 0 disables the guard.
        private long regexTimeoutMs = 1000;

        public FilterConfiguration build() {

            // Validate the configuration. This throws an exception if it is invalid.
            validate();

            return new FilterConfiguration(
                    strategies,
                    contextService,
                    random,
                    ignored,
                    ignoredFiles,
                    ignoredPatterns,
                    crypto,
                    fpe,
                    windowSize,
                    priority,
                    regexTimeoutMs
            );

        }

        /**
         * Validate the configuration of the filter.
         * Throw an exception if the configuration is invalid.
         */
        private void validate() {

            if(CollectionUtils.isNotEmpty(strategies)) {

                for (final AbstractFilterStrategy strategy : strategies) {

                    if (Strings.CI.equals(FPE_ENCRYPT_REPLACE, strategy.getStrategy())) {

                        if (this.fpe != null) {

                            if (StringUtils.isEmpty(this.fpe.getKey())) {
                                throw new RuntimeException("Invalid configuration for filter: Missing FPE encryption key.");
                            }

                            if (StringUtils.isEmpty(this.fpe.getTweak())) {
                                throw new RuntimeException("Invalid configuration for filter: Missing FPE encryption tweak value.");
                            }

                        } else {
                            throw new RuntimeException("Invalid configuration for filter: Missing FPE encryption property.");
                        }

                    } else if (Strings.CI.equals(CRYPTO_REPLACE, strategy.getStrategy())) {

                        if (this.crypto != null) {

                            final String cryptoKey = this.crypto.getKey();

                            if (StringUtils.isEmpty(cryptoKey)) {
                                throw new RuntimeException("Invalid configuration for filter: Missing crypto encryption key.");
                            }

                            // Validate the key is decodable hex of a legal AES length so a bad key fails
                            // fast at configuration time rather than per-document at encryption time. The
                            // key value itself is never included in the message.
                            final byte[] cryptoKeyBytes;
                            try {
                                cryptoKeyBytes = Hex.decodeHex(cryptoKey);
                            } catch (final DecoderException ex) {
                                throw new RuntimeException("Invalid configuration for filter: The crypto encryption key is not valid hexadecimal.");
                            }
                            if (cryptoKeyBytes.length != 16 && cryptoKeyBytes.length != 24 && cryptoKeyBytes.length != 32) {
                                throw new RuntimeException("Invalid configuration for filter: The crypto encryption key must be a 128-, 192-, "
                                        + "or 256-bit AES key (16, 24, or 32 bytes); got " + cryptoKeyBytes.length + " bytes.");
                            }

                            // No IV is required: AES-GCM generates a fresh random nonce per value,
                            // so the policy's iv (if any) is not used.

                        } else {
                            throw new RuntimeException("Invalid configuration for filter: Missing crypto encryption property.");
                        }

                    }

                }

            }

        }

        public FilterConfigurationBuilder withStrategies(List<? extends AbstractFilterStrategy> strategies) {
            this.strategies = strategies;
            return this;
        }

        public FilterConfigurationBuilder withContextService(ContextService contextService) {
            this.contextService = contextService;
            return this;
        }

        public FilterConfigurationBuilder withRandom(Random random) {
            this.random = random;
            return this;
        }

        public FilterConfigurationBuilder withIgnored(Set<String> ignored) {
            this.ignored = ignored;
            return this;
        }

        public FilterConfigurationBuilder withIgnoredFiles(Set<String> ignoredFiles) {
            this.ignoredFiles = ignoredFiles;
            return this;
        }

        public FilterConfigurationBuilder withIgnoredPatterns(List<IgnoredPattern> ignoredPatterns) {
            this.ignoredPatterns = ignoredPatterns;
            return this;
        }

        public FilterConfigurationBuilder withCrypto(Crypto crypto) {
            this.crypto = crypto;
            return this;
        }

        public FilterConfigurationBuilder withFPE(FPE fpe) {
            this.fpe = fpe;
            return this;
        }

        public FilterConfigurationBuilder withWindowSize(int windowSize) {
            this.windowSize = windowSize;
            return this;
        }

        public FilterConfigurationBuilder withPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public FilterConfigurationBuilder withRegexTimeoutMs(long regexTimeoutMs) {
            this.regexTimeoutMs = regexTimeoutMs;
            return this;
        }

    }

    public List<? extends AbstractFilterStrategy> getStrategies() {
        return strategies;
    }

    public ContextService getContextService() {
        return contextService;
    }

    public Random getRandom() {
        return random;
    }

    public Set<String> getIgnored() {
        return ignored;
    }

    public Set<String> getIgnoredFiles() {
        return ignoredFiles;
    }

    public List<IgnoredPattern> getIgnoredPatterns() {
        return ignoredPatterns;
    }

    public Crypto getCrypto() {
        return crypto;
    }

    public FPE getFPE() {
        return fpe;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public int getPriority() {
        return priority;
    }

    public long getRegexTimeoutMs() {
        return regexTimeoutMs;
    }

}
