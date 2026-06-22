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
package ai.philterd.phileas.services;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.filters.Filter;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.rules.dictionary.FuzzyDictionaryFilter;
import ai.philterd.phileas.filters.rules.dictionary.SetDictionaryFilter;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.SensitivityLevel;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.CustomDictionary;
import ai.philterd.phileas.policy.filters.Identifier;
import ai.philterd.phileas.policy.filters.PhEye;
import ai.philterd.phileas.policy.filters.Section;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeConfiguration;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeFilter;
import ai.philterd.phileas.services.filters.custom.PhoneNumberRulesFilter;
import ai.philterd.phileas.services.filters.regex.AgeFilter;
import ai.philterd.phileas.services.filters.regex.BankRoutingNumberFilter;
import ai.philterd.phileas.services.filters.regex.BitcoinAddressFilter;
import ai.philterd.phileas.services.filters.regex.CreditCardFilter;
import ai.philterd.phileas.services.filters.regex.CurrencyFilter;
import ai.philterd.phileas.services.filters.regex.DateFilter;
import ai.philterd.phileas.services.filters.regex.DriversLicenseFilter;
import ai.philterd.phileas.services.filters.regex.EmailAddressFilter;
import ai.philterd.phileas.services.filters.regex.IbanCodeFilter;
import ai.philterd.phileas.services.filters.regex.IdentifierFilter;
import ai.philterd.phileas.services.filters.regex.IpAddressFilter;
import ai.philterd.phileas.services.filters.regex.MacAddressFilter;
import ai.philterd.phileas.services.filters.regex.PassportNumberFilter;
import ai.philterd.phileas.services.filters.regex.PhoneNumberExtensionFilter;
import ai.philterd.phileas.services.filters.regex.PhysicianNameFilter;
import ai.philterd.phileas.services.filters.regex.SectionFilter;
import ai.philterd.phileas.services.filters.regex.SsnFilter;
import ai.philterd.phileas.services.filters.regex.StateAbbreviationFilter;
import ai.philterd.phileas.services.filters.regex.StreetAddressFilter;
import ai.philterd.phileas.services.filters.regex.TrackingNumberFilter;
import ai.philterd.phileas.services.filters.regex.UrlFilter;
import ai.philterd.phileas.services.filters.regex.VinFilter;
import ai.philterd.phileas.services.filters.regex.ZipCodeFilter;
import ai.philterd.phileas.services.validators.DateSpanValidator;
import ai.philterd.phileas.services.validators.IdentifierValidators;
import ai.philterd.phileas.services.validators.SpanValidator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import java.security.SecureRandom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;

public class FilterPolicyLoader {

    private static final Logger LOGGER = LogManager.getLogger(FilterPolicyLoader.class);

    private final PhileasConfiguration phileasConfiguration;
    private final SecureRandom random;
    private final HttpClient httpClient;

    public FilterPolicyLoader(final PhileasConfiguration phileasConfiguration,
                              final SecureRandom random,
                              final HttpClient httpClient) {

        this.phileasConfiguration = phileasConfiguration;
        this.random = random;
        this.httpClient = httpClient;

    }

    /**
     * Load the filters for a given policy.
     * @param policy The {@link Policy} containing the filters.
     * @param filterCache The filter cache.
     * @return A list of {@link Filter} from the policy.
     * @throws Exception Thrown if the policy cannot be read or the filters cannot be instantiated.
     */
    public List<Filter> getFiltersForPolicy(final Policy policy, final Map<String, List<Filter>> filterCache) throws Exception {

        LOGGER.debug("Getting filters for policy.");

        final String policyKey = policy.getCacheKey();

        // The policy key is a hash of the entire policy, so it fully determines the filter set. If the
        // complete list of filters for this policy has already been built, reuse it. Caching the whole
        // list (rather than individual filters by type) means the list-based custom dictionary,
        // identifier, and ph-eye filters are cached too, instead of being rebuilt on every call.
        //
        // computeIfAbsent builds the filter set at most once per policy even when multiple threads
        // race a cold cache, so a single FilterService can be shared across threads (for example a
        // per-row Spark/Kafka UDF) without each racing thread redundantly building the same filters.
        // The build throws checked exceptions, which a mapping function cannot, so they are wrapped
        // in a CompletionException and unwrapped here to preserve this method's throws contract.
        try {
            return filterCache.computeIfAbsent(policyKey, key -> {
                try {
                    return buildFilters(policy, policyKey);
                } catch (final Exception e) {
                    throw new CompletionException(e);
                }
            });
        } catch (final CompletionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        }

    }

