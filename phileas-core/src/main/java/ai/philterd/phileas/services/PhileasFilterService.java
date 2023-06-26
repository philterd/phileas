/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
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

import ai.philterd.phileas.services.filters.ai.opennlp.PersonsV2Filter;
import ai.philterd.phileas.services.filters.ai.opennlp.PersonsV3Filter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ai.philterd.phileas.configuration.PhileasConfiguration;
import ai.philterd.phileas.metrics.PhileasMetricsService;
import ai.philterd.phileas.model.domain.Domain;
import ai.philterd.phileas.model.domain.HealthDomain;
import ai.philterd.phileas.model.domain.LegalDomain;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.enums.MimeType;
import ai.philterd.phileas.model.enums.SensitivityLevel;
import ai.philterd.phileas.model.filter.Filter;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.rules.dictionary.BloomFilterDictionaryFilter;
import ai.philterd.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import ai.philterd.phileas.model.objects.*;
import ai.philterd.phileas.model.profile.FilterProfile;
import ai.philterd.phileas.model.profile.Ignored;
import ai.philterd.phileas.model.profile.filters.CustomDictionary;
import ai.philterd.phileas.model.profile.filters.Identifier;
import ai.philterd.phileas.model.profile.filters.Section;
import ai.philterd.phileas.model.profile.graphical.BoundingBox;
import ai.philterd.phileas.model.responses.BinaryDocumentFilterResponse;
import ai.philterd.phileas.model.responses.FilterResponse;
import ai.philterd.phileas.model.serializers.PlaceholderDeserializer;
import ai.philterd.phileas.model.services.*;
import ai.philterd.phileas.processors.unstructured.UnstructuredDocumentProcessor;
import ai.philterd.phileas.services.alerts.AlertServiceFactory;
import ai.philterd.phileas.services.analyzers.DocumentAnalyzer;
import ai.philterd.phileas.services.anonymization.*;
import ai.philterd.phileas.services.anonymization.cache.AnonymizationCacheServiceFactory;
import ai.philterd.phileas.services.disambiguation.VectorBasedSpanDisambiguationService;
import ai.philterd.phileas.services.filters.ai.python.PersonsV1Filter;
import ai.philterd.phileas.services.filters.custom.PhoneNumberRulesFilter;
import ai.philterd.phileas.services.filters.regex.*;
import ai.philterd.phileas.services.postfilters.*;
import ai.philterd.phileas.services.profiles.LocalFilterProfileService;
import ai.philterd.phileas.services.profiles.S3FilterProfileService;
import ai.philterd.phileas.services.profiles.utils.FilterProfileUtils;
import ai.philterd.phileas.services.split.SplitFactory;
import ai.philterd.phileas.services.validators.DateSpanValidator;
import ai.philterd.services.pdf.PdfRedacter;
import ai.philterd.services.pdf.PdfTextExtractor;
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
import java.util.concurrent.ConcurrentHashMap;

public class PhileasFilterService implements FilterService {

	private static final Logger LOGGER = LogManager.getLogger(PhileasFilterService.class);

	private final PhileasConfiguration phileasConfiguration;

    private final FilterProfileService filterProfileService;
    private final FilterProfileUtils filterProfileUtils;
    private final MetricsService metricsService;

    private final Map<String, DescriptiveStatistics> stats;
    private final Gson gson;

    private final AnonymizationCacheService anonymizationCacheService;
    private final AlertService alertService;
    private final SpanDisambiguationService spanDisambiguationService;
    private final String indexDirectory;
    private final double bloomFilterFpp;

    private final DocumentProcessor unstructuredDocumentProcessor;
    private final DocumentAnalyzer documentAnalyzer;

    private final Map<String, Map<FilterType, Filter>> filterCache;

    // PHL-223: Face recognition
    //private final ImageProcessor imageProcessor;

    private final int windowSize;

