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
package ai.philterd.phileas.services.filters.filtering;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.filters.Filter;
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.Explanation;
import ai.philterd.phileas.model.filtering.IncrementalRedaction;
import ai.philterd.phileas.model.filtering.LeftoverCheckResult;
import ai.philterd.phileas.model.filtering.LeftoverFinding;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.config.Pdf;
import ai.philterd.phileas.policy.graphical.BoundingBox;
import ai.philterd.phileas.policy.config.LeftoverCheck;
import ai.philterd.phileas.services.LeftoverChecker;
import ai.philterd.phileas.services.ReviewSampler;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.disambiguation.SpanDisambiguationServiceFactory;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.documentprocessors.DocumentProcessor;
import ai.philterd.phileas.services.documentprocessors.UnstructuredDocumentProcessor;
import ai.philterd.phileas.services.filters.postfilters.PostFilter;
import ai.philterd.phileas.services.pdf.PdfLine;
import ai.philterd.phileas.services.pdf.PdfPagePolicies;
import ai.philterd.phileas.services.pdf.PdfRedactionOptions;
import ai.philterd.phileas.services.pdf.PdfRedactor;
import ai.philterd.phileas.services.pdf.PdfTextExtractor;
import ai.philterd.phileas.services.tokens.TokenCounter;
import ai.philterd.phileas.services.tokens.WhitespaceTokenCounter;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link FilterService} that filters PDF documents.
 */
public class PdfFilterService extends BinaryFilterService {

	private static final Logger LOGGER = LogManager.getLogger(PdfFilterService.class);

    private final DocumentProcessor unstructuredDocumentProcessor;
    private final TokenCounter tokenCounter;
    private final int maxPages;
    private final long maxPixelsPerPage;
    private final long maxStreamBytes;

    // Bound at construction for the no-service overload; null for a warm, per-call instance.
    private final ContextService defaultContextService;
    private final VectorService defaultVectorService;

    public PdfFilterService(final PhileasConfiguration phileasConfiguration,
                            final ContextService contextService,
                            final VectorService vectorService,
                            final HttpClient httpClient) {

        this(phileasConfiguration, contextService, vectorService, new SecureRandom(), httpClient);

    }

    public PdfFilterService(final PhileasConfiguration phileasConfiguration,
                            final ContextService contextService,
                            final VectorService vectorService,
                            final SecureRandom random,
                            final HttpClient httpClient) {

        super(phileasConfiguration, random, httpClient);

        LOGGER.info("Initializing PDF filter service.");

        this.maxPages = phileasConfiguration.pdfMaxPages();
        this.maxPixelsPerPage = phileasConfiguration.pdfMaxPixelsPerPage();
        this.maxStreamBytes = phileasConfiguration.pdfMaxStreamBytes();

        this.defaultContextService = contextService;
        this.defaultVectorService = vectorService;

        this.tokenCounter = new WhitespaceTokenCounter();

        this.unstructuredDocumentProcessor = new UnstructuredDocumentProcessor(
                SpanDisambiguationServiceFactory.getSpanDisambiguationService(phileasConfiguration),
                phileasConfiguration.incrementalRedactionsEnabled()
        );

    }

    /** Creates a warm, reusable service whose context and vector services are supplied per call. */
    public PdfFilterService(final PhileasConfiguration phileasConfiguration,
                            final HttpClient httpClient) {

        this(phileasConfiguration, new SecureRandom(), httpClient);

    }

    /** Warm, reusable service using the given thread-safe {@link SecureRandom} for anonymization. */
    public PdfFilterService(final PhileasConfiguration phileasConfiguration,
                            final SecureRandom random,
                            final HttpClient httpClient) {

        super(phileasConfiguration, random, httpClient);

        LOGGER.info("Initializing PDF filter service.");

        this.maxPages = phileasConfiguration.pdfMaxPages();
        this.maxPixelsPerPage = phileasConfiguration.pdfMaxPixelsPerPage();
        this.maxStreamBytes = phileasConfiguration.pdfMaxStreamBytes();

        this.defaultContextService = null;
        this.defaultVectorService = null;

        this.tokenCounter = new WhitespaceTokenCounter();

        this.unstructuredDocumentProcessor = new UnstructuredDocumentProcessor(
                SpanDisambiguationServiceFactory.getSpanDisambiguationService(phileasConfiguration),
                phileasConfiguration.incrementalRedactionsEnabled()
        );

    }

    @Override
    public BinaryDocumentFilterResult filter(final Policy policy, final String context,
                                             final byte[] input, final MimeType outputMimeType) throws Exception {
        requireBoundServices();
        return filter(policy, defaultContextService, defaultVectorService, context, input, outputMimeType);
    }

