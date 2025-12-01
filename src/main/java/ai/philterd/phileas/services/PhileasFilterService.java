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
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.Explanation;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.IncrementalRedaction;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Ignored;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.config.Pdf;
import ai.philterd.phileas.policy.graphical.BoundingBox;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.disambiguation.vector.VectorBasedSpanDisambiguationService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.documentprocessors.DocumentProcessor;
import ai.philterd.phileas.services.documentprocessors.UnstructuredDocumentProcessor;
import ai.philterd.phileas.services.filters.FilterService;
import ai.philterd.phileas.services.filters.postfilters.IgnoredPatternsFilter;
import ai.philterd.phileas.services.filters.postfilters.IgnoredTermsFilter;
import ai.philterd.phileas.services.filters.postfilters.PostFilter;
import ai.philterd.phileas.services.filters.postfilters.TrailingNewLinePostFilter;
import ai.philterd.phileas.services.filters.postfilters.TrailingPeriodPostFilter;
import ai.philterd.phileas.services.filters.postfilters.TrailingSpacePostFilter;
import ai.philterd.phileas.services.pdf.PdfRedacter;
import ai.philterd.phileas.services.pdf.PdfRedactionOptions;
import ai.philterd.phileas.services.pdf.PdfTextExtractor;
import ai.philterd.phileas.services.pdf.Redacter;
import ai.philterd.phileas.services.split.SplitFactory;
import ai.philterd.phileas.services.split.SplitService;
import ai.philterd.phileas.services.tokens.TokenCounter;
import ai.philterd.phileas.services.tokens.WhitespaceTokenCounter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PhileasFilterService implements FilterService {

	private static final Logger LOGGER = LogManager.getLogger(PhileasFilterService.class);

    private final DocumentProcessor unstructuredDocumentProcessor;
    private final FilterPolicyLoader filterPolicyLoader;
    private final TokenCounter tokenCounter;

    // A map that gives each filter profile its own cache of filters.
    private final Map<String, Map<FilterType, Filter>> filterCache;

    // PHL-223: Face recognition
    //private final ImageProcessor imageProcessor;

    public PhileasFilterService(final PhileasConfiguration phileasConfiguration,
                                final ContextService contextService,
                                final VectorService vectorService) {

        LOGGER.info("Initializing Phileas engine.");

        this.filterCache = new ConcurrentHashMap<>();

        // Set the token counter.
        this.tokenCounter = new WhitespaceTokenCounter();

        // The filter loader for policies.
        this.filterPolicyLoader = new FilterPolicyLoader(contextService, phileasConfiguration);

        // Create a new unstructured document processor.
        this.unstructuredDocumentProcessor = new UnstructuredDocumentProcessor(
                new VectorBasedSpanDisambiguationService(phileasConfiguration, vectorService),
                phileasConfiguration.incrementalRedactionsEnabled()
        );

    }

    @Override
    public TextFilterResult filter(final Policy policy, final String context, final String input) throws Exception {

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);
        final List<PostFilter> postFilters = getPostFiltersForPolicy(policy);

        final TextFilterResult textFilterResult;

        // Do we need to split the input text due to its size?
        // Is the appliesToFilter = "*" or is at least one of the filters in the policy in the appliesToFilter list?
        if (policy.getConfig().getSplitting().isEnabled() && input.length() >= policy.getConfig().getSplitting().getThreshold()) {

                // Get the splitter to use from the policy.
                final SplitService splitService = SplitFactory.getSplitService(
                        policy.getConfig().getSplitting().getMethod(),
                        policy.getConfig().getSplitting().getThreshold()
                );

                // Holds all filter responses that will ultimately be combined into a single response.
                final List<TextFilterResult> filterResponse = new LinkedList<>();

                // Split the string.
                final List<String> splits = splitService.split(input);

                // Process each split.
                for (int i = 0; i < splits.size(); i++) {
                    final TextFilterResult fr = unstructuredDocumentProcessor.process(policy, filters, postFilters, context, i, splits.get(i));
                    filterResponse.add(fr);
                }

                // Combine the results into a single filterResponse object.
                textFilterResult = TextFilterResult.combine(filterResponse, context, splitService.getSeparator());

        } else {

            // Do not split. Process the entire string at once.
            textFilterResult = unstructuredDocumentProcessor.process(policy, filters, postFilters, context, 0, input);

        }

        return textFilterResult;

    }

    @Override
    public BinaryDocumentFilterResult filter(final Policy policy, final String context,
                                             final byte[] input, final MimeType mimeType,
                                             final MimeType outputMimeType) throws Exception {

        final BinaryDocumentFilterResult binaryDocumentFilterResult;

        final List<IncrementalRedaction> incrementalRedactions = new ArrayList<>();

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

            long tokens = 0;

            // TODO: The following code really only needs to be done if there is at least one filter defined in the policy.

            // Process each line looking for sensitive information in each line.
            for (final String line : lines) {

                final int piece = 0;
                tokens += tokenCounter.countTokens(line);

                // Process the text.
                final TextFilterResult textFilterResult = unstructuredDocumentProcessor.process(policy, filters, postFilters, context, piece, line);

                // Add all the found spans to the list of spans.
                spans.addAll(textFilterResult.getExplanation().appliedSpans());

                // Add the incremental redactions to the list.
                incrementalRedactions.addAll(textFilterResult.getIncrementalRedactions());

                for (final Span span : textFilterResult.getExplanation().appliedSpans()) {
                    span.setCharacterStart(span.getCharacterStart() + offset);
                    span.setCharacterEnd(span.getCharacterEnd() + offset);
                    nonRelativeSpans.add(span);
                }

                offset += line.length();

            }

            // Load the PDF config from the policy and apply to the PdfRedactionOptions that are used when
            // generating the new PDF document from the result of the redaction
            final Pdf policyPdfConfig = policy.getConfig().getPdf();
            final PdfRedactionOptions pdfRedactionOptions = new PdfRedactionOptions(
                    policyPdfConfig.getDpi(),
                    policyPdfConfig.getCompressionQuality(),
                    policyPdfConfig.getScale(),
                    policyPdfConfig.getPreserveUnredactedPages()
            );

            // Redact those terms in the document along with any bounding boxes identified in the policy.
            final List<BoundingBox> boundingBoxes = getBoundingBoxes(policy, mimeType);
            final Redacter redacter = new PdfRedacter(policy, spans, pdfRedactionOptions, boundingBoxes);
            final byte[] redacted = redacter.process(input, outputMimeType);

            // Create the response.
            final List<Span> spansList = new ArrayList<>(nonRelativeSpans);

            // TODO: The identified vs the applied will actually be different
            // but we are setting the same here. Fix this at some point.
            final Explanation explanation = new Explanation(spansList, spansList);
            binaryDocumentFilterResult = new BinaryDocumentFilterResult(redacted, context, explanation, tokens, incrementalRedactions);

        /*} else if(mimeType == MimeType.IMAGE_JPEG) {
            // PHL-223: Face recognition

            // TODO: Get options from the policy.
            final ImageFilterResponse imageFilterResponse = imageProcessor.process(input);

            // TODO: Explanation?
            final Explanation explanation = new Explanation(Collections.emptyList(), Collections.emptyList());

            binaryDocumentFilterResponse = new BinaryDocumentFilterResponse(imageFilterResponse.getImage(),
                    context, explanation);*/

        } else {
            // Should never happen but just in case.
            throw new Exception("Unknown mime type.");
        }

        return binaryDocumentFilterResult;

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

        final List<PostFilter> postFilters = new LinkedList<>();

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
