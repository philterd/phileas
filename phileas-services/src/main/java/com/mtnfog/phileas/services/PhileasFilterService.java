package com.mtnfog.phileas.services;

import com.google.gson.Gson;
import com.mtnfog.phileas.ai.PyTorchFilter;
import com.mtnfog.phileas.fhir.FhirDocumentProcessor;
import com.mtnfog.phileas.metrics.PhileasMetricsService;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.MimeType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.exceptions.InvalidFilterProfileException;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.Ignored;
import com.mtnfog.phileas.model.profile.filters.CustomDictionary;
import com.mtnfog.phileas.model.profile.filters.Identifier;
import com.mtnfog.phileas.model.profile.filters.Section;
import com.mtnfog.phileas.model.responses.FilterResponse;
import com.mtnfog.phileas.model.services.*;
import com.mtnfog.phileas.services.anonymization.*;
import com.mtnfog.phileas.services.filters.custom.PhoneNumberRulesFilter;
import com.mtnfog.phileas.services.filters.regex.*;
import com.mtnfog.phileas.services.postfilters.IgnoredTermsFilter;
import com.mtnfog.phileas.services.postfilters.TrailingPeriodPostFilter;
import com.mtnfog.phileas.services.postfilters.TrailingSpacePostFilter;
import com.mtnfog.phileas.services.processors.UnstructuredDocumentProcessor;
import com.mtnfog.phileas.services.validators.DateSpanValidator;
import com.mtnfog.phileas.store.ElasticsearchStore;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class PhileasFilterService implements FilterService, Serializable {

	private static final long serialVersionUID = 6998388861197152049L;

	private static final Logger LOGGER = LogManager.getLogger(PhileasFilterService.class);

    private FilterProfileService filterProfileService;
    private MetricsService metricsService;
    private Store store;

    private Map<String, DescriptiveStatistics> stats = new HashMap<>();
    private List<PostFilter> postFilters = new LinkedList<>();
    private Map<String, List<Filter>> filters = new HashMap<>();
    private Gson gson = new Gson();

    private String philterNerEndpoint;
    private AnonymizationCacheService anonymizationCacheService;
    private String indexDirectory;

    private DocumentProcessor unstructuredDocumentProcessor;
    private DocumentProcessor fhirDocumentProcessor;

    public PhileasFilterService(Properties applicationProperties, FilterProfileService filterProfileService, AnonymizationCacheService anonymizationCacheService, String philterNerEndpoint) throws IOException {

        LOGGER.info("Initializing Phileas engine.");

        // Configure metrics.
        this.metricsService = new PhileasMetricsService(applicationProperties);

        // Set the filter profile services.
        this.filterProfileService = filterProfileService;

        // Configure store.
        final boolean storeEnabled = StringUtils.equalsIgnoreCase(applicationProperties.getProperty("store.enabled", "false"), "true");

        if(storeEnabled) {
            final String index = applicationProperties.getProperty("store.elasticsearch.index", "philter");
            final String host = applicationProperties.getProperty("store.elasticsearch.host", "localhost");
            final String scheme = applicationProperties.getProperty("store.elasticsearch.scheme", "http");
            final int port = Integer.valueOf(applicationProperties.getProperty("store.elasticsearch.port", "9200"));
            this.store = new ElasticsearchStore(index, scheme, host, port);
        }

        this.indexDirectory = applicationProperties.getProperty("indexes.directory", System.getProperty("user.dir") + "/indexes/");
        this.anonymizationCacheService = anonymizationCacheService;
        this.philterNerEndpoint = philterNerEndpoint;
        this.fhirDocumentProcessor = new FhirDocumentProcessor(metricsService);

        // Load the filter profiles from the services into a map.
        reloadFilterProfiles();

    }

    @Override
    public List<Span> replacements(String documentId) throws IOException {

        return store.getByDocumentId(documentId);

    }

    @Override
    public FilterResponse filter(String filterProfileName, String context, String documentId, String input, MimeType mimeType) throws Exception {

        // Get the filter profile.
        String filterProfileJson = filterProfileService.get(filterProfileName, false);

        if(filterProfileJson == null) {

            // Reload the filter profiles. This may be one this instance does not know about.
            reloadFilterProfiles();

            filterProfileJson = filterProfileService.get(filterProfileName, false);

            // We still can't find the filter profile so throw an exception.
            if(filterProfileJson == null) {
                throw new InvalidFilterProfileException("The filter profile [" + filterProfileName + "] does not exist.");
            }

        }

        final FilterProfile filterProfile = gson.fromJson(filterProfileJson, FilterProfile.class);

        // See if we need to generate a document ID.
        if(StringUtils.isEmpty(documentId)) {

            // PHL-58: Use a hash function to generate the document ID.
            documentId = DigestUtils.md5Hex(UUID.randomUUID().toString() + "-" + context + "-" + filterProfileName + "-" + input);

        }

        if(mimeType == MimeType.TEXT_PLAIN) {
            return unstructuredDocumentProcessor.process(filterProfile, filters.get(filterProfile.getName()), context, documentId, input);
        } else if(mimeType == MimeType.APPLICATION_FHIRJSON) {
            return fhirDocumentProcessor.process(filterProfile, filters.get(filterProfile.getName()), context, documentId, input);
        }

        // Should never happen but just in case.
        throw new Exception("Unknown mime type.");

    }

    @Override
    public void reloadFilterProfiles() throws IOException {

        LOGGER.info("Reloading filter profiles.");

        // Clear the current filters.
        filters.clear();
        postFilters.clear();

        // Load the actual filter profile objects into memory.
        final Map<String, String> filterProfiles = filterProfileService.getAll(true);
        for(final String filterProfileName : filterProfiles.keySet()) {

            final FilterProfile filterProfile = gson.fromJson(filterProfiles.get(filterProfileName), FilterProfile.class);

            final List<Filter> enabledFilters = new LinkedList<>();

            // Rules filters.

            if(filterProfile.getIdentifiers().hasFilter(FilterType.AGE)) {
                if(filterProfile.getIdentifiers().getAge().isEnabled()) {
                    enabledFilters.add(new AgeFilter(filterProfile.getIdentifiers().getAge().getAgeFilterStrategies(), new AgeAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getAge().getIgnored(), filterProfile.getCrypto()));
                }
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.CREDIT_CARD)) {
                if(filterProfile.getIdentifiers().getCreditCard().isEnabled()) {
                    enabledFilters.add(new CreditCardFilter(filterProfile.getIdentifiers().getCreditCard().getCreditCardFilterStrategies(), new CreditCardAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getCreditCard().isOnlyValidCreditCardNumbers(), filterProfile.getIdentifiers().getCreditCard().getIgnored(), filterProfile.getCrypto()));
                }
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.DATE)) {
                if(filterProfile.getIdentifiers().getDate().isEnabled()) {
                    enabledFilters.add(new DateFilter(filterProfile.getIdentifiers().getDate().getDateFilterStrategies(), new DateAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getDate().isOnlyValidDates(), new DateSpanValidator(), filterProfile.getIdentifiers().getDate().getIgnored(), filterProfile.getCrypto()));
                }
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.EMAIL_ADDRESS)) {
                if(filterProfile.getIdentifiers().getEmailAddress().isEnabled()) {
                    enabledFilters.add(new EmailAddressFilter(filterProfile.getIdentifiers().getEmailAddress().getEmailAddressFilterStrategies(), new EmailAddressAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getEmailAddress().getIgnored(), filterProfile.getCrypto()));
                }
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.IP_ADDRESS)) {
                if(filterProfile.getIdentifiers().getIpAddress().isEnabled()) {
                    enabledFilters.add(new IpAddressFilter(filterProfile.getIdentifiers().getIpAddress().getIpAddressFilterStrategies(), new IpAddressAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getIpAddress().getIgnored(), filterProfile.getCrypto()));
                }
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.MAC_ADDRESS)) {
                if(filterProfile.getIdentifiers().getMacAddress().isEnabled()) {
                    enabledFilters.add(new MacAddressFilter(filterProfile.getIdentifiers().getIpAddress().getIpAddressFilterStrategies(), new MacAddressAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getMacAddress().getIgnored(), filterProfile.getCrypto()));
                }
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.PHONE_NUMBER_EXTENSION)) {
                if(filterProfile.getIdentifiers().getPhoneNumberExtension().isEnabled()) {
                    enabledFilters.add(new PhoneNumberExtensionFilter(filterProfile.getIdentifiers().getPhoneNumberExtension().getPhoneNumberExtensionFilterStrategies(), new AlphanumericAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getPhoneNumberExtension().getIgnored(), filterProfile.getCrypto()));
                }
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.PHONE_NUMBER)) {
                if(filterProfile.getIdentifiers().getPhoneNumber().isEnabled()) {
                    enabledFilters.add(new PhoneNumberRulesFilter(filterProfile.getIdentifiers().getPhoneNumber().getPhoneNumberFilterStrategies(), new AlphanumericAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getPhoneNumber().getIgnored(), filterProfile.getCrypto()));
                }
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.SECTION)) {

                final List<Section> sections = filterProfile.getIdentifiers().getSections();

                for(final Section section : sections) {

                    if(section.isEnabled()) {
                        enabledFilters.add(new SectionFilter(section.getSectionFilterStrategies(), new AlphanumericAnonymizationService(anonymizationCacheService), section.getStartPattern(), section.getEndPattern(), section.getIgnored(), filterProfile.getCrypto()));
                    }

                }

            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.SSN)) {
                if(filterProfile.getIdentifiers().getSsn().isEnabled()) {
                    enabledFilters.add(new SsnFilter(filterProfile.getIdentifiers().getSsn().getSsnFilterStrategies(), new AlphanumericAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getSsn().getIgnored(), filterProfile.getCrypto()));
                }
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.STATE_ABBREVIATION)) {
                if(filterProfile.getIdentifiers().getStateAbbreviation().isEnabled()) {
                    enabledFilters.add(new StateAbbreviationFilter(filterProfile.getIdentifiers().getStateAbbreviation().getStateAbbreviationsFilterStrategies(), new StateAbbreviationAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getStateAbbreviation().getIgnored(), filterProfile.getCrypto()));
                }
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.URL)) {
                if(filterProfile.getIdentifiers().getUrl().isEnabled()) {
                    enabledFilters.add(new UrlFilter(filterProfile.getIdentifiers().getUrl().getUrlFilterStrategies(), new UrlAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getUrl().isRequireHttpWwwPrefix(), filterProfile.getIdentifiers().getUrl().getIgnored(), filterProfile.getCrypto()));
                }
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.VIN)) {
                if(filterProfile.getIdentifiers().getVin().isEnabled()) {
                    enabledFilters.add(new VinFilter(filterProfile.getIdentifiers().getVin().getVinFilterStrategies(), new VinAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getVin().getIgnored(), filterProfile.getCrypto()));
                }
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.ZIP_CODE)) {
                if(filterProfile.getIdentifiers().getZipCode().isEnabled()) {
                    enabledFilters.add(new ZipCodeFilter(filterProfile.getIdentifiers().getZipCode().getZipCodeFilterStrategies(), new ZipCodeAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getZipCode().getIgnored(), filterProfile.getCrypto()));
                }
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
                for(CustomDictionary customDictionary : filterProfile.getIdentifiers().getCustomDictionaries()) {

                    // There is no anonymization service because we don't know what to replace custom dictionary items with.
                    final AnonymizationService anonymizationService = null;

                    if(customDictionary.isEnabled()) {

                        enabledFilters.add(new LuceneDictionaryFilter(FilterType.CUSTOM_DICTIONARY, customDictionary.getCustomDictionaryFilterStrategies(),
                                SensitivityLevel.fromName(customDictionary.getSensitivity()), anonymizationService,
                                customDictionary.getType(), customDictionary.getTerms(), index, customDictionary.getIgnored(), filterProfile.getCrypto()));

                        index++;

                    }

                }

            } else {

                LOGGER.debug("Filter profile {} has no custom dictionaries.", filterProfile.getName());

            }

            // Lucene dictionary filters.

            if(filterProfile.getIdentifiers().hasFilter(FilterType.LOCATION_CITY)) {
                if(filterProfile.getIdentifiers().getCity().isEnabled()) {
                    enabledFilters.add(new LuceneDictionaryFilter(FilterType.LOCATION_CITY, filterProfile.getIdentifiers().getCity().getCityFilterStrategies(), indexDirectory + "cities", filterProfile.getIdentifiers().getCity().getSensitivityLevel(), new CityAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getCity().getIgnored(), filterProfile.getCrypto()));
                }
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.LOCATION_COUNTY)) {
                if(filterProfile.getIdentifiers().getCounty().isEnabled()) {
                    enabledFilters.add(new LuceneDictionaryFilter(FilterType.LOCATION_COUNTY, filterProfile.getIdentifiers().getCounty().getCountyFilterStrategies(), indexDirectory + "states", filterProfile.getIdentifiers().getCounty().getSensitivityLevel(), new CountyAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getCounty().getIgnored(), filterProfile.getCrypto()));
                }
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.LOCATION_STATE)) {
                if(filterProfile.getIdentifiers().getState().isEnabled()) {
                    enabledFilters.add(new LuceneDictionaryFilter(FilterType.LOCATION_STATE, filterProfile.getIdentifiers().getState().getStateFilterStrategies(), indexDirectory + "states", filterProfile.getIdentifiers().getState().getSensitivityLevel(), new StateAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getState().getIgnored(), filterProfile.getCrypto()));
                }
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.HOSPITAL)) {
                if(filterProfile.getIdentifiers().getHospital().isEnabled()) {
                    enabledFilters.add(new LuceneDictionaryFilter(FilterType.HOSPITAL, filterProfile.getIdentifiers().getHospital().getHospitalFilterStrategies(), indexDirectory + "hospitals", filterProfile.getIdentifiers().getHospital().getSensitivityLevel(), new HospitalAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getHospital().getIgnored(), filterProfile.getCrypto()));
                }
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.HOSPITAL_ABBREVIATION)) {
                if(filterProfile.getIdentifiers().getHospitalAbbreviation().isEnabled()) {
                    enabledFilters.add(new LuceneDictionaryFilter(FilterType.HOSPITAL_ABBREVIATION, filterProfile.getIdentifiers().getHospitalAbbreviation().getHospitalAbbreviationFilterStrategies(), indexDirectory + "hospital-abbreviations", filterProfile.getIdentifiers().getHospitalAbbreviation().getSensitivityLevel(), new HospitalAbbreviationAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getHospitalAbbreviation().getIgnored(), filterProfile.getCrypto()));
                }
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.FIRST_NAME)) {
                if(filterProfile.getIdentifiers().getFirstName().isEnabled()) {
                    enabledFilters.add(new LuceneDictionaryFilter(FilterType.FIRST_NAME, filterProfile.getIdentifiers().getFirstName().getFirstNameFilterStrategies(), indexDirectory + "names", filterProfile.getIdentifiers().getFirstName().getSensitivityLevel(), new PersonsAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getFirstName().getIgnored(), filterProfile.getCrypto()));
                }
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.SURNAME)) {
                if(filterProfile.getIdentifiers().getSurname().isEnabled()) {
                    enabledFilters.add(new LuceneDictionaryFilter(FilterType.SURNAME, filterProfile.getIdentifiers().getSurname().getSurnameFilterStrategies(), indexDirectory + "surnames", filterProfile.getIdentifiers().getSurname().getSensitivityLevel(), new SurnameAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getSurname().getIgnored(), filterProfile.getCrypto()));
                }
            }

            // Enable ID filter last since it is a pretty generic pattern that might also match SSN, et. al.

            if(filterProfile.getIdentifiers().hasFilter(FilterType.IDENTIFIER)) {

                final List<Identifier> identifiers = filterProfile.getIdentifiers().getIdentifiers();

                for(final Identifier identifier : identifiers) {

                    if(identifier.isEnabled()) {
                        enabledFilters.add(new IdentifierFilter(identifier.getLabel(), identifier.getPattern(), identifier.isCaseSensitive(), identifier.getIdentifierFilterStrategies(), new AlphanumericAnonymizationService(anonymizationCacheService), identifier.getIgnored(), filterProfile.getCrypto()));
                    }

                }

            }

            // PyTorch filters.

            if(filterProfile.getIdentifiers().hasFilter(FilterType.NER_ENTITY)) {
                if(filterProfile.getIdentifiers().getNer().isEnabled()) {
                    // TODO: Allow a single PyTorchFilter to extract many types of entities instead of just one, i.e. "PER".
                    enabledFilters.add(new PyTorchFilter(philterNerEndpoint, FilterType.NER_ENTITY, filterProfile.getIdentifiers().getNer().getNerStrategies(), "PER", stats, metricsService, new PersonsAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getNer().getIgnored(), filterProfile.getIdentifiers().getNer().isRemovePunctuation(), filterProfile.getCrypto()));
                }
            }

            // Put the enabled filters in a map with the filter profile name.
            filters.put(filterProfile.getName(), enabledFilters);

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

        }

        unstructuredDocumentProcessor = new UnstructuredDocumentProcessor(postFilters, metricsService, store);

    }

}