    // Fails fast when a warm instance is used through an overload that needs construction-bound services.
    private void requireBoundServices() {
        if (defaultContextService == null || defaultVectorService == null) {
            throw new IllegalStateException(
                    "This PdfFilterService was created for per-call context and vector services. "
                            + "Call filter(policy, contextService, vectorService, context, input, outputMimeType) instead, "
                            + "or construct it with a ContextService and VectorService.");
        }
    }

    @Override
    public BinaryDocumentFilterResult filter(final Policy policy, final ContextService contextService,
                                             final VectorService vectorService, final String context,
                                             final byte[] input, final MimeType outputMimeType,
                                             final FilterOptions options) throws Exception {
        return filter(policy, contextService, vectorService, context, input, outputMimeType, null, options);
    }

    /**
     * Filters a PDF with a policy per page set.
     * @param policy The policy for pages not in {@code pagePolicies}. Its {@code config.pdf} settings render the output.
     * @param pagePolicies The policies assigned to page sets, or {@code null} to use {@code policy} for every page.
     * @throws IllegalArgumentException before redaction if a page set extends beyond the document's last page.
     */
    public BinaryDocumentFilterResult filter(final Policy policy, final String context, final byte[] input,
                                             final MimeType outputMimeType, final PdfPagePolicies pagePolicies) throws Exception {
        requireBoundServices();
        return filter(policy, defaultContextService, defaultVectorService, context, input, outputMimeType, pagePolicies);
    }

    /**
     * Filters a PDF with a policy per page set, using the services bound at construction and options for the call.
     * @param policy The policy for pages not in {@code pagePolicies}. Its {@code config.pdf} settings render the
     *               output, and its {@code config.review} settings apply to the document.
     * @param pagePolicies The policies assigned to page sets, or {@code null} to use {@code policy} for every page.
     * @param options The {@link FilterOptions} for this call.
     * @throws IllegalArgumentException before redaction if a page set extends beyond the document's last page.
     */
    public BinaryDocumentFilterResult filter(final Policy policy, final String context, final byte[] input,
                                             final MimeType outputMimeType, final PdfPagePolicies pagePolicies,
                                             final FilterOptions options) throws Exception {
        requireBoundServices();
        return filter(policy, defaultContextService, defaultVectorService, context, input, outputMimeType, pagePolicies, options);
    }

    /**
     * Filters a PDF with a policy per page set. One context and context service are used for the whole document.
     * @param policy The policy for pages not in {@code pagePolicies}. Its {@code config.pdf} settings render the output.
     * @param pagePolicies The policies assigned to page sets, or {@code null} to use {@code policy} for every page.
     * @throws IllegalArgumentException before redaction if a page set extends beyond the document's last page.
     */
    public BinaryDocumentFilterResult filter(final Policy policy, final ContextService contextService,
                                             final VectorService vectorService, final String context,
                                             final byte[] input, final MimeType outputMimeType,
                                             final PdfPagePolicies pagePolicies) throws Exception {
        return filter(policy, contextService, vectorService, context, input, outputMimeType, pagePolicies, FilterOptions.defaults());
    }