    /**
     * Build the complete list of filters for a policy. This is the cache-miss path of
     * {@link #getFiltersForPolicy(Policy, Map)} and is invoked at most once per distinct policy.
     * @param policy The {@link Policy} containing the filters.
     * @param policyKey A hash of the policy, used only for logging.
     * @return A list of {@link Filter} from the policy.
     * @throws Exception Thrown if the policy cannot be read or the filters cannot be instantiated.
     */
    private List<Filter> buildFilters(final Policy policy, final String policyKey) throws Exception {

        final List<Filter> enabledFilters = new LinkedList<>();

        // Rules filters.

        if(policy.getIdentifiers().hasFilter(FilterType.AGE) && policy.getIdentifiers().getAge().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getAge().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getAge().getAgeFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getAge().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getAge().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getAge().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getAge().getPriority())
                    .build();

            final Filter filter = new AgeFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.BANK_ROUTING_NUMBER) && policy.getIdentifiers().getBankRoutingNumber().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getBankRoutingNumber().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getBankRoutingNumber().getBankRoutingNumberFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getBankRoutingNumber().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getBankRoutingNumber().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getBankRoutingNumber().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getBankRoutingNumber().getPriority())
                    .build();

            final Filter filter = new BankRoutingNumberFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.BITCOIN_ADDRESS) && policy.getIdentifiers().getBitcoinAddress().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getBitcoinAddress().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getBitcoinAddress().getBitcoinFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getBitcoinAddress().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getBitcoinAddress().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getBitcoinAddress().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getBitcoinAddress().getPriority())
                    .build();

            final Filter filter = new BitcoinAddressFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.CREDIT_CARD) && policy.getIdentifiers().getCreditCard().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getCreditCard().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getCreditCard().getCreditCardFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getCreditCard().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getCreditCard().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getCreditCard().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getCreditCard().getPriority())
                    .build();

            final boolean onlyValidCreditCardNumbers = policy.getIdentifiers().getCreditCard().isOnlyValidCreditCardNumbers();
            final boolean ignoreWhenInUnixTimestamp = policy.getIdentifiers().getCreditCard().isIgnoreWhenInUnixTimestamp();
            final boolean onlyWordBoundaries = policy.getIdentifiers().getCreditCard().isOnlyWordBoundaries();

            final Filter filter = new CreditCardFilter(filterConfiguration, onlyValidCreditCardNumbers, ignoreWhenInUnixTimestamp, onlyWordBoundaries);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.CURRENCY) && policy.getIdentifiers().getCurrency().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getCurrency().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getCurrency().getCurrencyFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getCurrency().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getCurrency().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getCurrency().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getCurrency().getPriority())
                    .build();

            final Filter filter = new CurrencyFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.DATE) && policy.getIdentifiers().getDate().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getDate().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getDate().getDateFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getDate().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getDate().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getDate().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getDate().getPriority())
                    .build();

            final boolean onlyValidDates = policy.getIdentifiers().getDate().isOnlyValidDates();
            final SpanValidator dateSpanValidator = DateSpanValidator.getInstance();

            final Filter filter = new DateFilter(filterConfiguration, onlyValidDates, dateSpanValidator);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.DRIVERS_LICENSE_NUMBER) && policy.getIdentifiers().getDriversLicense().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getDriversLicense().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getDriversLicense().getDriversLicenseFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getDriversLicense().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getDriversLicense().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getDriversLicense().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getDriversLicense().getPriority())
                    .build();

            final Filter filter = new DriversLicenseFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.EMAIL_ADDRESS) && policy.getIdentifiers().getEmailAddress().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getEmailAddress().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getEmailAddress().getEmailAddressFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getEmailAddress().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getEmailAddress().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getEmailAddress().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getEmailAddress().getPriority())
                    .build();

            final boolean isStrict = policy.getIdentifiers().getEmailAddress().isOnlyStrictMatches();
            final boolean onlyValidTLDs = policy.getIdentifiers().getEmailAddress().isOnlyValidTLDs();

            final Filter filter = new EmailAddressFilter(filterConfiguration, isStrict, onlyValidTLDs);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.IBAN_CODE) && policy.getIdentifiers().getIbanCode().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getIbanCode().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getIbanCode().getIbanCodeFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getIbanCode().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getIbanCode().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getIbanCode().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getIbanCode().getPriority())
                    .build();

            final boolean onlyValidIBANCodes = policy.getIdentifiers().getIbanCode().isOnlyValidIBANCodes();
            final boolean allowSpaces = policy.getIdentifiers().getIbanCode().isAllowSpaces();

            final Filter filter = new IbanCodeFilter(filterConfiguration, onlyValidIBANCodes, allowSpaces);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.IP_ADDRESS) && policy.getIdentifiers().getIpAddress().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getIpAddress().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getIpAddress().getIpAddressFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getIpAddress().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getIpAddress().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getIpAddress().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getIpAddress().getPriority())
                    .build();

            final Filter filter = new IpAddressFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.MAC_ADDRESS) && policy.getIdentifiers().getMacAddress().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getMacAddress().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getMacAddress().getMacAddressFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getMacAddress().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getMacAddress().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getMacAddress().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getMacAddress().getPriority())
                    .build();

            final Filter filter = new MacAddressFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.PASSPORT_NUMBER) && policy.getIdentifiers().getPassportNumber().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getPassportNumber().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getPassportNumber().getPassportNumberFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getPassportNumber().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getPassportNumber().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getPassportNumber().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getPassportNumber().getPriority())
                    .build();

            final Filter filter = new PassportNumberFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.PHONE_NUMBER_EXTENSION) && policy.getIdentifiers().getPhoneNumberExtension().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getPhoneNumberExtension().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getPhoneNumberExtension().getPhoneNumberExtensionFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getPhoneNumberExtension().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getPhoneNumberExtension().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getPhoneNumberExtension().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getPhoneNumberExtension().getPriority())
                    .build();

            final Filter filter = new PhoneNumberExtensionFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.PHONE_NUMBER) && policy.getIdentifiers().getPhoneNumber().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getPhoneNumber().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getPhoneNumber().getPhoneNumberFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getPhoneNumber().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getPhoneNumber().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getPhoneNumber().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getPhoneNumber().getPriority())
                    .build();

            final Filter filter = new PhoneNumberRulesFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.PHYSICIAN_NAME) && policy.getIdentifiers().getPhysicianName().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getPhysicianName().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getPhysicianName().getPhysicianNameFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getPhysicianName().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getPhysicianName().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getPhysicianName().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getPhysicianName().getPriority())
                    .build();

            final Filter filter = new PhysicianNameFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.SECTION)) {

            final List<Section> sections = policy.getIdentifiers().getSections();

            for(final Section section : sections) {

                if(section.isEnabled()) {

                    final int windowSize = section.getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());

                    final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                            .withStrategies(section.getSectionFilterStrategies())
                            .withIgnored(section.getIgnored())
                            .withIgnoredFiles(section.getIgnoredFiles())
                            .withIgnoredPatterns(section.getIgnoredPatterns())
                            .withCrypto(policy.getCrypto())
                            .withWindowSize(windowSize)
                            .withPriority(section.getPriority())
                            // The section start/end patterns come from the policy, so bound matching to guard against ReDoS.
                            .withRegexTimeoutMs(phileasConfiguration.regexTimeoutMs())
                            .build();

                    final String startPattern = section.getStartPattern();
                    final String endPattern = section.getEndPattern();

                    enabledFilters.add(new SectionFilter(filterConfiguration, startPattern, endPattern));

                }

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.SSN) && policy.getIdentifiers().getSsn().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getSsn().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getSsn().getSsnFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getSsn().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getSsn().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getSsn().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getSsn().getPriority())
                    .build();

            final Filter filter = new SsnFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.STATE_ABBREVIATION) && policy.getIdentifiers().getStateAbbreviation().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getStateAbbreviation().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getStateAbbreviation().getStateAbbreviationsFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getStateAbbreviation().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getStateAbbreviation().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getStateAbbreviation().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getStateAbbreviation().getPriority())
                    .build();

            final Filter filter = new StateAbbreviationFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.STREET_ADDRESS) && policy.getIdentifiers().getStreetAddress().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getStreetAddress().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getStreetAddress().getStreetAddressFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getStreetAddress().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getStreetAddress().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getStreetAddress().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getStreetAddress().getPriority())
                    .build();

            final Filter filter = new StreetAddressFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.TRACKING_NUMBER) && policy.getIdentifiers().getTrackingNumber().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getTrackingNumber().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getTrackingNumber().getTrackingNumberFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getTrackingNumber().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getTrackingNumber().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getTrackingNumber().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getTrackingNumber().getPriority())
                    .build();

            final boolean ups = policy.getIdentifiers().getTrackingNumber().isUps();
            final boolean fedex = policy.getIdentifiers().getTrackingNumber().isFedex();
            final boolean usps = policy.getIdentifiers().getTrackingNumber().isUsps();

            final Filter filter = new TrackingNumberFilter(filterConfiguration, ups, fedex, usps);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.URL) && policy.getIdentifiers().getUrl().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getUrl().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getUrl().getUrlFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getUrl().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getUrl().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getUrl().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getUrl().getPriority())
                    .build();

            final boolean requireHttpWwwPrefix = policy.getIdentifiers().getUrl().isRequireHttpWwwPrefix();

            final Filter filter = new UrlFilter(filterConfiguration, requireHttpWwwPrefix);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.VIN) && policy.getIdentifiers().getVin().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getVin().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getVin().getVinFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getVin().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getVin().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getVin().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getVin().getPriority())
                    .build();

            final Filter filter = new VinFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.ZIP_CODE) && policy.getIdentifiers().getZipCode().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getZipCode().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getZipCode().getZipCodeFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getZipCode().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getZipCode().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getZipCode().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getZipCode().getPriority())
                    .build();

            final boolean requireDelimiter = policy.getIdentifiers().getZipCode().isRequireDelimiter();
            final boolean validate = policy.getIdentifiers().getZipCode().isValidate();

            final Filter filter = new ZipCodeFilter(filterConfiguration, requireDelimiter, validate);
            enabledFilters.add(filter);

        }

        // Custom dictionary filters.

        if(policy.getIdentifiers().hasFilter(FilterType.CUSTOM_DICTIONARY)) {

            LOGGER.info("Policy {} has {} custom dictionaries.", policyKey, policy.getIdentifiers().getCustomDictionaries().size());

            // There can be multiple custom dictionary filters because it is a list.
            for(final CustomDictionary customDictionary : policy.getIdentifiers().getCustomDictionaries()) {

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

                    final int windowSize = customDictionary.getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


                    final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                            .withStrategies(customDictionary.getCustomDictionaryFilterStrategies())
                            .withIgnored(customDictionary.getIgnored())
                            .withIgnoredFiles(customDictionary.getIgnoredFiles())
                            .withIgnoredPatterns(customDictionary.getIgnoredPatterns())
                            .withCrypto(policy.getCrypto())
                            .withWindowSize(windowSize)
                            .withPriority(customDictionary.getPriority())
                            .build();

                    // Only enable the filter if there is at least one term present.
                    if(!terms.isEmpty()) {

                        final String classification = customDictionary.getClassification();
                        final boolean capitalized = customDictionary.isCapitalized();
                        LOGGER.info("Custom dictionary contains {} terms.", terms.size());

                        if(customDictionary.isFuzzy()) {

                            final SensitivityLevel sensitivityLevel = SensitivityLevel.fromName(customDictionary.getSensitivity());
                            enabledFilters.add(new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, sensitivityLevel, terms, capitalized));

                        } else {

                            // Use an exact, case-insensitive set lookup when the dictionary is not fuzzy.
                            // A bloom pre-filter is not used here: the term set is an in-memory O(1) lookup,
                            // so a bloom filter would only add work and memory regardless of the term count.
                            enabledFilters.add(new SetDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, terms, classification));

                        }

                    } else {
                        LOGGER.warn("Custom dictionary contains no terms and will not be enabled.");
                    }

                }

            }

        } else {

            LOGGER.debug("Policy {} has no custom dictionaries.", policyKey);

        }

        // Fuzzy dictionary filters.

        if(policy.getIdentifiers().hasFilter(FilterType.LOCATION_CITY) && policy.getIdentifiers().getCity().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getCity().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getCity().getCityFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getCity().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getCity().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getCity().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getCity().getPriority())
                    .build();

            final Filter filter;

            if(policy.getIdentifiers().getCity().isFuzzy()) {

                final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getCity().getSensitivityLevel();
                final boolean capitalized = policy.getIdentifiers().getCity().isCapitalized();
                filter = new FuzzyDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, sensitivityLevel, capitalized);

            } else {
                filter = new SetDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration);
            }

            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.LOCATION_COUNTY) && policy.getIdentifiers().getCounty().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getCounty().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getCounty().getCountyFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getCounty().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getCounty().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getCounty().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getCounty().getPriority())
                    .build();

            final Filter filter;

            if(policy.getIdentifiers().getCounty().isFuzzy()) {

                final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getCounty().getSensitivityLevel();
                final boolean capitalized = policy.getIdentifiers().getCounty().isCapitalized();
                filter = new FuzzyDictionaryFilter(FilterType.LOCATION_COUNTY, filterConfiguration, sensitivityLevel, capitalized);

            } else {
                filter = new SetDictionaryFilter(FilterType.LOCATION_COUNTY, filterConfiguration);
            }

            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.LOCATION_STATE) && policy.getIdentifiers().getState().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getState().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getState().getStateFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getState().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getState().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getState().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getState().getPriority())
                    .build();

            final Filter filter;

            if(policy.getIdentifiers().getState().isFuzzy()) {

                final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getState().getSensitivityLevel();
                final boolean capitalized = policy.getIdentifiers().getState().isCapitalized();
                filter = new FuzzyDictionaryFilter(FilterType.LOCATION_STATE, filterConfiguration, sensitivityLevel, capitalized);

            } else {
                filter = new SetDictionaryFilter(FilterType.LOCATION_STATE, filterConfiguration);
            }

            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.HOSPITAL) && policy.getIdentifiers().getHospital().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getHospital().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getHospital().getHospitalFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getHospital().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getHospital().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getHospital().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getHospital().getPriority())
                    .build();

            final Filter filter;

            if(policy.getIdentifiers().getHospital().isFuzzy()) {

                final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getHospital().getSensitivityLevel();
                final boolean capitalized = policy.getIdentifiers().getHospital().isCapitalized();
                filter = new FuzzyDictionaryFilter(FilterType.HOSPITAL, filterConfiguration, sensitivityLevel, capitalized);

            } else {
                filter = new SetDictionaryFilter(FilterType.HOSPITAL, filterConfiguration);
            }

            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.FIRST_NAME) && policy.getIdentifiers().getFirstName().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getFirstName().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getFirstName().getFirstNameFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getFirstName().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getFirstName().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getFirstName().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getFirstName().getPriority())
                    .build();

            final Filter filter;

            if(policy.getIdentifiers().getFirstName().isFuzzy()) {

                final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getFirstName().getSensitivityLevel();
                final boolean capitalized = policy.getIdentifiers().getFirstName().isCapitalized();
                filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, sensitivityLevel, capitalized);

            } else {
                filter = new SetDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration);
            }

            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.SURNAME) && policy.getIdentifiers().getSurname().isEnabled()) {

            final int windowSize = policy.getIdentifiers().getSurname().getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(policy.getIdentifiers().getSurname().getSurnameFilterStrategies())
                    .withIgnored(policy.getIdentifiers().getSurname().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getSurname().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getSurname().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(policy.getIdentifiers().getSurname().getPriority())
                    .build();

            final Filter filter;

            if(policy.getIdentifiers().getSurname().isFuzzy()) {

                final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getSurname().getSensitivityLevel();
                final boolean capitalized = policy.getIdentifiers().getSurname().isCapitalized();
                filter = new FuzzyDictionaryFilter(FilterType.SURNAME, filterConfiguration, sensitivityLevel, capitalized);

            } else {
                filter = new SetDictionaryFilter(FilterType.SURNAME, filterConfiguration);
            }

            enabledFilters.add(filter);

        }

        // Enable ID filter last since it is a pretty generic pattern that might also match SSN, et. al.

        if(policy.getIdentifiers().hasFilter(FilterType.IDENTIFIER)) {

            final List<Identifier> identifiers = policy.getIdentifiers().getIdentifiers();

            for (final Identifier identifier : identifiers) {

                if (identifier.isEnabled()) {

                    final int windowSize = identifier.getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());


                    final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                            .withStrategies(identifier.getIdentifierFilterStrategies())
                            .withIgnored(identifier.getIgnored())
                            .withIgnoredFiles(identifier.getIgnoredFiles())
                            .withIgnoredPatterns(identifier.getIgnoredPatterns())
                            .withCrypto(policy.getCrypto())
                            .withFPE(policy.getFpe())
                            .withWindowSize(windowSize)
                            .withPriority(identifier.getPriority())
                            // The identifier pattern comes from the policy, so bound matching to guard against ReDoS.
                            .withRegexTimeoutMs(phileasConfiguration.regexTimeoutMs())
                            .build();

                    final String classification = identifier.getClassification();
                    final String pattern = identifier.getPattern();
                    final boolean caseSensitive = identifier.isCaseSensitive();
                    final int groupNumber = identifier.getGroupNumber();
                    final SpanValidator validator = IdentifierValidators.fromPolicy(identifier.getValidator());

                    final Filter identifierFilter = new IdentifierFilter(
                            filterConfiguration, classification, pattern, caseSensitive, groupNumber, validator
                    );

                    enabledFilters.add(identifierFilter);

                }

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.PH_EYE)) {

            // There can be multiple ph-eye filters because it is a list.
            for (final PhEye phEye : policy.getIdentifiers().getPhEyes()) {

                if(phEye.isEnabled()) {

                    final int windowSize = phEye.getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());

                    final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                            .withStrategies(phEye.getPhEyeFilterStrategies())
                            .withIgnored(phEye.getIgnored())
                            .withIgnoredFiles(phEye.getIgnoredFiles())
                            .withIgnoredPatterns(phEye.getIgnoredPatterns())
                            .withCrypto(policy.getCrypto())
                            .withFPE(policy.getFpe())
                            .withWindowSize(windowSize)
                            .withPriority(phEye.getPriority())
                            .build();

                    final ai.philterd.phileas.policy.filters.pheye.PhEyeConfiguration phEyePolicyConfig = phEye.getPhEyeConfiguration();

                    final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration(phEyePolicyConfig.getEndpoint());
                    phEyeConfiguration.setTimeout(phEyePolicyConfig.getTimeout());
                    phEyeConfiguration.setMaxIdleConnections(phEyePolicyConfig.getMaxIdleConnections());
                    phEyeConfiguration.setBearerToken(phEyePolicyConfig.getBearerToken());
                    phEyeConfiguration.setLabels(phEyePolicyConfig.getLabels());
                    phEyeConfiguration.setModelPath(phEyePolicyConfig.getModelPath());

                    final Filter filter = new PhEyeFilter(
                            filterConfiguration,
                            phEyeConfiguration,
                            phEye.isRemovePunctuation(),
                            phEye.getThresholds(),
                            FilterType.PH_EYE,
                            httpClient
                    );

                    enabledFilters.add(filter);

                }

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.PERSON) && policy.getIdentifiers().getPerson().isEnabled()) {

            final PhEye phEye = policy.getIdentifiers().getPerson();
            final int windowSize = phEye.getWindowSizeOrDefault(phileasConfiguration.spanWindowSize());

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(random)
                    .withStrategies(phEye.getPhEyeFilterStrategies())
                    .withIgnored(phEye.getIgnored())
                    .withIgnoredFiles(phEye.getIgnoredFiles())
                    .withIgnoredPatterns(phEye.getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withFPE(policy.getFpe())
                    .withWindowSize(windowSize)
                    .withPriority(phEye.getPriority())
                    .build();

            final ai.philterd.phileas.policy.filters.pheye.PhEyeConfiguration phEyePolicyConfig = phEye.getPhEyeConfiguration();

            final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration(phEyePolicyConfig.getEndpoint());
            phEyeConfiguration.setTimeout(phEyePolicyConfig.getTimeout());
            phEyeConfiguration.setMaxIdleConnections(phEyePolicyConfig.getMaxIdleConnections());
            phEyeConfiguration.setBearerToken(phEyePolicyConfig.getBearerToken());
            phEyeConfiguration.setLabels(phEyePolicyConfig.getLabels());
            phEyeConfiguration.setModelPath(phEyePolicyConfig.getModelPath());

            final Filter filter = new PhEyeFilter(
                    filterConfiguration,
                    phEyeConfiguration,
                    phEye.isRemovePunctuation(),
                    phEye.getThresholds(),
                    FilterType.PERSON,
                    httpClient
            );

            enabledFilters.add(filter);

        }

        return enabledFilters;

    }

}