    public PhileasFilterService(PhileasConfiguration phileasConfiguration) throws IOException {

        LOGGER.info("Initializing Phileas engine.");

        this.phileasConfiguration = phileasConfiguration;
        this.filterCache = new ConcurrentHashMap<>();

        // Configure the deserialization.
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(String.class, new PlaceholderDeserializer());
        gson = gsonBuilder.create();

        // Configure metrics.
        this.metricsService = new PhileasMetricsService(phileasConfiguration);

        // Set the filter profile services.
        this.filterProfileService = buildFilterProfileService(phileasConfiguration);
        this.filterProfileUtils = new FilterProfileUtils(filterProfileService, gson);

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
        this.unstructuredDocumentProcessor = new UnstructuredDocumentProcessor(metricsService, spanDisambiguationService);

        // Create a new document analyzer.
        this.documentAnalyzer = new DocumentAnalyzer();

        // Get the window size.
        this.windowSize = phileasConfiguration.spanWindowSize();
        LOGGER.info("Using window size {}", this.windowSize);

        this.indexDirectory = phileasConfiguration.indexesDirectory();
        LOGGER.info("Using indexes directory {}", this.indexDirectory);

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
    public FilterResponse filter(List<String> filterProfileNames, String context, String documentId, String input, MimeType mimeType) throws Exception {

        // Get the filter profile.
        final FilterProfile filterProfile = filterProfileUtils.getCombinedFilterProfiles(filterProfileNames);

        // Load default values based on the domain.
        if(StringUtils.equalsIgnoreCase(Domain.DOMAIN_LEGAL, filterProfile.getDomain())) {

            // PHL-209: Implement legal domain.
            filterProfile.getIgnored().add(LegalDomain.getInstance().getIgnored());

            // TODO: Add filters.

        } else if(StringUtils.equalsIgnoreCase(Domain.DOMAIN_HEALTH, filterProfile.getDomain())) {

            // PHL-210: Implement health domain.
            filterProfile.getIgnored().add(HealthDomain.getInstance().getIgnored());

            // TODO: Add filters.

        }

        // Analyze the document.
        final DocumentAnalysis documentAnalysis;
        if(filterProfile.getConfig().getAnalysis().isEnabled()) {
            documentAnalysis = documentAnalyzer.analyze(input);
        } else {
            documentAnalysis = new DocumentAnalysis();
        }

        final List<Filter> filters = getFiltersForFilterProfile(filterProfile, documentAnalysis);
        final List<PostFilter> postFilters = getPostFiltersForFilterProfile(filterProfile);

        // See if we need to generate a document ID.
        if(StringUtils.isEmpty(documentId)) {

            // PHL-58: Use a hash function to generate the document ID.
            documentId = DigestUtils.md5Hex(UUID.randomUUID().toString() + "-" + context + "-" + filterProfile.getName() + "-" + input);
            LOGGER.debug("Generated document ID {}", documentId);

        }

        final FilterResponse filterResponse;

        if(mimeType == MimeType.TEXT_PLAIN) {

            // PHL-145: Do we need to split the input text due to its size?
            if (filterProfile.getConfig().getSplitting().isEnabled() && input.length() >= filterProfile.getConfig().getSplitting().getThreshold()) {

                // Get the splitter to use from the filter profile.
                final SplitService splitService = SplitFactory.getSplitService(filterProfile.getConfig().getSplitting().getMethod());

                // Holds all of the filter responses that will ultimately be combined into a single response.
                final List<FilterResponse> filterResponses = new LinkedList<>();

                // Split the string.
                final List<String> splits = splitService.split(input);

                // Process each split.
                for (int i = 0; i < splits.size(); i++) {
                    filterResponses.add(unstructuredDocumentProcessor.process(filterProfile, filters, postFilters, context, documentId, i, splits.get(i)));
                }

                // Combine the results into a single filterResponse object.
                filterResponse = FilterResponse.combine(filterResponses, context, documentId, splitService.getSeparator());

            } else {

                // Do not split. Process the entire string at once.
                filterResponse = unstructuredDocumentProcessor.process(filterProfile, filters, postFilters, context, documentId, 0, input);

            }

        } else {
            // Should never happen but just in case.
            throw new Exception("Unknown mime type.");
        }

        return filterResponse;

    }

    @Override
    public BinaryDocumentFilterResponse filter(List<String> filterProfileNames, String context, String documentId, byte[] input, MimeType mimeType, MimeType outputMimeType) throws Exception {

        // Get the filter profile.
        final FilterProfile filterProfile = filterProfileUtils.getCombinedFilterProfiles(filterProfileNames);

        // See if we need to generate a document ID.
        if(StringUtils.isEmpty(documentId)) {

            // PHL-58: Use a hash function to generate the document ID.
            documentId = DigestUtils.md5Hex(UUID.randomUUID().toString() + "-" + context + "-" + filterProfile.getName() + "-" + input);
            LOGGER.debug("Generated document ID {}", documentId);

        }

        final BinaryDocumentFilterResponse binaryDocumentFilterResponse;

        if(mimeType == MimeType.APPLICATION_PDF) {

            // Get the lines of text from the PDF file.
            final PdfTextExtractor pdfTextExtractor = new PdfTextExtractor();
            final List<String> lines = pdfTextExtractor.getLines(input);

            // A list of identified spans.
            // These spans start/end are relative to the line containing the span.
            final Set<Span> spans = new LinkedHashSet<>();

            // A list of identified spans.
            // These spans start/end are relative to the whole document.
            final Set<Span> nonRelativeSpans = new LinkedHashSet<>();

            // Track the document offset.
            int offset = 0;

            // Analyze the lines to determine the type of document.
            final DocumentAnalysis documentAnalysis;
            if (filterProfile.getConfig().getAnalysis().isEnabled()) {
                documentAnalysis = documentAnalyzer.analyze(lines);
            } else {
                documentAnalysis = new DocumentAnalysis();
            }

            final List<Filter> filters = getFiltersForFilterProfile(filterProfile, documentAnalysis);
            final List<PostFilter> postFilters = getPostFiltersForFilterProfile(filterProfile);

            // TODO: The following code really only needs to be done if there is at least
            // one filter defined in the filter profile.

            // Process each line looking for sensitive information in each line.
            for (final String line : lines) {

                final int piece = 0;

                // Process the text.
                final FilterResponse filterResponse = unstructuredDocumentProcessor.process(filterProfile, filters, postFilters, context, documentId, piece, line);

                // Add all the found spans to the list of spans.
                spans.addAll(filterResponse.getExplanation().getAppliedSpans());

                for (final Span span : filterResponse.getExplanation().getAppliedSpans()) {
                    span.setCharacterStart(span.getCharacterStart() + offset);
                    span.setCharacterEnd(span.getCharacterEnd() + offset);
                    nonRelativeSpans.add(span);
                }

                offset += line.length();

            }

            // TODO: Build this from the config in the filter profile.
            final PdfRedactionOptions pdfRedactionOptions = new PdfRedactionOptions();
            pdfRedactionOptions.setDpi(150);
            pdfRedactionOptions.setScale(0.25f);
            pdfRedactionOptions.setCompressionQuality(1.0f);

            // Redact those terms in the document along with any bounding boxes identified in the filter profile.
            final List<BoundingBox> boundingBoxes = getBoundingBoxes(filterProfile, mimeType);
            final Redacter redacter = new PdfRedacter(filterProfile, spans, pdfRedactionOptions, boundingBoxes);
            final byte[] redacted = redacter.process(input, outputMimeType);

            // Create the response.
            final List<Span> spansList = new ArrayList<>(nonRelativeSpans);

            // TODO: The identified vs the applied will actually be different
            // but we are setting the same here. Fix this at some point.
            final Explanation explanation = new Explanation(spansList, spansList);
            binaryDocumentFilterResponse = new BinaryDocumentFilterResponse(redacted, context, documentId, explanation);

            // TODO: PHL-212: Persist the spans.
            // Store the spans, if enabled.
            /*if(phileasConfiguration.storeEnabled()) {
                store.insert(binaryDocumentFilterResponse.getExplanation().getAppliedSpans());
            }*/

        /*} else if(mimeType == MimeType.IMAGE_JPEG) {
            // PHL-223: Face recognition

            // TODO: Get options from the filter profile.
            final ImageFilterResponse imageFilterResponse = imageProcessor.process(input);

            // TODO: Explanation?
            final Explanation explanation = new Explanation(Collections.emptyList(), Collections.emptyList());

            binaryDocumentFilterResponse = new BinaryDocumentFilterResponse(imageFilterResponse.getImage(),
                    context, documentId, explanation);*/

        } else {
            // Should never happen but just in case.
            throw new Exception("Unknown mime type.");
        }

        return binaryDocumentFilterResponse;

    }

    /**
     * Get the bounding boxes from the filter profile for a given mime type.
     * @param filterProfile The filter profile.
     * @param mimeType The mime type.
     * @return A list of bounding boxes from the filter profile for the given mime type.
     */
    private List<BoundingBox> getBoundingBoxes(final FilterProfile filterProfile, final MimeType mimeType) {

        final List<BoundingBox> boundingBoxes = new LinkedList<>();

        for(final BoundingBox boundingBox : filterProfile.getGraphical().getBoundingBoxes()) {
            if(StringUtils.equalsIgnoreCase(boundingBox.getMimeType(), mimeType.toString())) {
                boundingBoxes.add(boundingBox);
            }
        }

        return boundingBoxes;

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

    private List<PostFilter> getPostFiltersForFilterProfile(final FilterProfile filterProfile) throws IOException {

        LOGGER.debug("Reloading filter profiles.");

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

        // Ignored patterns filter. Looks for terms matching a pattern in the scope of the whole document (and not just a particular filter).
        // No matter what filter found the span, it is subject to this ignore list.
        if(CollectionUtils.isNotEmpty(filterProfile.getIgnoredPatterns())) {
            postFilters.add(new IgnoredPatternsFilter(filterProfile.getIgnoredPatterns()));
        }

        // Add the post filters if they are enabled in the filter profile.

        if(filterProfile.getConfig().getPostFilters().isRemoveTrailingPeriods()) {
            postFilters.add(TrailingPeriodPostFilter.getInstance());
        }

        if(filterProfile.getConfig().getPostFilters().isRemoveTrailingSpaces()) {
            postFilters.add(TrailingSpacePostFilter.getInstance());
        }

        if(filterProfile.getConfig().getPostFilters().isRemoveTrailingNewLines()) {
            postFilters.add(TrailingNewLinePostFilter.getInstance());
        }

        return postFilters;

    }

    private List<Filter> getFiltersForFilterProfile(final FilterProfile filterProfile, DocumentAnalysis documentAnalysis) throws Exception {

        LOGGER.debug("Getting filters for filter profile [{}]", filterProfile.getName());

        // See if this filter is already cached.
        filterCache.putIfAbsent(filterProfile.getName(), new ConcurrentHashMap<>());
        final Map<FilterType, Filter> cache = filterCache.get(filterProfile.getName());

        final List<Filter> enabledFilters = new LinkedList<>();

        // Rules filters.

        if(filterProfile.getIdentifiers().hasFilter(FilterType.AGE) && filterProfile.getIdentifiers().getAge().isEnabled()) {

            if(cache.containsKey(FilterType.AGE)) {
                enabledFilters.add(cache.get(FilterType.AGE));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getAge().getAgeFilterStrategies())
                        .withAnonymizationService(new AgeAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getAge().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getAge().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getAge().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new AgeFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.AGE, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.BANK_ROUTING_NUMBER) && filterProfile.getIdentifiers().getBankRoutingNumber().isEnabled()) {

            if(cache.containsKey(FilterType.BANK_ROUTING_NUMBER)) {
                enabledFilters.add(cache.get(FilterType.BANK_ROUTING_NUMBER));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getBankRoutingNumber().getBankRoutingNumberFilterStrategies())
                        .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getBankRoutingNumber().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getBankRoutingNumber().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getBankRoutingNumber().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withFPE(filterProfile.getFpe())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new BankRoutingNumberFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.BANK_ROUTING_NUMBER, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.BITCOIN_ADDRESS) && filterProfile.getIdentifiers().getBitcoinAddress().isEnabled()) {

            if(cache.containsKey(FilterType.BITCOIN_ADDRESS)) {
                enabledFilters.add(cache.get(FilterType.BITCOIN_ADDRESS));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getBitcoinAddress().getBitcoinFilterStrategies())
                        .withAnonymizationService(new BitcoinAddressAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getBitcoinAddress().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getBitcoinAddress().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getBitcoinAddress().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withFPE(filterProfile.getFpe())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new BitcoinAddressFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.BITCOIN_ADDRESS, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.CREDIT_CARD) && filterProfile.getIdentifiers().getCreditCard().isEnabled()) {

            if(cache.containsKey(FilterType.CREDIT_CARD)) {
                enabledFilters.add(cache.get(FilterType.CREDIT_CARD));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getCreditCard().getCreditCardFilterStrategies())
                        .withAnonymizationService(new CreditCardAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getCreditCard().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getCreditCard().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getCreditCard().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withFPE(filterProfile.getFpe())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final boolean onlyValidCreditCardNumbers = filterProfile.getIdentifiers().getCreditCard().isOnlyValidCreditCardNumbers();

                final Filter filter = new CreditCardFilter(filterConfiguration, onlyValidCreditCardNumbers);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.CREDIT_CARD, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.CURRENCY) && filterProfile.getIdentifiers().getCurrency().isEnabled()) {

            if(cache.containsKey(FilterType.CURRENCY)) {
                enabledFilters.add(cache.get(FilterType.CURRENCY));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getCurrency().getCurrencyFilterStrategies())
                        .withAnonymizationService(new CurrencyAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getCurrency().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getCurrency().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getCurrency().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new CurrencyFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.CURRENCY, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.DATE) && filterProfile.getIdentifiers().getDate().isEnabled()) {

            if(cache.containsKey(FilterType.DATE)) {
                enabledFilters.add(cache.get(FilterType.DATE));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getDate().getDateFilterStrategies())
                        .withAnonymizationService(new DateAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getDate().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getDate().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getDate().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final boolean onlyValidDates = filterProfile.getIdentifiers().getDate().isOnlyValidDates();
                final SpanValidator dateSpanValidator = DateSpanValidator.getInstance();

                final Filter filter = new DateFilter(filterConfiguration, onlyValidDates, dateSpanValidator);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.DATE, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.DRIVERS_LICENSE_NUMBER) && filterProfile.getIdentifiers().getDriversLicense().isEnabled()) {

            if(cache.containsKey(FilterType.DRIVERS_LICENSE_NUMBER)) {
                enabledFilters.add(cache.get(FilterType.DRIVERS_LICENSE_NUMBER));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getDriversLicense().getDriversLicenseFilterStrategies())
                        .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getDriversLicense().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getDriversLicense().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getDriversLicense().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withFPE(filterProfile.getFpe())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new DriversLicenseFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.DRIVERS_LICENSE_NUMBER, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.EMAIL_ADDRESS) && filterProfile.getIdentifiers().getEmailAddress().isEnabled()) {

            if(cache.containsKey(FilterType.EMAIL_ADDRESS)) {
                enabledFilters.add(cache.get(FilterType.EMAIL_ADDRESS));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getEmailAddress().getEmailAddressFilterStrategies())
                        .withAnonymizationService(new EmailAddressAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getEmailAddress().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getEmailAddress().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getEmailAddress().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new EmailAddressFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.EMAIL_ADDRESS, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.IBAN_CODE) && filterProfile.getIdentifiers().getIbanCode().isEnabled()) {

            if(cache.containsKey(FilterType.IBAN_CODE)) {
                enabledFilters.add(cache.get(FilterType.IBAN_CODE));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getIbanCode().getIbanCodeFilterStrategies())
                        .withAnonymizationService(new IbanCodeAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getIbanCode().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getIbanCode().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getIbanCode().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withFPE(filterProfile.getFpe())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final boolean onlyValidIBANCodes = filterProfile.getIdentifiers().getIbanCode().isOnlyValidIBANCodes();
                final boolean allowSpaces = filterProfile.getIdentifiers().getIbanCode().isAllowSpaces();

                final Filter filter = new IbanCodeFilter(filterConfiguration, onlyValidIBANCodes, allowSpaces);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.IBAN_CODE, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.IP_ADDRESS) && filterProfile.getIdentifiers().getIpAddress().isEnabled()) {

            if(cache.containsKey(FilterType.IP_ADDRESS)) {
                enabledFilters.add(cache.get(FilterType.IP_ADDRESS));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getIpAddress().getIpAddressFilterStrategies())
                        .withAnonymizationService(new IpAddressAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getIpAddress().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getIpAddress().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getIpAddress().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new IpAddressFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.IP_ADDRESS, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.MAC_ADDRESS) && filterProfile.getIdentifiers().getMacAddress().isEnabled()) {

            if(cache.containsKey(FilterType.MAC_ADDRESS)) {
                enabledFilters.add(cache.get(FilterType.MAC_ADDRESS));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getMacAddress().getMacAddressFilterStrategies())
                        .withAnonymizationService(new MacAddressAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getMacAddress().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getMacAddress().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getMacAddress().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new MacAddressFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.MAC_ADDRESS, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.PASSPORT_NUMBER) && filterProfile.getIdentifiers().getPassportNumber().isEnabled()) {

            if(cache.containsKey(FilterType.PASSPORT_NUMBER)) {
                enabledFilters.add(cache.get(FilterType.PASSPORT_NUMBER));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getPassportNumber().getPassportNumberFilterStrategies())
                        .withAnonymizationService(new PassportNumberAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getPassportNumber().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getPassportNumber().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getPassportNumber().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withFPE(filterProfile.getFpe())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new PassportNumberFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.PASSPORT_NUMBER, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.PHONE_NUMBER_EXTENSION) && filterProfile.getIdentifiers().getPhoneNumberExtension().isEnabled()) {

            if(cache.containsKey(FilterType.PHONE_NUMBER_EXTENSION)) {
                enabledFilters.add(cache.get(FilterType.PHONE_NUMBER_EXTENSION));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getPhoneNumberExtension().getPhoneNumberExtensionFilterStrategies())
                        .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getPhoneNumberExtension().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getPhoneNumberExtension().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getPhoneNumberExtension().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new PhoneNumberExtensionFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.PHONE_NUMBER_EXTENSION, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.PHONE_NUMBER) && filterProfile.getIdentifiers().getPhoneNumber().isEnabled()) {

            if(cache.containsKey(FilterType.PHONE_NUMBER)) {
                enabledFilters.add(cache.get(FilterType.PHONE_NUMBER));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getPhoneNumber().getPhoneNumberFilterStrategies())
                        .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getPhoneNumber().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getPhoneNumber().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getPhoneNumber().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new PhoneNumberRulesFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.PHONE_NUMBER, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.PHYSICIAN_NAME) && filterProfile.getIdentifiers().getPhysicianName().isEnabled()) {

            if(cache.containsKey(FilterType.PHYSICIAN_NAME)) {
                enabledFilters.add(cache.get(FilterType.PHYSICIAN_NAME));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getPhysicianName().getPhysicianNameFilterStrategies())
                        .withAnonymizationService(new PersonsAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getPhysicianName().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getPhysicianName().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getPhysicianName().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new PhysicianNameFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.PHYSICIAN_NAME, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.SECTION)) {

            final List<Section> sections = filterProfile.getIdentifiers().getSections();

            for(final Section section : sections) {

                if(section.isEnabled()) {

                    final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                            .withStrategies(section.getSectionFilterStrategies())
                            .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                            .withAlertService(alertService)
                            .withIgnored(section.getIgnored())
                            .withIgnoredFiles(section.getIgnoredFiles())
                            .withIgnoredPatterns(section.getIgnoredPatterns())
                            .withCrypto(filterProfile.getCrypto())
                            .withWindowSize(windowSize)
                            .withDocumentAnalysis(documentAnalysis)
                            .build();

                    final String startPattern = section.getStartPattern();
                    final String endPattern = section.getEndPattern();

                    enabledFilters.add(new SectionFilter(filterConfiguration, startPattern, endPattern));

                }

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.SSN) && filterProfile.getIdentifiers().getSsn().isEnabled()) {

            if(cache.containsKey(FilterType.SSN)) {
                enabledFilters.add(cache.get(FilterType.SSN));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getSsn().getSsnFilterStrategies())
                        .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getSsn().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getSsn().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getSsn().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withFPE(filterProfile.getFpe())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new SsnFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.SSN, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.STATE_ABBREVIATION) && filterProfile.getIdentifiers().getStateAbbreviation().isEnabled()) {

            if(cache.containsKey(FilterType.STATE_ABBREVIATION)) {
                enabledFilters.add(cache.get(FilterType.STATE_ABBREVIATION));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getStateAbbreviation().getStateAbbreviationsFilterStrategies())
                        .withAnonymizationService(new StateAbbreviationAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getStateAbbreviation().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getStateAbbreviation().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getStateAbbreviation().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new StateAbbreviationFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.STATE_ABBREVIATION, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.STREET_ADDRESS) && filterProfile.getIdentifiers().getStreetAddress().isEnabled()) {

            if(cache.containsKey(FilterType.STREET_ADDRESS)) {
                enabledFilters.add(cache.get(FilterType.STREET_ADDRESS));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getStreetAddress().getStreetAddressFilterStrategies())
                        .withAnonymizationService(new StreetAddressAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getStreetAddress().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getStreetAddress().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getStreetAddress().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new StreetAddressFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.STREET_ADDRESS, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.TRACKING_NUMBER) && filterProfile.getIdentifiers().getTrackingNumber().isEnabled()) {

            if(cache.containsKey(FilterType.TRACKING_NUMBER)) {
                enabledFilters.add(cache.get(FilterType.TRACKING_NUMBER));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getTrackingNumber().getTrackingNumberFilterStrategies())
                        .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getTrackingNumber().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getTrackingNumber().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getTrackingNumber().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withFPE(filterProfile.getFpe())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final boolean ups = filterProfile.getIdentifiers().getTrackingNumber().isUps();
                final boolean fedex = filterProfile.getIdentifiers().getTrackingNumber().isFedex();
                final boolean usps = filterProfile.getIdentifiers().getTrackingNumber().isUsps();

                final Filter filter = new TrackingNumberFilter(filterConfiguration, ups, fedex, usps);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.TRACKING_NUMBER, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.URL) && filterProfile.getIdentifiers().getUrl().isEnabled()) {

            if(cache.containsKey(FilterType.URL)) {
                enabledFilters.add(cache.get(FilterType.URL));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getUrl().getUrlFilterStrategies())
                        .withAnonymizationService(new UrlAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getUrl().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getUrl().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getUrl().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final boolean requireHttpWwwPrefix = filterProfile.getIdentifiers().getUrl().isRequireHttpWwwPrefix();

                final Filter filter = new UrlFilter(filterConfiguration, requireHttpWwwPrefix);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.URL, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.VIN) && filterProfile.getIdentifiers().getVin().isEnabled()) {

            if(cache.containsKey(FilterType.VIN)) {
                enabledFilters.add(cache.get(FilterType.VIN));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getVin().getVinFilterStrategies())
                        .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getVin().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getVin().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getVin().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withFPE(filterProfile.getFpe())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new VinFilter(filterConfiguration);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.VIN, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.ZIP_CODE) && filterProfile.getIdentifiers().getZipCode().isEnabled()) {

            if(cache.containsKey(FilterType.ZIP_CODE)) {
                enabledFilters.add(cache.get(FilterType.ZIP_CODE));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getZipCode().getZipCodeFilterStrategies())
                        .withAnonymizationService(new ZipCodeAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getZipCode().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getZipCode().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getZipCode().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final boolean requireDelimiter = filterProfile.getIdentifiers().getZipCode().isRequireDelimiter();

                final Filter filter = new ZipCodeFilter(filterConfiguration, requireDelimiter);
                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.ZIP_CODE, filter);

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
            for(final CustomDictionary customDictionary : filterProfile.getIdentifiers().getCustomDictionaries()) {

                // TODO: Add caching of the filter profile (see Age for example)

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
                                .withIgnored(filterProfile.getIdentifiers().getZipCode().getIgnored())
                                .withIgnoredFiles(filterProfile.getIdentifiers().getZipCode().getIgnoredFiles())
                                .withIgnoredPatterns(filterProfile.getIdentifiers().getZipCode().getIgnoredPatterns())
                                .withCrypto(filterProfile.getCrypto())
                                .withWindowSize(windowSize)
                                .withDocumentAnalysis(documentAnalysis)
                                .build();

                        final SensitivityLevel sensitivityLevel = SensitivityLevel.fromName(customDictionary.getSensitivity());
                        final String classification = customDictionary.getClassification();
                        final boolean capitalized = false;

                        enabledFilters.add(new LuceneDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, sensitivityLevel,
                                terms, capitalized, classification, index));

                    } else {

                        LOGGER.info("Custom dictionary contains {} terms.", terms.size());

                        // Only enable the filter if there is at least one term.
                        // TODO: Should a bloom filter be used for small numbers of terms?
                        if(terms.size() > 0) {

                            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                                    .withStrategies(customDictionary.getCustomDictionaryFilterStrategies())
                                    .withAnonymizationService(new ZipCodeAnonymizationService(anonymizationCacheService))
                                    .withAlertService(alertService)
                                    .withIgnored(customDictionary.getIgnored())
                                    .withIgnoredFiles(customDictionary.getIgnoredFiles())
                                    .withIgnoredPatterns(customDictionary.getIgnoredPatterns())
                                    .withCrypto(filterProfile.getCrypto())
                                    .withWindowSize(windowSize)
                                    .withDocumentAnalysis(documentAnalysis)
                                    .build();

                            final String classification = customDictionary.getClassification();

                            enabledFilters.add(new BloomFilterDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration,
                                    terms, classification, bloomFilterFpp));

                        }

                    }

                    index++;

                }

            }

        } else {

            LOGGER.debug("Filter profile {} has no custom dictionaries.", filterProfile.getName());

        }

        // Lucene dictionary filters.

        if(filterProfile.getIdentifiers().hasFilter(FilterType.LOCATION_CITY) && filterProfile.getIdentifiers().getCity().isEnabled()) {

            // TODO: Add caching of the filter profile (see Age for example)

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(filterProfile.getIdentifiers().getCity().getCityFilterStrategies())
                    .withAnonymizationService(new CityAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(filterProfile.getIdentifiers().getCity().getIgnored())
                    .withIgnoredFiles(filterProfile.getIdentifiers().getCity().getIgnoredFiles())
                    .withIgnoredPatterns(filterProfile.getIdentifiers().getCity().getIgnoredPatterns())
                    .withCrypto(filterProfile.getCrypto())
                    .withWindowSize(windowSize)
                    .withDocumentAnalysis(documentAnalysis)
                    .build();

            final SensitivityLevel sensitivityLevel = filterProfile.getIdentifiers().getCity().getSensitivityLevel();
            final boolean capitalized = filterProfile.getIdentifiers().getCity().isCapitalized();

            enabledFilters.add(new LuceneDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, indexDirectory + "cities", sensitivityLevel, capitalized));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.LOCATION_COUNTY) && filterProfile.getIdentifiers().getCounty().isEnabled()) {

            // TODO: Add caching of the filter profile (see Age for example)

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(filterProfile.getIdentifiers().getCounty().getCountyFilterStrategies())
                    .withAnonymizationService(new CountyAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(filterProfile.getIdentifiers().getCounty().getIgnored())
                    .withIgnoredFiles(filterProfile.getIdentifiers().getCounty().getIgnoredFiles())
                    .withIgnoredPatterns(filterProfile.getIdentifiers().getCounty().getIgnoredPatterns())
                    .withCrypto(filterProfile.getCrypto())
                    .withWindowSize(windowSize)
                    .withDocumentAnalysis(documentAnalysis)
                    .build();

            final SensitivityLevel sensitivityLevel = filterProfile.getIdentifiers().getCounty().getSensitivityLevel();
            final boolean capitalized = filterProfile.getIdentifiers().getCounty().isCapitalized();

            enabledFilters.add(new LuceneDictionaryFilter(FilterType.LOCATION_COUNTY, filterConfiguration, indexDirectory + "counties", sensitivityLevel, capitalized));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.LOCATION_STATE) && filterProfile.getIdentifiers().getState().isEnabled()) {

            // TODO: Add caching of the filter profile (see Age for example)

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(filterProfile.getIdentifiers().getState().getStateFilterStrategies())
                    .withAnonymizationService(new StateAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(filterProfile.getIdentifiers().getState().getIgnored())
                    .withIgnoredFiles(filterProfile.getIdentifiers().getState().getIgnoredFiles())
                    .withIgnoredPatterns(filterProfile.getIdentifiers().getState().getIgnoredPatterns())
                    .withCrypto(filterProfile.getCrypto())
                    .withWindowSize(windowSize)
                    .withDocumentAnalysis(documentAnalysis)
                    .build();

            final SensitivityLevel sensitivityLevel = filterProfile.getIdentifiers().getState().getSensitivityLevel();
            final boolean capitalized = filterProfile.getIdentifiers().getState().isCapitalized();

            enabledFilters.add(new LuceneDictionaryFilter(FilterType.LOCATION_STATE, filterConfiguration, indexDirectory + "states", sensitivityLevel, capitalized));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.HOSPITAL) && filterProfile.getIdentifiers().getHospital().isEnabled()) {

            // TODO: Add caching of the filter profile (see Age for example)

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(filterProfile.getIdentifiers().getHospital().getHospitalFilterStrategies())
                    .withAnonymizationService(new HospitalAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(filterProfile.getIdentifiers().getHospital().getIgnored())
                    .withIgnoredFiles(filterProfile.getIdentifiers().getHospital().getIgnoredFiles())
                    .withIgnoredPatterns(filterProfile.getIdentifiers().getHospital().getIgnoredPatterns())
                    .withCrypto(filterProfile.getCrypto())
                    .withWindowSize(windowSize)
                    .withDocumentAnalysis(documentAnalysis)
                    .build();

            final SensitivityLevel sensitivityLevel = filterProfile.getIdentifiers().getHospital().getSensitivityLevel();
            final boolean capitalized = filterProfile.getIdentifiers().getHospital().isCapitalized();

            enabledFilters.add(new LuceneDictionaryFilter(FilterType.HOSPITAL, filterConfiguration, indexDirectory + "hospitals", sensitivityLevel, capitalized));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.HOSPITAL_ABBREVIATION) && filterProfile.getIdentifiers().getHospitalAbbreviation().isEnabled()) {

            // TODO: Add caching of the filter profile (see Age for example)

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(filterProfile.getIdentifiers().getHospitalAbbreviation().getHospitalAbbreviationFilterStrategies())
                    .withAnonymizationService(new HospitalAbbreviationAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(filterProfile.getIdentifiers().getHospitalAbbreviation().getIgnored())
                    .withIgnoredFiles(filterProfile.getIdentifiers().getHospitalAbbreviation().getIgnoredFiles())
                    .withIgnoredPatterns(filterProfile.getIdentifiers().getHospitalAbbreviation().getIgnoredPatterns())
                    .withCrypto(filterProfile.getCrypto())
                    .withWindowSize(windowSize)
                    .withDocumentAnalysis(documentAnalysis)
                    .build();

            final SensitivityLevel sensitivityLevel = filterProfile.getIdentifiers().getHospitalAbbreviation().getSensitivityLevel();
            final boolean capitalized = filterProfile.getIdentifiers().getHospitalAbbreviation().isCapitalized();

            enabledFilters.add(new LuceneDictionaryFilter(FilterType.HOSPITAL_ABBREVIATION, filterConfiguration, indexDirectory + "hospital-abbreviations", sensitivityLevel, capitalized));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.FIRST_NAME) && filterProfile.getIdentifiers().getFirstName().isEnabled()) {

            // TODO: Add caching of the filter profile (see Age for example)

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(filterProfile.getIdentifiers().getFirstName().getFirstNameFilterStrategies())
                    .withAnonymizationService(new PersonsAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(filterProfile.getIdentifiers().getFirstName().getIgnored())
                    .withIgnoredFiles(filterProfile.getIdentifiers().getFirstName().getIgnoredFiles())
                    .withIgnoredPatterns(filterProfile.getIdentifiers().getFirstName().getIgnoredPatterns())
                    .withCrypto(filterProfile.getCrypto())
                    .withWindowSize(windowSize)
                    .withDocumentAnalysis(documentAnalysis)
                    .build();

            final SensitivityLevel sensitivityLevel = filterProfile.getIdentifiers().getFirstName().getSensitivityLevel();
            final boolean capitalized = filterProfile.getIdentifiers().getFirstName().isCapitalized();

            enabledFilters.add(new LuceneDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, indexDirectory + "names", sensitivityLevel, capitalized));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.SURNAME) && filterProfile.getIdentifiers().getSurname().isEnabled()) {

            // TODO: Add caching of the filter profile (see Age for example)

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(filterProfile.getIdentifiers().getSurname().getSurnameFilterStrategies())
                    .withAnonymizationService(new SurnameAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(filterProfile.getIdentifiers().getSurname().getIgnored())
                    .withIgnoredFiles(filterProfile.getIdentifiers().getSurname().getIgnoredFiles())
                    .withIgnoredPatterns(filterProfile.getIdentifiers().getSurname().getIgnoredPatterns())
                    .withCrypto(filterProfile.getCrypto())
                    .withWindowSize(windowSize)
                    .withDocumentAnalysis(documentAnalysis)
                    .build();

            final SensitivityLevel sensitivityLevel = filterProfile.getIdentifiers().getSurname().getSensitivityLevel();
            final boolean capitalized = filterProfile.getIdentifiers().getSurname().isCapitalized();

            enabledFilters.add(new LuceneDictionaryFilter(FilterType.SURNAME, filterConfiguration, indexDirectory + "surnames", sensitivityLevel, capitalized));

        }

        // Enable ID filter last since it is a pretty generic pattern that might also match SSN, et. al.

        if(filterProfile.getIdentifiers().hasFilter(FilterType.IDENTIFIER)) {

            final List<Identifier> identifiers = filterProfile.getIdentifiers().getIdentifiers();

            for(final Identifier identifier : identifiers) {

                // TODO: Add caching of the filter profile (see Age for example)

                if(identifier.isEnabled()) {

                    final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                            .withStrategies(identifier.getIdentifierFilterStrategies())
                            .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                            .withAlertService(alertService)
                            .withIgnored(identifier.getIgnored())
                            .withIgnoredFiles(identifier.getIgnoredFiles())
                            .withIgnoredPatterns(identifier.getIgnoredPatterns())
                            .withCrypto(filterProfile.getCrypto())
                            .withWindowSize(windowSize)
                            .withDocumentAnalysis(documentAnalysis)
                            .build();

                    final String classification = identifier.getClassification();
                    final String pattern = identifier.getPattern();
                    final boolean caseSensitive = identifier.isCaseSensitive();

                    enabledFilters.add(new IdentifierFilter(filterConfiguration, classification, pattern, caseSensitive));

                }

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.PERSON) && filterProfile.getIdentifiers().getPerson().isEnabled()) {

            if(cache.containsKey(FilterType.PERSON)) {
                enabledFilters.add(cache.get(FilterType.PERSON));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getPerson().getNerStrategies())
                        .withAnonymizationService(new PersonsAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getPerson().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getPerson().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getPerson().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new PersonsV1Filter(
                        filterConfiguration,
                        phileasConfiguration,
                        "PER",
                        stats,
                        metricsService,
                        filterProfile.getIdentifiers().getPerson().isRemovePunctuation(),
                        filterProfile.getIdentifiers().getPerson().getThresholds()
                );

                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.PERSON, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.PERSON_V2) && filterProfile.getIdentifiers().getPersonV2().isEnabled()) {

            if(cache.containsKey(FilterType.PERSON_V2)) {
                enabledFilters.add(cache.get(FilterType.PERSON_V2));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getPersonV2().getNerStrategies())
                        .withAnonymizationService(new PersonsAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getPersonV2().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getPersonV2().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getPersonV2().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new PersonsV2Filter(
                        filterConfiguration,
                        filterProfile.getIdentifiers().getPersonV2().getModel(),
                        filterProfile.getIdentifiers().getPersonV2().getVocab(),
                        stats,
                        metricsService,
                        filterProfile.getIdentifiers().getPersonV2().getThresholds());

                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.PERSON_V2, filter);

            }

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.PERSON_V3) && filterProfile.getIdentifiers().getPersonV3().isEnabled()) {

            if(cache.containsKey(FilterType.PERSON_V3)) {
                enabledFilters.add(cache.get(FilterType.PERSON_V3));
            } else {

                final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                        .withStrategies(filterProfile.getIdentifiers().getPersonV3().getNerStrategies())
                        .withAnonymizationService(new PersonsAnonymizationService(anonymizationCacheService))
                        .withAlertService(alertService)
                        .withIgnored(filterProfile.getIdentifiers().getPersonV3().getIgnored())
                        .withIgnoredFiles(filterProfile.getIdentifiers().getPersonV3().getIgnoredFiles())
                        .withIgnoredPatterns(filterProfile.getIdentifiers().getPersonV3().getIgnoredPatterns())
                        .withCrypto(filterProfile.getCrypto())
                        .withWindowSize(windowSize)
                        .withDocumentAnalysis(documentAnalysis)
                        .build();

                final Filter filter = new PersonsV3Filter(
                        filterConfiguration,
                        filterProfile.getIdentifiers().getPersonV3().getModel(),
                        stats,
                        metricsService,
                        filterProfile.getIdentifiers().getPersonV3().getThresholds());

                enabledFilters.add(filter);
                filterCache.get(filterProfile.getName()).put(FilterType.PERSON_V3, filter);

            }

        }

        return enabledFilters;

    }

}
