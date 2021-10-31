package com.mtnfog.phileas.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.metrics.PhileasMetricsService;
import com.mtnfog.phileas.model.domain.Domain;
import com.mtnfog.phileas.model.domain.HealthDomain;
import com.mtnfog.phileas.model.domain.LegalDomain;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.MimeType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.exceptions.api.PayloadTooLargeException;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.filter.FilterConfiguration;
import com.mtnfog.phileas.model.filter.rules.dictionary.BloomFilterDictionaryFilter;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import com.mtnfog.phileas.model.objects.DocumentAnalysis;
import com.mtnfog.phileas.model.objects.Explanation;
import com.mtnfog.phileas.model.objects.RedactionOptions;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.Ignored;
import com.mtnfog.phileas.model.profile.filters.CustomDictionary;
import com.mtnfog.phileas.model.profile.filters.Identifier;
import com.mtnfog.phileas.model.profile.filters.Section;
import com.mtnfog.phileas.model.profile.graphical.BoundingBox;
import com.mtnfog.phileas.model.responses.BinaryDocumentFilterResponse;
import com.mtnfog.phileas.model.responses.FilterResponse;
import com.mtnfog.phileas.model.serializers.PlaceholderDeserializer;
import com.mtnfog.phileas.model.services.*;
import com.mtnfog.phileas.processors.unstructured.UnstructuredDocumentProcessor;
import com.mtnfog.phileas.service.ai.PyTorchFilter;
import com.mtnfog.phileas.services.alerts.AlertServiceFactory;
import com.mtnfog.phileas.services.analyzers.DocumentAnalyzer;
import com.mtnfog.phileas.services.anonymization.*;
import com.mtnfog.phileas.services.anonymization.cache.AnonymizationCacheServiceFactory;
import com.mtnfog.phileas.services.disambiguation.VectorBasedSpanDisambiguationService;
import com.mtnfog.phileas.services.filters.custom.PhoneNumberRulesFilter;
import com.mtnfog.phileas.services.filters.regex.*;
import com.mtnfog.phileas.services.postfilters.*;
import com.mtnfog.phileas.services.profiles.LocalFilterProfileService;
import com.mtnfog.phileas.services.profiles.S3FilterProfileService;
import com.mtnfog.phileas.services.profiles.utils.FilterProfileUtils;
import com.mtnfog.phileas.services.split.SplitFactory;
import com.mtnfog.phileas.services.validators.DateSpanValidator;
import com.mtnfog.services.pdf.PdfRedacter;
import com.mtnfog.services.pdf.PdfTextExtractor;
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

	private final PhileasConfiguration phileasConfiguration;

    private final FilterProfileService filterProfileService;
    private final FilterProfileUtils filterProfileUtils;
    private final MetricsService metricsService;

    private final Map<String, DescriptiveStatistics> stats;
    private final Gson gson;

    private final String philterNerEndpoint;
    private final AnonymizationCacheService anonymizationCacheService;
    private final AlertService alertService;
    private final SpanDisambiguationService spanDisambiguationService;
    private final String indexDirectory;
    private final double bloomFilterFpp;

    private final DocumentProcessor unstructuredDocumentProcessor;
    private final DocumentAnalyzer documentAnalyzer;

    // PHL-223: Face recognition
    //private final ImageProcessor imageProcessor;

    private final int windowSize;

    public PhileasFilterService(PhileasConfiguration phileasConfiguration) throws IOException {

        LOGGER.info("Initializing Phileas engine.");

        this.phileasConfiguration = phileasConfiguration;

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

        // PHL-145: Accept long text or throw an exception?
        if(!filterProfile.getConfig().getSplitting().isEnabled()) {
            if(filterProfile.getConfig().getSplitting().getThreshold() != -1) {
                if (input.length() >= filterProfile.getConfig().getSplitting().getThreshold()) {
                    throw new PayloadTooLargeException("The request body was too large. Either reduce the size or enable text splitting.");
                }
            }
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

        /*} else if(mimeType == MimeType.TEXT_HTML) {

            // Remove the HTML tags.
            final String plain = Jsoup.clean(input, Whitelist.none());*/



        //} else if(mimeType == MimeType.APPLICATION_FHIRJSON) {
        //    filterResponse = fhirDocumentProcessor.process(filterProfile, filters, postFilters, context, documentId, input);

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

                // Process the text.
                final FilterResponse filterResponse = unstructuredDocumentProcessor.process(filterProfile, filters, postFilters, context, documentId, 0, line);

                // Add all the found spans to the list of spans.
                spans.addAll(filterResponse.getExplanation().getAppliedSpans());

                for (final Span span : filterResponse.getExplanation().getAppliedSpans()) {
                    span.setCharacterStart(span.getCharacterStart() + offset);
                    span.setCharacterEnd(span.getCharacterEnd() + offset);
                    nonRelativeSpans.add(span);
                }

                offset += line.length();

            }

            final RedactionOptions redactionOptions = new RedactionOptions();

            // Redact those terms in the document along with any bounding boxes identified in the filter profile.
            final List<BoundingBox> boundingBoxes = getBoundingBoxes(filterProfile, mimeType);
            final Redacter redacter = new PdfRedacter(filterProfile, spans, redactionOptions, boundingBoxes);
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

    private List<Filter> getFiltersForFilterProfile(final FilterProfile filterProfile, DocumentAnalysis documentAnalysis) throws IOException {

        LOGGER.debug("Getting filters for filter profile [{}]", filterProfile.getName());

        final List<Filter> enabledFilters = new LinkedList<>();

        // Rules filters.

        if(filterProfile.getIdentifiers().hasFilter(FilterType.AGE) && filterProfile.getIdentifiers().getAge().isEnabled()) {

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

            enabledFilters.add(new AgeFilter(filterConfiguration));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.BANK_ROUTING_NUMBER) && filterProfile.getIdentifiers().getBankRoutingNumber().isEnabled()) {

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(filterProfile.getIdentifiers().getBankRoutingNumber().getBankRoutingNumberFilterStrategies())
                    .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(filterProfile.getIdentifiers().getBankRoutingNumber().getIgnored())
                    .withIgnoredFiles(filterProfile.getIdentifiers().getBankRoutingNumber().getIgnoredFiles())
                    .withIgnoredPatterns(filterProfile.getIdentifiers().getBankRoutingNumber().getIgnoredPatterns())
                    .withCrypto(filterProfile.getCrypto())
                    .withWindowSize(windowSize)
                    .withDocumentAnalysis(documentAnalysis)
                    .build();

            enabledFilters.add(new BankRoutingNumberFilter(filterConfiguration));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.BITCOIN_ADDRESS) && filterProfile.getIdentifiers().getBitcoinAddress().isEnabled()) {

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(filterProfile.getIdentifiers().getBitcoinAddress().getBitcoinFilterStrategies())
                    .withAnonymizationService(new BitcoinAddressAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(filterProfile.getIdentifiers().getBitcoinAddress().getIgnored())
                    .withIgnoredFiles(filterProfile.getIdentifiers().getBitcoinAddress().getIgnoredFiles())
                    .withIgnoredPatterns(filterProfile.getIdentifiers().getBitcoinAddress().getIgnoredPatterns())
                    .withCrypto(filterProfile.getCrypto())
                    .withWindowSize(windowSize)
                    .withDocumentAnalysis(documentAnalysis)
                    .build();

            enabledFilters.add(new BitcoinAddressFilter(filterConfiguration));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.CREDIT_CARD) && filterProfile.getIdentifiers().getCreditCard().isEnabled()) {

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(filterProfile.getIdentifiers().getCreditCard().getCreditCardFilterStrategies())
                    .withAnonymizationService(new CreditCardAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(filterProfile.getIdentifiers().getCreditCard().getIgnored())
                    .withIgnoredFiles(filterProfile.getIdentifiers().getCreditCard().getIgnoredFiles())
                    .withIgnoredPatterns(filterProfile.getIdentifiers().getCreditCard().getIgnoredPatterns())
                    .withCrypto(filterProfile.getCrypto())
                    .withWindowSize(windowSize)
                    .withDocumentAnalysis(documentAnalysis)
                    .build();

            final boolean onlyValidCreditCardNumbers = filterProfile.getIdentifiers().getCreditCard().isOnlyValidCreditCardNumbers();

            enabledFilters.add(new CreditCardFilter(filterConfiguration, onlyValidCreditCardNumbers));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.CURRENCY) && filterProfile.getIdentifiers().getCurrency().isEnabled()) {

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

            enabledFilters.add(new CurrencyFilter(filterConfiguration));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.DATE) && filterProfile.getIdentifiers().getDate().isEnabled()) {

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

            enabledFilters.add(new DateFilter(filterConfiguration, onlyValidDates, dateSpanValidator));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.DRIVERS_LICENSE_NUMBER) && filterProfile.getIdentifiers().getDriversLicense().isEnabled()) {

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(filterProfile.getIdentifiers().getDriversLicense().getDriversLicenseFilterStrategies())
                    .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(filterProfile.getIdentifiers().getDriversLicense().getIgnored())
                    .withIgnoredFiles(filterProfile.getIdentifiers().getDriversLicense().getIgnoredFiles())
                    .withIgnoredPatterns(filterProfile.getIdentifiers().getDriversLicense().getIgnoredPatterns())
                    .withCrypto(filterProfile.getCrypto())
                    .withWindowSize(windowSize)
                    .withDocumentAnalysis(documentAnalysis)
                    .build();

            enabledFilters.add(new DriversLicenseFilter(filterConfiguration));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.EMAIL_ADDRESS) && filterProfile.getIdentifiers().getEmailAddress().isEnabled()) {

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

            enabledFilters.add(new EmailAddressFilter(filterConfiguration));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.IBAN_CODE) && filterProfile.getIdentifiers().getIbanCode().isEnabled()) {

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(filterProfile.getIdentifiers().getIbanCode().getIbanCodeFilterStrategies())
                    .withAnonymizationService(new IbanCodeAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(filterProfile.getIdentifiers().getIbanCode().getIgnored())
                    .withIgnoredFiles(filterProfile.getIdentifiers().getIbanCode().getIgnoredFiles())
                    .withIgnoredPatterns(filterProfile.getIdentifiers().getIbanCode().getIgnoredPatterns())
                    .withCrypto(filterProfile.getCrypto())
                    .withWindowSize(windowSize)
                    .withDocumentAnalysis(documentAnalysis)
                    .build();

            final boolean onlyValidIBANCodes = filterProfile.getIdentifiers().getIbanCode().isOnlyValidIBANCodes();
            final boolean allowSpaces = filterProfile.getIdentifiers().getIbanCode().isAllowSpaces();

            enabledFilters.add(new IbanCodeFilter(filterConfiguration, onlyValidIBANCodes, allowSpaces));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.IP_ADDRESS) && filterProfile.getIdentifiers().getIpAddress().isEnabled()) {

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

            enabledFilters.add(new IpAddressFilter(filterConfiguration));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.MAC_ADDRESS) && filterProfile.getIdentifiers().getMacAddress().isEnabled()) {

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

            enabledFilters.add(new MacAddressFilter(filterConfiguration));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.PASSPORT_NUMBER) && filterProfile.getIdentifiers().getPassportNumber().isEnabled()) {

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(filterProfile.getIdentifiers().getPassportNumber().getPassportNumberFilterStrategies())
                    .withAnonymizationService(new PassportNumberAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(filterProfile.getIdentifiers().getPassportNumber().getIgnored())
                    .withIgnoredFiles(filterProfile.getIdentifiers().getPassportNumber().getIgnoredFiles())
                    .withIgnoredPatterns(filterProfile.getIdentifiers().getPassportNumber().getIgnoredPatterns())
                    .withCrypto(filterProfile.getCrypto())
                    .withWindowSize(windowSize)
                    .withDocumentAnalysis(documentAnalysis)
                    .build();

            enabledFilters.add(new PassportNumberFilter(filterConfiguration));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.PHONE_NUMBER_EXTENSION) && filterProfile.getIdentifiers().getPhoneNumberExtension().isEnabled()) {

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

            enabledFilters.add(new PhoneNumberExtensionFilter(filterConfiguration));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.PHONE_NUMBER) && filterProfile.getIdentifiers().getPhoneNumber().isEnabled()) {

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

            enabledFilters.add(new PhoneNumberRulesFilter(filterConfiguration));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.PHYSICIAN_NAME) && filterProfile.getIdentifiers().getPhysicianName().isEnabled()) {

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

            enabledFilters.add(new PhysicianNameFilter(filterConfiguration));

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

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(filterProfile.getIdentifiers().getSsn().getSsnFilterStrategies())
                    .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(filterProfile.getIdentifiers().getSsn().getIgnored())
                    .withIgnoredFiles(filterProfile.getIdentifiers().getSsn().getIgnoredFiles())
                    .withIgnoredPatterns(filterProfile.getIdentifiers().getSsn().getIgnoredPatterns())
                    .withCrypto(filterProfile.getCrypto())
                    .withWindowSize(windowSize)
                    .withDocumentAnalysis(documentAnalysis)
                    .build();

            enabledFilters.add(new SsnFilter(filterConfiguration));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.STATE_ABBREVIATION) && filterProfile.getIdentifiers().getStateAbbreviation().isEnabled()) {

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

            enabledFilters.add(new StateAbbreviationFilter(filterConfiguration));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.STREET_ADDRESS) && filterProfile.getIdentifiers().getStreetAddress().isEnabled()) {

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

            enabledFilters.add(new StreetAddressFilter(filterConfiguration));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.TRACKING_NUMBER) && filterProfile.getIdentifiers().getTrackingNumber().isEnabled()) {

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(filterProfile.getIdentifiers().getTrackingNumber().getTrackingNumberFilterStrategies())
                    .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(filterProfile.getIdentifiers().getTrackingNumber().getIgnored())
                    .withIgnoredFiles(filterProfile.getIdentifiers().getTrackingNumber().getIgnoredFiles())
                    .withIgnoredPatterns(filterProfile.getIdentifiers().getTrackingNumber().getIgnoredPatterns())
                    .withCrypto(filterProfile.getCrypto())
                    .withWindowSize(windowSize)
                    .withDocumentAnalysis(documentAnalysis)
                    .build();

            enabledFilters.add(new TrackingNumberFilter(filterConfiguration));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.URL) && filterProfile.getIdentifiers().getUrl().isEnabled()) {

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

            enabledFilters.add(new UrlFilter(filterConfiguration, requireHttpWwwPrefix));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.VIN) && filterProfile.getIdentifiers().getVin().isEnabled()) {

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(filterProfile.getIdentifiers().getVin().getVinFilterStrategies())
                    .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(filterProfile.getIdentifiers().getVin().getIgnored())
                    .withIgnoredFiles(filterProfile.getIdentifiers().getVin().getIgnoredFiles())
                    .withIgnoredPatterns(filterProfile.getIdentifiers().getVin().getIgnoredPatterns())
                    .withCrypto(filterProfile.getCrypto())
                    .withWindowSize(windowSize)
                    .withDocumentAnalysis(documentAnalysis)
                    .build();

            enabledFilters.add(new VinFilter(filterConfiguration));

        }

        if(filterProfile.getIdentifiers().hasFilter(FilterType.ZIP_CODE) && filterProfile.getIdentifiers().getZipCode().isEnabled()) {

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

            enabledFilters.add(new ZipCodeFilter(filterConfiguration, requireDelimiter));

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

        // PyTorch filters.

        if(filterProfile.getIdentifiers().hasFilter(FilterType.NER_ENTITY) && filterProfile.getIdentifiers().getNer().isEnabled()) {

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(filterProfile.getIdentifiers().getNer().getNerStrategies())
                    .withAnonymizationService(new PersonsAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(filterProfile.getIdentifiers().getNer().getIgnored())
                    .withIgnoredFiles(filterProfile.getIdentifiers().getNer().getIgnoredFiles())
                    .withIgnoredPatterns(filterProfile.getIdentifiers().getNer().getIgnoredPatterns())
                    .withCrypto(filterProfile.getCrypto())
                    .withWindowSize(windowSize)
                    .withDocumentAnalysis(documentAnalysis)
                    .build();

            // TODO: Allow a single PyTorchFilter to extract many types of entities instead of just one, i.e. "PER".
            enabledFilters.add(new PyTorchFilter(filterConfiguration, philterNerEndpoint,
                    phileasConfiguration, "PER", stats, metricsService,
                    filterProfile.getIdentifiers().getNer().isRemovePunctuation(),
                    filterProfile.getIdentifiers().getNer().getThresholds()
                    ));

        }

        return enabledFilters;

    }

}
