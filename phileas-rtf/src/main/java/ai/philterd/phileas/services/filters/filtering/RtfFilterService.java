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
import ai.philterd.phileas.model.filtering.LeftoverCheckResult;
import ai.philterd.phileas.model.filtering.LeftoverFinding;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.ReplacedRange;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.config.LeftoverCheck;
import ai.philterd.phileas.policy.config.Word;
import ai.philterd.phileas.services.LeftoverChecker;
import ai.philterd.phileas.services.ReviewSampler;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DocumentContextService;
import ai.philterd.phileas.services.disambiguation.SpanDisambiguationServiceFactory;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.documentprocessors.DocumentProcessor;
import ai.philterd.phileas.services.documentprocessors.UnstructuredDocumentProcessor;
import ai.philterd.phileas.services.filters.postfilters.PostFilter;
import ai.philterd.phileas.services.rtf.RtfRedactor;
import ai.philterd.phileas.services.tokens.TokenCounter;
import ai.philterd.phileas.services.tokens.WhitespaceTokenCounter;
import org.apache.hc.client5.http.classic.HttpClient;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Redacts RTF (.rtf) documents with a policy, applying the policy's {@code config.word} settings that RTF has a
 * counterpart for. The output is RTF, with text replaced in place and the rest of the document written back as it
 * was. Spans have offsets into their paragraph, cell, or other run of text and its index as their
 * {@link Span#getParagraphNumber() paragraph number}; {@link #apply} re-applies such spans.
 */
public class RtfFilterService extends BinaryFilterService {

    private final DocumentProcessor unstructuredDocumentProcessor;
    private final TokenCounter tokenCounter;

    // Bound at construction for the no-service overloads; null for a warm, per-call instance.
    private final ContextService defaultContextService;
    private final VectorService defaultVectorService;

    public RtfFilterService(final PhileasConfiguration phileasConfiguration,
                            final ContextService contextService,
                            final VectorService vectorService,
                            final HttpClient httpClient) {
        this(phileasConfiguration, contextService, vectorService, new SecureRandom(), httpClient);
    }

    public RtfFilterService(final PhileasConfiguration phileasConfiguration,
                            final ContextService contextService,
                            final VectorService vectorService,
                            final SecureRandom random,
                            final HttpClient httpClient) {
        super(phileasConfiguration, random, httpClient);
        this.defaultContextService = contextService;
        this.defaultVectorService = vectorService;
        this.tokenCounter = new WhitespaceTokenCounter();
        this.unstructuredDocumentProcessor = new UnstructuredDocumentProcessor(
                SpanDisambiguationServiceFactory.getSpanDisambiguationService(phileasConfiguration),
                phileasConfiguration.incrementalRedactionsEnabled());
    }

    /** Creates a warm, reusable service whose context and vector services are supplied per call. */
    public RtfFilterService(final PhileasConfiguration phileasConfiguration, final HttpClient httpClient) {
        this(phileasConfiguration, null, null, new SecureRandom(), httpClient);
    }

    @Override
    public BinaryDocumentFilterResult filter(final Policy policy, final String context, final byte[] input,
                                             final MimeType outputMimeType) throws Exception {
        requireBoundServices();
        return filter(policy, defaultContextService, defaultVectorService, context, input, outputMimeType);
    }

    /**
     * Filters an RTF document using the services bound at construction, with options for the call.
     * @param policy The {@link Policy} to apply.
     * @param context The redaction context.
     * @param input The RTF document.
     * @param outputMimeType {@link MimeType#APPLICATION_RTF} or <code>null</code>.
     * @param filterOptions The {@link FilterOptions} for this call.
     * @return A {@link BinaryDocumentFilterResult}.
     * @throws Exception Thrown if the document cannot be filtered.
     */
    public BinaryDocumentFilterResult filter(final Policy policy, final String context, final byte[] input,
                                             final MimeType outputMimeType, final FilterOptions filterOptions) throws Exception {
        requireBoundServices();
        return filter(policy, defaultContextService, defaultVectorService, context, input, outputMimeType, filterOptions);
    }

    private void requireBoundServices() {
        if (defaultContextService == null || defaultVectorService == null) {
            throw new IllegalStateException(
                    "This RtfFilterService was created for per-call context and vector services. "
                            + "Call filter(policy, contextService, vectorService, context, input, outputMimeType) instead, "
                            + "or construct it with a ContextService and VectorService.");
        }
    }

    @Override
    public BinaryDocumentFilterResult filter(final Policy policy, final ContextService callerContextService,
                                             final VectorService vectorService, final String context,
                                             final byte[] input, final MimeType outputMimeType,
                                             final FilterOptions filterOptions) throws Exception {

        // One map of DOCUMENT-scoped replacements for the whole call, apart from the caller's context.
        final ContextService contextService = DocumentContextService.forDocument(callerContextService);

        // A call for text has no output document, so its type is not checked.
        if (filterOptions == null || !filterOptions.isTextOutput()) {
            requireRtf(outputMimeType);
        }

        final boolean reviewSampled = ReviewSampler.isSelected(policy);
        final List<IncrementalRedaction> incrementalRedactions = new ArrayList<>();
        final long[] tokens = {0};
        final Word word = policy.getConfig().getWord();

        if (filterOptions != null && filterOptions.isTextOutput()) {
            final RtfRedactor.TextResult result = RtfRedactor.redactToText(input,
                    textFilter(policy, contextService, vectorService, context, incrementalRedactions, tokens), word,
                    filterOptions.isTextMetadata());
            final LeftoverCheckResult leftoverCheck = LeftoverChecker.isDue(policy, filterOptions)
                    ? runLeftoverCheck(() -> textLeftoverCheck(policy, result.text().parts(), filterOptions)) : null;
            return new BinaryDocumentFilterResult(null, result.text().build(), context,
                    new Explanation(result.spans(), result.spans()), tokens[0], incrementalRedactions, reviewSampled,
                    leftoverCheck, null);
        }

        final RtfRedactor.Result result = RtfRedactor.redact(input,
                textFilter(policy, contextService, vectorService, context, incrementalRedactions, tokens), word);

        final LeftoverCheckResult leftoverCheck = LeftoverChecker.isDue(policy, filterOptions)
                ? runLeftoverCheck(() -> leftoverCheck(policy, word, result, filterOptions)) : null;

        return new BinaryDocumentFilterResult(result.document(), context, new Explanation(result.spans(), result.spans()),
                tokens[0], incrementalRedactions, reviewSampled, leftoverCheck);

    }

    /** Applies spans by their paragraph number and offsets, after the policy's {@code config.word} settings. */
    @Override
    public byte[] apply(final Policy policy, final byte[] input, final List<Span> spans, final MimeType outputMimeType) {
        requireRtf(outputMimeType);
        return RtfRedactor.applySpans(input, spans, policy.getConfig().getWord());
    }

    /*
     * Scans the redacted document's runs of text as the redaction did, leaving out a finding inside a replacement
     * the redaction put in its run.
     */
    private LeftoverCheckResult leftoverCheck(final Policy policy, final Word word, final RtfRedactor.Result redaction,
                                              final FilterOptions filterOptions) {

        final LeftoverCheck.Using using = leftoverCheckUsing(policy);
        final Function<String, TextFilterResult> check = text -> {
            try {
                final List<Span> found = leftoverSpans(policy, using, text);
                return new TextFilterResult(text, "", 0, new Explanation(found, found), List.of(), 0);
            } catch (final Exception e) {
                throw new IllegalStateException("Unable to check the redacted document text.", e);
            }
        };

        final Map<Integer, List<ReplacedRange>> replaced = replacedRanges(redaction.spans());
        final List<String> paragraphs = RtfRedactor.readParagraphs(redaction.document(), word);
        final List<LeftoverFinding> findings = new ArrayList<>();
        for (final Span span : RtfRedactor.detect(redaction.document(), check, word)) {
            final int paragraph = span.getParagraphNumber();
            if (!LeftoverChecker.isInserted(span, replaced.getOrDefault(paragraph, List.of()))) {
                findings.add(LeftoverChecker.finding(span, paragraphs.get(paragraph), filterOptions, 0, 0, paragraph));
            }
        }
        return LeftoverCheckResult.performed(findings);

    }

    // Where each run's replacements are in the redacted text, from their places in the original.
    private static Map<Integer, List<ReplacedRange>> replacedRanges(final List<Span> spans) {
        final Map<Integer, List<Span>> byParagraph = new HashMap<>();
        for (final Span span : spans) {
            byParagraph.computeIfAbsent(span.getParagraphNumber(), k -> new ArrayList<>()).add(span);
        }
        final Map<Integer, List<ReplacedRange>> ranges = new HashMap<>();
        for (final Map.Entry<Integer, List<Span>> entry : byParagraph.entrySet()) {
            final List<Span> sorted = new ArrayList<>(entry.getValue());
            sorted.sort(Comparator.comparingInt(Span::getCharacterStart));
            final List<ReplacedRange> paragraph = new ArrayList<>();
            int shift = 0;
            for (final Span span : sorted) {
                final String replacement = span.getReplacement() == null ? "" : span.getReplacement();
                final int start = span.getCharacterStart() + shift;
                paragraph.add(new ReplacedRange(start, start + replacement.length()));
                shift += replacement.length() - (span.getCharacterEnd() - span.getCharacterStart());
            }
            ranges.put(entry.getKey(), paragraph);
        }
        return ranges;
    }

    private Function<String, TextFilterResult> textFilter(final Policy policy, final ContextService contextService,
                                                          final VectorService vectorService, final String context,
                                                          final List<IncrementalRedaction> incrementalRedactions,
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

    private static void requireRtf(final MimeType outputMimeType) {
        if (outputMimeType != null && outputMimeType != MimeType.APPLICATION_RTF) {
            throw new IllegalArgumentException("An RTF document can only be redacted to " + MimeType.APPLICATION_RTF
                    + ", not " + outputMimeType + ".");
        }
    }

}
