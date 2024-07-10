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
import ai.philterd.phileas.model.objects.Explanation;
import ai.philterd.phileas.model.objects.PdfRedactionOptions;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.Ignored;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.policy.filters.CustomDictionary;
import ai.philterd.phileas.model.policy.filters.Identifier;
import ai.philterd.phileas.model.policy.filters.Section;
import ai.philterd.phileas.model.policy.graphical.BoundingBox;
import ai.philterd.phileas.model.responses.BinaryDocumentFilterResponse;
import ai.philterd.phileas.model.responses.FilterResponse;
import ai.philterd.phileas.model.serializers.PlaceholderDeserializer;
import ai.philterd.phileas.model.services.*;
import ai.philterd.phileas.processors.unstructured.UnstructuredDocumentProcessor;
import ai.philterd.phileas.service.ai.sentiment.OpenNLPSentimentDetector;
import ai.philterd.phileas.services.alerts.AlertServiceFactory;
import ai.philterd.phileas.services.anonymization.*;
import ai.philterd.phileas.services.anonymization.cache.AnonymizationCacheServiceFactory;
import ai.philterd.phileas.services.disambiguation.VectorBasedSpanDisambiguationService;
import ai.philterd.phileas.services.filters.ai.opennlp.PersonsV2Filter;
import ai.philterd.phileas.services.filters.ai.opennlp.PersonsV3Filter;
import ai.philterd.phileas.services.filters.ai.python.PersonsV1Filter;
import ai.philterd.phileas.services.filters.custom.PhoneNumberRulesFilter;
import ai.philterd.phileas.services.filters.regex.*;
import ai.philterd.phileas.services.policies.LocalPolicyService;
import ai.philterd.phileas.services.policies.S3PolicyService;
import ai.philterd.phileas.services.policies.utils.PolicyUtils;
import ai.philterd.phileas.services.postfilters.*;
import ai.philterd.phileas.services.split.SplitFactory;
import ai.philterd.phileas.services.validators.DateSpanValidator;
import ai.philterd.services.pdf.PdfRedacter;
import ai.philterd.services.pdf.PdfTextExtractor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    private final PolicyService policyService;
    private final PolicyUtils policyUtils;
    private final MetricsService metricsService;

    private final Map<String, DescriptiveStatistics> stats;

    private final AnonymizationCacheService anonymizationCacheService;
    private final AlertService alertService;
    private final SpanDisambiguationService spanDisambiguationService;
    private final String indexDirectory;
    private final double bloomFilterFpp;

    private final DocumentProcessor unstructuredDocumentProcessor;

    private final Map<String, Map<FilterType, Filter>> filterCache;

    // PHL-223: Face recognition
    //private final ImageProcessor imageProcessor;

    private final int windowSize;

    public PhileasFilterService(final PhileasConfiguration phileasConfiguration) throws IOException {

        LOGGER.info("Initializing Phileas engine.");

        this.phileasConfiguration = phileasConfiguration;
        this.filterCache = new ConcurrentHashMap<>();

        // Configure the deserialization.
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(String.class, new PlaceholderDeserializer());
        final Gson gson = gsonBuilder.create();

        // Configure metrics.
        this.metricsService = new PhileasMetricsService(phileasConfiguration);

        // Set the policy services.
        this.policyService = buildPolicyService(phileasConfiguration);
        this.policyUtils = new PolicyUtils(policyService, gson);

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

        // Get the window size.
        this.windowSize = phileasConfiguration.spanWindowSize();
        LOGGER.info("Using window size {}", this.windowSize);

        this.indexDirectory = phileasConfiguration.indexesDirectory();
        LOGGER.info("Using indexes directory {}", this.indexDirectory);

    }

    @Override
    public PolicyService getPolicyService() {
        return policyService;
    }

    @Override
    public AlertService getAlertService() {
        return alertService;
    }

    @Override
    public FilterResponse filter(final Policy policy, final String context, String documentId,
                                 final String input, final MimeType mimeType) throws Exception {

        // Initialize potential attributes that are associated with the input text.
        final Map<String, String> attributes = new HashMap<>();

        // Load default values based on the domain.
        if(StringUtils.equalsIgnoreCase(Domain.DOMAIN_LEGAL, policy.getDomain())) {

            // PHL-209: Implement legal domain.
            policy.getIgnored().add(LegalDomain.getInstance().getIgnored());

            // TODO: Add filters.

        } else if(StringUtils.equalsIgnoreCase(Domain.DOMAIN_HEALTH, policy.getDomain())) {

            // PHL-210: Implement health domain.
            policy.getIgnored().add(HealthDomain.getInstance().getIgnored());

            // TODO: Add filters.

        }

        final List<Filter> filters = getFiltersForPolicy(policy);
        final List<PostFilter> postFilters = getPostFiltersForPolicy(policy);

        // Run sentiment analysis on the text.
        if(policy.getConfig().getAnalysis().getSentiment().isEnabled()) {

            final SentimentDetector sentimentDetector = new OpenNLPSentimentDetector();
            final Classification classification = sentimentDetector.classify(policy, input);

            if(classification != null) {
                attributes.put("sentiment", classification.label());
                attributes.put("sentiment-confidence", String.valueOf(classification.confidence()));
            }

        }

        // Run offensive analysis on the text.
        if(policy.getConfig().getAnalysis().getOffensiveness().isEnabled()) {

            final SentimentDetector sentimentDetector = new OpenNLPSentimentDetector();
            final Classification classification = sentimentDetector.classify(policy, input);

            if(classification != null) {
                attributes.put("offensiveness", classification.label());
                attributes.put("offensiveness-confidence", String.valueOf(classification.confidence()));
            }

        }

        // See if we need to generate a document ID.
        if(StringUtils.isEmpty(documentId)) {

            // PHL-58: Use a hash function to generate the document ID.
            documentId = DigestUtils.md5Hex(UUID.randomUUID() + "-" + context + "-" + policy.getName() + "-" + input);
            LOGGER.debug("Generated document ID {}", documentId);

        }

        final FilterResponse filterResponse;

        if(mimeType == MimeType.TEXT_PLAIN) {

            // PHL-145: Do we need to split the input text due to its size?
            if (policy.getConfig().getSplitting().isEnabled() && input.length() >= policy.getConfig().getSplitting().getThreshold()) {

                // Get the splitter to use from the policy.
                final SplitService splitService = SplitFactory.getSplitService(policy.getConfig().getSplitting().getMethod());

                // Holds all filter responses that will ultimately be combined into a single response.
                final List<FilterResponse> filterResponses = new LinkedList<>();

                // Split the string.
                final List<String> splits = splitService.split(input);

                // Process each split.
                for (int i = 0; i < splits.size(); i++) {
                    final FilterResponse fr = unstructuredDocumentProcessor.process(policy, filters, postFilters, context, documentId, i, splits.get(i), attributes);
                    filterResponses.add(fr);
                }

                // Combine the results into a single filterResponse object.
                filterResponse = FilterResponse.combine(filterResponses, context, documentId, splitService.getSeparator());

            } else {

                // Do not split. Process the entire string at once.
                filterResponse = unstructuredDocumentProcessor.process(policy, filters, postFilters, context, documentId, 0, input, attributes);

            }

        } else {
            // Should never happen but just in case.
            throw new Exception("Unknown mime type.");
        }

        return filterResponse;

    }

    @Override
    public FilterResponse filter(final List<String> policyNames, final String context, String documentId,
                                 final String input, final MimeType mimeType) throws Exception {

        // Get the combined policy.
        final Policy policy = policyUtils.getCombinedPolicys(policyNames);

        // Do the filtering.
        return filter(policy, context, documentId, input, mimeType);

    }

    @Override
    public BinaryDocumentFilterResponse filter(final List<String> policyNames, final String context, String documentId,
                                               final byte[] input, final MimeType mimeType,
                                               final MimeType outputMimeType) throws Exception {

        // Get the policy.
        final Policy policy = policyUtils.getCombinedPolicys(policyNames);

        // Initialize potential attributes that are associated with the input text.
        // NOTE: Binary documents do not currently have any attributes.
        final Map<String, String> attributes = new HashMap<>();

        // See if we need to generate a document ID.
        if(StringUtils.isEmpty(documentId)) {

            // PHL-58: Use a hash function to generate the document ID.
            documentId = DigestUtils.md5Hex(UUID.randomUUID() + "-" + context + "-" + policy.getName() + "-" + Arrays.toString(input));
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

            final List<Filter> filters = getFiltersForPolicy(policy);
            final List<PostFilter> postFilters = getPostFiltersForPolicy(policy);

            // TODO: The following code really only needs to be done if there is at least one filter defined in the policy.

            // Process each line looking for sensitive information in each line.
            for (final String line : lines) {

                final int piece = 0;

                // Process the text.
                final FilterResponse filterResponse = unstructuredDocumentProcessor.process(policy, filters, postFilters, context, documentId, piece, line, attributes);

                // Add all the found spans to the list of spans.
                spans.addAll(filterResponse.explanation().appliedSpans());

                for (final Span span : filterResponse.explanation().appliedSpans()) {
                    span.setCharacterStart(span.getCharacterStart() + offset);
                    span.setCharacterEnd(span.getCharacterEnd() + offset);
                    nonRelativeSpans.add(span);
                }

                offset += line.length();

            }

            // TODO: Build this from the config in the policy.
            final PdfRedactionOptions pdfRedactionOptions = new PdfRedactionOptions();
            pdfRedactionOptions.setDpi(150);
            pdfRedactionOptions.setScale(0.25f);
            pdfRedactionOptions.setCompressionQuality(1.0f);

            // Redact those terms in the document along with any bounding boxes identified in the policy.
            final List<BoundingBox> boundingBoxes = getBoundingBoxes(policy, mimeType);
            final Redacter redacter = new PdfRedacter(policy, spans, pdfRedactionOptions, boundingBoxes);
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

            // TODO: Get options from the policy.
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
     * Get the bounding boxes from the policy for a given mime type.
     * @param policy The policy.
     * @param mimeType The mime type.
     * @return A list of bounding boxes from the policy for the given mime type.
     */
    private List<BoundingBox> getBoundingBoxes(final Policy policy, final MimeType mimeType) {

        final List<BoundingBox> boundingBoxes = new LinkedList<>();

        for(final BoundingBox boundingBox : policy.getGraphical().getBoundingBoxes()) {
            if(StringUtils.equalsIgnoreCase(boundingBox.getMimeType(), mimeType.toString())) {
                boundingBoxes.add(boundingBox);
            }
        }

        return boundingBoxes;

    }

    private PolicyService buildPolicyService(final PhileasConfiguration phileasConfiguration) throws IOException {

        final PolicyService policyService;
        final String s3Bucket = phileasConfiguration.policiesS3Bucket();

        // If an S3 bucket is provided then instantiate an S3PolicyService.
        if(StringUtils.isNotEmpty(s3Bucket)) {

            LOGGER.info("Initializing configuration for policies S3 bucket.");
            policyService = new S3PolicyService(phileasConfiguration, false);

        } else {

            LOGGER.info("Using local storage for policies.");
            policyService = new LocalPolicyService(phileasConfiguration);

        }

        return policyService;

    }

    private List<PostFilter> getPostFiltersForPolicy(final Policy policy) throws IOException {

        LOGGER.debug("Reloading policies.");

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
        if(CollectionUtils.isNotEmpty(policy.getIgnored())) {

            // Make a post filter for each Ignored item in the list.
            for(final Ignored ignored : policy.getIgnored()) {
                postFilters.add(new IgnoredTermsFilter(ignored));
            }

        }

        // Ignored patterns filter. Looks for terms matching a pattern in the scope of the whole document (and not just a particular filter).
        // No matter what filter found the span, it is subject to this ignore list.
        if(CollectionUtils.isNotEmpty(policy.getIgnoredPatterns())) {
            postFilters.add(new IgnoredPatternsFilter(policy.getIgnoredPatterns()));
        }

        // Add the post filters if they are enabled in the policy.

        if(policy.getConfig().getPostFilters().isRemoveTrailingPeriods()) {
            postFilters.add(TrailingPeriodPostFilter.getInstance());
        }

        if(policy.getConfig().getPostFilters().isRemoveTrailingSpaces()) {
            postFilters.add(TrailingSpacePostFilter.getInstance());
        }

        if(policy.getConfig().getPostFilters().isRemoveTrailingNewLines()) {
            postFilters.add(TrailingNewLinePostFilter.getInstance());
        }

        return postFilters;

    }

    private List<Filter> getFiltersForPolicy(final Policy policy) throws Exception {

        LOGGER.debug("Getting filters for policy [{}]", policy.getName());

        // See if this filter is already cached.
        filterCache.putIfAbsent(policy.getName(), new ConcurrentHashMap<>());
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
                        .build();

                final boolean onlyValidCreditCardNumbers = policy.getIdentifiers().getCreditCard().isOnlyValidCreditCardNumbers();

                final Filter filter = new CreditCardFilter(filterConfiguration, onlyValidCreditCardNumbers);
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
                        .build();

                final Filter filter = new EmailAddressFilter(filterConfiguration);
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
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
                            .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
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

                // TODO: Add caching of the policy (see Age for example)

                if(customDictionary.isEnabled()) {

                    // TODO: Should there be an anonymization service?
                    // There is no anonymization service because we don't know what to replace custom dictionary items with.
                    final AnonymizationService anonymizationService = null;

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
                                .withWindowSize(windowSize)
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
                        if(!terms.isEmpty()) {

                            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                                    .withStrategies(customDictionary.getCustomDictionaryFilterStrategies())
                                    .withAnonymizationService(new ZipCodeAnonymizationService(anonymizationCacheService))
                                    .withAlertService(alertService)
                                    .withIgnored(customDictionary.getIgnored())
                                    .withIgnoredFiles(customDictionary.getIgnoredFiles())
                                    .withIgnoredPatterns(customDictionary.getIgnoredPatterns())
                                    .withCrypto(policy.getCrypto())
                                    .withWindowSize(windowSize)
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

            LOGGER.debug("Policy {} has no custom dictionaries.", policy.getName());

        }

        // Lucene dictionary filters.

        if(policy.getIdentifiers().hasFilter(FilterType.LOCATION_CITY) && policy.getIdentifiers().getCity().isEnabled()) {

            // TODO: Add caching of the policy (see Age for example)

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(policy.getIdentifiers().getCity().getCityFilterStrategies())
                    .withAnonymizationService(new CityAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(policy.getIdentifiers().getCity().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getCity().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getCity().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withWindowSize(windowSize)
                    .build();

            final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getCity().getSensitivityLevel();
            final boolean capitalized = policy.getIdentifiers().getCity().isCapitalized();

            enabledFilters.add(new LuceneDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, indexDirectory + "cities", sensitivityLevel, capitalized));

        }

        if(policy.getIdentifiers().hasFilter(FilterType.LOCATION_COUNTY) && policy.getIdentifiers().getCounty().isEnabled()) {

            // TODO: Add caching of the policy (see Age for example)

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(policy.getIdentifiers().getCounty().getCountyFilterStrategies())
                    .withAnonymizationService(new CountyAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(policy.getIdentifiers().getCounty().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getCounty().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getCounty().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withWindowSize(windowSize)
                    .build();

            final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getCounty().getSensitivityLevel();
            final boolean capitalized = policy.getIdentifiers().getCounty().isCapitalized();

            enabledFilters.add(new LuceneDictionaryFilter(FilterType.LOCATION_COUNTY, filterConfiguration, indexDirectory + "counties", sensitivityLevel, capitalized));

        }

        if(policy.getIdentifiers().hasFilter(FilterType.LOCATION_STATE) && policy.getIdentifiers().getState().isEnabled()) {

            // TODO: Add caching of the policy (see Age for example)

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(policy.getIdentifiers().getState().getStateFilterStrategies())
                    .withAnonymizationService(new StateAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(policy.getIdentifiers().getState().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getState().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getState().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withWindowSize(windowSize)
                    .build();

            final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getState().getSensitivityLevel();
            final boolean capitalized = policy.getIdentifiers().getState().isCapitalized();

            enabledFilters.add(new LuceneDictionaryFilter(FilterType.LOCATION_STATE, filterConfiguration, indexDirectory + "states", sensitivityLevel, capitalized));

        }

        if(policy.getIdentifiers().hasFilter(FilterType.HOSPITAL) && policy.getIdentifiers().getHospital().isEnabled()) {

            // TODO: Add caching of the policy (see Age for example)

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(policy.getIdentifiers().getHospital().getHospitalFilterStrategies())
                    .withAnonymizationService(new HospitalAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(policy.getIdentifiers().getHospital().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getHospital().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getHospital().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withWindowSize(windowSize)
                    .build();

            final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getHospital().getSensitivityLevel();
            final boolean capitalized = policy.getIdentifiers().getHospital().isCapitalized();

            enabledFilters.add(new LuceneDictionaryFilter(FilterType.HOSPITAL, filterConfiguration, indexDirectory + "hospitals", sensitivityLevel, capitalized));

        }

        if(policy.getIdentifiers().hasFilter(FilterType.HOSPITAL_ABBREVIATION) && policy.getIdentifiers().getHospitalAbbreviation().isEnabled()) {

            // TODO: Add caching of the policy (see Age for example)

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(policy.getIdentifiers().getHospitalAbbreviation().getHospitalAbbreviationFilterStrategies())
                    .withAnonymizationService(new HospitalAbbreviationAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(policy.getIdentifiers().getHospitalAbbreviation().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getHospitalAbbreviation().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getHospitalAbbreviation().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withWindowSize(windowSize)
                    .build();

            final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getHospitalAbbreviation().getSensitivityLevel();
            final boolean capitalized = policy.getIdentifiers().getHospitalAbbreviation().isCapitalized();

            enabledFilters.add(new LuceneDictionaryFilter(FilterType.HOSPITAL_ABBREVIATION, filterConfiguration, indexDirectory + "hospital-abbreviations", sensitivityLevel, capitalized));

        }

        if(policy.getIdentifiers().hasFilter(FilterType.FIRST_NAME) && policy.getIdentifiers().getFirstName().isEnabled()) {

            // TODO: Add caching of the policy (see Age for example)

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(policy.getIdentifiers().getFirstName().getFirstNameFilterStrategies())
                    .withAnonymizationService(new PersonsAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(policy.getIdentifiers().getFirstName().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getFirstName().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getFirstName().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withWindowSize(windowSize)
                    .build();

            final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getFirstName().getSensitivityLevel();
            final boolean capitalized = policy.getIdentifiers().getFirstName().isCapitalized();

            enabledFilters.add(new LuceneDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, indexDirectory + "names", sensitivityLevel, capitalized));

        }

        if(policy.getIdentifiers().hasFilter(FilterType.SURNAME) && policy.getIdentifiers().getSurname().isEnabled()) {

            // TODO: Add caching of the policy (see Age for example)

            final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(policy.getIdentifiers().getSurname().getSurnameFilterStrategies())
                    .withAnonymizationService(new SurnameAnonymizationService(anonymizationCacheService))
                    .withAlertService(alertService)
                    .withIgnored(policy.getIdentifiers().getSurname().getIgnored())
                    .withIgnoredFiles(policy.getIdentifiers().getSurname().getIgnoredFiles())
                    .withIgnoredPatterns(policy.getIdentifiers().getSurname().getIgnoredPatterns())
                    .withCrypto(policy.getCrypto())
                    .withWindowSize(windowSize)
                    .build();

            final SensitivityLevel sensitivityLevel = policy.getIdentifiers().getSurname().getSensitivityLevel();
            final boolean capitalized = policy.getIdentifiers().getSurname().isCapitalized();

            enabledFilters.add(new LuceneDictionaryFilter(FilterType.SURNAME, filterConfiguration, indexDirectory + "surnames", sensitivityLevel, capitalized));

        }

        // Enable ID filter last since it is a pretty generic pattern that might also match SSN, et. al.

        if(policy.getIdentifiers().hasFilter(FilterType.IDENTIFIER)) {

            final List<Identifier> identifiers = policy.getIdentifiers().getIdentifiers();

            for(final Identifier identifier : identifiers) {

                // TODO: Add caching of the policy (see Age for example)

                if(identifier.isEnabled()) {

                    final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                            .withStrategies(identifier.getIdentifierFilterStrategies())
                            .withAnonymizationService(new AlphanumericAnonymizationService(anonymizationCacheService))
                            .withAlertService(alertService)
                            .withIgnored(identifier.getIgnored())
                            .withIgnoredFiles(identifier.getIgnoredFiles())
                            .withIgnoredPatterns(identifier.getIgnoredPatterns())
                            .withCrypto(policy.getCrypto())
                            .withWindowSize(windowSize)
                            .build();

                    final String classification = identifier.getClassification();
                    final String pattern = identifier.getPattern();
                    final boolean caseSensitive = identifier.isCaseSensitive();
                    final int groupNumber = identifier.getGroupNumber();

                    enabledFilters.add(new IdentifierFilter(filterConfiguration, classification, pattern, caseSensitive, groupNumber));

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
                        .withWindowSize(windowSize)
                        .build();

                final Filter filter = new PersonsV1Filter(
                        filterConfiguration,
                        phileasConfiguration,
                        "PER",
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
                        .withWindowSize(windowSize)
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
                        .withWindowSize(windowSize)
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
