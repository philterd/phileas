package com.mtnfog.phileas.services;

import com.google.gson.Gson;
import com.mtnfog.phileas.ai.PyTorchFilter;
import com.mtnfog.phileas.metrics.PhileasMetricsService;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.exceptions.InvalidFilterProfileException;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import com.mtnfog.phileas.model.objects.Explanation;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.CustomDictionary;
import com.mtnfog.phileas.model.profile.filters.Identifier;
import com.mtnfog.phileas.model.responses.FilterResponse;
import com.mtnfog.phileas.model.services.*;
import com.mtnfog.phileas.services.anonymization.*;
import com.mtnfog.phileas.services.filters.custom.PhoneNumberRulesFilter;
import com.mtnfog.phileas.services.filters.regex.*;
import com.mtnfog.phileas.services.postfilters.IgnoredTermsFilter;
import com.mtnfog.phileas.services.validators.DateSpanValidator;
import com.mtnfog.phileas.store.ElasticsearchStore;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class PhileasFilterService implements FilterService, Serializable {

	private static final long serialVersionUID = 6998388861197152049L;

	private static final Logger LOGGER = LogManager.getLogger(PhileasFilterService.class);

    private List<FilterProfileService> filterProfileServices;
    private MetricsService metricsService;
    private Store store;

    private Map<String, FilterProfile> filterProfiles = new HashMap<>();
    private Map<String, DescriptiveStatistics> stats = new HashMap<>();
    private List<PostFilter> postFilters = new LinkedList<>();
    private Map<String, List<Filter>> filters = new HashMap<>();
    private Gson gson = new Gson();

    private String philterNerEndpoint;
    private AnonymizationCacheService anonymizationCacheService;
    private String indexDirectory;

    public PhileasFilterService(Properties applicationProperties, List<FilterProfileService> filterProfileServices, AnonymizationCacheService anonymizationCacheService, String philterNerEndpoint) throws IOException {

        LOGGER.info("Initializing Phileas engine.");

        // Configure metrics.
        this.metricsService = new PhileasMetricsService(applicationProperties);

        // Set the filter profile services.
        this.filterProfileServices = filterProfileServices;

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

        // Load the filter profiles from the services into a map.
        reloadFilterProfiles();

    }

    @Override
    public List<Span> replacements(String documentId) throws IOException {

        return store.getByDocumentId(documentId);

    }

    @Override
    public FilterResponse filter(String filterProfileName, String context, String input) throws InvalidFilterProfileException, IOException {

        if(!filterProfiles.containsKey(filterProfileName)) {
            throw new InvalidFilterProfileException("The filter profile does not exist.");
        }

        // Get the enabled filters for this filter profile.
        final List<Filter> enabledFilters = filters.get(filterProfileName);

        // Get the filter profile.
        final FilterProfile filterProfile = filterProfiles.get(filterProfileName);

        // The list that will contain the spans containing PHI/PII.
        List<Span> spans = new LinkedList<>();

        // Generate a random document ID.
        final String documentId = UUID.randomUUID().toString();

        // Execute each filter.
        for(final Filter f : enabledFilters) {
            spans.addAll(f.filter(filterProfile, context, documentId, input));
        }

        // Drop overlapping spans.
        spans = Span.dropOverlappingSpans(spans);

        // Drop ignored spans.
        spans = Span.dropIgnoredSpans(spans);

        // Sort the spans based on the confidence.
        spans.sort(Comparator.comparing(Span::getConfidence));

        // Perform post-filtering for false positives.
        for(final PostFilter postFilter : postFilters) {
            spans = postFilter.filter(input, spans);
        }

        // The spans that will be persisted. Has to be a deep copy because the shift
        // below will change the indexes. Doing this to save the original locations of the spans.
        final List<Span> appliedSpans = spans.stream().map(d -> d.copy()).collect(toList());

        // TODO: Set a flag on each "span" not in appliedSpans that it was not used.

        // Log a metric for each filter type.
        appliedSpans.forEach(k -> metricsService.incrementFilterType(k.getFilterType()));

        // Define the explanation.
        final Explanation explanation = new Explanation(appliedSpans, spans);

        // Used to manipulate the text.
        final StringBuffer buffer = new StringBuffer(input);

        // Initialize this to the the input length but it may grow in length if redactions/replacements
        // are longer than the original spans.
        int stringLength = input.length();

        // Go character by character through the input.
        for(int i = 0; i < stringLength; i++) {

            // Is index i the start of a span?
            final Span span = Span.doesIndexStartSpan(i, spans);

            if(span != null) {

                // Get the replacement. This might be the token itself or an anonymized version.
                final String replacement = span.getReplacement();

                final int spanLength = span.getCharacterEnd() - span.getCharacterStart();
                final int replacementLength = replacement.length();

                if(spanLength != replacementLength) {

                    // We need to adjust the characterStart and characterEnd for the remaining spans.
                    // A negative value means shift left.
                    // A positive value means shift right.
                    final int shift = (spanLength - replacementLength) * -1;

                    // Shift the remaining spans by the shift value.
                    spans = Span.shiftSpans(shift, span, spans);

                    // Update the length of the string.
                    stringLength += shift;

                }

                // We can now do the replacement.
                buffer.replace(span.getCharacterStart(), span.getCharacterEnd(), replacement);

                // Jump ahead outside of this span.
                i = span.getCharacterEnd();

            }

        }

        metricsService.incrementProcessed();

        // Store the applied spans in the database.
        if(store != null) {
            store.insert(appliedSpans);
        }

        return new FilterResponse(buffer.toString(), context, documentId, explanation);

    }

    @Override
    public void reloadFilterProfiles() throws IOException {

        LOGGER.info("Reloading filter profiles.");

        // Clear the current filters.
        filterProfiles.clear();
        filters.clear();
        postFilters.clear();

        // Load all of the filter profiles into memory from each filter profile service.
        for(FilterProfileService filterProfileService : filterProfileServices) {
            final Map<String, String> fp = filterProfileService.getAll();
            for(String k : fp.keySet()) {
                filterProfiles.put(k, gson.fromJson(fp.get(k), FilterProfile.class));
            }
        }

        // Load the actual filter profile objects into memory.
        for(final FilterProfile filterProfile : filterProfiles.values()) {

            final List<Filter> enabledFilters = new LinkedList<>();

            // Rules filters.

            if(filterProfile.getIdentifiers().hasFilter(FilterType.AGE)) {
                enabledFilters.add(new AgeFilter(filterProfile.getIdentifiers().getAge().getAgeFilterStrategies(), new AgeAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getAge().getIgnored()));
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.CREDIT_CARD)) {
                enabledFilters.add(new CreditCardFilter(filterProfile.getIdentifiers().getCreditCard().getCreditCardFilterStrategies(), new CreditCardAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getCreditCard().isOnlyValidCreditCardNumbers(), filterProfile.getIdentifiers().getCreditCard().getIgnored()));
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.DATE)) {
                enabledFilters.add(new DateFilter(filterProfile.getIdentifiers().getDate().getDateFilterStrategies(), new DateAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getDate().isOnlyValidDates(), new DateSpanValidator(), filterProfile.getIdentifiers().getDate().getIgnored()));
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.EMAIL_ADDRESS)) {
                enabledFilters.add(new EmailAddressFilter(filterProfile.getIdentifiers().getEmailAddress().getEmailAddressFilterStrategies(), new EmailAddressAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getEmailAddress().getIgnored()));
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.IP_ADDRESS)) {
                enabledFilters.add(new IpAddressFilter(filterProfile.getIdentifiers().getIpAddress().getIpAddressFilterStrategies(), new IpAddressAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getIpAddress().getIgnored()));
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.PHONE_NUMBER_EXTENSION)) {
                enabledFilters.add(new PhoneNumberExtensionFilter(filterProfile.getIdentifiers().getPhoneNumberExtension().getPhoneNumberExtensionFilterStrategies(), new AlphanumericAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getPhoneNumberExtension().getIgnored()));
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.PHONE_NUMBER)) {
                enabledFilters.add(new PhoneNumberRulesFilter(filterProfile.getIdentifiers().getPhoneNumber().getPhoneNumberFilterStrategies(), new AlphanumericAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getPhoneNumber().getIgnored()));
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.SSN)) {
                enabledFilters.add(new SsnFilter(filterProfile.getIdentifiers().getSsn().getSsnFilterStrategies(), new AlphanumericAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getSsn().getIgnored()));
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.STATE_ABBREVIATION)) {
                enabledFilters.add(new StateAbbreviationFilter(filterProfile.getIdentifiers().getStateAbbreviation().getStateAbbreviationsFilterStrategies(), new StateAbbreviationAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getStateAbbreviation().getIgnored()));
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.URL)) {
                enabledFilters.add(new UrlFilter(filterProfile.getIdentifiers().getUrl().getUrlFilterStrategies(), new UrlAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getUrl().getIgnored()));
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.VIN)) {
                enabledFilters.add(new VinFilter(filterProfile.getIdentifiers().getVin().getVinFilterStrategies(), new VinAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getVin().getIgnored()));
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.ZIP_CODE)) {
                enabledFilters.add(new ZipCodeFilter(filterProfile.getIdentifiers().getZipCode().getZipCodeFilterStrategies(), new ZipCodeAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getZipCode().getIgnored()));
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

                    enabledFilters.add(new LuceneDictionaryFilter(FilterType.CUSTOM_DICTIONARY, customDictionary.getCustomDictionaryFilterStrategies(),
                            SensitivityLevel.fromName(customDictionary.getSensitivity()), anonymizationService,
                            customDictionary.getType(), customDictionary.getTerms(), index, customDictionary.getIgnored()));

                    index++;

                }

            } else {

                LOGGER.info("Filter profile {} has no custom dictionaries.", filterProfile.getName());

            }

            // Lucene dictionary filters.

            if(filterProfile.getIdentifiers().hasFilter(FilterType.LOCATION_CITY)) {
                enabledFilters.add(new LuceneDictionaryFilter(FilterType.LOCATION_CITY, filterProfile.getIdentifiers().getCity().getCityFilterStrategies(), indexDirectory + "cities", filterProfile.getIdentifiers().getCity().getSensitivityLevel(), new CityAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getCity().getIgnored()));
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.LOCATION_COUNTY)) {
                enabledFilters.add(new LuceneDictionaryFilter(FilterType.LOCATION_COUNTY, filterProfile.getIdentifiers().getCounty().getCountyFilterStrategies(), indexDirectory + "states", filterProfile.getIdentifiers().getCounty().getSensitivityLevel(), new CountyAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getCounty().getIgnored()));
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.LOCATION_STATE)) {
                enabledFilters.add(new LuceneDictionaryFilter(FilterType.LOCATION_STATE, filterProfile.getIdentifiers().getState().getStateFilterStrategies(), indexDirectory + "states", filterProfile.getIdentifiers().getState().getSensitivityLevel(), new StateAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getState().getIgnored()));
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.HOSPITAL)) {
                enabledFilters.add(new LuceneDictionaryFilter(FilterType.HOSPITAL, filterProfile.getIdentifiers().getHospital().getHospitalFilterStrategies(), indexDirectory + "hospitals", filterProfile.getIdentifiers().getHospital().getSensitivityLevel(), new HospitalAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getHospital().getIgnored()));
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.HOSPITAL_ABBREVIATION)) {
                enabledFilters.add(new LuceneDictionaryFilter(FilterType.HOSPITAL_ABBREVIATION, filterProfile.getIdentifiers().getHospitalAbbreviation().getHospitalAbbreviationFilterStrategies(), indexDirectory + "hospital-abbreviations", filterProfile.getIdentifiers().getHospitalAbbreviation().getSensitivityLevel(), new HospitalAbbreviationAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getHospitalAbbreviation().getIgnored()));
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.FIRST_NAME)) {
                enabledFilters.add(new LuceneDictionaryFilter(FilterType.FIRST_NAME, filterProfile.getIdentifiers().getFirstName().getFirstNameFilterStrategies(), indexDirectory + "names", filterProfile.getIdentifiers().getFirstName().getSensitivityLevel(), new PersonsAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getFirstName().getIgnored()));
            }

            if(filterProfile.getIdentifiers().hasFilter(FilterType.SURNAME)) {
                enabledFilters.add(new LuceneDictionaryFilter(FilterType.SURNAME, filterProfile.getIdentifiers().getSurname().getSurnameFilterStrategies(), indexDirectory + "surnames", filterProfile.getIdentifiers().getSurname().getSensitivityLevel(), new SurnameAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getSurname().getIgnored()));
            }

            // Enable ID filter last since it is a pretty generic pattern that might also match SSN, et. al.

            if(filterProfile.getIdentifiers().hasFilter(FilterType.IDENTIFIER)) {

                final List<Identifier> identifiers = filterProfile.getIdentifiers().getIdentifiers();
                for(Identifier identifier : identifiers) {
                    enabledFilters.add(new IdentifierFilter(identifier.getLabel(), identifier.getPattern(), identifier.isCaseSensitive(), identifier.getIdentifierFilterStrategies(), new AlphanumericAnonymizationService(anonymizationCacheService), identifier.getIgnored()));
                }

            }

            // PyTorch filters.

            if(filterProfile.getIdentifiers().hasFilter(FilterType.NER_ENTITY)) {
                enabledFilters.add(new PyTorchFilter(philterNerEndpoint, FilterType.NER_ENTITY, filterProfile.getIdentifiers().getNer().getNerStrategies(), "PER", stats, metricsService, new PersonsAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getNer().getIgnored()));
                enabledFilters.add(new PyTorchFilter(philterNerEndpoint, FilterType.NER_ENTITY, filterProfile.getIdentifiers().getNer().getNerStrategies(), "LOC", stats, metricsService, new LocationsAnonymizationService(anonymizationCacheService), filterProfile.getIdentifiers().getNer().getIgnored()));
            }

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
                postFilters.add(new IgnoredTermsFilter(filterProfile, false));
            }

        }

    }

}
