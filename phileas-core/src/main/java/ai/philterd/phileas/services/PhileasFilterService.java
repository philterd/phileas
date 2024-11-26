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
import ai.philterd.phileas.model.domain.Domain;
import ai.philterd.phileas.model.domain.HealthDomain;
import ai.philterd.phileas.model.domain.LegalDomain;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.enums.MimeType;
import ai.philterd.phileas.model.filter.Filter;
import ai.philterd.phileas.model.objects.Explanation;
import ai.philterd.phileas.model.objects.PdfRedactionOptions;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.Ignored;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.policy.graphical.BoundingBox;
import ai.philterd.phileas.model.responses.BinaryDocumentFilterResponse;
import ai.philterd.phileas.model.responses.FilterResponse;
import ai.philterd.phileas.model.serializers.PlaceholderDeserializer;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.model.services.AnonymizationCacheService;
import ai.philterd.phileas.model.services.Classification;
import ai.philterd.phileas.model.services.DocumentProcessor;
import ai.philterd.phileas.model.services.FilterService;
import ai.philterd.phileas.model.services.MetricsService;
import ai.philterd.phileas.model.services.PolicyService;
import ai.philterd.phileas.model.services.PostFilter;
import ai.philterd.phileas.model.services.Redacter;
import ai.philterd.phileas.model.services.SentimentDetector;
import ai.philterd.phileas.model.services.SplitService;
import ai.philterd.phileas.processors.unstructured.UnstructuredDocumentProcessor;
import ai.philterd.phileas.service.ai.sentiment.OpenNLPSentimentDetector;
import ai.philterd.phileas.services.alerts.AlertServiceFactory;
import ai.philterd.phileas.services.anonymization.cache.AnonymizationCacheServiceFactory;
import ai.philterd.phileas.services.disambiguation.VectorBasedSpanDisambiguationService;
import ai.philterd.phileas.services.metrics.NoOpMetricsService;
import ai.philterd.phileas.services.policies.LocalPolicyService;
import ai.philterd.phileas.services.policies.utils.PolicyUtils;
import ai.philterd.phileas.services.postfilters.IgnoredPatternsFilter;
import ai.philterd.phileas.services.postfilters.IgnoredTermsFilter;
import ai.philterd.phileas.services.postfilters.TrailingNewLinePostFilter;
import ai.philterd.phileas.services.postfilters.TrailingPeriodPostFilter;
import ai.philterd.phileas.services.postfilters.TrailingSpacePostFilter;
import ai.philterd.phileas.services.split.SplitFactory;
import ai.philterd.services.pdf.PdfRedacter;
import ai.philterd.services.pdf.PdfTextExtractor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PhileasFilterService implements FilterService {

	private static final Logger LOGGER = LogManager.getLogger(PhileasFilterService.class);

    private final PolicyService policyService;
    private final PolicyUtils policyUtils;
    private final AlertService alertService;

    private final DocumentProcessor unstructuredDocumentProcessor;
    private final FilterPolicyLoader filterPolicyLoader;

    // A map that gives each filter profile its own cache of filters.
    private final Map<String, Map<FilterType, Filter>> filterCache;

    // PHL-223: Face recognition
    //private final ImageProcessor imageProcessor;

    public PhileasFilterService(final PhileasConfiguration phileasConfiguration) throws IOException {
        this(phileasConfiguration, new NoOpMetricsService());
    }

    public PhileasFilterService(final PhileasConfiguration phileasConfiguration, final MetricsService metricsService) throws IOException {

        LOGGER.info("Initializing Phileas engine.");

        this.filterCache = new ConcurrentHashMap<>();

        // Configure the deserialization.
        final Gson gson = new GsonBuilder().registerTypeAdapter(String.class, new PlaceholderDeserializer()).create();

        // Set the policy services.
        this.policyService = new LocalPolicyService(phileasConfiguration);
        this.policyUtils = new PolicyUtils(policyService, gson);

        // Set the anonymization cache service.
        final AnonymizationCacheService anonymizationCacheService = AnonymizationCacheServiceFactory.getAnonymizationCacheService(phileasConfiguration);

        // Set the alert service.
        this.alertService = AlertServiceFactory.getAlertService(phileasConfiguration);

        // Instantiate the stats.
        Map<String, DescriptiveStatistics> stats = new HashMap<>();

        // The filter loader for policies.
        this.filterPolicyLoader = new FilterPolicyLoader(alertService, anonymizationCacheService, metricsService, stats, phileasConfiguration);

        // Create a new unstructured document processor.
        this.unstructuredDocumentProcessor = new UnstructuredDocumentProcessor(metricsService, new VectorBasedSpanDisambiguationService(phileasConfiguration));

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

            // TODO: #109 Add filters.

        } else if(StringUtils.equalsIgnoreCase(Domain.DOMAIN_HEALTH, policy.getDomain())) {

            // PHL-210: Implement health domain.
            policy.getIgnored().add(HealthDomain.getInstance().getIgnored());

            // TODO: #110 Add filters.

        }

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);
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
        final Policy policy = policyUtils.getCombinedPolicies(policyNames);

        // Do the filtering.
        return filter(policy, context, documentId, input, mimeType);

    }

    @Override
    public BinaryDocumentFilterResponse filter(final List<String> policyNames, final String context, String documentId,
                                               final byte[] input, final MimeType mimeType,
                                               final MimeType outputMimeType) throws Exception {

        // Get the policy.
        final Policy policy = policyUtils.getCombinedPolicies(policyNames);

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

            final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);
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

}
