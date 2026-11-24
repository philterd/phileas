/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
import ai.philterd.phileas.services.ReviewSampler;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.SpanDisambiguationServiceFactory;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.documentprocessors.DocumentProcessor;
import ai.philterd.phileas.services.documentprocessors.UnstructuredDocumentProcessor;
import ai.philterd.phileas.services.filters.postfilters.PostFilter;
import ai.philterd.phileas.services.ooxml.OfficeRedactionSpan;
import ai.philterd.phileas.services.ooxml.OoxmlDocumentType;
import ai.philterd.phileas.services.ooxml.OoxmlRedactionOptions;
import ai.philterd.phileas.services.ooxml.OoxmlRedactionResult;
import ai.philterd.phileas.services.ooxml.TextFilter;
import ai.philterd.phileas.services.ooxml.WordDocumentRedactor;
import ai.philterd.phileas.services.ooxml.XlsxRedactor;
import ai.philterd.phileas.services.tokens.TokenCounter;
import ai.philterd.phileas.services.tokens.WhitespaceTokenCounter;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Redacts Word (.docx) and Excel (.xlsx) documents with a policy. The output is a document of the same
 * format, still editable. Spans are returned with offsets relative to their paragraph or cell and
 * {@link Span#getParagraphNumber()} set to its index in the document's canonical order; {@link #apply}
 * re-applies such spans by position.
 */
public class OoxmlFilterService extends BinaryFilterService {

    private static final Logger LOGGER = LogManager.getLogger(OoxmlFilterService.class);

    private final DocumentProcessor unstructuredDocumentProcessor;
    private final TokenCounter tokenCounter;
    private final OoxmlRedactionOptions options;

    // Bound at construction for the no-service overloads; null for a warm, per-call instance.
    private final ContextService defaultContextService;
    private final VectorService defaultVectorService;

    public OoxmlFilterService(final PhileasConfiguration phileasConfiguration,
                              final ContextService contextService,
                              final VectorService vectorService,
                              final HttpClient httpClient) {
        this(phileasConfiguration, contextService, vectorService, new SecureRandom(), httpClient, OoxmlRedactionOptions.defaults());
    }

    public OoxmlFilterService(final PhileasConfiguration phileasConfiguration,
                              final ContextService contextService,
                              final VectorService vectorService,
                              final SecureRandom random,
                              final HttpClient httpClient,
                              final OoxmlRedactionOptions options) {
        super(phileasConfiguration, random, httpClient);
        LOGGER.info("Initializing Word and Excel filter service.");
        this.defaultContextService = contextService;
        this.defaultVectorService = vectorService;
        this.options = options == null ? OoxmlRedactionOptions.defaults() : options;
        this.tokenCounter = new WhitespaceTokenCounter();
        this.unstructuredDocumentProcessor = new UnstructuredDocumentProcessor(
                SpanDisambiguationServiceFactory.getSpanDisambiguationService(phileasConfiguration),
                phileasConfiguration.incrementalRedactionsEnabled());
    }

    /** Creates a warm, reusable service whose context and vector services are supplied per call. */
    public OoxmlFilterService(final PhileasConfiguration phileasConfiguration,
                              final HttpClient httpClient) {
        this(phileasConfiguration, new SecureRandom(), httpClient, OoxmlRedactionOptions.defaults());
    }

    /** Warm, reusable service with the given options and thread-safe {@link SecureRandom}. */
    public OoxmlFilterService(final PhileasConfiguration phileasConfiguration,
                              final SecureRandom random,
                              final HttpClient httpClient,
                              final OoxmlRedactionOptions options) {
        this(phileasConfiguration, null, null, random, httpClient, options);
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
                    "This OoxmlFilterService was created for per-call context and vector services. "
                            + "Call filter(policy, contextService, vectorService, context, input, outputMimeType) instead, "
                            + "or construct it with a ContextService and VectorService.");
        }
    }

    @Override
    public BinaryDocumentFilterResult filter(final Policy policy, final ContextService contextService,
                                             final VectorService vectorService, final String context,
                                             final byte[] input, final MimeType outputMimeType) throws Exception {

        // One draw for the document, not one per paragraph or cell.
        final boolean reviewSampled = ReviewSampler.isSelected(policy);

        final OoxmlDocumentType type = documentType(input, outputMimeType);

        final List<IncrementalRedaction> incrementalRedactions = new ArrayList<>();
        final long[] tokens = {0};
        final TextFilter textFilter = textFilter(policy, contextService, vectorService, context, incrementalRedactions, tokens);

        final OoxmlRedactionResult result = type == OoxmlDocumentType.DOCX
                ? WordDocumentRedactor.redact(input, textFilter, options)
                : XlsxRedactor.redact(input, textFilter, options);

        final List<Span> spans = new ArrayList<>();
        for (final OfficeRedactionSpan officeSpan : result.spans()) {
            final Span span = officeSpan.toSpan();
            if (span != null) {
                spans.add(span);
            }
        }

        // As with PDF, the identified and applied spans are reported as the same list.
        final Explanation explanation = new Explanation(spans, spans);
        return new BinaryDocumentFilterResult(result.document(), context, explanation, tokens[0], incrementalRedactions, reviewSampled);

    }

    /**
     * Applies spans by position (their paragraph number and offsets), then re-redacts the content spans
     * cannot be placed in (drawings, hyperlink targets, field instructions, tracked deletions, charts,
     * comments and headers in Excel, pivot caches, and embedded objects) with the policy. Uses the
     * construction-bound context and vector services when there are any, otherwise new in-memory ones.
     */
    @Override
    public byte[] apply(final Policy policy, final byte[] input, final List<Span> spans, final MimeType outputMimeType) throws Exception {
        final ContextService contextService = defaultContextService != null ? defaultContextService : new DefaultContextService();
        final VectorService vectorService = defaultVectorService != null ? defaultVectorService : new InMemoryVectorService();
        return apply(policy, contextService, vectorService, "default", input, spans, outputMimeType);
    }

    /** As {@link #apply(Policy, byte[], List, MimeType)}, with the context and vector services given. */
    public byte[] apply(final Policy policy, final ContextService contextService, final VectorService vectorService,
                        final String context, final byte[] input, final List<Span> spans,
                        final MimeType outputMimeType) throws Exception {

        final OoxmlDocumentType type = documentType(input, outputMimeType);

        final List<OfficeRedactionSpan> officeSpans = new ArrayList<>();
        for (final Span span : spans) {
            officeSpans.add(OfficeRedactionSpan.fromSpan(span));
        }

        final TextFilter textFilter = textFilter(policy, contextService, vectorService, context, new ArrayList<>(), new long[1]);

        return type == OoxmlDocumentType.DOCX
                ? WordDocumentRedactor.applySpans(input, officeSpans, options, textFilter)
                : XlsxRedactor.applySpans(input, officeSpans, options, textFilter);

    }

    private TextFilter textFilter(final Policy policy, final ContextService contextService, final VectorService vectorService,
                                  final String context, final List<IncrementalRedaction> incrementalRedactions,
                                  final long[] tokens) throws Exception {
        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);
        final List<PostFilter> postFilters = getPostFiltersForPolicy(policy);
        return text -> {
            tokens[0] += tokenCounter.countTokens(text);
            try {
                final TextFilterResult result = unstructuredDocumentProcessor.process(contextService, vectorService,
                        policy, filters, postFilters, context, 0, text);
                incrementalRedactions.addAll(result.getIncrementalRedactions());
                return result;
            } catch (final Exception e) {
                throw new IllegalStateException("Unable to filter document text.", e);
            }
        };
    }

    // The input's format, which must also be the requested output format.
    private static OoxmlDocumentType documentType(final byte[] input, final MimeType outputMimeType) {
        final OoxmlDocumentType type = OoxmlDocumentType.of(input);
        if (type == null) {
            throw new IllegalArgumentException("The document is not a Word (.docx) or Excel (.xlsx) document.");
        }
        if (outputMimeType != null && outputMimeType != type.mimeType()) {
            throw new IllegalArgumentException("A " + type.mimeType() + " document can only be redacted to "
                    + type.mimeType() + ", not " + outputMimeType + ".");
        }
        return type;
    }

}
