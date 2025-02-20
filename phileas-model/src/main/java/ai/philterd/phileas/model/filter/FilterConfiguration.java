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
package ai.philterd.phileas.model.filter;

import ai.philterd.phileas.model.policy.Crypto;
import ai.philterd.phileas.model.policy.FPE;
import ai.philterd.phileas.model.policy.IgnoredPattern;
import ai.philterd.phileas.model.policy.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.model.services.AnonymizationService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static ai.philterd.phileas.model.policy.filters.strategies.AbstractFilterStrategy.CRYPTO_REPLACE;
import static ai.philterd.phileas.model.policy.filters.strategies.AbstractFilterStrategy.FPE_ENCRYPT_REPLACE;

public class FilterConfiguration {

    private final List<? extends AbstractFilterStrategy> strategies;
    private final AnonymizationService anonymizationService;
    private final AlertService alertService;
    private final Set<String> ignored;
    private final Set<String> ignoredFiles;
    private final List<IgnoredPattern> ignoredPatterns;
    private final Crypto crypto;
    private final FPE fpe;
    private int windowSize = 5;
    private String alternateName;

    private FilterConfiguration(
            final List<? extends AbstractFilterStrategy> strategies,
            final AnonymizationService anonymizationService,
            final AlertService alertService,
            final Set<String> ignored,
            final Set<String> ignoredFiles,
            final List<IgnoredPattern> ignoredPatterns,
            final Crypto crypto,
            final FPE fpe,
            final int windowSize,
            final String alternateName
    ) {

        this.strategies = strategies;
        this.anonymizationService = anonymizationService;
        this.alertService = alertService;
        this.ignored = ignored;
        this.ignoredFiles = ignoredFiles;
        this.ignoredPatterns = ignoredPatterns;
        this.crypto = crypto;
        this.fpe = fpe;
        this.windowSize = windowSize;
        this.alternateName = alternateName;

    }

    public static class FilterConfigurationBuilder {

        private List<? extends AbstractFilterStrategy> strategies;
        private AnonymizationService anonymizationService;
        private AlertService alertService;
        private Set<String> ignored;
        private Set<String> ignoredFiles;
        private List<IgnoredPattern> ignoredPatterns;
        private Crypto crypto;
        private FPE fpe;
        private int windowSize;
        private String alternateName;

        public FilterConfiguration build() {

            // Validate the configuration. This throws an exception if it is invalid.
            validate();

            return new FilterConfiguration(
                    strategies,
                    anonymizationService,
                    alertService,
                    ignored,
                    ignoredFiles,
                    ignoredPatterns,
                    crypto,
                    fpe,
                    windowSize,
                    alternateName
            );

        }

        /**
         * Validate the configuration of the filter.
         * Throw an exception if the configuration is invalid.
         */
        private void validate() {

            if(CollectionUtils.isNotEmpty(strategies)) {

                for (final AbstractFilterStrategy strategy : strategies) {

                    if (StringUtils.equalsIgnoreCase(FPE_ENCRYPT_REPLACE, strategy.getStrategy())) {

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

                    } else if (StringUtils.equalsIgnoreCase(CRYPTO_REPLACE, strategy.getStrategy())) {

                        if (this.crypto != null) {

                            if (StringUtils.isEmpty(this.crypto.getKey())) {
                                throw new RuntimeException("Invalid configuration for filter: Missing crypto encryption key.");
                            }

                            if (StringUtils.isEmpty(this.crypto.getIv())) {
                                throw new RuntimeException("Invalid configuration for filter: Missing crypto encryption IV value.");
                            }

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

        public FilterConfigurationBuilder withAnonymizationService(AnonymizationService anonymizationService) {
            this.anonymizationService = anonymizationService;
            return this;
        }

        public FilterConfigurationBuilder withAlertService(AlertService alertService) {
            this.alertService = alertService;
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

    }

    public List<? extends AbstractFilterStrategy> getStrategies() {
        return strategies;
    }

    public AnonymizationService getAnonymizationService() {
        return anonymizationService;
    }

    public AlertService getAlertService() {
        return alertService;
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

}
