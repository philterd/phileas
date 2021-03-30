package com.mtnfog.phileas.model.filter;

import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.IgnoredPattern;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.util.*;

public class FilterConfiguration {

    private List<? extends AbstractFilterStrategy> strategies = new LinkedList<>();
    private AnonymizationService anonymizationService;
    private AlertService alertService;
    private Set<String> ignored = new LinkedHashSet<>();
    private Set<String> ignoredFiles = new LinkedHashSet<>();
    private List<IgnoredPattern> ignoredPatterns = new LinkedList<>();
    private Crypto crypto;
    private int windowSize = 5;

    private FilterConfiguration(
            List<? extends AbstractFilterStrategy> strategies,
            AnonymizationService anonymizationService,
            AlertService alertService,
            Set<String> ignored,
            Set<String> ignoredFiles,
            List<IgnoredPattern> ignoredPatterns,
            Crypto crypto,
            int windowSize
    ) {

        this.strategies = strategies;
        this.anonymizationService = anonymizationService;
        this.alertService = alertService;
        this.ignored = ignored;
        this.ignoredFiles = ignoredFiles;
        this.ignoredPatterns = ignoredPatterns;
        this.crypto = crypto;
        this.windowSize = windowSize;

    }

    public static class FilterConfigurationBuilder {

        private List<? extends AbstractFilterStrategy> strategies;
        private AnonymizationService anonymizationService;
        private AlertService alertService;
        private Set<String> ignored;
        private Set<String> ignoredFiles;
        private List<IgnoredPattern> ignoredPatterns;
        private Crypto crypto;
        private int windowSize;

        public FilterConfiguration build() {
            return new FilterConfiguration(
                    strategies,
                    anonymizationService,
                    alertService,
                    ignored,
                    ignoredFiles,
                    ignoredPatterns,
                    crypto,
                    windowSize
            );
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

    public int getWindowSize() {
        return windowSize;
    }

}
