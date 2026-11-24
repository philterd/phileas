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
import ai.philterd.phileas.policy.Generator;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.AbstractFilter;
import ai.philterd.phileas.policy.filters.CustomDictionary;
import ai.philterd.phileas.policy.filters.Identifier;
import ai.philterd.phileas.policy.filters.PhEye;
import ai.philterd.phileas.policy.filters.Section;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeConfiguration;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeFilter;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.filters.custom.PhoneNumberRulesFilter;
import ai.philterd.phileas.services.generators.OllamaReplacementGenerator;
import ai.philterd.phileas.services.generators.ReplacementGenerator;
import ai.philterd.phileas.services.generators.ReplacementValidator;
import ai.philterd.phileas.services.split.SplitFactory;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.filters.regex.AgeFilter;
import ai.philterd.phileas.services.filters.regex.BankRoutingNumberFilter;
import ai.philterd.phileas.services.filters.regex.BitcoinAddressFilter;
import ai.philterd.phileas.services.filters.regex.CanadaSinFilter;
import ai.philterd.phileas.services.filters.regex.CreditCardFilter;
import ai.philterd.phileas.services.filters.regex.CurrencyFilter;
import ai.philterd.phileas.services.filters.regex.DateFilter;
import ai.philterd.phileas.services.filters.regex.DriversLicenseFilter;
import ai.philterd.phileas.services.filters.regex.EinFilter;
import ai.philterd.phileas.services.filters.regex.EmailAddressFilter;
import ai.philterd.phileas.services.filters.regex.IbanCodeFilter;
import ai.philterd.phileas.services.filters.regex.IdentifierFilter;
import ai.philterd.phileas.services.filters.regex.IpAddressFilter;
import ai.philterd.phileas.services.filters.regex.ItinFilter;
import ai.philterd.phileas.services.filters.regex.DeaNumberFilter;
import ai.philterd.phileas.services.filters.regex.DeviceIdentifierFilter;
import ai.philterd.phileas.services.filters.regex.HealthPlanMemberIdFilter;
import ai.philterd.phileas.services.filters.regex.MbiFilter;
import ai.philterd.phileas.services.filters.regex.NpiFilter;
import ai.philterd.phileas.services.filters.regex.MacAddressFilter;
import ai.philterd.phileas.services.filters.regex.PassportNumberFilter;
import ai.philterd.phileas.services.filters.regex.PhoneNumberExtensionFilter;
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
import ai.philterd.phileas.utils.CollectionUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import java.security.SecureRandom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;

/** Builds the list of filters for a policy from its configuration. */
public class FilterPolicyLoader {

    private static final Logger LOGGER = LogManager.getLogger(FilterPolicyLoader.class);

    private final PhileasConfiguration phileasConfiguration;
    private final SecureRandom random;
    private final HttpClient httpClient;

    // Reads the files a policy names, as the configuration allows.
    private final PolicyFiles policyFiles;

