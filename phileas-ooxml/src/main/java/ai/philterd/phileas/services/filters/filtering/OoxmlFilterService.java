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
import ai.philterd.phileas.filters.ColumnFilter;
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
import ai.philterd.phileas.policy.Tabular;
import ai.philterd.phileas.model.filtering.ReplacedRange;
import ai.philterd.phileas.policy.config.LeftoverCheck;
import ai.philterd.phileas.services.LeftoverChecker;
import ai.philterd.phileas.services.ReviewSampler;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.SpanDisambiguationServiceFactory;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.documentprocessors.DocumentProcessor;
import ai.philterd.phileas.services.documentprocessors.UnstructuredDocumentProcessor;
import ai.philterd.phileas.services.filters.postfilters.PostFilter;
import ai.philterd.phileas.services.strategies.ColumnFilterStrategy;
import ai.philterd.phileas.services.ooxml.ColumnRules;
import ai.philterd.phileas.services.ooxml.OfficeRedactionSpan;
import ai.philterd.phileas.services.ooxml.OoxmlDocumentType;
import ai.philterd.phileas.services.ooxml.OoxmlRedactionOptions;
import ai.philterd.phileas.services.ooxml.OoxmlRedactionResult;
import ai.philterd.phileas.services.ooxml.TextFilter;
import ai.philterd.phileas.services.ooxml.WordDocumentRedactor;
import ai.philterd.phileas.services.ooxml.XlsxRedactor;
import ai.philterd.phileas.services.tokens.TokenCounter;
import ai.philterd.phileas.services.tokens.WhitespaceTokenCounter;
import com.google.gson.Gson;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    // Column filters by policy and strategies, built once like the policy's detecting filters, so the policy's
    // strategy objects are wired once rather than on every request.
    private final Map<String, ColumnFilter> columnFilterCache = new ConcurrentHashMap<>();
    private static final Gson GSON = new Gson();

    // Bound at construction for the no-service overloads; null for a warm, per-call instance.
    private final ContextService defaultContextService;
    private final VectorService defaultVectorService;

    public OoxmlFilterService(final PhileasConfiguration phileasConfiguration,
                              final ContextService contextService,
                              final VectorService vectorService,
                              final HttpClient httpClient) {
        this(phileasConfiguration, contextService, vectorService, new SecureRandom(), httpClient, OoxmlRedactionOptions.defaults());
    }

    /**
     * A service with per-call options. Of {@code options}, only the worksheet, fully redacted columns, and header
     * context are used: the Word and Excel settings come from each call's policy ({@code config.word} and
     * {@code config.excel}).
     */
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

    /**
     * Filters a Word or Excel document using the services bound at construction, with options for the call.
     * @param policy The {@link Policy} to apply.
     * @param context The redaction context.
     * @param input The input document as a byte array.
     * @param outputMimeType The output {@link MimeType}.
     * @param options The {@link FilterOptions} for this call.
     * @return A {@link BinaryDocumentFilterResult}.
     * @throws Exception Thrown if the document cannot be filtered.
     */
    public BinaryDocumentFilterResult filter(final Policy policy, final String context, final byte[] input,
                                             final MimeType outputMimeType, final FilterOptions options) throws Exception {
        requireBoundServices();
        return filter(policy, defaultContextService, defaultVectorService, context, input, outputMimeType, options);
    }

    @Override
    public BinaryDocumentFilterResult filter(final Policy policy, final ContextService contextService,
                                             final VectorService vectorService, final String context,
                                             final byte[] input, final MimeType outputMimeType,
                                             final FilterOptions filterOptions) throws Exception {

        // One draw for the document, not one per paragraph or cell.
        final boolean reviewSampled = ReviewSampler.isSelected(policy);

        final OoxmlDocumentType type = documentType(input, outputMimeType);

        final List<IncrementalRedaction> incrementalRedactions = new ArrayList<>();
        final long[] tokens = {0};
        final TextFilter textFilter = textFilter(policy, contextService, vectorService, context, incrementalRedactions, tokens);
        final ColumnRules columnRules = columnRules(policy, contextService, vectorService, context, incrementalRedactions, tokens);

        final OoxmlRedactionResult result = type == OoxmlDocumentType.DOCX
                ? WordDocumentRedactor.redact(input, textFilter, callOptions(policy), columnRules)
                : XlsxRedactor.redact(input, textFilter, callOptions(policy), columnRules);

        for (final String warning : result.warnings()) {
            LOGGER.warn(warning);
        }

        final List<Span> spans = new ArrayList<>();
        for (final OfficeRedactionSpan officeSpan : result.spans()) {
            final Span span = officeSpan.toSpan();
            if (span != null) {
                spans.add(span);
            }
        }

        // As with PDF, the identified and applied spans are reported as the same list.
        final Explanation explanation = new Explanation(spans, spans);

        final LeftoverCheckResult leftoverCheck = LeftoverChecker.isDue(policy, filterOptions)
                ? runLeftoverCheck(() -> leftoverCheck(policy, type, result, filterOptions, columnRules)) : null;

        return new BinaryDocumentFilterResult(result.document(), context, explanation, tokens[0], incrementalRedactions,
                reviewSampled, leftoverCheck);

    }

    /*
     * Scans the redacted document with the redactor's detect pass, which visits the same text the redaction did.
     * A finding's position is its paragraph or cell and its offsets there, and a finding inside a replacement the
     * redaction put in that paragraph or cell is left out. Text that is not a paragraph or cell (drawings,
     * comments in Excel, hyperlink targets, and so on) has no position to compare, so there a finding is left out
     * when it is a replacement the redaction put in such text, or part of one.
     */
    private LeftoverCheckResult leftoverCheck(final Policy policy, final OoxmlDocumentType type,
                                              final OoxmlRedactionResult redaction, final FilterOptions filterOptions,
                                              final ColumnRules columnRules) throws Exception {

        final LeftoverCheck.Using using = leftoverCheckUsing(policy);

        // Reports each finding as a redaction so the detect pass records where it is. Nothing is written.
        final TextFilter check = text -> {
            try {
                final List<Span> found = leftoverSpans(policy, using, text);
                final StringBuilder marked = new StringBuilder(text);
                for (int i = found.size() - 1; i >= 0; i--) {
                    final Span span = found.get(i);
                    marked.replace(span.getCharacterStart(), span.getCharacterEnd(), span.getReplacement());
                }
                return new TextFilterResult(marked.toString(), "", 0, new Explanation(found, found), List.of(), 0);
            } catch (final Exception e) {
                throw new IllegalStateException("Unable to check the redacted document text.", e);
            }
        };

        final byte[] redacted = redaction.document();
        final List<OfficeRedactionSpan> detected = type == OoxmlDocumentType.DOCX
                ? WordDocumentRedactor.detect(redacted, check, callOptions(policy), columnRules)
                : XlsxRedactor.detect(redacted, check, callOptions(policy), columnRules);

        final Map<Integer, List<ReplacedRange>> replacedByParagraph = replacedRanges(redaction.spans());
        final List<String> replacedElsewhere = new ArrayList<>();
        for (final OfficeRedactionSpan span : redaction.spans()) {
            if (span.getParagraphIndex() < 0 && !span.getReplacement().isEmpty()) {
                replacedElsewhere.add(span.getReplacement());
            }
        }

        final boolean matchedText = filterOptions != null && filterOptions.isLeftoverCheckMatchedText();
        final List<LeftoverFinding> findings = new ArrayList<>();

        for (final OfficeRedactionSpan officeSpan : detected) {

            final Span span = officeSpan.toSpan();

            // A marker, such as an embedded object, is not a detection.
            if (span == null || officeSpan.getFilterType().isEmpty()) {
                continue;
            }

            final int paragraph = officeSpan.getParagraphIndex();

            final boolean inserted = paragraph >= 0
                    ? LeftoverChecker.isInserted(span, replacedByParagraph.getOrDefault(paragraph, List.of()))
                    : replacedElsewhere.stream().anyMatch(replacement -> replacement.contains(officeSpan.getText()));

            if (!inserted) {
                findings.add(new LeftoverFinding(span.getFilterType(), span.getClassification(),
                        officeSpan.getCharacterStart(), officeSpan.getCharacterEnd(), span.getConfidence(),
                        0, 0, paragraph, matchedText ? officeSpan.getText() : null));
            }

        }

        return LeftoverCheckResult.performed(findings);

    }

    // Where each paragraph's or cell's replacements are in the redacted text, from their places in the original.
    private static Map<Integer, List<ReplacedRange>> replacedRanges(final List<OfficeRedactionSpan> redactions) {

        final Map<Integer, List<OfficeRedactionSpan>> byParagraph = new HashMap<>();
        for (final OfficeRedactionSpan span : redactions) {
            if (span.getParagraphIndex() >= 0 && span.getCharacterEnd() > span.getCharacterStart()) {
                byParagraph.computeIfAbsent(span.getParagraphIndex(), k -> new ArrayList<>()).add(span);
            }
        }

        final Map<Integer, List<ReplacedRange>> ranges = new HashMap<>();
        for (final Map.Entry<Integer, List<OfficeRedactionSpan>> entry : byParagraph.entrySet()) {

            final List<OfficeRedactionSpan> spans = new ArrayList<>(entry.getValue());
            // The same order the redaction resolves overlaps in: by start, the longer first.
            spans.sort(Comparator.comparingInt(OfficeRedactionSpan::getCharacterStart)
                    .thenComparing(Comparator.comparingInt(OfficeRedactionSpan::getCharacterEnd).reversed()));

            final List<ReplacedRange> paragraphRanges = new ArrayList<>();
            int shift = 0;
            int lastEnd = -1;
            for (final OfficeRedactionSpan span : spans) {
                // As the redaction applies them, an overlapping span is not applied.
                if (span.getCharacterStart() < lastEnd) {
                    continue;
                }
                final int start = span.getCharacterStart() + shift;
                paragraphRanges.add(new ReplacedRange(start, start + span.getReplacement().length()));
                shift += span.getReplacement().length() - (span.getCharacterEnd() - span.getCharacterStart());
                lastEnd = span.getCharacterEnd();
            }

            ranges.put(entry.getKey(), paragraphRanges);

        }

        return ranges;

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

        final List<IncrementalRedaction> incrementalRedactions = new ArrayList<>();
        final long[] tokens = new long[1];
        final TextFilter textFilter = textFilter(policy, contextService, vectorService, context, incrementalRedactions, tokens);
        final ColumnRules columnRules = columnRules(policy, contextService, vectorService, context, incrementalRedactions, tokens);

        return type == OoxmlDocumentType.DOCX
                ? WordDocumentRedactor.applySpans(input, officeSpans, callOptions(policy), textFilter, columnRules)
                : XlsxRedactor.applySpans(input, officeSpans, callOptions(policy), textFilter, columnRules);

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

    // The constructor's per-call options with the policy's Word and Excel settings.
    private OoxmlRedactionOptions callOptions(final Policy policy) {
        return options.copy().setWord(policy.getConfig().getWord()).setExcel(policy.getConfig().getExcel());
    }

    /*
     * The policy's column rules, or null when it has none. A filter rule's cells are scanned with a copy of the
     * policy that keeps only the rule's identifiers, and a redact rule's cells are replaced by a column filter.
     */
    private ColumnRules columnRules(final Policy policy, final ContextService contextService, final VectorService vectorService,
                                    final String context, final List<IncrementalRedaction> incrementalRedactions,
                                    final long[] tokens) {

        final Tabular tabular = policy.getTabular();
        if (tabular == null) {
            return null;
        }

        return new ColumnRules() {

            @Override
            public Tabular tabular() {
                return tabular;
            }

            @Override
            public TextFilter filter(final List<String> identifierKeys) {
                try {
                    return textFilter(Tabular.restrictIdentifiers(policy, identifierKeys), contextService, vectorService,
                            context, incrementalRedactions, tokens);
                } catch (final RuntimeException e) {
                    throw e;
                } catch (final Exception e) {
                    throw new IllegalStateException("Unable to build the filters of a tabular column rule.", e);
                }
            }

            @Override
            public String replace(final List<ColumnFilterStrategy> strategies, final String value) {
                final ColumnFilter columnFilter = columnFilterCache.computeIfAbsent(
                        policy.getCacheKey() + "\n" + GSON.toJson(strategies),
                        key -> filterPolicyLoader.getColumnFilter(policy, strategies));
                try {
                    return columnFilter.replace(contextService, policy, context, value).getReplacement();
                } catch (final Exception e) {
                    throw new IllegalStateException("Unable to replace a cell with a tabular column rule's strategies.", e);
                }
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