    /**
     * Filters a PDF with a policy per page set and options for the call. One context and context service are
     * used for the whole document.
     * @param policy The policy for pages not in {@code pagePolicies}. Its {@code config.pdf} settings render the
     *               output, and its {@code config.review} settings apply to the document.
     * @param pagePolicies The policies assigned to page sets, or {@code null} to use {@code policy} for every page.
     * @param options The {@link FilterOptions} for this call.
     * @throws IllegalArgumentException before redaction if a page set extends beyond the document's last page.
     */
    public BinaryDocumentFilterResult filter(final Policy policy, final ContextService contextService,
                                             final VectorService vectorService, final String context,
                                             final byte[] input, final MimeType outputMimeType,
                                             final PdfPagePolicies pagePolicies, final FilterOptions options) throws Exception {

        // One draw for the document, with the policy that also sets its rendering, not one per page or page policy.
        final boolean reviewSampled = ReviewSampler.isSelected(policy);

        final List<IncrementalRedaction> incrementalRedactions = new ArrayList<>();

        // Get the lines of text from the PDF file.
        final PdfTextExtractor pdfTextExtractor = new PdfTextExtractor(maxPages, maxPixelsPerPage, maxStreamBytes);
        final List<PdfLine> pdfLines = pdfTextExtractor.getLines(input);

        if (pagePolicies != null) {
            pagePolicies.validate(pdfTextExtractor.getPageCount());
        }

        // A list of identified spans whose start/end are relative to the whole document.
        final Set<Span> nonRelativeSpans = new LinkedHashSet<>();

        // Track the document offset.
        int offset = 0;

        // Track the line number.
        int lineNumber = 1;

        // The filters and post-filters of each policy, keyed by its assignment (null for the default). All are
        // loaded before any text is filtered, so an invalid policy fails even on a document with no text.
        final Map<PdfPagePolicies.Assignment, List<Filter>> filtersByAssignment = new IdentityHashMap<>();
        final Map<PdfPagePolicies.Assignment, List<PostFilter>> postFiltersByAssignment = new IdentityHashMap<>();
        filtersByAssignment.put(null, filterPolicyLoader.getFiltersForPolicy(policy, filterCache));
        postFiltersByAssignment.put(null, getPostFiltersForPolicy(policy));
        if (pagePolicies != null) {
            for (final PdfPagePolicies.Assignment assignment : pagePolicies.getAssignments()) {
                filtersByAssignment.put(assignment, filterPolicyLoader.getFiltersForPolicy(assignment.policy(), filterCache));
                postFiltersByAssignment.put(assignment, getPostFiltersForPolicy(assignment.policy()));
            }
        }

        long tokens = 0;

        // Process each line looking for sensitive information in each line.
        for (final PdfLine pdfLine : pdfLines) {

            // Line page numbers count from 0; page sets count from 1.
            final PdfPagePolicies.Assignment assignment =
                    pagePolicies == null ? null : pagePolicies.assignmentFor(pdfLine.getPageNumber() + 1);
            final Policy linePolicy = assignment == null ? policy : assignment.policy();

            final List<Filter> filters = filtersByAssignment.get(assignment);

            // A policy may have no filters, only bounding boxes, so there is nothing to look for in its text.
            if (!filters.isEmpty()) {

                final int piece = 0;
                tokens += tokenCounter.countTokens(pdfLine.getText());

                // Process the text.
                final TextFilterResult textFilterResult = unstructuredDocumentProcessor.process(contextService, vectorService,
                        linePolicy, filters, postFiltersByAssignment.get(assignment), context, piece, pdfLine.getText());

                // Add the incremental redactions to the list.
                incrementalRedactions.addAll(textFilterResult.getIncrementalRedactions());

                for (final Span span : textFilterResult.getExplanation().appliedSpans()) {

                    final int characterStart = span.getCharacterStart() + offset;
                    final int characterEnd = span.getCharacterEnd() + offset;

                    span.setCharacterStart(characterStart);
                    span.setCharacterEnd(characterEnd);
                    span.setLineNumber(lineNumber);
                    span.setPageNumber(pdfLine.getPageNumber());
                    span.setLineHash(pdfLine.getLineHash());

                    if (pagePolicies != null) {
                        span.setPageAssignment(assignment == null ? PdfPagePolicies.DEFAULT_ASSIGNMENT : assignment.label());
                    }

                    nonRelativeSpans.add(span);

                }

            }

            lineNumber++;
            offset += pdfLine.getText().length();

        }

        final List<Span> spansList = new ArrayList<>(nonRelativeSpans);

        // Load the PDF config from the policy and apply to the PdfRedactionOptions that are used when
        // generating the new PDF document from the result of the redaction
        final PdfRedactionOptions pdfRedactionOptions = redactionOptions(policy.getConfig().getPdf());

        final PdfRedactor redacter = new PdfRedactor(policy, boundingBoxes(policy, pagePolicies), spansList, pdfRedactionOptions);
        final byte[] redacted = redacter.process(input, outputMimeType);

        // TODO: The identified vs the applied will actually be different
        // but we are setting the same here. Fix this at some point.
        final Explanation explanation = new Explanation(spansList, spansList);

        final LeftoverCheckResult leftoverCheck = LeftoverChecker.isDue(policy, options)
                ? runLeftoverCheck(() -> leftoverCheck(policy, pagePolicies, redacted, outputMimeType, options)) : null;

        return new BinaryDocumentFilterResult(redacted, context, explanation, tokens, incrementalRedactions,
                reviewSampled, leftoverCheck);

    }