    public FilterPolicyLoader(final PhileasConfiguration phileasConfiguration,
                              final SecureRandom random,
                              final HttpClient httpClient) {

        this.phileasConfiguration = phileasConfiguration;
        this.random = random;
        this.httpClient = httpClient;

        this.policyFiles = PolicyFiles.of(phileasConfiguration);
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
     * Resolves the policy's named {@link Generator} definitions into invocable
     * {@link ReplacementGenerator} instances, keyed by generator name. Called once per policy load;
     * the resulting instances are shared across every filter built for the policy.
     * @param policy The {@link Policy} whose generators block is resolved.
     * @return A map of generator name to its resolved {@link ReplacementGenerator}. Empty if the
     *         policy declares no generators.
     */
    private Map<String, ReplacementGenerator> buildReplacementGenerators(final Policy policy) {

        final Map<String, Generator> definitions = policy.getGenerators();

        if (definitions == null || definitions.isEmpty()) {
            return Map.of();
        }

        final Map<String, ReplacementGenerator> resolved = new HashMap<>();

        for (final Map.Entry<String, Generator> entry : definitions.entrySet()) {

            final Generator generator = entry.getValue();

            if (generator == null || generator.getType() == null) {
                LOGGER.warn("Generator '{}' has no type and will be ignored.", entry.getKey());
                continue;
            }

            if (Generator.TYPE_OLLAMA.equalsIgnoreCase(generator.getType())) {
                resolved.put(entry.getKey(), new OllamaReplacementGenerator(generator, httpClient));
            } else {
                LOGGER.warn("Generator '{}' has unsupported type '{}' and will be ignored.",
                        entry.getKey(), generator.getType());
            }

        }

        return resolved;

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

        // The medicalCondition identifier never built a filter, so a policy setting it redacted nothing.
        // It was removed rather than left as a setting that silently does nothing.
        if (policy.getIdentifiers().hasMedicalCondition()) {
            throw new IllegalArgumentException("The medicalCondition filter has been removed: it never detected "
                    + "anything. To detect medical conditions, use a pheyes filter with a model that detects them, "
                    + "or a custom dictionary of condition names.");
        }

        if (policy.getConfig() != null && policy.getConfig().getReview() != null) {
            policy.getConfig().getReview().validate();
        }

        // Refuse a file the policy may not read here, before any filter is built, including the top-level
        // ignored files that are otherwise read only when a document is filtered.
        new PolicyFileValidator(policyFiles).validate(policy);

        // Refuse an environment variable the policy may not name: resolving each key through its getter throws.
        if (policy.getCrypto() != null) {
            policy.getCrypto().getKey();
            policy.getCrypto().getIv();
        }
        if (policy.getFpe() != null) {
            policy.getFpe().getKey();
            policy.getFpe().getTweak();
        }

        // Reject a bad pattern here, where the cost is paid once per policy, rather than on whichever
        // document first reaches it.
        new PolicyPatternValidator(phileasConfiguration.regexTimeoutMs()).validate(policy);

        // Reject an unknown split method here too, so a policy prepared once for per-row callers fails
        // when it is prepared rather than on its first row.
        if (policy.getConfig().getSplitting().isEnabled()) {
            SplitFactory.getSplitService(policy.getConfig().getSplitting().getMethod(),
                    policy.getConfig().getSplitting().getThreshold());
        }

        final List<Filter> enabledFilters = new LinkedList<>();

        // Resolve the policy's named generators to invocable ReplacementGenerators once, and share them
        // across every filter. A MAP_REPLACE strategy references a generator by name; the resolved
        // instance is injected into the strategy when its filter is built.
        final Map<String, ReplacementGenerator> replacementGenerators = buildReplacementGenerators(policy);

        // Rules filters.

        if(policy.getIdentifiers().hasFilter(FilterType.AGE) && policy.getIdentifiers().getAge().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getAge(), policy.getIdentifiers().getAge().getAgeFilterStrategies(), replacementGenerators);

            final Filter filter = new AgeFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.BANK_ROUTING_NUMBER) && policy.getIdentifiers().getBankRoutingNumber().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getBankRoutingNumber(), policy.getIdentifiers().getBankRoutingNumber().getBankRoutingNumberFilterStrategies(), replacementGenerators);

