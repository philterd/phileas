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
package ai.philterd.phileas.services;

import ai.philterd.phileas.model.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.enums.SensitivityLevel;
import ai.philterd.phileas.model.filter.Filter;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.rules.dictionary.BloomFilterDictionaryFilter;
import ai.philterd.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.policy.filters.CustomDictionary;
import ai.philterd.phileas.model.policy.filters.Identifier;
import ai.philterd.phileas.model.policy.filters.Section;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.model.services.AnonymizationCacheService;
import ai.philterd.phileas.model.services.MetricsService;
import ai.philterd.phileas.model.services.SpanValidator;
import ai.philterd.phileas.services.anonymization.*;
import ai.philterd.phileas.services.filters.ai.opennlp.PersonsV2Filter;
import ai.philterd.phileas.services.filters.ai.opennlp.PersonsV3Filter;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeConfiguration;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeFilter;
import ai.philterd.phileas.services.filters.custom.PhoneNumberRulesFilter;
import ai.philterd.phileas.services.filters.regex.*;
import ai.philterd.phileas.services.validators.DateSpanValidator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FilterPolicyLoader {

    private static final Logger LOGGER = LogManager.getLogger(FilterPolicyLoader.class);

    private final AnonymizationCacheService anonymizationCacheService;
    private final AlertService alertService;
    private final MetricsService metricsService;
    private final Map<String, DescriptiveStatistics> stats;
    private final PhileasConfiguration phileasConfiguration;

    public FilterPolicyLoader(final AlertService alertService, final AnonymizationCacheService anonymizationCacheService,
                              final MetricsService metricsService, final Map<String, DescriptiveStatistics> stats,
                              final PhileasConfiguration phileasConfiguration) {

        this.alertService = alertService;
        this.anonymizationCacheService = anonymizationCacheService;
        this.metricsService = metricsService;
        this.stats = new HashMap<>();
        
        this.phileasConfiguration = phileasConfiguration;

    }

    /**
     * Load the filters for a given policy.
     * @param policy The {@link Policy} containing the filters.
     * @param filterCache The filter cache.
     * @return A list of {@link Filter} from the policy.
     * @throws Exception Thrown if the policy cannot be read or the filters cannot be instantiated.
     */
    public List<Filter> getFiltersForPolicy(final Policy policy, final Map<String, Map<FilterType, Filter>> filterCache) throws Exception {

        LOGGER.debug("Getting filters for policy [{}]", policy.getName());

        // See if this filter is already cached.
        filterCache.putIfAbsent(policy.getName(), new ConcurrentHashMap<>());

        // Each policy has its own filter cache.
        final Map<FilterType, Filter> cache = filterCache.get(policy.getName());

        final List<Filter> enabledFilters = new LinkedList<>();

        // Rules filters.

        if(policy.getIdentifiers().hasFilter(FilterType.AGE) && policy.getIdentifiers().getAge().isEnabled()) {

            if(cache.containsKey(FilterType.AGE)) {
                enabledFilters.add(cache.get(FilterType.AGE));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getAge().getAgeFilterStrategies())
                        .withAnonymizationService(new AgeAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getAge().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getAge().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getAge().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final Filter filter = new AgeFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.AGE, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.BANK_ROUTING_NUMBER) && policy.getIdentifiers().getBankRoutingNumber().isEnabled()) {

            if(cache.containsKey(FilterType.BANK_ROUTING_NUMBER)) {
                enabledFilters.add(cache.get(FilterType.BANK_ROUTING_NUMBER));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getBankRoutingNumber().getBankRoutingNumberFilterStrategies())
                        .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getBankRoutingNumber().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getBankRoutingNumber().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getBankRoutingNumber().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withFPE(policy.getFpe())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final Filter filter = new BankRoutingNumberFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.BANK_ROUTING_NUMBER, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.BITCOIN_ADDRESS) && policy.getIdentifiers().getBitcoinAddress().isEnabled()) {

            if(cache.containsKey(FilterType.BITCOIN_ADDRESS)) {
                enabledFilters.add(cache.get(FilterType.BITCOIN_ADDRESS));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getBitcoinAddress().getBitcoinFilterStrategies())
                        .withAnonymizationService(new BitcoinAddressAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getBitcoinAddress().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getBitcoinAddress().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getBitcoinAddress().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withFPE(policy.getFpe())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final Filter filter = new BitcoinAddressFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.BITCOIN_ADDRESS, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.CREDIT_CARD) && policy.getIdentifiers().getCreditCard().isEnabled()) {

            if(cache.containsKey(FilterType.CREDIT_CARD)) {
                enabledFilters.add(cache.get(FilterType.CREDIT_CARD));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getCreditCard().getCreditCardFilterStrategies())
                        .withAnonymizationService(new CreditCardAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getCreditCard().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getCreditCard().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getCreditCard().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withFPE(policy.getFpe())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final boolean onlyValidCreditCardNumbers = policy.getIdentifiers().getCreditCard().isOnlyValidCreditCardNumbers();
                final boolean ignoreWhenInUnixTimestamp = policy.getIdentifiers().getCreditCard().isIgnoreWhenInUnixTimestamp();

                final Filter filter = new CreditCardFilter(filterConfiguration, onlyValidCreditCardNumbers, ignoreWhenInUnixTimestamp);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.CREDIT_CARD, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.CURRENCY) && policy.getIdentifiers().getCurrency().isEnabled()) {

            if(cache.containsKey(FilterType.CURRENCY)) {
                enabledFilters.add(cache.get(FilterType.CURRENCY));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getCurrency().getCurrencyFilterStrategies())
                        .withAnonymizationService(new CurrencyAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getCurrency().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getCurrency().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getCurrency().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final Filter filter = new CurrencyFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.CURRENCY, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.DATE) && policy.getIdentifiers().getDate().isEnabled()) {

            if(cache.containsKey(FilterType.DATE)) {
                enabledFilters.add(cache.get(FilterType.DATE));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getDate().getDateFilterStrategies())
                        .withAnonymizationService(new DateAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getDate().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getDate().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getDate().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final boolean onlyValidDates = policy.getIdentifiers().getDate().isOnlyValidDates();
                final SpanValidator dateSpanValidator = DateSpanValidator.getInstance();

                final Filter filter = new DateFilter(filterConfiguration, onlyValidDates, dateSpanValidator);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.DATE, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.DRIVERS_LICENSE_NUMBER) && policy.getIdentifiers().getDriversLicense().isEnabled()) {

            if(cache.containsKey(FilterType.DRIVERS_LICENSE_NUMBER)) {
                enabledFilters.add(cache.get(FilterType.DRIVERS_LICENSE_NUMBER));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getDriversLicense().getDriversLicenseFilterStrategies())
                        .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getDriversLicense().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getDriversLicense().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getDriversLicense().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withFPE(policy.getFpe())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final Filter filter = new DriversLicenseFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.DRIVERS_LICENSE_NUMBER, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.EMAIL_ADDRESS) && policy.getIdentifiers().getEmailAddress().isEnabled()) {

            if(cache.containsKey(FilterType.EMAIL_ADDRESS)) {
                enabledFilters.add(cache.get(FilterType.EMAIL_ADDRESS));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getEmailAddress().getEmailAddressFilterStrategies())
                        .withAnonymizationService(new EmailAddressAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getEmailAddress().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getEmailAddress().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getEmailAddress().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final boolean isStrict = policy.getIdentifiers().getEmailAddress().isOnlyStrictMatches();
                final boolean onlyValidTLDs = policy.getIdentifiers().getEmailAddress().isOnlyValidTLDs();

                final Filter filter = new EmailAddressFilter(filterConfiguration, isStrict, onlyValidTLDs);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.EMAIL_ADDRESS, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.IBAN_CODE) && policy.getIdentifiers().getIbanCode().isEnabled()) {

            if(cache.containsKey(FilterType.IBAN_CODE)) {
                enabledFilters.add(cache.get(FilterType.IBAN_CODE));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getIbanCode().getIbanCodeFilterStrategies())
                        .withAnonymizationService(new IbanCodeAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getIbanCode().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getIbanCode().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getIbanCode().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withFPE(policy.getFpe())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final boolean onlyValidIBANCodes = policy.getIdentifiers().getIbanCode().isOnlyValidIBANCodes();
                final boolean allowSpaces = policy.getIdentifiers().getIbanCode().isAllowSpaces();

                final Filter filter = new IbanCodeFilter(filterConfiguration, onlyValidIBANCodes, allowSpaces);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.IBAN_CODE, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.IP_ADDRESS) && policy.getIdentifiers().getIpAddress().isEnabled()) {

            if(cache.containsKey(FilterType.IP_ADDRESS)) {
                enabledFilters.add(cache.get(FilterType.IP_ADDRESS));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getIpAddress().getIpAddressFilterStrategies())
                        .withAnonymizationService(new IpAddressAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getIpAddress().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getIpAddress().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getIpAddress().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final Filter filter = new IpAddressFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.IP_ADDRESS, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.MAC_ADDRESS) && policy.getIdentifiers().getMacAddress().isEnabled()) {

            if(cache.containsKey(FilterType.MAC_ADDRESS)) {
                enabledFilters.add(cache.get(FilterType.MAC_ADDRESS));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getMacAddress().getMacAddressFilterStrategies())
                        .withAnonymizationService(new MacAddressAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getMacAddress().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getMacAddress().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getMacAddress().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final Filter filter = new MacAddressFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.MAC_ADDRESS, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.PASSPORT_NUMBER) && policy.getIdentifiers().getPassportNumber().isEnabled()) {

            if(cache.containsKey(FilterType.PASSPORT_NUMBER)) {
                enabledFilters.add(cache.get(FilterType.PASSPORT_NUMBER));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getPassportNumber().getPassportNumberFilterStrategies())
                        .withAnonymizationService(new PassportNumberAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getPassportNumber().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getPassportNumber().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getPassportNumber().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withFPE(policy.getFpe())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final Filter filter = new PassportNumberFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.PASSPORT_NUMBER, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.PHONE_NUMBER_EXTENSION) && policy.getIdentifiers().getPhoneNumberExtension().isEnabled()) {

            if(cache.containsKey(FilterType.PHONE_NUMBER_EXTENSION)) {
                enabledFilters.add(cache.get(FilterType.PHONE_NUMBER_EXTENSION));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getPhoneNumberExtension().getPhoneNumberExtensionFilterStrategies())
                        .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getPhoneNumberExtension().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getPhoneNumberExtension().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getPhoneNumberExtension().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final Filter filter = new PhoneNumberExtensionFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.PHONE_NUMBER_EXTENSION, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.PHONE_NUMBER) && policy.getIdentifiers().getPhoneNumber().isEnabled()) {

            if(cache.containsKey(FilterType.PHONE_NUMBER)) {
                enabledFilters.add(cache.get(FilterType.PHONE_NUMBER));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getPhoneNumber().getPhoneNumberFilterStrategies())
                        .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getPhoneNumber().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getPhoneNumber().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getPhoneNumber().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final Filter filter = new PhoneNumberRulesFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.PHONE_NUMBER, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.PHYSICIAN_NAME) && policy.getIdentifiers().getPhysicianName().isEnabled()) {

            if(cache.containsKey(FilterType.PHYSICIAN_NAME)) {
                enabledFilters.add(cache.get(FilterType.PHYSICIAN_NAME));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getPhysicianName().getPhysicianNameFilterStrategies())
                        .withAnonymizationService(new PersonsAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getPhysicianName().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getPhysicianName().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getPhysicianName().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final Filter filter = new PhysicianNameFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.PHYSICIAN_NAME, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.SECTION)) {

            final List<Section> sections = policy.getIdentifiers().getSections();

            for(final Section section : sections) {

                if(section.isEnabled()) {

                    final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                            .withStrategies(section.getSectionFilterStrategies())
                            .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                            .withAlertService(alertService)
                            .withIgnored(section.getIgnored())
                            .withIgnoredFiles(section.getIgnoredFiles())
                            .withIgnoredPatterns(section.getIgnoredPatterns())
                            .withCrypto(policy.getCrypto())
                            .withWindowSize(phileasConfiguration.spanWindowSize())
                            .build();

                    final String startPattern = section.getStartPattern();
                    final String endPattern = section.getEndPattern();

                    enabledFilters.add(new SectionFilter(filterConfiguration, startPattern, endPattern));

                }

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.SSN) && policy.getIdentifiers().getSsn().isEnabled()) {

            if(cache.containsKey(FilterType.SSN)) {
                enabledFilters.add(cache.get(FilterType.SSN));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getSsn().getSsnFilterStrategies())
                        .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getSsn().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getSsn().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getSsn().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withFPE(policy.getFpe())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final Filter filter = new SsnFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.SSN, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.STATE_ABBREVIATION) && policy.getIdentifiers().getStateAbbreviation().isEnabled()) {

            if(cache.containsKey(FilterType.STATE_ABBREVIATION)) {
                enabledFilters.add(cache.get(FilterType.STATE_ABBREVIATION));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getStateAbbreviation().getStateAbbreviationsFilterStrategies())
                        .withAnonymizationService(new StateAbbreviationAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getStateAbbreviation().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getStateAbbreviation().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getStateAbbreviation().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final Filter filter = new StateAbbreviationFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.STATE_ABBREVIATION, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.STREET_ADDRESS) && policy.getIdentifiers().getStreetAddress().isEnabled()) {

            if(cache.containsKey(FilterType.STREET_ADDRESS)) {
                enabledFilters.add(cache.get(FilterType.STREET_ADDRESS));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getStreetAddress().getStreetAddressFilterStrategies())
                        .withAnonymizationService(new StreetAddressAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getStreetAddress().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getStreetAddress().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getStreetAddress().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final Filter filter = new StreetAddressFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.STREET_ADDRESS, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.TRACKING_NUMBER) && policy.getIdentifiers().getTrackingNumber().isEnabled()) {

            if(cache.containsKey(FilterType.TRACKING_NUMBER)) {
                enabledFilters.add(cache.get(FilterType.TRACKING_NUMBER));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getTrackingNumber().getTrackingNumberFilterStrategies())
                        .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getTrackingNumber().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getTrackingNumber().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getTrackingNumber().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withFPE(policy.getFpe())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final boolean ups = policy.getIdentifiers().getTrackingNumber().isUps();
                final boolean fedex = policy.getIdentifiers().getTrackingNumber().isFedex();
                final boolean usps = policy.getIdentifiers().getTrackingNumber().isUsps();

                final Filter filter = new TrackingNumberFilter(filterConfiguration, ups, fedex, usps);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.TRACKING_NUMBER, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.URL) && policy.getIdentifiers().getUrl().isEnabled()) {

            if(cache.containsKey(FilterType.URL)) {
                enabledFilters.add(cache.get(FilterType.URL));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getUrl().getUrlFilterStrategies())
                        .withAnonymizationService(new UrlAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getUrl().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getUrl().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getUrl().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final boolean requireHttpWwwPrefix = policy.getIdentifiers().getUrl().isRequireHttpWwwPrefix();

                final Filter filter = new UrlFilter(filterConfiguration, requireHttpWwwPrefix);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.URL, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.VIN) && policy.getIdentifiers().getVin().isEnabled()) {

            if(cache.containsKey(FilterType.VIN)) {
                enabledFilters.add(cache.get(FilterType.VIN));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getVin().getVinFilterStrategies())
                        .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getVin().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getVin().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getVin().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withFPE(policy.getFpe())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final Filter filter = new VinFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.VIN, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.ZIP_CODE) && policy.getIdentifiers().getZipCode().isEnabled()) {

            if(cache.containsKey(FilterType.ZIP_CODE)) {
                enabledFilters.add(cache.get(FilterType.ZIP_CODE));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getZipCode().getZipCodeFilterStrategies())
                        .withAnonymizationService(new ZipCodeAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getZipCode().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getZipCode().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getZipCode().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final boolean requireDelimiter = policy.getIdentifiers().getZipCode().isRequireDelimiter();

                final Filter filter = new ZipCodeFilter(filterConfiguration, requireDelimiter);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.ZIP_CODE, filter);

            }

        }

        // Custom dictionary filters.

        if(policy.getIdentifiers().hasFilter(FilterType.CUSTOM_DICTIONARY)) {

            LOGGER.info("Policy {} has {} custom dictionaries.", policy.getName(), policy.getIdentifiers().getCustomDictionaries().size());

            // We keep track of the index of the custom dictionary in the list so we know
            // how to retrieve the strategy for the custom dictionary. This is because
            // there can be multiple custom dictionaries and not a 1-to-1 between filter
            // and strategy.
            int index = 0;

            // There can be multiple custom dictionary filters because it is a list.
            for(final CustomDictionary customDictionary : policy.getIdentifiers().getCustomDictionaries()) {

                // TODO: #111 Add caching of the policy (see Age for example)

                if(customDictionary.isEnabled()) {

                    // All the custom terms.
                    final Set<String> terms = new LinkedHashSet<>();

                    // First, read the terms from the policy.
                    if(CollectionUtils.isNotEmpty(customDictionary.getTerms())) {
                        terms.addAll(customDictionary.getTerms());
                    }

                    // Next, read terms from files, if given.
                    if(CollectionUtils.isNotEmpty(customDictionary.getFiles())) {
                        for (final String file : customDictionary.getFiles()) {
                            terms.addAll(FileUtils.readLines(new File(file), Charset.defaultCharset()));
                        }
                    }

                    if(customDictionary.isFuzzy()) {

                        LOGGER.info("Custom fuzzy dictionary contains {} terms.", terms.size());

                        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                                .withStrategies(customDictionary.getCustomDictionaryFilterStrategies())
                                .withAnonymizationService(new ZipCodeAnonymizationService(anonymizationCacheService))
                                .withAlertService(alertService)
                                .withIgnored(policy.getIdentifiers().getZipCode().getIgnored())
                                .withIgnoredFiles(policy.getIdentifiers().getZipCode().getIgnoredFiles())
                                .withIgnoredPatterns(policy.getIdentifiers().getZipCode().getIgnoredPatterns())
                                .withCrypto(policy.getCrypto())
                                .withWindowSize(phileasConfiguration.spanWindowSize())
                                .build();

                        final SensitivityLevel sensitivityLevel = SensitivityLevel.fromName(customDictionary.getSensitivity());
                        final String classification = customDictionary.getClassification();
                        final boolean capitalized = false;

                        enabledFilters.add(new LuceneDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, sensitivityLevel,
                                terms, capitalized, classification, index));

                    } else {

                        LOGGER.info("Custom dictionary contains {} terms.", terms.size());

                        // Only enable the filter if there is at least one term.
                        // TODO: #112 Don't use a bloom filter for a small number of terms.
                        if(!terms.isEmpty()) {

                            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                                    .withStrategies(customDictionary.getCustomDictionaryFilterStrategies())
                                    .withAnonymizationService(new ZipCodeAnonymizationService(anonymizationCacheService))
                                    .withAlertService(alertService)
                                    .withIgnored(customDictionary.getIgnored())
                                    .withIgnoredFiles(customDictionary.getIgnoredFiles())
                                    .withIgnoredPatterns(customDictionary.getIgnoredPatterns())
                                    .withCrypto(policy.getCrypto())
                                    .withWindowSize(phileasConfiguration.spanWindowSize())
                                    .build();

                            final String classification = customDictionary.getClassification();

                            enabledFilters.add(new BloomFilterDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration,
                                    terms, classification, phileasConfiguration.bloomFilterFpp()));

                        }

                    }

                    index++;

                }

            }

        } else {

            LOGGER.debug("Policy {} has no custom dictionaries.", policy.getName());

        }

        // Lucene dictionary filters.

        if(policy.getIdentifiers().hasFilter(FilterType.LOCATION_CITY) && policy.getIdentifiers().getCity().isEnabled()) {

            if(cache.containsKey(FilterType.LOCATION_CITY)) {
                enabledFilters.add(cache.get(FilterType.LOCATION_CITY));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getCity().getCityFilterStrategies())
                        .withAnonymizationService(new CityAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getCity().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getCity().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getCity().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getCity().getSensitivityLevel();
                final boolean capitalized = policy.getIdentifiers().getCity().isCapitalized();

                final Filter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, phileasConfiguration.indexesDirectory() + "cities", sensitivityLevel, capitalized);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.LOCATION_CITY, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.LOCATION_COUNTY) && policy.getIdentifiers().getCounty().isEnabled()) {

            if(cache.containsKey(FilterType.LOCATION_COUNTY)) {
                enabledFilters.add(cache.get(FilterType.LOCATION_COUNTY));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getCounty().getCountyFilterStrategies())
                        .withAnonymizationService(new CountyAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getCounty().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getCounty().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getCounty().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getCounty().getSensitivityLevel();
                final boolean capitalized = policy.getIdentifiers().getCounty().isCapitalized();

                final Filter filter = new LuceneDictionaryFilter(FilterType.LOCATION_COUNTY, filterConfiguration, phileasConfiguration.indexesDirectory() + "counties", sensitivityLevel, capitalized);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.LOCATION_COUNTY, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.LOCATION_STATE) && policy.getIdentifiers().getState().isEnabled()) {

            if(cache.containsKey(FilterType.LOCATION_STATE)) {
                enabledFilters.add(cache.get(FilterType.LOCATION_STATE));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getState().getStateFilterStrategies())
                        .withAnonymizationService(new StateAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getState().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getState().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getState().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getState().getSensitivityLevel();
                final boolean capitalized = policy.getIdentifiers().getState().isCapitalized();

                final Filter filter = new LuceneDictionaryFilter(FilterType.LOCATION_STATE, filterConfiguration, phileasConfiguration.indexesDirectory() + "states", sensitivityLevel, capitalized);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.LOCATION_STATE, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.HOSPITAL) && policy.getIdentifiers().getHospital().isEnabled()) {

            if(cache.containsKey(FilterType.HOSPITAL)) {
                enabledFilters.add(cache.get(FilterType.HOSPITAL));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getHospital().getHospitalFilterStrategies())
                        .withAnonymizationService(new HospitalAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getHospital().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getHospital().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getHospital().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getHospital().getSensitivityLevel();
                final boolean capitalized = policy.getIdentifiers().getHospital().isCapitalized();

                final Filter filter = new LuceneDictionaryFilter(FilterType.HOSPITAL, filterConfiguration, phileasConfiguration.indexesDirectory() + "hospitals", sensitivityLevel, capitalized);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.HOSPITAL, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.HOSPITAL_ABBREVIATION) && policy.getIdentifiers().getHospitalAbbreviation().isEnabled()) {

            if(cache.containsKey(FilterType.HOSPITAL_ABBREVIATION)) {
                enabledFilters.add(cache.get(FilterType.HOSPITAL_ABBREVIATION));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getHospitalAbbreviation().getHospitalAbbreviationFilterStrategies())
                        .withAnonymizationService(new HospitalAbbreviationAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getHospitalAbbreviation().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getHospitalAbbreviation().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getHospitalAbbreviation().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getHospitalAbbreviation().getSensitivityLevel();
                final boolean capitalized = policy.getIdentifiers().getHospitalAbbreviation().isCapitalized();

                final Filter filter = new LuceneDictionaryFilter(FilterType.HOSPITAL_ABBREVIATION, filterConfiguration, phileasConfiguration.indexesDirectory() + "hospital-abbreviations", sensitivityLevel, capitalized);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.HOSPITAL_ABBREVIATION, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.FIRST_NAME) && policy.getIdentifiers().getFirstName().isEnabled()) {

            if(cache.containsKey(FilterType.FIRST_NAME)) {
                enabledFilters.add(cache.get(FilterType.FIRST_NAME));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getFirstName().getFirstNameFilterStrategies())
                        .withAnonymizationService(new PersonsAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getFirstName().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getFirstName().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getFirstName().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getFirstName().getSensitivityLevel();
                final boolean capitalized = policy.getIdentifiers().getFirstName().isCapitalized();

                final Filter filter = new LuceneDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, phileasConfiguration.indexesDirectory() + "names", sensitivityLevel, capitalized);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.FIRST_NAME, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.SURNAME) && policy.getIdentifiers().getSurname().isEnabled()) {

            if(cache.containsKey(FilterType.SURNAME)) {
                enabledFilters.add(cache.get(FilterType.SURNAME));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getSurname().getSurnameFilterStrategies())
                        .withAnonymizationService(new SurnameAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getSurname().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getSurname().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getSurname().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getSurname().getSensitivityLevel();
                final boolean capitalized = policy.getIdentifiers().getSurname().isCapitalized();

                final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, filterConfiguration, phileasConfiguration.indexesDirectory() + "surnames", sensitivityLevel, capitalized);
                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.SURNAME, filter);

            }

        }

        // Enable ID filter last since it is a pretty generic pattern that might also match SSN, et. al.

        if(policy.getIdentifiers().hasFilter(FilterType.IDENTIFIER)) {

            final List<Identifier> identifiers = policy.getIdentifiers().getIdentifiers();

            for (final Identifier identifier : identifiers) {

                if (identifier.isEnabled()) {

                    // TODO: #115 How to best cache the individual identifier filters?

                    final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                            .withStrategies(identifier.getIdentifierFilterStrategies())
                            .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                            .withAlertService(alertService)
                            .withIgnored(identifier.getIgnored())
                            .withIgnoredFiles(identifier.getIgnoredFiles())
                            .withIgnoredPatterns(identifier.getIgnoredPatterns())
                            .withCrypto(policy.getCrypto())
                            .withWindowSize(phileasConfiguration.spanWindowSize())
                            .build();

                    final String classification = identifier.getClassification();
                    final String pattern = identifier.getPattern();
                    final boolean caseSensitive = identifier.isCaseSensitive();
                    final int groupNumber = identifier.getGroupNumber();

                    final Filter identifierFilter = new IdentifierFilter(
                            filterConfiguration, classification, pattern, caseSensitive, groupNumber
                    );

                    enabledFilters.add(identifierFilter);

                }

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.PERSON) && policy.getIdentifiers().getPerson().isEnabled()) {

            if(cache.containsKey(FilterType.PERSON)) {
                enabledFilters.add(cache.get(FilterType.PERSON));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getPerson().getNerStrategies())
                        .withAnonymizationService(new PersonsAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getPerson().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getPerson().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getPerson().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();
                
                final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration(policy.getIdentifiers().getPerson().getPhEyeConfiguration().getEndpoint());
                phEyeConfiguration.setTimeout(policy.getIdentifiers().getPerson().getPhEyeConfiguration().getTimeout());
                phEyeConfiguration.setKeepAliveDurationMs(policy.getIdentifiers().getPerson().getPhEyeConfiguration().getKeepAliveDurationMs());
                phEyeConfiguration.setMaxIdleConnections(policy.getIdentifiers().getPerson().getPhEyeConfiguration().getMaxIdleConnections());

                final Filter filter = new PhEyeFilter(
                        filterConfiguration,
                        phEyeConfiguration,
                        stats,
                        metricsService,
                        policy.getIdentifiers().getPerson().isRemovePunctuation(),
                        policy.getIdentifiers().getPerson().getThresholds()
                );

                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.PERSON, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.PERSON_V2) && policy.getIdentifiers().getPersonV2().isEnabled()) {

            if(cache.containsKey(FilterType.PERSON_V2)) {
                enabledFilters.add(cache.get(FilterType.PERSON_V2));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getPersonV2().getNerStrategies())
                        .withAnonymizationService(new PersonsAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getPersonV2().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getPersonV2().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getPersonV2().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final Filter filter = new PersonsV2Filter(
                        filterConfiguration,
                        policy.getIdentifiers().getPersonV2().getModel(),
                        policy.getIdentifiers().getPersonV2().getVocab(),
                        stats,
                        metricsService,
                        policy.getIdentifiers().getPersonV2().getThresholds());

                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.PERSON_V2, filter);

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.PERSON_V3) && policy.getIdentifiers().getPersonV3().isEnabled()) {

            if(cache.containsKey(FilterType.PERSON_V3)) {
                enabledFilters.add(cache.get(FilterType.PERSON_V3));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(policy.getIdentifiers().getPersonV3().getNerStrategies())
                        .withAnonymizationService(new PersonsAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(policy.getIdentifiers().getPersonV3().getIgnored())
                        .withIgnoredFiles(policy.getIdentifiers().getPersonV3().getIgnoredFiles())
                        .withIgnoredPatterns(policy.getIdentifiers().getPersonV3().getIgnoredPatterns())
                        .withCrypto(policy.getCrypto())
                        .withWindowSize(phileasConfiguration.spanWindowSize())
                        .build();

                final Filter filter = new PersonsV3Filter(
                        filterConfiguration,
                        policy.getIdentifiers().getPersonV3().getModel(),
                        stats,
                        metricsService,
                        policy.getIdentifiers().getPersonV3().getThresholds());

                enabledFilters.add(filter);
                filterCache.get(policy.getName()).put(FilterType.PERSON_V3, filter);

            }

        }

        return enabledFilters;

    }

}
