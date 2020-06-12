package com.mtnfog.phileas.services;

import com.google.gson.Gson;
import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.metrics.PhileasMetricsService;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.MimeType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.filter.rules.dictionary.BloomFilterDictionaryFilter;
import com.mtnfog.phileas.model.filter.rules.dictionary.DictionaryFilter;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.Ignored;
import com.mtnfog.phileas.model.profile.filters.CustomDictionary;
import com.mtnfog.phileas.model.profile.filters.Identifier;
import com.mtnfog.phileas.model.profile.filters.Section;
import com.mtnfog.phileas.model.responses.FilterResponse;
import com.mtnfog.phileas.model.services.*;
import com.mtnfog.phileas.processors.unstructured.UnstructuredDocumentProcessor;
import com.mtnfog.phileas.service.ai.PyTorchFilter;
import com.mtnfog.phileas.services.alerts.AlertServiceFactory;
import com.mtnfog.phileas.services.anonymization.*;
import com.mtnfog.phileas.services.anonymization.cache.AnonymizationCacheServiceFactory;
import com.mtnfog.phileas.services.disambiguation.VectorBasedSpanDisambiguationService;
import com.mtnfog.phileas.services.filters.custom.PhoneNumberRulesFilter;
import com.mtnfog.phileas.services.filters.regex.*;
import com.mtnfog.phileas.services.postfilters.IgnoredTermsFilter;
import com.mtnfog.phileas.services.postfilters.TrailingPeriodPostFilter;
import com.mtnfog.phileas.services.postfilters.TrailingSpacePostFilter;
import com.mtnfog.phileas.services.profiles.LocalFilterProfileService;
import com.mtnfog.phileas.services.profiles.S3FilterProfileService;
import com.mtnfog.phileas.services.validators.DateSpanValidator;
import com.mtnfog.phileas.store.ElasticsearchStore;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public class PhileasFilterService implements FilterService {

	private static final Logger LOGGER = LogManager.getLogger(PhileasFilterService.class);

    private FilterProfileService filterProfileService;
    private MetricsService metricsService;
    private Store store;

    private Map<String, DescriptiveStatistics> stats;
    private Gson gson = new Gson();

    private String philterNerEndpoint;
    private AnonymizationCacheService anonymizationCacheService;
    private AlertService alertService;
    private SpanDisambiguationService spanDisambiguationService;
    private String indexDirectory;
    private double bloomFilterFpp;

    private DocumentProcessor unstructuredDocumentProcessor;

    private final int windowSize;

    public PhileasFilterService(PhileasConfiguration phileasConfiguration) throws IOException {

        LOGGER.info("Initializing Phileas engine.");

        // Configure metrics.
        this.metricsService = new PhileasMetricsService(phileasConfiguration);

        // Set the filter profile services.
        this.filterProfileService = buildFilterProfileService(phileasConfiguration);

        // Set the anonymization cache service.
        this.anonymizationCacheService = AnonymizationCacheServiceFactory.getAnonymizationCacheService(phileasConfiguration);

        // Set the alert service.
        this.alertService = AlertServiceFactory.getAlertService(phileasConfiguration);

        // Instantiate the stats.
        this.stats = new HashMap<>();

        // Set the bloom filter FPP.
        this.bloomFilterFpp = phileasConfiguration.bloomFilterFpp();

        // Configure span disambiguation.
        this.spanDisambiguationService = new VectorBasedSpanDisambiguationService(phileasConfiguration);

        // Create a new unstructured document processor.
        this.unstructuredDocumentProcessor = new UnstructuredDocumentProcessor(metricsService, spanDisambiguationService, store);

        // Get the window size.
        this.windowSize = phileasConfiguration.spanWindowSize();
        LOGGER.info("Using window size {}", this.windowSize);

        // Configure store.
        final boolean storeEnabled = phileasConfiguration.storeEnabled();

        if(storeEnabled) {

            LOGGER.info("Store is enabled.");

            final String index = phileasConfiguration.storeElasticSearchIndex();
            final String host = phileasConfiguration.storeElasticSearchHost();
            final String scheme = phileasConfiguration.storeElasticSearchScheme();
            final int port = phileasConfiguration.storeElasticSearchPort();
            this.store = new ElasticsearchStore(index, scheme, host, port);

        } else {

            LOGGER.info("Store is disabled.");

        }

        this.indexDirectory = phileasConfiguration.indexesDirectory();
        LOGGER.info("Using indexes directory {}", this.indexDirectory);

        this.philterNerEndpoint = phileasConfiguration.philterNerEndpoint();
        LOGGER.info("Using Philter NER endpoint {}", phileasConfiguration.philterNerEndpoint());

    }

    @Override
    public FilterProfileService getFilterProfileService() {
        return filterProfileService;
    }

    @Override
    public AlertService getAlertService() {
        return alertService;
    }

    @Override
    public List<Span> replacements(String documentId) throws IOException {
        return store.getByDocumentId(documentId);
    }

    @Override
    public FilterResponse filter(PhileasConfiguration phileasConfiguration, String filterProfileName, String context, String documentId, String input, MimeType mimeType) throws Exception {

        // Get the filter profile.
        // This will ALWAYS return a filter profile because if it is not in the cache it will be
        // retrieved from the cache.
        // TODO: How to trigger a reload if the profile had to be retrieved from disk?
        final String filterProfileJson = filterProfileService.get(filterProfileName);

        LOGGER.debug("Deserializing filter profile [{}]", filterProfileName);
        final FilterProfile filterProfile = gson.fromJson(filterProfileJson, FilterProfile.class);

        // See if we need to generate a document ID.
        if(StringUtils.isEmpty(documentId)) {

            // PHL-58: Use a hash function to generate the document ID.
            documentId = DigestUtils.md5Hex(UUID.randomUUID().toString() + "-" + context + "-" + filterProfileName + "-" + input);
            LOGGER.debug("Generated document ID {}", documentId);

        }

        final List<Filter> filters = getFiltersForFilterProfile(filterProfileName);
        final List<PostFilter> postFilters = getPostFiltersForFilterProfile(filterProfileName);

        final FilterResponse filterResponse;

        if(mimeType == MimeType.TEXT_PLAIN) {
            filterResponse = unstructuredDocumentProcessor.process(filterProfile, filters, postFilters, context, documentId, input);
        //} else if(mimeType == MimeType.APPLICATION_FHIRJSON) {
        //    filterResponse = fhirDocumentProcessor.process(filterProfile, filters, postFilters, context, documentId, input);
        } else {
            // Should never happen but just in case.
            throw new Exception("Unknown mime type.");
        }


        // Store the spans, if enabled.
        if(phileasConfiguration.storeEnabled()) {
            store.insert(filterResponse.getExplanation().getAppliedSpans());
        }

        return filterResponse;

    }

    private FilterProfileService buildFilterProfileService(PhileasConfiguration phileasConfiguration) throws IOException {

        final FilterProfileService filterProfileService;
        final String s3Bucket = phileasConfiguration.filterProfilesS3Bucket();

        // If an S3 bucket is provided then instantiate an S3FilterProfileService.
        if(StringUtils.isNotEmpty(s3Bucket)) {

            LOGGER.info("Initializing configuration for filter profiles S3 bucket.");
            filterProfileService = new S3FilterProfileService(phileasConfiguration, false);

        } else {

            LOGGER.info("Using local storage for filter profiles.");
            filterProfileService = new LocalFilterProfileService(phileasConfiguration);

        }

        return filterProfileService;

    }

    public List<PostFilter> getPostFiltersForFilterProfile(final String filterProfileName) throws IOException {

        LOGGER.debug("Reloading filter profiles.");

        final String filterProfileJson = filterProfileService.get(filterProfileName);
        final FilterProfile filterProfile = gson.fromJson(filterProfileJson, FilterProfile.class);

        final List<PostFilter> postFilters = new LinkedList<>();

        // Post filters.

        // Configure post filters.
        // PHL-1: Allow for multi-word tokens.
        /*final boolean posTagPostFilterEnabled = StringUtils.equalsIgnoreCase(applicationProperties.getProperty("post.filter.pos.enabled", "true"), "true");
        if(posTagPostFilterEnabled) {
            final InputStream is = PhileasFilterService.class.getClassLoader().getResourceAsStream("en-pos-perceptron.bin");
            postFilters.add(new PartOfSpeechFalsePositiveFilter(is));
        }*/

        // Ignored terms filter. Looks for ignored terms in the scope of the whole document (and not just a particular filter).
        // No matter what filter found the span, it is subject to this ignore list.
        if(CollectionUtils.isNotEmpty(filterProfile.getIgnored())) {

            // Make a post filter for each Ignored item in the list.
            for(final Ignored ignored : filterProfile.getIgnored()) {
                postFilters.add(new IgnoredTermsFilter(ignored));
            }

        }

        // Remove trailing periods from filters.
        postFilters.add(new TrailingPeriodPostFilter());
        postFilters.add(new TrailingSpacePostFilter());

        return postFilters;

    }

    public List<Filter> getFiltersForFilterProfile(final String filterProfileName) throws IOException {

        LOGGER.debug("Getting filters for filter profile [{}]", filterProfileName);

        final String filterProfileJson = filterProfileService.get(filterProfileName);
        final FilterProfile filterProfile = gson.fromJson(filterProfileJson, FilterProfile.class);

        final List<Filter> enabledFilters = new LinkedList<>();

        // Rules filters.

        if(filterProfile.getIdentifiers().hasFilter(FilterType.AGE) && filterProfile.getIdentifiers().getAge().isEnabled()) {
            enabledFilters.add(new AgeFilter(filterProfile.getIdentifiers().getAge().getAgeFilterStrategies(), new AgeAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getAge().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.BITCOIN_ADDRESS) && filterProfile.getIdentifiers().getBitcoinAddress().isEnabled()) {
            enabledFilters.add(new BitcoinAddressFilter(filterProfile.getIdentifiers().getBitcoinAddress().getBitcoinFilterStrategies(), new BitcoinAddressAnonymizationService(anonymizationCacheService), alertService,  filterProfile.getIdentifiers().getBitcoinAddress().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.CREDIT_CARD) && filterProfile.getIdentifiers().getCreditCard().isEnabled()) {
            enabledFilters.add(new CreditCardFilter(filterProfile.getIdentifiers().getCreditCard().getCreditCardFilterStrategies(), new CreditCardAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getCreditCard().isOnlyValidCreditCardNumbers(), filterProfile.getIdentifiers().getCreditCard().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.DATE) && filterProfile.getIdentifiers().getDate().isEnabled()) {
            enabledFilters.add(new DateFilter(filterProfile.getIdentifiers().getDate().getDateFilterStrategies(), new DateAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getDate().isOnlyValidDates(), DateSpanValidator.getInstance(), filterProfile.getIdentifiers().getDate().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.DRIVERS_LICENSE_NUMBER) && filterProfile.getIdentifiers().getDriversLicense().isEnabled()) {
            enabledFilters.add(new DriversLicenseFilter(filterProfile.getIdentifiers().getDriversLicense().getDriversLicenseFilterStrategies(), new AlphanumericAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getDriversLicense().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.EMAIL_ADDRESS) && filterProfile.getIdentifiers().getEmailAddress().isEnabled()) {
            enabledFilters.add(new EmailAddressFilter(filterProfile.getIdentifiers().getEmailAddress().getEmailAddressFilterStrategies(), new EmailAddressAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getEmailAddress().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.IBAN_CODE) && filterProfile.getIdentifiers().getIbanCode().isEnabled()) {
            enabledFilters.add(new IbanCodeFilter(filterProfile.getIdentifiers().getIbanCode().getIbanCodeFilterStrategies(), new IbanCodeAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getIbanCode().getIgnored(), filterProfile.getCrypto(), filterProfile.getIdentifiers().getIbanCode().isOnlyValidIBANCodes(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.IP_ADDRESS) && filterProfile.getIdentifiers().getIpAddress().isEnabled()) {
            enabledFilters.add(new IpAddressFilter(filterProfile.getIdentifiers().getIpAddress().getIpAddressFilterStrategies(), new IpAddressAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getIpAddress().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.MAC_ADDRESS) && filterProfile.getIdentifiers().getMacAddress().isEnabled()) {
            enabledFilters.add(new MacAddressFilter(filterProfile.getIdentifiers().getIpAddress().getIpAddressFilterStrategies(), new MacAddressAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getMacAddress().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.PASSPORT_NUMBER) && filterProfile.getIdentifiers().getPassportNumber().isEnabled()) {
            enabledFilters.add(new PassportNumberFilter(filterProfile.getIdentifiers().getPassportNumber().getPassportNumberFilterStrategies(), new PassportNumberAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getPassportNumber().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.PHONE_NUMBER_EXTENSION) && filterProfile.getIdentifiers().getPhoneNumberExtension().isEnabled()) {
            enabledFilters.add(new PhoneNumberExtensionFilter(filterProfile.getIdentifiers().getPhoneNumberExtension().getPhoneNumberExtensionFilterStrategies(), new AlphanumericAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getPhoneNumberExtension().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.PHONE_NUMBER) && filterProfile.getIdentifiers().getPhoneNumber().isEnabled()) {
            enabledFilters.add(new PhoneNumberRulesFilter(filterProfile.getIdentifiers().getPhoneNumber().getPhoneNumberFilterStrategies(), new AlphanumericAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getPhoneNumber().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.SECTION)) {

            final List<Section> sections = filterProfile.getIdentifiers().getSections();

            for(final Section section : sections) {

                if(section.isEnabled()) {
                    enabledFilters.add(new SectionFilter(section.getSectionFilterStrategies(), new AlphanumericAnonymizationService(anonymizationCacheService), alertService, section.getStartPattern(), section.getEndPattern(), section.getIgnored(), filterProfile.getCrypto(), windowSize));
                }

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.SSN) && filterProfile.getIdentifiers().getSsn().isEnabled()) {
            enabledFilters.add(new SsnFilter(filterProfile.getIdentifiers().getSsn().getSsnFilterStrategies(), new AlphanumericAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getSsn().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.STATE_ABBREVIATION) && filterProfile.getIdentifiers().getStateAbbreviation().isEnabled()) {
            enabledFilters.add(new StateAbbreviationFilter(filterProfile.getIdentifiers().getStateAbbreviation().getStateAbbreviationsFilterStrategies(), new StateAbbreviationAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getStateAbbreviation().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.TRACKING_NUMBER) && filterProfile.getIdentifiers().getTrackingNumber().isEnabled()) {
            enabledFilters.add(new TrackingNumberFilter(filterProfile.getIdentifiers().getTrackingNumber().getTrackingNumberFilterStrategies(), new AlphanumericAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getTrackingNumber().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.URL) && filterProfile.getIdentifiers().getUrl().isEnabled()) {
            enabledFilters.add(new UrlFilter(filterProfile.getIdentifiers().getUrl().getUrlFilterStrategies(), new UrlAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getUrl().isRequireHttpWwwPrefix(), filterProfile.getIdentifiers().getUrl().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.VIN) && filterProfile.getIdentifiers().getVin().isEnabled()) {
            enabledFilters.add(new VinFilter(filterProfile.getIdentifiers().getVin().getVinFilterStrategies(), new VinAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getVin().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.ZIP_CODE) && filterProfile.getIdentifiers().getZipCode().isEnabled()) {
            enabledFilters.add(new ZipCodeFilter(filterProfile.getIdentifiers().getZipCode().getZipCodeFilterStrategies(), new ZipCodeAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getZipCode().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        // Custom dictionary filters.

        if(filterProfile.getIdentifiers().hasFilter(FilterType.CUSTOM_DICTIONARY)) {

            LOGGER.info("Filter profile {} has {} custom dictionaries.", filterProfile.getName(), filterProfile.getIdentifiers().getCustomDictionaries().size());

            // We keep track of the index of the custom dictionary in the list so we know
            // how to retrieve the strategy for the custom dictionary. This is because
            // there can be multiple custom dictionaries and not a 1-to-1 between filter
            // and strategy.
            int index = 0;

            // There can be multiple custom dictionary filters because it is a list.
            for(final CustomDictionary customDictionary : filterProfile.getIdentifiers().getCustomDictionaries()) {

                if(customDictionary.isEnabled()) {

                    // TODO: Should there be an anonymization service?
                    // There is no anonymization service because we don't know what to replace custom dictionary items with.
                    final AnonymizationService anonymizationService = null;

                    // All of the custom terms.
                    final Set<String> terms = new LinkedHashSet<>();

                    // First, read the terms from the filter profile.
                    if(CollectionUtils.isNotEmpty(customDictionary.getTerms())) {
                        terms.addAll(customDictionary.getTerms());
                    }

                    // Next, read terms from files, if given.
                    for(final String file : customDictionary.getFiles()) {
                        terms.addAll(FileUtils.readLines(new File(file), Charset.defaultCharset()));
                    }

                    if(customDictionary.isFuzzy()) {

                        LOGGER.info("Custom fuzzy dictionary contains {} terms.", terms.size());

                        // Use a Lucene dictionary filter.
                        final DictionaryFilter dictionaryFilter = new LuceneDictionaryFilter(
                                FilterType.CUSTOM_DICTIONARY,
                                customDictionary.getCustomDictionaryFilterStrategies(),
                                SensitivityLevel.fromName(customDictionary.getSensitivity()),
                                anonymizationService,
                                alertService,
                                customDictionary.getClassification(),
                                terms,
                                index,
                                customDictionary.getIgnored(),
                                filterProfile.getCrypto(),
                                windowSize);

                        enabledFilters.add(dictionaryFilter);

                    } else {

                        LOGGER.info("Custom dictionary contains {} terms.", terms.size());

                        // Use a bloomfilter.
                        final DictionaryFilter dictionaryFilter = new BloomFilterDictionaryFilter(FilterType.CUSTOM_DICTIONARY,
                                customDictionary.getCustomDictionaryFilterStrategies(), terms, customDictionary.getClassification(),
                                bloomFilterFpp, anonymizationService, alertService,
                                customDictionary.getIgnored(), filterProfile.getCrypto(), windowSize);

                        enabledFilters.add(dictionaryFilter);

                    }

                    index++;

                }

            }

        } else {

            LOGGER.debug("Filter profile {} has no custom dictionaries.", filterProfile.getName());

        }

        // Lucene dictionary filters.

        if(filterProfile.getIdentifiers().hasFilter(FilterType.LOCATION_CITY) && filterProfile.getIdentifiers().getCity().isEnabled()) {
            enabledFilters.add(new LuceneDictionaryFilter(FilterType.LOCATION_CITY, filterProfile.getIdentifiers().getCity().getCityFilterStrategies(), indexDirectory + "cities", filterProfile.getIdentifiers().getCity().getSensitivityLevel(), new CityAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getCity().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.LOCATION_COUNTY) && filterProfile.getIdentifiers().getCounty().isEnabled()) {
            enabledFilters.add(new LuceneDictionaryFilter(FilterType.LOCATION_COUNTY, filterProfile.getIdentifiers().getCounty().getCountyFilterStrategies(), indexDirectory + "states", filterProfile.getIdentifiers().getCounty().getSensitivityLevel(), new CountyAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getCounty().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.LOCATION_STATE) && filterProfile.getIdentifiers().getState().isEnabled()) {
            enabledFilters.add(new LuceneDictionaryFilter(FilterType.LOCATION_STATE, filterProfile.getIdentifiers().getState().getStateFilterStrategies(), indexDirectory + "states", filterProfile.getIdentifiers().getState().getSensitivityLevel(), new StateAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getState().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.HOSPITAL) && filterProfile.getIdentifiers().getHospital().isEnabled()) {
            enabledFilters.add(new LuceneDictionaryFilter(FilterType.HOSPITAL, filterProfile.getIdentifiers().getHospital().getHospitalFilterStrategies(), indexDirectory + "hospitals", filterProfile.getIdentifiers().getHospital().getSensitivityLevel(), new HospitalAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getHospital().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.HOSPITAL_ABBREVIATION) && filterProfile.getIdentifiers().getHospitalAbbreviation().isEnabled()) {
            enabledFilters.add(new LuceneDictionaryFilter(FilterType.HOSPITAL_ABBREVIATION, filterProfile.getIdentifiers().getHospitalAbbreviation().getHospitalAbbreviationFilterStrategies(), indexDirectory + "hospital-abbreviations", filterProfile.getIdentifiers().getHospitalAbbreviation().getSensitivityLevel(), new HospitalAbbreviationAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getHospitalAbbreviation().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.FIRST_NAME) && filterProfile.getIdentifiers().getFirstName().isEnabled()) {
            enabledFilters.add(new LuceneDictionaryFilter(FilterType.FIRST_NAME, filterProfile.getIdentifiers().getFirstName().getFirstNameFilterStrategies(), indexDirectory + "names", filterProfile.getIdentifiers().getFirstName().getSensitivityLevel(), new PersonsAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getFirstName().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.SURNAME) && filterProfile.getIdentifiers().getSurname().isEnabled()) {
            enabledFilters.add(new LuceneDictionaryFilter(FilterType.SURNAME, filterProfile.getIdentifiers().getSurname().getSurnameFilterStrategies(), indexDirectory + "surnames", filterProfile.getIdentifiers().getSurname().getSensitivityLevel(), new SurnameAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getSurname().getIgnored(), filterProfile.getCrypto(), windowSize));
        }

        // Enable ID filter last since it is a pretty generic pattern that might also match SSN, et. al.

        if(filterProfile.getIdentifiers().hasFilter(FilterType.IDENTIFIER)) {

            final List<Identifier> identifiers = filterProfile.getIdentifiers().getIdentifiers();

            for(final Identifier identifier : identifiers) {

                if(identifier.isEnabled()) {
                    enabledFilters.add(new IdentifierFilter(identifier.getClassification(), identifier.getPattern(), identifier.isCaseSensitive(), identifier.getIdentifierFilterStrategies(), new AlphanumericAnonymizationService(anonymizationCacheService), alertService, identifier.getIgnored(), filterProfile.getCrypto(), windowSize));
                }

            }

        }

        // PyTorch filters.

        if(filterProfile.getIdentifiers().hasFilter(FilterType.NER_ENTITY) && filterProfile.getIdentifiers().getNer().isEnabled()) {
            // TODO: Allow a single PyTorchFilter to extract many types of entities instead of just one, i.e. "PER".
            enabledFilters.add(new PyTorchFilter(philterNerEndpoint, FilterType.NER_ENTITY, filterProfile.getIdentifiers().getNer().getNerStrategies(), "PER", stats, metricsService, new PersonsAnonymizationService(anonymizationCacheService), alertService, filterProfile.getIdentifiers().getNer().getIgnored(), filterProfile.getIdentifiers().getNer().isRemovePunctuation(), filterProfile.getCrypto(), windowSize));
        }

        return enabledFilters;

    }

}