            final Filter filter = new BankRoutingNumberFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.BITCOIN_ADDRESS) && policy.getIdentifiers().getBitcoinAddress().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getBitcoinAddress(), policy.getIdentifiers().getBitcoinAddress().getBitcoinFilterStrategies(), replacementGenerators);

            final Filter filter = new BitcoinAddressFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.CREDIT_CARD) && policy.getIdentifiers().getCreditCard().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getCreditCard(), policy.getIdentifiers().getCreditCard().getCreditCardFilterStrategies(), replacementGenerators);

            final boolean onlyValidCreditCardNumbers = policy.getIdentifiers().getCreditCard().isOnlyValidCreditCardNumbers();
            final boolean ignoreWhenInUnixTimestamp = policy.getIdentifiers().getCreditCard().isIgnoreWhenInUnixTimestamp();
            final boolean onlyWordBoundaries = policy.getIdentifiers().getCreditCard().isOnlyWordBoundaries();

            final Filter filter = new CreditCardFilter(filterConfiguration, onlyValidCreditCardNumbers, ignoreWhenInUnixTimestamp, onlyWordBoundaries);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.CURRENCY) && policy.getIdentifiers().getCurrency().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getCurrency(), policy.getIdentifiers().getCurrency().getCurrencyFilterStrategies(), replacementGenerators);

            final Filter filter = new CurrencyFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.DATE) && policy.getIdentifiers().getDate().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getDate(), policy.getIdentifiers().getDate().getDateFilterStrategies(), replacementGenerators);

            final boolean onlyValidDates = policy.getIdentifiers().getDate().isOnlyValidDates();
            final SpanValidator dateSpanValidator = DateSpanValidator.getInstance();

            final Filter filter = new DateFilter(filterConfiguration, onlyValidDates, dateSpanValidator);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.DRIVERS_LICENSE_NUMBER) && policy.getIdentifiers().getDriversLicense().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getDriversLicense(), policy.getIdentifiers().getDriversLicense().getDriversLicenseFilterStrategies(), replacementGenerators);

            final Filter filter = new DriversLicenseFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.EMAIL_ADDRESS) && policy.getIdentifiers().getEmailAddress().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getEmailAddress(), policy.getIdentifiers().getEmailAddress().getEmailAddressFilterStrategies(), replacementGenerators);

            final boolean isStrict = policy.getIdentifiers().getEmailAddress().isOnlyStrictMatches();
            final boolean onlyValidTLDs = policy.getIdentifiers().getEmailAddress().isOnlyValidTLDs();

            final Filter filter = new EmailAddressFilter(filterConfiguration, isStrict, onlyValidTLDs);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.IBAN_CODE) && policy.getIdentifiers().getIbanCode().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getIbanCode(), policy.getIdentifiers().getIbanCode().getIbanCodeFilterStrategies(), replacementGenerators);

            final boolean onlyValidIBANCodes = policy.getIdentifiers().getIbanCode().isOnlyValidIBANCodes();
            final boolean allowSpaces = policy.getIdentifiers().getIbanCode().isAllowSpaces();

            final Filter filter = new IbanCodeFilter(filterConfiguration, onlyValidIBANCodes, allowSpaces);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.IP_ADDRESS) && policy.getIdentifiers().getIpAddress().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getIpAddress(), policy.getIdentifiers().getIpAddress().getIpAddressFilterStrategies(), replacementGenerators);

            final Filter filter = new IpAddressFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.MAC_ADDRESS) && policy.getIdentifiers().getMacAddress().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getMacAddress(), policy.getIdentifiers().getMacAddress().getMacAddressFilterStrategies(), replacementGenerators);

            final Filter filter = new MacAddressFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.PASSPORT_NUMBER) && policy.getIdentifiers().getPassportNumber().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getPassportNumber(), policy.getIdentifiers().getPassportNumber().getPassportNumberFilterStrategies(), replacementGenerators);

            final Filter filter = new PassportNumberFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.PHONE_NUMBER_EXTENSION) && policy.getIdentifiers().getPhoneNumberExtension().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getPhoneNumberExtension(), policy.getIdentifiers().getPhoneNumberExtension().getPhoneNumberExtensionFilterStrategies(), replacementGenerators);

            final Filter filter = new PhoneNumberExtensionFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.PHONE_NUMBER) && policy.getIdentifiers().getPhoneNumber().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getPhoneNumber(), policy.getIdentifiers().getPhoneNumber().getPhoneNumberFilterStrategies(), replacementGenerators);

            final Filter filter = new PhoneNumberRulesFilter(filterConfiguration,
                    policy.getIdentifiers().getPhoneNumber().getRegion(),
                    policy.getIdentifiers().getPhoneNumber().getLeniency());
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.SECTION)) {

            final List<Section> sections = policy.getIdentifiers().getSections();

            for(final Section section : sections) {

                if(section.isEnabled()) {

                    final FilterConfiguration filterConfiguration = configuration(policy, section, section.getSectionFilterStrategies(), replacementGenerators);

                    final String startPattern = section.getStartPattern();
                    final String endPattern = section.getEndPattern();

                    enabledFilters.add(new SectionFilter(filterConfiguration, startPattern, endPattern));

                }

            }

        }

        if(policy.getIdentifiers().hasFilter(FilterType.EIN) && policy.getIdentifiers().getEin().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getEin(), policy.getIdentifiers().getEin().getEinFilterStrategies(), replacementGenerators);

            final Filter filter = new EinFilter(filterConfiguration, policy.getIdentifiers().getEin().isOnlyValidPrefixes());
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.ITIN) && policy.getIdentifiers().getItin().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getItin(), policy.getIdentifiers().getItin().getItinFilterStrategies(), replacementGenerators);

            final Filter filter = new ItinFilter(filterConfiguration, policy.getIdentifiers().getItin().isOnlyValidRanges());
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.CANADA_SIN) && policy.getIdentifiers().getCanadaSin().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getCanadaSin(), policy.getIdentifiers().getCanadaSin().getCanadaSinFilterStrategies(), replacementGenerators);

            final Filter filter = new CanadaSinFilter(filterConfiguration, policy.getIdentifiers().getCanadaSin().isOnlyValidPrefixes());
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.NPI) && policy.getIdentifiers().getNpi().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getNpi(), policy.getIdentifiers().getNpi().getNpiFilterStrategies(), replacementGenerators);

            final Filter filter = new NpiFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.DEA_NUMBER) && policy.getIdentifiers().getDeaNumber().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getDeaNumber(), policy.getIdentifiers().getDeaNumber().getDeaNumberFilterStrategies(), replacementGenerators);

            final Filter filter = new DeaNumberFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.MBI) && policy.getIdentifiers().getMbi().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getMbi(), policy.getIdentifiers().getMbi().getMbiFilterStrategies(), replacementGenerators);

            final Filter filter = new MbiFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.HEALTH_PLAN_MEMBER_ID) && policy.getIdentifiers().getHealthPlanMemberId().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getHealthPlanMemberId(), policy.getIdentifiers().getHealthPlanMemberId().getHealthPlanMemberIdFilterStrategies(), replacementGenerators);

            final Filter filter = new HealthPlanMemberIdFilter(filterConfiguration,
                    policy.getIdentifiers().getHealthPlanMemberId().getPatterns(),
                    policy.getIdentifiers().getHealthPlanMemberId().getCueWords(),
                    policy.getIdentifiers().getHealthPlanMemberId().isDefaultCueWords());
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.DEVICE_IDENTIFIER) && policy.getIdentifiers().getDeviceIdentifier().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getDeviceIdentifier(), policy.getIdentifiers().getDeviceIdentifier().getDeviceIdentifierFilterStrategies(), replacementGenerators);

            final Filter filter = new DeviceIdentifierFilter(filterConfiguration,
                    policy.getIdentifiers().getDeviceIdentifier().getPatterns(),
                    policy.getIdentifiers().getDeviceIdentifier().getCueWords(),
                    policy.getIdentifiers().getDeviceIdentifier().isDefaultCueWords());
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.SSN) && policy.getIdentifiers().getSsn().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getSsn(), policy.getIdentifiers().getSsn().getSsnFilterStrategies(), replacementGenerators);

            final Filter filter = new SsnFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.STATE_ABBREVIATION) && policy.getIdentifiers().getStateAbbreviation().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getStateAbbreviation(), policy.getIdentifiers().getStateAbbreviation().getStateAbbreviationsFilterStrategies(), replacementGenerators);

            final Filter filter = new StateAbbreviationFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.STREET_ADDRESS) && policy.getIdentifiers().getStreetAddress().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getStreetAddress(), policy.getIdentifiers().getStreetAddress().getStreetAddressFilterStrategies(), replacementGenerators);

            final Filter filter = new StreetAddressFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.TRACKING_NUMBER) && policy.getIdentifiers().getTrackingNumber().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getTrackingNumber(), policy.getIdentifiers().getTrackingNumber().getTrackingNumberFilterStrategies(), replacementGenerators);

            final boolean ups = policy.getIdentifiers().getTrackingNumber().isUps();
            final boolean fedex = policy.getIdentifiers().getTrackingNumber().isFedex();
            final boolean usps = policy.getIdentifiers().getTrackingNumber().isUsps();
            final boolean allowSpaces = policy.getIdentifiers().getTrackingNumber().isAllowSpaces();

            final Filter filter = new TrackingNumberFilter(filterConfiguration, ups, fedex, usps, allowSpaces);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.URL) && policy.getIdentifiers().getUrl().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getUrl(), policy.getIdentifiers().getUrl().getUrlFilterStrategies(), replacementGenerators);

            final boolean requireHttpWwwPrefix = policy.getIdentifiers().getUrl().isRequireHttpWwwPrefix();

            final Filter filter = new UrlFilter(filterConfiguration, requireHttpWwwPrefix);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.VIN) && policy.getIdentifiers().getVin().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getVin(), policy.getIdentifiers().getVin().getVinFilterStrategies(), replacementGenerators);

            final Filter filter = new VinFilter(filterConfiguration);
            enabledFilters.add(filter);

        }

        if(policy.getIdentifiers().hasFilter(FilterType.ZIP_CODE) && policy.getIdentifiers().getZipCode().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getZipCode(), policy.getIdentifiers().getZipCode().getZipCodeFilterStrategies(), replacementGenerators);

            final boolean requireDelimiter = policy.getIdentifiers().getZipCode().isRequireDelimiter();
            final boolean validate = policy.getIdentifiers().getZipCode().isValidate();

            final Filter filter = new ZipCodeFilter(filterConfiguration, requireDelimiter, validate);
            enabledFilters.add(filter);

        }

        // Custom dictionary filters.

        if(policy.getIdentifiers().hasFilter(FilterType.CUSTOM_DICTIONARY)) {

            LOGGER.info("Policy {} has {} custom dictionaries.", policyKey, policy.getIdentifiers().getCustomDictionaries().size());

            // There can be multiple custom dictionary filters because it is a list.
            int dictionaryNumber = 0;
            for(final CustomDictionary customDictionary : policy.getIdentifiers().getCustomDictionaries()) {

                dictionaryNumber++;

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
                            final List<String> lines = policyFiles.readLines("custom dictionary " + dictionaryNumber + "'s files", file);
                            if (lines == null) {
                                // As before: a dictionary file that does not exist fails the policy.
                                throw new NoSuchFileException(file);
                            }
                            terms.addAll(lines);
                        }
                    }

                    final FilterConfiguration filterConfiguration = configuration(policy, customDictionary, customDictionary.getCustomDictionaryFilterStrategies(), replacementGenerators);

                    // Only enable the filter if there is at least one term present.
                    if(!terms.isEmpty()) {

                        final String classification = customDictionary.getClassification();
                        final boolean capitalized = customDictionary.isCapitalized();
                        LOGGER.info("Custom dictionary contains {} terms.", terms.size());

                        // Parsed whether or not the dictionary is fuzzy, so a misspelled level is refused
                        // before fuzzy matching is turned on rather than after.
                        final SensitivityLevel sensitivityLevel = sensitivityLevel(customDictionary.getSensitivity(),
                                "custom dictionary " + dictionaryNumber);

                        if(customDictionary.isFuzzy()) {

                            enabledFilters.add(new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, sensitivityLevel, terms, capitalized, classification));

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

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getCity(), policy.getIdentifiers().getCity().getCityFilterStrategies(), replacementGenerators);

            enabledFilters.add(createDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration,
                    policy.getIdentifiers().getCity().getTerms(),
                    policy.getIdentifiers().getCity().isFuzzy(),
                    policy.getIdentifiers().getCity().getSensitivity(),
                    policy.getIdentifiers().getCity().isCapitalized()));

        }

        if(policy.getIdentifiers().hasFilter(FilterType.LOCATION_COUNTY) && policy.getIdentifiers().getCounty().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getCounty(), policy.getIdentifiers().getCounty().getCountyFilterStrategies(), replacementGenerators);

            enabledFilters.add(createDictionaryFilter(FilterType.LOCATION_COUNTY, filterConfiguration,
                    policy.getIdentifiers().getCounty().getTerms(),
                    policy.getIdentifiers().getCounty().isFuzzy(),
                    policy.getIdentifiers().getCounty().getSensitivity(),
                    policy.getIdentifiers().getCounty().isCapitalized()));

        }

        if(policy.getIdentifiers().hasFilter(FilterType.LOCATION_STATE) && policy.getIdentifiers().getState().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getState(), policy.getIdentifiers().getState().getStateFilterStrategies(), replacementGenerators);

            enabledFilters.add(createDictionaryFilter(FilterType.LOCATION_STATE, filterConfiguration,
                    policy.getIdentifiers().getState().getTerms(),
                    policy.getIdentifiers().getState().isFuzzy(),
                    policy.getIdentifiers().getState().getSensitivity(),
                    policy.getIdentifiers().getState().isCapitalized()));

        }

        if(policy.getIdentifiers().hasFilter(FilterType.HOSPITAL) && policy.getIdentifiers().getHospital().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getHospital(), policy.getIdentifiers().getHospital().getHospitalFilterStrategies(), replacementGenerators);

            enabledFilters.add(createDictionaryFilter(FilterType.HOSPITAL, filterConfiguration,
                    policy.getIdentifiers().getHospital().getTerms(),
                    policy.getIdentifiers().getHospital().isFuzzy(),
                    policy.getIdentifiers().getHospital().getSensitivity(),
                    policy.getIdentifiers().getHospital().isCapitalized()));

        }

        if(policy.getIdentifiers().hasFilter(FilterType.FIRST_NAME) && policy.getIdentifiers().getFirstName().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getFirstName(), policy.getIdentifiers().getFirstName().getFirstNameFilterStrategies(), replacementGenerators);

            enabledFilters.add(createDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration,
                    policy.getIdentifiers().getFirstName().getTerms(),
                    policy.getIdentifiers().getFirstName().isFuzzy(),
                    policy.getIdentifiers().getFirstName().getSensitivity(),
                    policy.getIdentifiers().getFirstName().isCapitalized()));

        }

        if(policy.getIdentifiers().hasFilter(FilterType.SURNAME) && policy.getIdentifiers().getSurname().isEnabled()) {

            final FilterConfiguration filterConfiguration = configuration(policy, policy.getIdentifiers().getSurname(), policy.getIdentifiers().getSurname().getSurnameFilterStrategies(), replacementGenerators);

            enabledFilters.add(createDictionaryFilter(FilterType.SURNAME, filterConfiguration,
                    policy.getIdentifiers().getSurname().getTerms(),
                    policy.getIdentifiers().getSurname().isFuzzy(),
                    policy.getIdentifiers().getSurname().getSensitivity(),
                    policy.getIdentifiers().getSurname().isCapitalized()));

        }

        // Enable ID filter last since it is a pretty generic pattern that might also match SSN, et. al.

        if(policy.getIdentifiers().hasFilter(FilterType.IDENTIFIER)) {

            final List<Identifier> identifiers = policy.getIdentifiers().getIdentifiers();

            for (final Identifier identifier : identifiers) {

                if (identifier.isEnabled()) {

                    final FilterConfiguration filterConfiguration = configuration(policy, identifier, identifier.getIdentifierFilterStrategies(), replacementGenerators);

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

                    final FilterConfiguration filterConfiguration = configuration(policy, phEye, phEye.getPhEyeFilterStrategies(), replacementGenerators);

                    final ai.philterd.phileas.policy.filters.pheye.PhEyeConfiguration phEyePolicyConfig = phEye.getPhEyeConfiguration();

                    final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration(phEyePolicyConfig.getEndpoint());
                    phEyeConfiguration.setTimeout(phEyePolicyConfig.getTimeout());
                    phEyeConfiguration.setMaxIdleConnections(phEyePolicyConfig.getMaxIdleConnections());
                    phEyeConfiguration.setBearerToken(phEyePolicyConfig.getBearerToken());
                    phEyeConfiguration.setLabels(phEyePolicyConfig.getLabels());
                    phEyeConfiguration.setModelPath(phEyePolicyConfig.getModelPath());
                    phEyeConfiguration.setThreshold(phEyePolicyConfig.getThreshold());

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
            final FilterConfiguration filterConfiguration = configuration(policy, phEye, phEye.getPhEyeFilterStrategies(), replacementGenerators);

            final ai.philterd.phileas.policy.filters.pheye.PhEyeConfiguration phEyePolicyConfig = phEye.getPhEyeConfiguration();

            final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration(phEyePolicyConfig.getEndpoint());
            phEyeConfiguration.setTimeout(phEyePolicyConfig.getTimeout());
            phEyeConfiguration.setMaxIdleConnections(phEyePolicyConfig.getMaxIdleConnections());
            phEyeConfiguration.setBearerToken(phEyePolicyConfig.getBearerToken());
            phEyeConfiguration.setLabels(phEyePolicyConfig.getLabels());
            phEyeConfiguration.setModelPath(phEyePolicyConfig.getModelPath());
            phEyeConfiguration.setThreshold(phEyePolicyConfig.getThreshold());

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

        // Wire the generated-value PII re-scan into every MAP_REPLACE strategy. The validator runs the
        // now-complete filter set over a candidate replacement and reports whether any PII is detected.
        wireReplacementValidators(policy, enabledFilters);

        return enabledFilters;

    }

    /**
     * Parses a dictionary filter's sensitivity, naming the filter when it is not valid.
     * @param sensitivity The sensitivity from the policy.
     * @param filter The filter, such as "the city filter", for the message.
     * @return The {@link SensitivityLevel}.
     */
    private static SensitivityLevel sensitivityLevel(final String sensitivity, final String filter) {
        try {
            return SensitivityLevel.fromName(sensitivity);
        } catch (final IllegalArgumentException ex) {
            throw new IllegalArgumentException("The sensitivity '" + sensitivity + "' of " + filter + " is not valid. "
                    + "Valid values are " + SensitivityLevel.VALID_NAMES + ".", ex);
        }
    }

    /**
     * Injects a {@link ReplacementValidator} into every MAP_REPLACE strategy in the built filter set.
     * The validator re-scans a generated replacement by running the policy's filters over it; if any
     * filter detects PII the generated value is rejected and the strategy falls back. The re-scan uses
     * a throwaway context so it never mutates the caller's context, and a per-thread guard prevents a
     * MAP_REPLACE strategy encountered during the re-scan from invoking its generator (which would
     * recurse). A re-scan that itself errors is treated as unsafe so the strategy falls back.
     * @param policy The policy being loaded.
     * @param filters The complete, built filter set for the policy.
     */
    // Terms given in the policy replace the word list bundled in the jar, so a deployment can
    // narrow or extend a dictionary-backed type. Without terms the bundled list is used, which is
    // the shared, process-wide instance.
    /**
     * The configuration of one filter: its strategies and the settings every filter takes from the policy and
     * from the policy filter (ignored terms, files, and patterns, window size, priority, and id). Every filter is
     * configured here, so a setting added here applies to every filter.
     * @param policy The {@link Policy}.
     * @param filter The policy's settings for the filter.
     * @param strategies The filter's strategies.
     * @param replacementGenerators The policy's replacement generators.
     * @return The {@link FilterConfiguration}.
     */
    private FilterConfiguration configuration(final Policy policy, final AbstractFilter filter,
                                              final List<? extends AbstractFilterStrategy> strategies,
                                              final Map<String, ReplacementGenerator> replacementGenerators) {

        return new FilterConfiguration.FilterConfigurationBuilder()
                .withRandom(random)
                .withRegexTimeoutMs(phileasConfiguration.regexTimeoutMs())
                .withPolicyFiles(policyFiles)
                .withStrategies(strategies)
                .withIgnored(filter.getIgnored())
                .withIgnoredFiles(filter.getIgnoredFiles())
                .withIgnoredPatterns(filter.getIgnoredPatterns())
                .withCrypto(policy.getCrypto())
                .withFPE(policy.getFpe())
                .withGenerators(replacementGenerators)
                .withWindowSize(filter.getWindowSizeOrDefault(phileasConfiguration.spanWindowSize()))
                .withPriority(filter.getPriority())
                .withId(filter.getId())
                .build();

    }

    private Filter createDictionaryFilter(final FilterType filterType, final FilterConfiguration filterConfiguration,
                                          final List<String> terms, final boolean fuzzy,
                                          final String sensitivity, final boolean capitalized)
            throws IOException {

        // Parsed whether or not the filter is fuzzy, so a misspelled level is refused before fuzzy
        // matching is turned on rather than after.
        // Named by its policy key; first names are the one type whose name differs from its key.
        final String policyKey = filterType == FilterType.FIRST_NAME ? "firstName" : filterType.getType();
        final SensitivityLevel sensitivityLevel = sensitivityLevel(sensitivity, "the " + policyKey + " filter");

        if(fuzzy) {

            return CollectionUtils.isNotEmpty(terms)
                    ? new FuzzyDictionaryFilter(filterType, filterConfiguration, sensitivityLevel, new LinkedHashSet<>(terms), capitalized)
                    : new FuzzyDictionaryFilter(filterType, filterConfiguration, sensitivityLevel, capitalized);

        }

        return CollectionUtils.isNotEmpty(terms)
                ? new SetDictionaryFilter(filterType, filterConfiguration, new LinkedHashSet<>(terms), null)
                : new SetDictionaryFilter(filterType, filterConfiguration);

    }

    private void wireReplacementValidators(final Policy policy, final List<Filter> filters) {

        final ReplacementValidator validator = (candidate) -> {

            if (AbstractFilterStrategy.isRescanning()) {
                return false;
            }

            AbstractFilterStrategy.setRescanning(true);
            try {
                // A throwaway context so the re-scan never mutates the caller's context. The context
                // name is immaterial to detection over this fresh context.
                final ContextService rescanContext = new DefaultContextService();
                for (final Filter filter : filters) {
                    if (!filter.filter(rescanContext, policy, "", 0, candidate).getSpans().isEmpty()) {
                        return true;
                    }
                }
                return false;
            } catch (final Exception e) {
                LOGGER.warn("Re-scan of a generated replacement failed; treating it as unsafe. Reason: {}", e.getMessage());
                return true;
            } finally {
                AbstractFilterStrategy.setRescanning(false);
            }

        };

        for (final Filter filter : filters) {
            for (final AbstractFilterStrategy strategy : filter.getStrategies()) {
                if (AbstractFilterStrategy.MAP_REPLACE.equalsIgnoreCase(strategy.getStrategy())) {
                    strategy.setReplacementValidator(validator);
                }
            }
        }

    }

}
