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
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.config.Pdf;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.disambiguation.SpanDisambiguationServiceFactory;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.documentprocessors.DocumentProcessor;
import ai.philterd.phileas.services.documentprocessors.UnstructuredDocumentProcessor;
import ai.philterd.phileas.services.filters.postfilters.PostFilter;
import ai.philterd.phileas.services.pdf.PdfLine;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link FilterService} that filters PDF documents.
 */
public class PdfFilterService extends BinaryFilterService {

	private static final Logger LOGGER = LogManager.getLogger(PdfFilterService.class);

    private final DocumentProcessor unstructuredDocumentProcessor;
    private final TokenCounter tokenCounter;

    // The context and vector services bound at construction, used by the no-service filter overload.
    // Null when the instance was created with a service-less constructor for warm, per-call reuse.
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

        this.defaultContextService = contextService;
        this.defaultVectorService = vectorService;

        // Set the token counter.
        this.tokenCounter = new WhitespaceTokenCounter();

        // Create a new unstructured document processor. The vector service is supplied per call, so
        // the disambiguation service is built once here without one.
        this.unstructuredDocumentProcessor = new UnstructuredDocumentProcessor(
                SpanDisambiguationServiceFactory.getSpanDisambiguationService(phileasConfiguration),
                phileasConfiguration.incrementalRedactionsEnabled()
        );

    }

    /**
     * Creates a warm, reusable service whose context and vector services are supplied per call.
     * @param phileasConfiguration The {@link PhileasConfiguration}.
     * @param httpClient The {@link HttpClient}.
     */
    public PdfFilterService(final PhileasConfiguration phileasConfiguration,
                            final HttpClient httpClient) {

        this(phileasConfiguration, new SecureRandom(), httpClient);

    }

    /**
     * Creates a warm, reusable service whose context and vector services are supplied per call, using
     * the given {@link SecureRandom} for anonymization. The instance is shared across requests, so the
     * {@link SecureRandom} must be thread-safe (the default {@link SecureRandom} is).
     * @param phileasConfiguration The {@link PhileasConfiguration}.
     * @param random The {@link SecureRandom} used for anonymization.
     * @param httpClient The {@link HttpClient}.
     */
    public PdfFilterService(final PhileasConfiguration phileasConfiguration,
                            final SecureRandom random,
                            final HttpClient httpClient) {

        super(phileasConfiguration, random, httpClient);

        LOGGER.info("Initializing PDF filter service.");

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
        return filter(policy, defaultContextService, defaultVectorService, context, input, outputMimeType);
    }

    @Override
    public BinaryDocumentFilterResult filter(final Policy policy, final ContextService contextService,
                                             final VectorService vectorService, final String context,
                                             final byte[] input, final MimeType outputMimeType) throws Exception {

        final List<IncrementalRedaction> incrementalRedactions = new ArrayList<>();

        // Get the lines of text from the PDF file.
        final PdfTextExtractor pdfTextExtractor = new PdfTextExtractor();
        final List<PdfLine> pdfLines = pdfTextExtractor.getLines(input);

        // A list of identified spans whose start/end are relative to the whole document.
        final Set<Span> nonRelativeSpans = new LinkedHashSet<>();

        // Track the document offset.
        int offset = 0;

        // Track the line number.
        int lineNumber = 1;

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);
        final List<PostFilter> postFilters = getPostFiltersForPolicy(policy);

        long tokens = 0;

        // If there are no filters in the policy, we don't need to process the text.
        // This can be the case if the policy contains bounding boxes but no filters.
        if(!filters.isEmpty()) {

            // Process each line looking for sensitive information in each line.
            for (final PdfLine pdfLine : pdfLines) {

                final int piece = 0;
                tokens += tokenCounter.countTokens(pdfLine.getText());

                // Process the text.
                final TextFilterResult textFilterResult = unstructuredDocumentProcessor.process(contextService, vectorService, policy, filters, postFilters, context, piece, pdfLine.getText());

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

                    nonRelativeSpans.add(span);

                }

                lineNumber++;
                offset += pdfLine.getText().length();

            }

        }

        final List<Span> spansList = new ArrayList<>(nonRelativeSpans);

        // Load the PDF config from the policy and apply to the PdfRedactionOptions that are used when
        // generating the new PDF document from the result of the redaction
        final Pdf policyPdfConfig = policy.getConfig().getPdf();
        final PdfRedactionOptions pdfRedactionOptions = new PdfRedactionOptions(
                policyPdfConfig.getDpi(),
                policyPdfConfig.getCompressionQuality(),
                policyPdfConfig.getScale(),
                policyPdfConfig.getPreserveUnredactedPages()
        );

        final PdfRedactor redacter = new PdfRedactor(policy, spansList, pdfRedactionOptions);
        final byte[] redacted = redacter.process(input, outputMimeType);

        // TODO: The identified vs the applied will actually be different
        // but we are setting the same here. Fix this at some point.
        final Explanation explanation = new Explanation(spansList, spansList);
        return new BinaryDocumentFilterResult(redacted, context, explanation, tokens, incrementalRedactions);

    }

    @Override
    public byte[] apply(final Policy policy, final byte[] input, final List<Span> spans, final MimeType outputMimeType) throws Exception {

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
        final PdfRedactor redacter = new PdfRedactor(policy, spans, pdfRedactionOptions);
        return redacter.process(input, outputMimeType);

    }

}
