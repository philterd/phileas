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
import ai.philterd.phileas.model.filtering.RedactedText;
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
import ai.philterd.phileas.services.pdf.PdfRedactedText;
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
import java.util.HashMap;
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

        final boolean textOutput = options != null && options.isTextOutput();
        final PdfRedactedText redactedText = textOutput ? new PdfRedactedText(pdfTextExtractor.getPageCount()) : null;
        final Map<Integer, List<BoundingBox>> boxesByPage = new HashMap<>();

        if (textOutput) {
            // A page whose policy has no filters is not scanned, so its text is left out rather than returned as is.
            for (int page = 0; page < pdfTextExtractor.getPageCount(); page++) {
                final PdfPagePolicies.Assignment assignment = pagePolicies == null ? null : pagePolicies.assignmentFor(page + 1);
                if (filtersByAssignment.get(assignment).isEmpty()) {
                    redactedText.notScanned(page);
                }
            }
            for (final BoundingBox box : boundingBoxes(policy, pagePolicies)) {
                boxesByPage.computeIfAbsent(box.getPage() - 1, k -> new ArrayList<>()).add(box);
            }
        }

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

                // Before the spans are moved to document offsets below. Text under a bounding box is left out.
                if (textOutput) {
                    redactedText.addLine(pdfLine.getPageNumber(), lineNumber, !pdfLine.getText().isBlank(),
                            PdfRedactedText.redactLine(pdfLine.getText(), textFilterResult.getFilteredText(),
                                    textFilterResult.getReplacedRanges(), textFilterResult.getExplanation().appliedSpans(),
                                    pdfLine.coveredBy(boxesByPage.getOrDefault(pdfLine.getPageNumber(), List.of()))),
                            pdfLine.getSeparatorAfter());
                }

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

        if (textOutput) {

            if (options.isTextMetadata()) {
                tokens += redactMetadata(policy, filtersByAssignment.get(null), postFiltersByAssignment.get(null),
                        contextService, vectorService, context, pdfTextExtractor, redactedText, incrementalRedactions);
            }

            // The document is not rendered: the text is the output.
            final RedactedText text = redactedText.build();

            final LeftoverCheckResult leftoverCheck = LeftoverChecker.isDue(policy, options)
                    ? runLeftoverCheck(() -> textLeftoverCheck(policy, pagePolicies, redactedText, options)) : null;

            return new BinaryDocumentFilterResult(null, text, context, new Explanation(spansList, spansList), tokens,
                    incrementalRedactions, reviewSampled, leftoverCheck, null);

        }

        // Load the PDF config from the policy and apply to the PdfRedactionOptions that are used when
        // generating the new PDF document from the result of the redaction
        final PdfRedactionOptions pdfRedactionOptions = redactionOptions(policy.getConfig().getPdf());

        final byte[] redacted = render(policy, boundingBoxes(policy, pagePolicies), spansList, pdfRedactionOptions,
                input, outputMimeType);

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

    /**
     * Writes the redacted document. A call for text output does not call it.
     * @param policy The policy whose {@code config.pdf} settings render the output.
     * @param boundingBoxes The bounding boxes to draw.
     * @param spans The spans to redact.
     * @param pdfRedactionOptions The rendering options.
     * @param input The input document.
     * @param outputMimeType The output {@link MimeType}.
     * @return The redacted document.
     * @throws Exception Thrown if the document cannot be written.
     */
    protected byte[] render(final Policy policy, final List<BoundingBox> boundingBoxes, final List<Span> spans,
                            final PdfRedactionOptions pdfRedactionOptions, final byte[] input,
                            final MimeType outputMimeType) throws Exception {
        return new PdfRedactor(policy, boundingBoxes, spans, pdfRedactionOptions).process(input, outputMimeType);
    }

    /*
     * Redacts the document information entries, as "key: value" lines, with the document's policy, and sets them as
     * the metadata segment. Their spans are not added to the explanation, since they are not in a page.
     * Returns the tokens counted.
     */
    private long redactMetadata(final Policy policy, final List<Filter> filters, final List<PostFilter> postFilters,
                                final ContextService contextService, final VectorService vectorService,
                                final String context, final PdfTextExtractor extractor,
                                final PdfRedactedText redactedText,
                                final List<IncrementalRedaction> incrementalRedactions) throws Exception {

        if (filters.isEmpty()) {
            redactedText.metadataNotScanned();
            return 0;
        }

        final List<String> lines = new ArrayList<>();
        for (final Map.Entry<String, String> entry : extractor.getDocumentInformation().entrySet()) {
            // One line per entry, so a value's line breaks become spaces.
            lines.add(entry.getKey() + ": " + entry.getValue().replaceAll("\\R", " "));
        }
        final String metadata = String.join(PdfRedactedText.LINE_SEPARATOR, lines);

        if (metadata.isEmpty()) {
            redactedText.setMetadata(new PdfRedactedText.Line("", List.of()));
            return 0;
        }

        final TextFilterResult result = unstructuredDocumentProcessor.process(contextService, vectorService, policy,
                filters, postFilters, context, 0, metadata);
        incrementalRedactions.addAll(result.getIncrementalRedactions());
        redactedText.setMetadata(new PdfRedactedText.Line(result.getFilteredText(), result.getReplacedRanges()));

        return tokenCounter.countTokens(metadata);

    }

    /*
     * Scans the text output, each page's text whole with the check built from its page's policy and the metadata
     * with the document's, leaving out what the redaction inserted. A finding's offsets are into its segment.
     */
    private LeftoverCheckResult textLeftoverCheck(final Policy policy, final PdfPagePolicies pagePolicies,
                                                  final PdfRedactedText redactedText,
                                                  final FilterOptions options) throws Exception {

        final List<PdfRedactedText.Part> parts = redactedText.parts();

        if (parts.isEmpty()) {
            return LeftoverCheckResult.notPerformed(LeftoverCheckResult.Reason.UNREADABLE_OUTPUT);
        }

        final LeftoverCheck.Using using = leftoverCheckUsing(policy);
        final List<LeftoverFinding> findings = new ArrayList<>();

        for (final PdfRedactedText.Part part : parts) {

            final PdfPagePolicies.Assignment assignment = pagePolicies == null || part.pageNumber() < 0
                    ? null : pagePolicies.assignmentFor(part.pageNumber() + 1);
            final Policy partPolicy = assignment == null ? policy : assignment.policy();

            for (final Span span : leftoverSpans(partPolicy, using, part.text())) {
                if (!LeftoverChecker.isInserted(span, part.replacedRanges())) {
                    findings.add(LeftoverChecker.finding(span, part.text(), options, Math.max(part.pageNumber(), 0),
                            part.lineNumberAt(span.getCharacterStart()), 0).inSegment(part.label()));
                }
            }

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