    /*
     * Scans the text of the redacted PDF line by line, as the redaction does, each line with the check built from
     * its page's policy. A page with a redaction is flattened to an image, so only pages kept as they were have
     * text to read, and none of it was inserted by the redaction. An output with no text at all is reported as
     * not checked.
     */
    private LeftoverCheckResult leftoverCheck(final Policy policy, final PdfPagePolicies pagePolicies,
                                              final byte[] redacted, final MimeType outputMimeType,
                                              final FilterOptions options) throws Exception {

        if (outputMimeType != MimeType.APPLICATION_PDF) {
            return LeftoverCheckResult.notPerformed(LeftoverCheckResult.Reason.UNREADABLE_OUTPUT);
        }

        final List<PdfLine> lines = new PdfTextExtractor(maxPages, maxPixelsPerPage, maxStreamBytes).getLines(redacted);

        if (lines.stream().allMatch(line -> line.getText().isBlank())) {
            return LeftoverCheckResult.notPerformed(LeftoverCheckResult.Reason.UNREADABLE_OUTPUT);
        }

        // The document's policy decides what the check uses on every page.
        final LeftoverCheck.Using using = leftoverCheckUsing(policy);

        final List<LeftoverFinding> findings = new ArrayList<>();
        int offset = 0;
        int lineNumber = 1;

        for (final PdfLine line : lines) {

            final PdfPagePolicies.Assignment assignment =
                    pagePolicies == null ? null : pagePolicies.assignmentFor(line.getPageNumber() + 1);
            final Policy linePolicy = assignment == null ? policy : assignment.policy();

            for (final Span span : leftoverSpans(linePolicy, using, line.getText())) {
                final LeftoverFinding finding = LeftoverChecker.finding(span, line.getText(), options,
                        line.getPageNumber(), lineNumber, 0);
                findings.add(new LeftoverFinding(finding.getFilterType(), finding.getClassification(),
                        finding.getCharacterStart() + offset, finding.getCharacterEnd() + offset,
                        finding.getConfidence(), finding.getPageNumber(), finding.getLineNumber(), 0,
                        finding.getMatchedText()));
            }

            lineNumber++;
            offset += line.getText().length();

        }

        return LeftoverCheckResult.performed(findings);

    }

    // Each policy's bounding boxes on the pages that policy applies to.
    private static List<BoundingBox> boundingBoxes(final Policy policy, final PdfPagePolicies pagePolicies) {

        if (pagePolicies == null) {
            return policy.getGraphical().getBoundingBoxes();
        }

        final List<BoundingBox> boxes = new ArrayList<>();

        for (final BoundingBox box : policy.getGraphical().getBoundingBoxes()) {
            if (pagePolicies.assignmentFor(box.getPage()) == null) {
                boxes.add(box);
            }
        }

        for (final PdfPagePolicies.Assignment assignment : pagePolicies.getAssignments()) {
            for (final BoundingBox box : assignment.policy().getGraphical().getBoundingBoxes()) {
                if (assignment.contains(box.getPage())) {
                    boxes.add(box);
                }
            }
        }

        return boxes;

    }

    private PdfRedactionOptions redactionOptions(final Pdf policyPdfConfig) {
        final PdfRedactionOptions pdfRedactionOptions = new PdfRedactionOptions(
                policyPdfConfig.getDpi(),
                policyPdfConfig.getCompressionQuality(),
                policyPdfConfig.getPreserveUnredactedPages()
        );
        pdfRedactionOptions.setMaxPages(maxPages);
        pdfRedactionOptions.setMaxPixelsPerPage(maxPixelsPerPage);
        pdfRedactionOptions.setMaxStreamBytes(maxStreamBytes);
        return pdfRedactionOptions;
    }

    @Override
    public byte[] apply(final Policy policy, final byte[] input, final List<Span> spans, final MimeType outputMimeType) throws Exception {
        return apply(policy, input, spans, outputMimeType, null);
    }

    /**
     * Applies spans to a PDF, drawing each policy's bounding boxes only on the pages assigned to it.
     * @param policy The policy for pages not in {@code pagePolicies}. Its {@code config.pdf} settings render the output.
     * @param pagePolicies The policies assigned to page sets, or {@code null} to use {@code policy} for every page.
     */
    public byte[] apply(final Policy policy, final byte[] input, final List<Span> spans, final MimeType outputMimeType,
                        final PdfPagePolicies pagePolicies) throws Exception {

        // Load the PDF config from the policy and apply to the PdfRedactionOptions that are used when
        // generating the new PDF document from the result of the redaction
        final PdfRedactionOptions pdfRedactionOptions = redactionOptions(policy.getConfig().getPdf());

        // Redact those terms in the document along with any bounding boxes identified in the policy.
        final PdfRedactor redacter = new PdfRedactor(policy, boundingBoxes(policy, pagePolicies), spans, pdfRedactionOptions);
        return redacter.process(input, outputMimeType);

    }

}
