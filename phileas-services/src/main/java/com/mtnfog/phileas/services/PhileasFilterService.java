package com.mtnfog.phileas.services;

import com.mtnfog.phileas.ai.PyTorchFilter;
import com.mtnfog.phileas.metrics.PhileasMetricsService;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.exceptions.InvalidFilterProfile;
import com.mtnfog.phileas.model.filter.dynamic.DynamicFilter;
import com.mtnfog.phileas.model.filter.rules.RulesFilter;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.responses.FilterResponse;
import com.mtnfog.phileas.model.services.*;
import com.mtnfog.phileas.services.anonymization.*;
import com.mtnfog.phileas.services.filters.custom.PhoneNumberRulesFilter;
import com.mtnfog.phileas.services.filters.regex.*;
import com.mtnfog.phileas.services.postfilters.PartOfSpeechFalsePositiveFilter;
import com.mtnfog.phileas.store.MongoDBStore;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class PhileasFilterService implements FilterService, Serializable {

	private static final long serialVersionUID = 6998388861197152049L;

	private static final Logger LOGGER = LogManager.getLogger(PhileasFilterService.class);

    private List<RulesFilter> rulesFilters;
    private List<DynamicFilter> dynamicFilters;
    private MetricsService metricsService;
    private Map<String, DescriptiveStatistics> stats;
    private Store store;
    private List<PostFilter> postFilters;
    private Map<String, FilterProfile> filterProfiles;
    private List<FilterProfileService> filterProfileServices;
    
    public PhileasFilterService(Properties applicationProperties, List<FilterProfileService> filterProfileServices, AnonymizationCacheService anonymizationCacheService) throws IOException {

        this.filterProfileServices = filterProfileServices;
        this.rulesFilters = new LinkedList<>();
        this.dynamicFilters = new LinkedList<>();
        this.stats = new HashMap<>();
        this.postFilters = new LinkedList<>();
        this.filterProfiles = new HashMap<>();

        // Configure metrics.
        this.metricsService = new PhileasMetricsService(applicationProperties);

        // Configure store.
        final boolean storeEnabled = StringUtils.equalsIgnoreCase(applicationProperties.getProperty("store.enabled", "false"), "true");
        if(storeEnabled) {
            final String storeUri = applicationProperties.getProperty("store.mongodb.uri", "mongodb://localhost:27017/philter");
            this.store = new MongoDBStore(storeUri);
        }

        // Path to the indexes directory.
        final String indexDirectory = applicationProperties.getProperty("indexes.directory", System.getProperty("user.dir") + "/indexes/");

        // Endpoint of the philter-ner API.
        final String philterNerEndpoint = applicationProperties.getProperty("ner.endpoint", "http://localhost:18080/");

        // Load the filter profiles into memory.
        for(FilterProfileService filterProfileService : filterProfileServices) {
            filterProfiles.putAll(filterProfileService.getAll());
        }

        // Regex filters.
        rulesFilters.add(new AgeFilter(new AgeAnonymizationService(anonymizationCacheService)));
        rulesFilters.add(new CreditCardFilter(new CreditCardAnonymizationService(anonymizationCacheService)));
        rulesFilters.add(new DateFilter(new DateAnonymizationService(anonymizationCacheService)));
        rulesFilters.add(new EmailAddressFilter(new EmailAddressAnonymizationService(anonymizationCacheService)));
        rulesFilters.add(new IpAddressFilter(new IpAddressAnonymizationService(anonymizationCacheService)));
        rulesFilters.add(new PhoneNumberExtensionFilter(new AlphanumericAnonymizationService(anonymizationCacheService)));
        rulesFilters.add(new PhoneNumberRulesFilter(new AlphanumericAnonymizationService(anonymizationCacheService)));
        rulesFilters.add(new SsnFilter(new AlphanumericAnonymizationService(anonymizationCacheService)));
        rulesFilters.add(new StateAbbreviationFilter(new StateAbbreviationAnonymizationService(anonymizationCacheService)));
        rulesFilters.add(new UrlFilter(new UrlAnonymizationService(anonymizationCacheService)));
        rulesFilters.add(new VinFilter(new VinAnonymizationService(anonymizationCacheService)));
        rulesFilters.add(new ZipCodeFilter(new ZipCodeAnonymizationService(anonymizationCacheService)));

        // Lucene dictionary filters.
        rulesFilters.add(new LuceneDictionaryFilter(FilterType.LOCATION_CITY, indexDirectory + "cities", LuceneDictionaryFilter.CITIES_DISTANCES, new CityAnonymizationService(anonymizationCacheService)));
        rulesFilters.add(new LuceneDictionaryFilter(FilterType.LOCATION_STATE, indexDirectory + "states", LuceneDictionaryFilter.CITIES_DISTANCES, new StateAnonymizationService(anonymizationCacheService)));
        rulesFilters.add(new LuceneDictionaryFilter(FilterType.LOCATION_COUNTY, indexDirectory + "counties", LuceneDictionaryFilter.COUNTIES_DISTANCES, new CountyAnonymizationService(anonymizationCacheService)));
        rulesFilters.add(new LuceneDictionaryFilter(FilterType.HOSPITAL, indexDirectory + "hospitals", LuceneDictionaryFilter.HOSPITALS_DISTANCES, new HospitalAnonymizationService(anonymizationCacheService)));
        rulesFilters.add(new LuceneDictionaryFilter(FilterType.HOSPITAL_ABBREVIATION, indexDirectory + "hospital-abbreviations", LuceneDictionaryFilter.HOSPITAL_ABBREVIATIONS_DISTANCES, new HospitalAbbreviationAnonymizationService(anonymizationCacheService)));
        rulesFilters.add(new LuceneDictionaryFilter(FilterType.FIRST_NAME, indexDirectory + "names", LuceneDictionaryFilter.SURNAME_DISTANCES, new PersonsAnonymizationService(anonymizationCacheService)));
        rulesFilters.add(new LuceneDictionaryFilter(FilterType.SURNAME, indexDirectory + "surnames", LuceneDictionaryFilter.SURNAME_DISTANCES, new SurnameAnonymizationService(anonymizationCacheService)));

        // Enable ID filter last since it is a pretty generic pattern that might also match SSN, et. al.
        rulesFilters.add(new IdentifierFilter(new AlphanumericAnonymizationService(anonymizationCacheService)));

        // PyTorch filters.
        dynamicFilters.add(new PyTorchFilter(philterNerEndpoint, FilterType.NER_ENTITY, "PER", stats, metricsService, new PersonsAnonymizationService(anonymizationCacheService)));
        dynamicFilters.add(new PyTorchFilter(philterNerEndpoint, FilterType.NER_ENTITY, "LOC", stats, metricsService, new LocationsAnonymizationService(anonymizationCacheService)));

        // Configure post filters.
        final boolean posTagPostFilterEnabled = StringUtils.equalsIgnoreCase(applicationProperties.getProperty("post.filter.pos.enabled", "true"), "true");
        if(posTagPostFilterEnabled) {
            final InputStream is = PhileasFilterService.class.getClassLoader().getResourceAsStream("en-pos-perceptron.bin");
            postFilters.add(new PartOfSpeechFalsePositiveFilter(is));
        }

    }

    @Override
    public List<Span> replacements(String documentId) {

        return store.getByDocumentId(documentId);

    }

    @Override
    public FilterResponse filter(String filterProfileName, String context, String input) throws InvalidFilterProfile, IOException {

        if(!filterProfiles.containsKey(filterProfileName)) {
            throw new InvalidFilterProfile("The filter profile does not exist.");
        }

        // Get the filter profile that we will be using.
        final FilterProfile filterProfile = filterProfiles.get(filterProfileName);

        // The list that will contain the spans containing PHI/PII.
        List<Span> spans = new LinkedList<>();

        // Generate a random document ID.
        // TODO: Move the ID generator so it is not specific to Mongo.
        final String documentId = MongoDBStore.generateId();

        // Execute the rules filters.
        for(RulesFilter rulesFilter : rulesFilters) {

            if(filterProfile.isFilterEnabled(rulesFilter.getFilterType())) {
                spans.addAll(rulesFilter.filter(filterProfile, context, documentId, input));
            }

        }

        // Execute the dynamic filters.
        for(DynamicFilter dynamicFilter : dynamicFilters) {

            if(filterProfile.isFilterEnabled(dynamicFilter.getFilterType())) {
                spans.addAll(dynamicFilter.filter(filterProfile, context, documentId, input));
            }

        }

        // Drop overlapping spans.
        spans = Span.dropOverlappingSpans(spans);

        // Sort the spans based on the confidence.
        spans.sort(Comparator.comparing(Span::getConfidence));

        // Perform post-filtering for false positives.
        for(PostFilter postFilter : postFilters) {
            spans = postFilter.filter(input, spans);
        }

        // The spans that will be persisted. Has to be a deep copy because the shift
        // below will change the indexes.
        final List<Span> appliedSpans = spans.stream().map(d -> d.copy()).collect(toList());

        // Used to manipulate the text.
        final StringBuffer buffer = new StringBuffer(input);

        // Go character by character through the input.
        for(int i = 0; i < input.length(); i++) {

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

        return new FilterResponse(buffer.toString(), context, documentId);

    }

}
