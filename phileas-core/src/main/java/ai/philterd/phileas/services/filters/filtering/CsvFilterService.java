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
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.IncrementalRedaction;
import ai.philterd.phileas.model.filtering.LeftoverCheckResult;
import ai.philterd.phileas.model.filtering.LeftoverFinding;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.RedactedTextBuilder;
import ai.philterd.phileas.model.filtering.ReplacedRange;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.Tabular;
import ai.philterd.phileas.policy.config.Csv;
import ai.philterd.phileas.policy.config.LeftoverCheck;
import ai.philterd.phileas.services.LeftoverChecker;
import ai.philterd.phileas.services.ReviewSampler;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.csv.CsvDocument;
import ai.philterd.phileas.services.csv.CsvRedactionOptions;
import ai.philterd.phileas.services.disambiguation.SpanDisambiguationServiceFactory;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.documentprocessors.DocumentProcessor;
import ai.philterd.phileas.services.documentprocessors.UnstructuredDocumentProcessor;
import ai.philterd.phileas.services.filters.postfilters.PostFilter;
import ai.philterd.phileas.services.strategies.ColumnFilterStrategy;
import ai.philterd.phileas.services.tokens.TokenCounter;
import ai.philterd.phileas.services.tokens.WhitespaceTokenCounter;
import com.google.gson.Gson;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Redacts CSV documents with a policy, field by field: each field is detected on its own, so a match never joins
 * neighboring fields. The document is read and written with the policy's {@code config.csv}, and the policy's
 * {@code tabular} entry without a worksheet applies to it. Fields with nothing redacted are written back unchanged,
 * with their quoting, and the line endings are kept. Spans have offsets into their field's value and the field's
 * {@link Span#getRow() row} and {@link Span#getColumn() column}, counting from 1.
 */
public class CsvFilterService extends BinaryFilterService {

    private static final Logger LOGGER = LogManager.getLogger(CsvFilterService.class);

    /** The replacement written into each data field of a column in {@code fullyRedactedColumns}. */
    public static final String COLUMN_REPLACEMENT = "{{{REDACTED}}}";

    private static final String HEADER_CONTEXT_SEPARATOR = ": ";
    private static final char BOM = '﻿';
    private static final Gson GSON = new Gson();

    private final DocumentProcessor unstructuredDocumentProcessor;
    private final TokenCounter tokenCounter;
    private final CsvRedactionOptions options;
    private final CsvDocument.Limits limits;

    // Column filters by policy and strategies, built once like the policy's detecting filters.
    private final Map<String, ColumnFilter> columnFilterCache = new ConcurrentHashMap<>();

    // Bound at construction for the no-service overloads; null for a warm, per-call instance.
    private final ContextService defaultContextService;
    private final VectorService defaultVectorService;

    public CsvFilterService(final PhileasConfiguration phileasConfiguration,
                            final ContextService contextService,
                            final VectorService vectorService,
                            final HttpClient httpClient) {
        this(phileasConfiguration, contextService, vectorService, new SecureRandom(), httpClient, CsvRedactionOptions.defaults());
    }

    public CsvFilterService(final PhileasConfiguration phileasConfiguration,
                            final ContextService contextService,
                            final VectorService vectorService,
                            final SecureRandom random,
                            final HttpClient httpClient,
                            final CsvRedactionOptions options) {
        super(phileasConfiguration, random, httpClient);
        this.defaultContextService = contextService;
        this.defaultVectorService = vectorService;
        this.options = options == null ? CsvRedactionOptions.defaults() : options;
        this.limits = new CsvDocument.Limits(phileasConfiguration.csvMaxRows(),
                phileasConfiguration.csvMaxFieldsPerRow(), phileasConfiguration.csvMaxFieldCharacters());
        this.tokenCounter = new WhitespaceTokenCounter();
        this.unstructuredDocumentProcessor = new UnstructuredDocumentProcessor(
                SpanDisambiguationServiceFactory.getSpanDisambiguationService(phileasConfiguration),
                phileasConfiguration.incrementalRedactionsEnabled());
    }

    /** Creates a warm, reusable service whose context and vector services are supplied per call. */
    public CsvFilterService(final PhileasConfiguration phileasConfiguration, final HttpClient httpClient) {
        this(phileasConfiguration, new SecureRandom(), httpClient, CsvRedactionOptions.defaults());
    }

    /** Warm, reusable service with the given options. */
    public CsvFilterService(final PhileasConfiguration phileasConfiguration, final SecureRandom random,
                            final HttpClient httpClient, final CsvRedactionOptions options) {
        this(phileasConfiguration, null, null, random, httpClient, options);
    }

    @Override
    public BinaryDocumentFilterResult filter(final Policy policy, final String context, final byte[] input,
                                             final MimeType outputMimeType) throws Exception {
        requireBoundServices();
        return filter(policy, defaultContextService, defaultVectorService, context, input, outputMimeType);
    }

    /**
     * Filters a CSV document using the services bound at construction, with options for the call.
     * @param policy The {@link Policy} to apply.
     * @param context The redaction context.
     * @param input The CSV document.
     * @param outputMimeType {@link MimeType#TEXT_CSV} or <code>null</code>.
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
                    "This CsvFilterService was created for per-call context and vector services. "
                            + "Call filter(policy, contextService, vectorService, context, input, outputMimeType) instead, "
                            + "or construct it with a ContextService and VectorService.");
        }
    }

    @Override
    public BinaryDocumentFilterResult filter(final Policy policy, final ContextService contextService,
                                             final VectorService vectorService, final String context,
                                             final byte[] input, final MimeType outputMimeType,
                                             final FilterOptions filterOptions) throws Exception {
        return filter(policy, contextService, vectorService, context, input, outputMimeType, filterOptions, null);
    }

    /**
     * Filters a CSV document whose character set is known from outside it, such as an email attachment's declared
     * charset. The document is read and written in that character set in place of {@code config.csv} {@code encoding}.
     * @param charset The document's character set, or {@code null} to use {@code config.csv} {@code encoding}.
     * @see #filter(Policy, ContextService, VectorService, String, byte[], MimeType, FilterOptions)
     */
    public BinaryDocumentFilterResult filter(final Policy policy, final ContextService contextService,
                                             final VectorService vectorService, final String context,
                                             final byte[] input, final MimeType outputMimeType,
                                             final FilterOptions filterOptions, final Charset charset) throws Exception {

        final boolean textOutput = filterOptions != null && filterOptions.isTextOutput();

        // A call for text has no output document, so its type is not checked.
        if (!textOutput) {
            requireCsv(outputMimeType);
        }

        final boolean reviewSampled = ReviewSampler.isSelected(policy);
        final List<IncrementalRedaction> incrementalRedactions = new ArrayList<>();
        final long[] tokens = {0};

        final Function<String, TextFilterResult> identifiers = textFilter(policy, contextService, vectorService,
                context, incrementalRedactions, tokens);

        final Csv csv = policy.getConfig().getCsv();
        final Decoded decoded = decode(input, charset != null ? charset : csv.getCharset());
        final CsvDocument document = CsvDocument.parse(decoded.text(), csv.getDelimiter(), csv.getQuote(), limits);
        final Plan plan = plan(policy, document);

        for (final String warning : plan.warnings()) {
            LOGGER.warn(warning);
        }

        final Map<CsvDocument.Field, String> values = new HashMap<>();
        final Map<CsvDocument.Field, List<ReplacedRange>> replaced = new HashMap<>();
        final Map<List<String>, Function<String, TextFilterResult>> keyFilters = new HashMap<>();
        final List<Span> spans = new ArrayList<>();
        final Set<CsvDocument.Field> unscanned = new HashSet<>();

        final List<List<CsvDocument.Field>> rows = document.rows();
        for (int r = 0; r < rows.size(); r++) {

            final List<CsvDocument.Field> row = rows.get(r);

            for (int c = 0; c < row.size(); c++) {

                final CsvDocument.Field field = row.get(c);
                final String original = field.value();
                final int column = c + 1;
                if (original.isEmpty()) {
                    continue;
                }

                final boolean header = r < plan.headerRows();

                if (options.getFullyRedactedColumns().contains(column)) {
                    if (!header) {
                        replaceWhole(field, r, column, COLUMN_REPLACEMENT, context, values, replaced, spans);
                    }
                    continue;
                }

                Function<String, TextFilterResult> filter = identifiers;
                if (plan.rules() != null) {
                    if (header) {
                        continue;
                    }
                    final Tabular.Column rule = plan.rules().ruleFor(column);
                    final Tabular.Action action = rule != null ? rule.getAction() : unlistedAction(plan.rules().sheet());
                    if (action == Tabular.Action.IGNORE) {
                        unscanned.add(field);
                        continue;
                    }
                    if (action == Tabular.Action.REDACT) {
                        final List<ColumnFilterStrategy> strategies = rule != null ? rule.getStrategies()
                                : plan.rules().sheet().getEffectiveUnlistedStrategies();
                        final String replacement = columnFilter(policy, strategies)
                                .replace(contextService, policy, context, original).getReplacement();
                        replaceWhole(field, r, column, replacement, context, values, replaced, spans);
                        continue;
                    }
                    if (action == Tabular.Action.FILTER) {
                        filter = keyFilters.computeIfAbsent(rule.getFilters(), keys -> keyFilter(policy, keys,
                                contextService, vectorService, context, incrementalRedactions, tokens));
                    }
                }

                final String contextHeader = options.isUseHeaderContext() && !header ? plan.header(column) : null;
                final List<Span> found = detect(original, contextHeader, filter);
                if (found.isEmpty()) {
                    continue;
                }

                final List<ReplacedRange> ranges = new ArrayList<>();
                final String redacted = applySpans(original, found, ranges);
                if (redacted.equals(original)) {
                    continue;
                }
                values.put(field, redacted);
                replaced.put(field, ranges);
                for (final Span span : found) {
                    span.setRow(r + 1);
                    span.setColumn(column);
                    spans.add(span);
                }

            }

        }

        if (textOutput) {
            final RedactedTextBuilder text = text(rows, plan.headerRows(), values, replaced, unscanned);
            final LeftoverCheckResult leftoverCheck = LeftoverChecker.isDue(policy, filterOptions)
                    ? runLeftoverCheck(() -> textLeftoverCheck(policy, text.parts(), filterOptions)) : null;
            return new BinaryDocumentFilterResult(null, text.build(), context, new Explanation(spans, spans), tokens[0],
                    incrementalRedactions, reviewSampled, leftoverCheck, null);
        }

        final String output = document.write(values);
        final byte[] bytes = encode(output, decoded);

        final LeftoverCheckResult leftoverCheck = LeftoverChecker.isDue(policy, filterOptions)
                ? runLeftoverCheck(() -> leftoverCheck(policy, csv, output, plan, replaced, document, filterOptions)) : null;

        return new BinaryDocumentFilterResult(bytes, context, new Explanation(spans, spans), tokens[0],
                incrementalRedactions, reviewSampled, leftoverCheck);

    }

    /**
     * Applies spans by their row, column, and offsets into the field's value. A span without a row and column,
     * or outside the document, is ignored.
     */
    @Override
    public byte[] apply(final Policy policy, final byte[] input, final List<Span> spans, final MimeType outputMimeType) {

        requireCsv(outputMimeType);

        final Csv csv = policy.getConfig().getCsv();
        final Decoded decoded = decode(input, csv.getCharset());
        final CsvDocument document = CsvDocument.parse(decoded.text(), csv.getDelimiter(), csv.getQuote(), limits);

        final Map<CsvDocument.Field, List<Span>> byField = new LinkedHashMap<>();
        for (final Span span : spans) {
            if (span.getRow() == null || span.getColumn() == null) {
                continue;
            }
            final int r = span.getRow() - 1;
            final int c = span.getColumn() - 1;
            if (r < 0 || r >= document.rows().size() || c < 0 || c >= document.rows().get(r).size()) {
                continue;
            }
            byField.computeIfAbsent(document.rows().get(r).get(c), k -> new ArrayList<>()).add(span);
        }

        final Map<CsvDocument.Field, String> values = new HashMap<>();
        for (final Map.Entry<CsvDocument.Field, List<Span>> entry : byField.entrySet()) {
            final String original = entry.getKey().value();
            final String redacted = applySpans(original, entry.getValue(), new ArrayList<>());
            if (!redacted.equals(original)) {
                values.put(entry.getKey(), redacted);
            }
        }

        return encode(document.write(values), decoded);

    }

    // --- rules -------------------------------------------------------------------------------------

    // The header rows and, when the policy has a tabular entry for a CSV, its rules resolved against the headers.
    private record Plan(int headerRows, Map<Integer, String> headers, Tabular.ResolvedColumns rules, List<String> warnings) {

        String header(final int column) {
            final String header = headers.get(column);
            return header == null || header.isBlank() ? null : header;
        }

    }

    private Plan plan(final Policy policy, final CsvDocument document) {
        final Tabular.Sheet sheet = policy.getTabular() == null ? null : policy.getTabular().sheetFor(null);
        final int headerRows = sheet != null ? sheet.getHeaderRows() : options.isHeaderRow() ? 1 : 0;
        final Map<Integer, String> headers = new HashMap<>();
        if (headerRows > 0 && !document.rows().isEmpty()) {
            final List<CsvDocument.Field> last = document.rows().get(Math.min(headerRows, document.rows().size()) - 1);
            for (int c = 0; c < last.size(); c++) {
                headers.put(c + 1, last.get(c).value());
            }
        }
        if (sheet == null) {
            return new Plan(headerRows, headers, null, List.of());
        }
        final Map<Integer, String> trimmed = new HashMap<>();
        headers.forEach((column, text) -> trimmed.put(column, text.strip()));
        final Tabular.ResolvedColumns resolved = sheet.resolve(trimmed);
        return new Plan(headerRows, headers, resolved, resolved.warnings());
    }

    private static Tabular.Action unlistedAction(final Tabular.Sheet sheet) {
        return switch (sheet.getUnlistedColumns()) {
            case IDENTIFIERS -> null;
            case IGNORE -> Tabular.Action.IGNORE;
            case REDACT -> Tabular.Action.REDACT;
        };
    }

    private ColumnFilter columnFilter(final Policy policy, final List<ColumnFilterStrategy> strategies) {
        return columnFilterCache.computeIfAbsent(policy.getCacheKey() + "\n" + GSON.toJson(strategies),
                key -> filterPolicyLoader.getColumnFilter(policy, strategies));
    }

    private Function<String, TextFilterResult> keyFilter(final Policy policy, final List<String> keys,
                                                         final ContextService contextService, final VectorService vectorService,
                                                         final String context, final List<IncrementalRedaction> incrementalRedactions,
                                                         final long[] tokens) {
        try {
            return textFilter(Tabular.restrictIdentifiers(policy, keys), contextService, vectorService, context,
                    incrementalRedactions, tokens);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException("Unable to build the filters of a tabular column rule.", e);
        }
    }

    /** The label of a CSV document's one segment of redacted text. */
    public static final String ROWS_LABEL = "rows";

    /*
     * The rows as the redaction leaves them, one "header: value" line per field with text and a blank line between
     * rows. A column's header is its field in the last header row as redaction leaves it, or "column N" without one.
     * Header rows are not lines of their own, and fields an ignore rule leaves unscanned are left out.
     */
    private static RedactedTextBuilder text(final List<List<CsvDocument.Field>> rows, final int headerRows,
                                            final Map<CsvDocument.Field, String> values,
                                            final Map<CsvDocument.Field, List<ReplacedRange>> replaced,
                                            final Set<CsvDocument.Field> unscanned) {

        final RedactedTextBuilder text = new RedactedTextBuilder();
        text.ensure(ROWS_LABEL);

        final Map<Integer, CsvDocument.Field> headers = new HashMap<>();
        if (headerRows > 0 && rows.size() >= headerRows) {
            final List<CsvDocument.Field> last = rows.get(headerRows - 1);
            for (int c = 0; c < last.size(); c++) {
                headers.put(c + 1, last.get(c));
            }
        }

        boolean previousRow = false;
        for (int r = headerRows; r < rows.size(); r++) {
            boolean thisRow = false;
            final List<CsvDocument.Field> row = rows.get(r);
            for (int c = 0; c < row.size(); c++) {
                final CsvDocument.Field field = row.get(c);
                if (field.value().isEmpty() || unscanned.contains(field)) {
                    continue;
                }
                if (previousRow && !thisRow) {
                    text.line(ROWS_LABEL, "", List.of());
                }
                thisRow = true;
                final CsvDocument.Field headerField = headers.get(c + 1);
                final String header = headerField == null ? ""
                        : RedactedTextBuilder.flatten(values.getOrDefault(headerField, headerField.value()));
                final List<ReplacedRange> ranges = new ArrayList<>();
                final String name;
                if (header.isBlank()) {
                    name = "column " + (c + 1);
                } else {
                    name = header;
                    ranges.addAll(replaced.getOrDefault(headerField, List.of()));
                }
                final String prefix = name + ": ";
                for (final ReplacedRange range : replaced.getOrDefault(field, List.of())) {
                    ranges.add(range.shift(prefix.length()));
                }
                text.line(ROWS_LABEL, prefix + RedactedTextBuilder.flatten(values.getOrDefault(field, field.value())), ranges);
            }
            previousRow |= thisRow;
        }

        return text;

    }

    // --- fields ------------------------------------------------------------------------------------

    private static void replaceWhole(final CsvDocument.Field field, final int r, final int column, final String replacement,
                                     final String context, final Map<CsvDocument.Field, String> values,
                                     final Map<CsvDocument.Field, List<ReplacedRange>> replaced, final List<Span> spans) {
        final String original = field.value();
        values.put(field, replacement);
        replaced.put(field, List.of(new ReplacedRange(0, replacement.length())));
        final Span span = Span.make(0, original.length(), FilterType.OTHER, context, 1.0, original, replacement, "",
                false, true, new String[0], 0);
        span.setClassification(ColumnFilterStrategy.CLASSIFICATION);
        span.setRow(r + 1);
        span.setColumn(column);
        spans.add(span);
    }

    // The spans found in a field, with offsets into it. With a header, detects on "header: value" and keeps the
    // spans wholly inside the value.
    private static List<Span> detect(final String value, final String header,
                                     final Function<String, TextFilterResult> filter) {
        final String prefix = header == null ? "" : header + HEADER_CONTEXT_SEPARATOR;
        final TextFilterResult result = filter.apply(prefix + value);
        final List<Span> out = new ArrayList<>();
        if (result.getExplanation() == null || result.getExplanation().appliedSpans() == null) {
            return out;
        }
        for (final Span s : result.getExplanation().appliedSpans()) {
            final int start = s.getCharacterStart() - prefix.length();
            final int end = s.getCharacterEnd() - prefix.length();
            if (start < 0 || end > value.length() || end <= start) {
                continue;
            }
            final Span span = s.copy();
            span.setPattern(s.getPattern());
            span.setWindow(s.getWindow());
            span.setCharacterStart(start);
            span.setCharacterEnd(end);
            out.add(span);
        }
        out.sort(Comparator.comparingInt(Span::getCharacterStart));
        return out;
    }

    // The value with the spans' replacements, skipping a span that overlaps an earlier one, and where each
    // replacement is in the result.
    private static String applySpans(final String value, final List<Span> spans, final List<ReplacedRange> replaced) {
        final List<Span> sorted = new ArrayList<>();
        for (final Span s : spans) {
            if (s.getCharacterStart() >= 0 && s.getCharacterEnd() <= value.length() && s.getCharacterEnd() > s.getCharacterStart()) {
                sorted.add(s);
            }
        }
        sorted.sort(Comparator.comparingInt(Span::getCharacterStart)
                .thenComparing(Comparator.comparingInt(Span::getCharacterEnd).reversed()));
        final StringBuilder out = new StringBuilder();
        int last = 0;
        for (final Span s : sorted) {
            if (s.getCharacterStart() < last) {
                continue;
            }
            final String replacement = s.getReplacement() == null ? "" : s.getReplacement();
            out.append(value, last, s.getCharacterStart());
            replaced.add(new ReplacedRange(out.length(), out.length() + replacement.length()));
            out.append(replacement);
            last = s.getCharacterEnd();
        }
        out.append(value, last, value.length());
        return out.toString();
    }

    // --- leftover check ----------------------------------------------------------------------------

    /*
     * Scans each field of the redacted document, as the redaction did, leaving out header rows and ignored
     * columns under column rules, and findings inside replacements the redaction put in the field.
     */
    private LeftoverCheckResult leftoverCheck(final Policy policy, final Csv csv, final String output, final Plan plan,
                                              final Map<CsvDocument.Field, List<ReplacedRange>> replaced,
                                              final CsvDocument original, final FilterOptions filterOptions) throws Exception {

        final CsvDocument redacted = CsvDocument.parse(output, csv.getDelimiter(), csv.getQuote(), CsvDocument.Limits.none());
        final LeftoverCheck.Using using = leftoverCheckUsing(policy);
        final List<LeftoverFinding> findings = new ArrayList<>();

        for (int r = 0; r < redacted.rows().size(); r++) {
            final List<CsvDocument.Field> row = redacted.rows().get(r);
            for (int c = 0; c < row.size(); c++) {
                final int column = c + 1;
                if (plan.rules() != null) {
                    final Tabular.Column rule = plan.rules().ruleFor(column);
                    final Tabular.Action action = rule != null ? rule.getAction() : unlistedAction(plan.rules().sheet());
                    if (r < plan.headerRows() || action == Tabular.Action.IGNORE) {
                        continue;
                    }
                }
                final String value = row.get(c).value();
                if (value.isEmpty()) {
                    continue;
                }
                final List<ReplacedRange> inserted = r < original.rows().size() && c < original.rows().get(r).size()
                        ? replaced.getOrDefault(original.rows().get(r).get(c), List.of()) : List.of();
                for (final Span span : leftoverSpans(policy, using, value)) {
                    if (!LeftoverChecker.isInserted(span, inserted)) {
                        findings.add(LeftoverChecker.finding(span, value, filterOptions).inField(r + 1, column));
                    }
                }
            }
        }

        return LeftoverCheckResult.performed(findings);

    }

    // --- text and encoding -------------------------------------------------------------------------

    private Function<String, TextFilterResult> textFilter(final Policy policy, final ContextService contextService,
                                                          final VectorService vectorService, final String context,
                                                          final List<IncrementalRedaction> incrementalRedactions,
                                                          final long[] tokens) throws Exception {
        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);
        final List<PostFilter> postFilters = getPostFiltersForPolicy(policy);
        return text -> {
            tokens[0] += tokenCounter.countTokens(text);
            try {
                final TextFilterResult result = unstructuredDocumentProcessor.process(contextService, vectorService, policy,
                        filters, postFilters, context, 0, text);
                incrementalRedactions.addAll(result.getIncrementalRedactions());
                return result;
            } catch (final Exception e) {
                throw new IllegalStateException("Unable to filter a CSV field.", e);
            }
        };
    }

    // The document's text without a byte-order mark, whether it had one, and the charset that writes it back with
    // the same byte order.
    private record Decoded(String text, boolean bom, Charset output) {
    }

    // Decodes strictly, so a document not in the policy's encoding is refused rather than redacted garbled.
    private static Decoded decode(final byte[] input, final Charset charset) {
        final String text;
        try {
            text = charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(input)).toString();
        } catch (final CharacterCodingException e) {
            throw new IllegalArgumentException("The CSV document is not valid " + charset.name()
                    + ". Set config.csv encoding, or the declared charset, to the document's encoding.", e);
        }
        // UTF-16 reads the byte order from the mark and drops it, and writes big-endian with a mark, so the output
        // charset is chosen from the input's bytes. Other charsets leave a mark in the text.
        if (StandardCharsets.UTF_16.equals(charset)) {
            if (input.length >= 2 && (input[0] & 0xFF) == 0xFF && (input[1] & 0xFF) == 0xFE) {
                return new Decoded(text, true, StandardCharsets.UTF_16LE);
            }
            return new Decoded(text, input.length >= 2 && (input[0] & 0xFF) == 0xFE && (input[1] & 0xFF) == 0xFF,
                    StandardCharsets.UTF_16BE);
        }
        if (!text.isEmpty() && text.charAt(0) == BOM) {
            return new Decoded(text.substring(1), true, charset);
        }
        return new Decoded(text, false, charset);
    }

    private static byte[] encode(final String text, final Decoded decoded) {
        return ((decoded.bom() ? String.valueOf(BOM) : "") + text).getBytes(decoded.output());
    }

    private static void requireCsv(final MimeType outputMimeType) {
        if (outputMimeType != null && outputMimeType != MimeType.TEXT_CSV) {
            throw new IllegalArgumentException("A CSV document can only be redacted to " + MimeType.TEXT_CSV
                    + ", not " + outputMimeType + ".");
        }
    }

}
