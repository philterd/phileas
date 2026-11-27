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
import ai.philterd.phileas.model.filtering.RedactedTextBuilder;
import ai.philterd.phileas.model.filtering.ReplacedRange;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
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
import ai.philterd.phileas.services.email.EmailFields;
import ai.philterd.phileas.services.email.EmailRedactor;
import ai.philterd.phileas.services.email.EmailText;
import ai.philterd.phileas.services.filters.postfilters.PostFilter;
import ai.philterd.phileas.services.tokens.TokenCounter;
import ai.philterd.phileas.services.tokens.WhitespaceTokenCounter;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Redacts email messages: {@code .eml} ({@code message/rfc822}) and Outlook {@code .msg}
 * ({@code application/vnd.ms-outlook}). Both are written out as {@code .eml}. The Subject, From, To, and Cc headers
 * and every text body part are redacted with the policy's identifiers; the policy's {@code config.email} decides
 * which other headers are removed and what happens to attachments and inline images. Spans are returned with offsets
 * relative to their field and {@link Span#getParagraphNumber()} set to the field's number, and {@link #apply}
 * re-applies such spans by position. The result's {@link BinaryDocumentFilterResult#getEmail()} lists attachments and
 * images left uninspected, attachments removed because they could not be redacted, and dropped signatures.
 */
public class EmailFilterService extends BinaryFilterService {

    private static final Logger LOGGER = LogManager.getLogger(EmailFilterService.class);

    private static final String FALLBACK_REPLACEMENT = "{{{REDACTED-custom}}}";

    private final DocumentProcessor unstructuredDocumentProcessor;
    private final TokenCounter tokenCounter;
    private final PdfFilterService pdfFilterService;
    private final OoxmlFilterService ooxmlFilterService;

    // Bound at construction for the no-service overloads; null for a warm, per-call instance.
    private final ContextService defaultContextService;
    private final VectorService defaultVectorService;

    public EmailFilterService(final PhileasConfiguration phileasConfiguration,
                              final ContextService contextService,
                              final VectorService vectorService,
                              final HttpClient httpClient) {
        this(phileasConfiguration, contextService, vectorService, new SecureRandom(), httpClient);
    }

    public EmailFilterService(final PhileasConfiguration phileasConfiguration,
                              final ContextService contextService,
                              final VectorService vectorService,
                              final SecureRandom random,
                              final HttpClient httpClient) {
        super(phileasConfiguration, random, httpClient);
        LOGGER.info("Initializing email filter service.");
        this.defaultContextService = contextService;
        this.defaultVectorService = vectorService;
        this.tokenCounter = new WhitespaceTokenCounter();
        this.unstructuredDocumentProcessor = new UnstructuredDocumentProcessor(
                SpanDisambiguationServiceFactory.getSpanDisambiguationService(phileasConfiguration),
                phileasConfiguration.incrementalRedactionsEnabled());
        this.pdfFilterService = new PdfFilterService(phileasConfiguration, random, httpClient);
        this.ooxmlFilterService = new OoxmlFilterService(phileasConfiguration, random, httpClient, null);
    }

    /** Creates a warm, reusable service whose context and vector services are supplied per call. */
    public EmailFilterService(final PhileasConfiguration phileasConfiguration, final HttpClient httpClient) {
        this(phileasConfiguration, new SecureRandom(), httpClient);
    }

    /** Warm, reusable service with the given thread-safe {@link SecureRandom}. */
    public EmailFilterService(final PhileasConfiguration phileasConfiguration, final SecureRandom random,
                              final HttpClient httpClient) {
        this(phileasConfiguration, null, null, random, httpClient);
    }

    @Override
    public BinaryDocumentFilterResult filter(final Policy policy, final String context,
                                             final byte[] input, final MimeType outputMimeType) throws Exception {
        return filter(policy, context, input, outputMimeType, FilterOptions.defaults());
    }

    /**
     * Filters an email message using the services bound at construction, with options for the call.
     * @param policy The {@link Policy} to apply.
     * @param context The redaction context.
     * @param input The {@code .eml} or {@code .msg} bytes.
     * @param outputMimeType {@link MimeType#MESSAGE_RFC822}, or {@code null}.
     * @param options The {@link FilterOptions} for this call.
     * @return A {@link BinaryDocumentFilterResult} whose document is an {@code .eml}.
     * @throws Exception Thrown if the message cannot be filtered.
     */
    public BinaryDocumentFilterResult filter(final Policy policy, final String context, final byte[] input,
                                             final MimeType outputMimeType, final FilterOptions options) throws Exception {
        requireBoundServices();
        return filter(policy, defaultContextService, defaultVectorService, context, input, outputMimeType, options);
    }

    // Fails fast when a warm instance is used through an overload that needs construction-bound services.
    private void requireBoundServices() {
        if (defaultContextService == null || defaultVectorService == null) {
            throw new IllegalStateException(
                    "This EmailFilterService was created for per-call context and vector services. "
                            + "Call filter(policy, contextService, vectorService, context, input, outputMimeType) instead, "
                            + "or construct it with a ContextService and VectorService.");
        }
    }

    /**
     * Filters an email message. An encrypted message is refused with an
     * {@link ai.philterd.phileas.services.email.EncryptedEmailException}.
     */
    @Override
    public BinaryDocumentFilterResult filter(final Policy policy, final ContextService contextService,
                                             final VectorService vectorService, final String context,
                                             final byte[] input, final MimeType outputMimeType,
                                             final FilterOptions options) throws Exception {

        final boolean textOutput = options != null && options.isTextOutput();

        // A call for text has no output document, so its type is not checked.
        if (!textOutput) {
            requireEmailOutput(outputMimeType);
        }

        // One draw for the message, not one per field or attachment.
        final boolean reviewSampled = ReviewSampler.isSelected(policy);

        final List<IncrementalRedaction> incrementalRedactions = new ArrayList<>();
        final long[] tokens = {0};
        final TextRedactor textRedactor = textRedactor(policy, contextService, vectorService, context, incrementalRedactions);

        final EmailRedactor.FieldRedactor fieldRedactor = (index, text, html) -> {
            tokens[0] += tokenCounter.countTokens(text);
            if (html) {
                return EmailFields.replace(text, EmailFields.detectHtml(text, value -> textRedactor.redact(value)
                        .getExplanation().appliedSpans()), FALLBACK_REPLACEMENT);
            }
            final TextFilterResult result = textRedactor.redact(text);
            return new EmailRedactor.FieldRedaction(result.getFilteredText(), result.getExplanation().appliedSpans(),
                    result.getReplacedRanges());
        };

        final List<RedactedAttachment> attachments = new ArrayList<>();

        final EmailRedactor.AttachmentRedactor attachmentRedactor = (name, document, type) -> {
            final BinaryFilterService service = attachmentService(type);
            final BinaryDocumentFilterResult result = service.filter(policy, contextService, vectorService, context,
                    document, type, options);
            tokens[0] += result.getTokens();
            incrementalRedactions.addAll(result.getIncrementalRedactions());
            attachments.add(new RedactedAttachment(name, result));
            // With text output the attachment's text is kept instead, and the message it would go in is not returned.
            return textOutput ? document : result.getDocument();
        };

        final EmailRedactor.Result result = EmailRedactor.redact(input, policy.getConfig().getEmail(),
                fieldRedactor, attachmentRedactor);

        final List<Span> spans = new ArrayList<>();
        for (final EmailRedactor.Field field : result.fields()) {
            for (final Span span : field.spans()) {
                final Span copy = span.copy();
                copy.setParagraphNumber(field.index());
                spans.add(copy);
            }
        }

        // As with PDF and Word, the identified and applied spans are reported as the same list.
        final Explanation explanation = new Explanation(spans, spans);

        if (textOutput) {
            final RedactedTextBuilder text = text(result, attachments);
            final LeftoverCheckResult leftoverCheck = LeftoverChecker.isDue(policy, options)
                    ? runLeftoverCheck(() -> textLeftoverCheck(policy, text, attachments, options)) : null;
            return new BinaryDocumentFilterResult(null, text.build(), context, explanation, tokens[0],
                    incrementalRedactions, reviewSampled, leftoverCheck, result.report());
        }

        final LeftoverCheckResult leftoverCheck = LeftoverChecker.isDue(policy, options)
                ? runLeftoverCheck(() -> leftoverCheck(policy, result, attachments, options)) : null;

        return new BinaryDocumentFilterResult(result.document(), context, explanation, tokens[0], incrementalRedactions,
                reviewSampled, leftoverCheck, result.report());

    }

    /** Segment labels of an email message's redacted text. */
    public static final String HEADERS_LABEL = "headers";
    public static final String BODY_LABEL = "body";
    private static final String ATTACHMENT_PREFIX = "attachment: ";

    /*
     * The message's fields as text: the headers it keeps, one "Name: value" line each, and its bodies, the plain
     * ones, or the HTML ones as visible text when it has no plain body. A nested message's segments are prefixed
     * with its number, and each redacted attachment's segments with its name.
     */
    private static RedactedTextBuilder text(final EmailRedactor.Result result, final List<RedactedAttachment> attachments) {

        final RedactedTextBuilder text = new RedactedTextBuilder();
        text.ensure(HEADERS_LABEL);
        text.ensure(BODY_LABEL);

        final Set<Integer> withPlainBody = new HashSet<>();
        for (final EmailRedactor.Field field : result.fields()) {
            if (isBody(field) && !field.html()) {
                withPlainBody.add(field.messageNumber());
            }
        }

        for (final EmailRedactor.Field field : result.fields()) {
            final String prefix = field.messageNumber() == 0 ? "" : "attached message " + field.messageNumber() + " / ";
            if (isBody(field)) {
                if (field.html() && withPlainBody.contains(field.messageNumber())) {
                    continue;
                }
                final EmailText.Rendered body = field.html()
                        ? EmailText.html(field.output(), field.replacedRanges())
                        : EmailText.plain(field.output(), field.replacedRanges());
                text.line(prefix + BODY_LABEL, body.text(), body.replacedRanges());
            } else {
                final String name = field.label() + ": ";
                final List<ReplacedRange> ranges = new ArrayList<>();
                for (final ReplacedRange range : field.replacedRanges()) {
                    ranges.add(range.shift(name.length()));
                }
                text.line(prefix + HEADERS_LABEL, name + RedactedTextBuilder.flatten(field.output()), ranges);
            }
        }

        for (final RedactedAttachment attachment : attachments) {
            if (attachment.result().getText() != null) {
                text.include(ATTACHMENT_PREFIX + attachment.name() + " / ", attachment.result().getText(), List.of());
            }
        }

        return text;

    }

    private static boolean isBody(final EmailRedactor.Field field) {
        return field.label().startsWith("Body (");
    }

    /*
     * Scans the message's own segments, and takes each attachment's check of its own text, naming the attachment
     * in its findings. An attachment whose check was not performed is listed as unchecked.
     */
    private LeftoverCheckResult textLeftoverCheck(final Policy policy, final RedactedTextBuilder text,
                                                  final List<RedactedAttachment> attachments,
                                                  final FilterOptions options) throws Exception {

        final List<RedactedTextBuilder.Part> own = text.parts().stream()
                .filter(part -> !part.label().startsWith(ATTACHMENT_PREFIX)).toList();
        final LeftoverCheckResult ownCheck = textLeftoverCheck(policy, own, options);

        final List<LeftoverFinding> findings = new ArrayList<>(ownCheck.isPerformed() ? ownCheck.getFindings() : List.of());
        final List<String> unchecked = new ArrayList<>();
        for (final RedactedAttachment attachment : attachments) {
            final LeftoverCheckResult check = attachment.result().getLeftoverCheck();
            if (check.isPerformed()) {
                for (final LeftoverFinding finding : check.getFindings()) {
                    findings.add(finding.inAttachment(attachment.name()));
                }
            } else {
                unchecked.add(attachment.name());
            }
        }

        if (!ownCheck.isPerformed() && unchecked.size() == attachments.size()) {
            return ownCheck;
        }
        return LeftoverCheckResult.performed(findings, unchecked);

    }

    /**
     * The service that redacts an attachment: the PDF service for a PDF, and the Word and Excel service otherwise.
     * @param type The attachment's {@link MimeType}, {@link MimeType#APPLICATION_PDF}, a Word, or an Excel type.
     * @return The {@link BinaryFilterService}.
     */
    protected BinaryFilterService attachmentService(final MimeType type) {
        return type == MimeType.APPLICATION_PDF ? pdfFilterService : ooxmlFilterService;
    }

    /*
     * Scans each redacted field, leaving out the replacements the redaction put there, and takes each redacted
     * attachment's own check, naming the attachment in its findings. An attachment whose output could not be read
     * is listed as unchecked. Attachments kept or removed are not checked; the email report lists those kept.
     */
    private LeftoverCheckResult leftoverCheck(final Policy policy, final EmailRedactor.Result result,
                                              final List<RedactedAttachment> attachments,
                                              final FilterOptions options) throws Exception {

        final LeftoverCheck.Using using = leftoverCheckUsing(policy);
        final EmailFields.Detector detector = text -> leftoverSpans(policy, using, text);

        final List<LeftoverFinding> findings = new ArrayList<>();

        for (final EmailRedactor.Field field : result.fields()) {
            final List<Span> found = field.html()
                    ? EmailFields.detectHtml(field.output(), detector)
                    : EmailFields.resolve(detector.detect(field.output()), field.output().length());
            for (final Span span : found) {
                if (!LeftoverChecker.isInserted(span, field.replacedRanges())) {
                    findings.add(LeftoverChecker.finding(span, field.output(), options, 0, 0, field.index()));
                }
            }
        }

        final List<String> unchecked = new ArrayList<>();
        for (final RedactedAttachment attachment : attachments) {
            final LeftoverCheckResult check = attachment.result().getLeftoverCheck();
            if (check.isPerformed()) {
                for (final LeftoverFinding finding : check.getFindings()) {
                    findings.add(finding.inAttachment(attachment.name()));
                }
            } else {
                unchecked.add(attachment.name());
            }
        }

        return LeftoverCheckResult.performed(findings, unchecked);

    }

    /**
     * Re-applies spans to an email message by position: each span's {@link Span#getParagraphNumber()} is a field's
     * number and its offsets are into that field, as {@link #filter} returns them. Headers, attachments, and inline
     * images are handled by the policy's {@code config.email}, and PDF, Word, and Excel attachments under
     * {@code attachments: redact} are redacted with the policy. Uses the construction-bound context and vector
     * services when there are any, otherwise new in-memory ones.
     */
    @Override
    public byte[] apply(final Policy policy, final byte[] input, final List<Span> spans, final MimeType outputMimeType) throws Exception {

        requireEmailOutput(outputMimeType);

        final ContextService contextService = defaultContextService != null ? defaultContextService : new DefaultContextService();
        final VectorService vectorService = defaultVectorService != null ? defaultVectorService : new InMemoryVectorService();

        final EmailRedactor.FieldRedactor fieldRedactor = (index, text, html) -> {
            final List<Span> fieldSpans = new ArrayList<>();
            for (final Span span : spans) {
                if (span.getParagraphNumber() == index) {
                    fieldSpans.add(span);
                }
            }
            return EmailFields.replace(text, EmailFields.resolve(fieldSpans, text.length()), FALLBACK_REPLACEMENT);
        };

        final EmailRedactor.AttachmentRedactor attachmentRedactor = (name, document, type) -> {
            final BinaryFilterService service = attachmentService(type);
            return service.filter(policy, contextService, vectorService, "default", document, type).getDocument();
        };

        return EmailRedactor.redact(input, policy.getConfig().getEmail(), fieldRedactor, attachmentRedactor).document();

    }

    private interface TextRedactor {
        TextFilterResult redact(String text) throws Exception;
    }

    private record RedactedAttachment(String name, BinaryDocumentFilterResult result) {
    }

    private TextRedactor textRedactor(final Policy policy, final ContextService contextService,
                                      final VectorService vectorService, final String context,
                                      final List<IncrementalRedaction> incrementalRedactions) throws Exception {
        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);
        final List<PostFilter> postFilters = getPostFiltersForPolicy(policy);
        return text -> {
            final TextFilterResult result = unstructuredDocumentProcessor.process(contextService, vectorService,
                    policy, filters, postFilters, context, 0, text);
            incrementalRedactions.addAll(result.getIncrementalRedactions());
            return result;
        };
    }

    private static void requireEmailOutput(final MimeType outputMimeType) {
        if (outputMimeType != null && outputMimeType != MimeType.MESSAGE_RFC822) {
            throw new IllegalArgumentException("An email message can only be redacted to " + MimeType.MESSAGE_RFC822
                    + " (.eml), not " + outputMimeType + ".");
        }
    }

}
