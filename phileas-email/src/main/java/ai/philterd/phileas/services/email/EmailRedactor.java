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
package ai.philterd.phileas.services.email;

import ai.philterd.phileas.model.filtering.EmailReport;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.ReplacedRange;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.config.Email;
import ai.philterd.phileas.services.ooxml.OoxmlDocumentType;
import jakarta.activation.DataHandler;
import jakarta.mail.BodyPart;
import jakarta.mail.Header;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimePart;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.internet.ParseException;
import jakarta.mail.util.ByteArrayDataSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redacts an email message ({@code .eml}, or an Outlook {@code .msg} read as MIME) and writes it out as
 * {@code .eml}. The policy's {@code config.email} decides which headers are removed and what happens to
 * attachments and inline images; the text fields (Subject, From, To, Cc, and every text body part) are redacted
 * by a {@link FieldRedactor}. Fields are numbered in a fixed order: each message's Subject, From, To, and Cc
 * headers, then its body parts in document order, with a nested message's fields where the message is.
 */
public final class EmailRedactor {

    private static final Set<String> TECHNICAL_HEADERS = Set.of("received", "return-path", "message-id",
            "user-agent", "x-mailer", "x-mimeole", "dkim-signature", "authentication-results", "received-spf",
            "autocrypt");

    private static final Set<String> IDENTITY_HEADERS = Set.of("bcc", "reply-to", "sender");

    // The headers redacted by the policy's identifiers, in field order.
    private static final List<String> REDACTED_HEADERS = List.of("Subject", "From", "To", "Cc");

    // Body parts that are read and redacted as plain text.
    private static final Set<String> PLAIN_TEXT_TYPES = Set.of("text/plain", "message/delivery-status",
            "message/disposition-notification", "text/rfc822-headers", "message/global-delivery-status",
            "message/global-headers", "message/global-disposition-notification");

    private static final Set<String> SIGNATURE_TYPES = Set.of("application/pkcs7-signature",
            "application/x-pkcs7-signature", "application/pgp-signature");

    private static final Set<String> ENCRYPTED_TYPES = Set.of("application/pkcs7-mime", "application/x-pkcs7-mime",
            "multipart/encrypted", "application/pgp-encrypted");

    private static final String PGP_MESSAGE = "-----BEGIN PGP MESSAGE-----";

    private static final String NEUTRALIZED_REFERENCE = "about:blank";

    /** How deeply parts and messages may nest. Real mail stays far below it; a deeper message is refused. */
    public static final int MAX_DEPTH = 100;

    // A MIME message has at least one of these; any other input is not taken for one.
    private static final Set<String> MESSAGE_HEADERS = Set.of("from", "to", "cc", "bcc", "subject", "date",
            "message-id", "mime-version", "received", "return-path", "sender", "reply-to", "in-reply-to",
            "references", "content-type");

    // A header name is printable ASCII other than the colon.
    private static final Pattern HEADER_NAME = Pattern.compile("[!-9;-~]+");

    /** One redacted text field. */
    public record Field(int index, int messageNumber, String label, boolean html, String original, String output,
                        List<Span> spans, List<ReplacedRange> replacedRanges) {
    }

    /** What a {@link FieldRedactor} did to one field. */
    public record FieldRedaction(String output, List<Span> spans, List<ReplacedRange> replacedRanges) {
    }

    /** Redacts the text of one field. */
    public interface FieldRedactor {

        /**
         * Redacts a field.
         * @param index The field's number.
         * @param text The field's text: a header's decoded value, or a body part's text (HTML as markup).
         * @param html Whether the text is HTML.
         * @return The redacted text, the spans applied with offsets into {@code text}, and where the replacements are.
         * @throws Exception Thrown if the text cannot be redacted.
         */
        FieldRedaction redact(int index, String text, boolean html) throws Exception;

    }

    /** Redacts an attachment that is a PDF, Word, Excel, RTF, CSV, or plain-text document. */
    public interface AttachmentRedactor {

        /**
         * Redacts an attachment.
         * @param name The attachment's filename, or its media type when it has none.
         * @param document The attachment.
         * @param type The attachment's type: PDF, Word, Excel, RTF, CSV, or plain text.
         * @param charset The character set the attachment's part declares, or {@code null} when it declares none.
         * @return The redacted attachment.
         * @throws Exception Thrown if the attachment cannot be redacted.
         */
        RedactedAttachment redact(String name, byte[] document, MimeType type, Charset charset) throws Exception;

    }

    /**
     * A redacted attachment.
     * @param document The redacted attachment.
     * @param charset For a text type, the character set it is written in; otherwise {@code null}. A part that
     *                declared no charset is labeled with it, so the label matches the bytes.
     */
    public record RedactedAttachment(byte[] document, Charset charset) {
    }

    /** The redacted message, its fields, and what was left uninspected or taken out. */
    public record Result(byte[] document, List<Field> fields, EmailReport report) {
    }

    private EmailRedactor() {
    }

    /**
     * Redacts an email message.
     * @param input The {@code .eml} or {@code .msg} bytes.
     * @param settings The policy's {@code config.email}.
     * @param fieldRedactor Redacts each text field.
     * @param attachmentRedactor Redacts PDF, Word, Excel, RTF, CSV, and plain-text attachments under
     *                           {@code attachments: redact}.
     * @return The {@link Result}, with the message as {@code .eml}.
     * @throws EncryptedEmailException Thrown if the message, or a message nested in it, is encrypted.
     * @throws IllegalArgumentException Thrown if the input is not an email message.
     * @throws Exception Thrown if the message cannot be redacted.
     */
    public static Result redact(final byte[] input, final Email settings, final FieldRedactor fieldRedactor,
                                final AttachmentRedactor attachmentRedactor) throws Exception {

        final Session session = session();
        final RedactableMessage message = parse(session, input);

        final Walk walk = new Walk(session, settings, fieldRedactor, attachmentRedactor);

        // Refuse before changing anything, and find the inline images whose references must be neutralized.
        walk.inspect(message, 0);

        walk.message(message, 0);
        message.saveChanges();

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        message.writeTo(out);

        return new Result(out.toByteArray(), walk.fields, new EmailReport(walk.uninspectedAttachments,
                walk.uninspectedInlineImages, walk.removedAttachments, walk.removedInlineImages, walk.droppedSignatures));

    }

    /**
     * Whether the input is an Outlook message rather than MIME.
     * @param input The input.
     * @return <code>true</code> for an Outlook {@code .msg}.
     */
    public static boolean isOutlookMessage(final byte[] input) {
        return MsgConverter.isOle2(input);
    }

    private static Session session() {
        final Properties properties = new Properties();
        // Header values written in UTF-8 rather than encoded words are read as UTF-8.
        properties.setProperty("mail.mime.allowutf8", "true");
        properties.setProperty("mail.mime.address.strict", "false");
        return Session.getInstance(properties);
    }

    private static RedactableMessage parse(final Session session, final byte[] input) throws Exception {

        if (MsgConverter.isOle2(input)) {
            return MsgConverter.convert(session, input);
        }

        final RedactableMessage message = new RedactableMessage(session, new ByteArrayInputStream(input));

        boolean known = false;
        final Enumeration<Header> headers = message.getAllHeaders();
        while (headers.hasMoreElements()) {
            final String name = headers.nextElement().getName();
            if (!HEADER_NAME.matcher(name).matches()) {
                throw new IllegalArgumentException("The input is not an email message.");
            }
            known |= MESSAGE_HEADERS.contains(name.toLowerCase(Locale.ROOT));
        }
        if (!known) {
            throw new IllegalArgumentException("The input is not an email message.");
        }

        return message;

    }

    /** One pass over a message tree. */
    private static final class Walk {

        private final Session session;
        private final Email settings;
        private final FieldRedactor fieldRedactor;
        private final AttachmentRedactor attachmentRedactor;

        private final List<Field> fields = new ArrayList<>();
        private final List<EmailReport.Part> uninspectedAttachments = new ArrayList<>();
        private final List<EmailReport.Part> uninspectedInlineImages = new ArrayList<>();
        private final List<EmailReport.Part> removedAttachments = new ArrayList<>();
        private final List<EmailReport.Part> removedInlineImages = new ArrayList<>();
        private final List<EmailReport.Part> droppedSignatures = new ArrayList<>();
        private final Set<String> removedContentIds = new LinkedHashSet<>();

        private int messages = 0;

        Walk(final Session session, final Email settings, final FieldRedactor fieldRedactor,
             final AttachmentRedactor attachmentRedactor) {
            this.session = session;
            this.settings = settings;
            this.fieldRedactor = fieldRedactor;
            this.attachmentRedactor = attachmentRedactor;
        }

        // A read-only pass: refuses an encrypted message, and notes the inline images to remove.
        void inspect(final Part part, final int depth) throws Exception {

            if (depth > MAX_DEPTH) {
                throw new IllegalArgumentException("The email message nests parts more than " + MAX_DEPTH
                        + " levels deep and was not processed.");
            }

            final String type = baseType(part);

            if (ENCRYPTED_TYPES.contains(type)) {
                throw new EncryptedEmailException("The email message's content is encrypted or wrapped (" + type
                        + ") and cannot be read, so it was not redacted.");
            }

            if (type.startsWith("multipart/")) {
                final Multipart multipart = (Multipart) part.getContent();
                for (int i = 0; i < multipart.getCount(); i++) {
                    inspect(multipart.getBodyPart(i), depth + 1);
                }
            } else if ("message/rfc822".equals(type)) {
                inspect(new RedactableMessage(session, rawContent(part)), depth + 1);
            } else if (isBodyText(part, type)) {
                if (readText(part, type).contains(PGP_MESSAGE)) {
                    throw new EncryptedEmailException("The email message has a PGP-encrypted body, which cannot be read, "
                            + "so it was not redacted.");
                }
            } else if (isInlineImage(part, type) && settings.getInlineImages() == Email.InlineImages.REMOVE
                    && part instanceof MimePart mimePart && mimePart.getContentID() != null) {
                removedContentIds.add(stripBrackets(mimePart.getContentID()));
            }

        }

        void message(final MimeMessage message, final int messageNumber) throws Exception {

            removeHeaders(message);

            for (final String name : REDACTED_HEADERS) {
                redactHeader(message, name, messageNumber);
            }

            if (content(message, messageNumber) == Outcome.REMOVE) {
                // The message's whole body was an attachment that is removed.
                message.setText("", StandardCharsets.UTF_8.name());
            }

        }

        private void removeHeaders(final MimeMessage message) throws MessagingException {

            final Email.Headers headers = settings.getHeaders();
            final Set<String> names = new LinkedHashSet<>();

            final Enumeration<Header> all = message.getAllHeaders();
            while (all.hasMoreElements()) {
                final String name = all.nextElement().getName();
                final String lower = name.toLowerCase(Locale.ROOT);
                if ((headers.isRemoveTechnical() && (TECHNICAL_HEADERS.contains(lower) || lower.startsWith("x-")
                        || lower.startsWith("arc-")))
                        || (headers.isRemoveIdentity() && (IDENTITY_HEADERS.contains(lower) || lower.startsWith("resent-")))
                        || (headers.isRemoveDate() && "date".equals(lower))
                        || (headers.isRemoveSubject() && "subject".equals(lower))) {
                    names.add(name);
                }
            }

            for (final String name : names) {
                message.removeHeader(name);
            }

        }

        private void redactHeader(final MimeMessage message, final String name, final int messageNumber) throws Exception {

            final String[] values = message.getHeader(name);
            if (values == null) {
                return;
            }

            final List<String> redacted = new ArrayList<>();
            for (final String value : values) {
                final String decoded = decodeHeader(value);
                if (decoded.isBlank()) {
                    redacted.add(value);
                } else {
                    final Field field = field(messageNumber, name, false, decoded);
                    // A replacement cannot end the header or start another. The length is kept, so offsets hold.
                    final String output = field.output().replace('\r', ' ').replace('\n', ' ');
                    redacted.add(field.output().equals(decoded) ? value
                            : MimeUtility.fold(name.length() + 2, MimeUtility.encodeText(output,
                            StandardCharsets.UTF_8.name(), null)));
                }
            }

            // setHeader keeps the header's place; any further values follow it.
            message.setHeader(name, redacted.get(0));
            for (int i = 1; i < redacted.size(); i++) {
                message.addHeader(name, redacted.get(i));
            }

        }

        private Field field(final int messageNumber, final String label, final boolean html, final String text) throws Exception {
            final int index = fields.size();
            final FieldRedaction redaction = fieldRedactor.redact(index, text, html);
            final Field field = new Field(index, messageNumber, label, html, text, redaction.output(),
                    List.copyOf(redaction.spans()), List.copyOf(redaction.replacedRanges()));
            fields.add(field);
            return field;
        }

        private enum Outcome { KEEP, REMOVE }

        // Processes a part's content. Returns REMOVE when the part itself is to be removed from its parent.
        private Outcome content(final MimePart part, final int messageNumber) throws Exception {

            final String type = baseType(part);

            if ("multipart/signed".equals(type)) {
                unwrapSigned(part, messageNumber);
                return content(part, messageNumber);
            }

            if (type.startsWith("multipart/")) {
                multipart(part, messageNumber);
                return Outcome.KEEP;
            }

            if ("message/rfc822".equals(type)) {
                if (settings.getAttachments() == Email.Attachments.REMOVE && isAttachment(part)) {
                    removedAttachments.add(reportPart(part, messageNumber, EmailReport.RemovalReason.POLICY));
                    return Outcome.REMOVE;
                }
                final int number = ++messages;
                final RedactableMessage nested = new RedactableMessage(session, rawContent(part));
                message(nested, number);
                nested.saveChanges();
                part.setContent(nested, "message/rfc822");
                return Outcome.KEEP;
            }

            if (isBodyText(part, type)) {
                bodyText(part, type, messageNumber);
                return Outcome.KEEP;
            }

            if (isInlineImage(part, type)) {
                if (settings.getInlineImages() == Email.InlineImages.REMOVE) {
                    removedInlineImages.add(reportPart(part, messageNumber, null));
                    return Outcome.REMOVE;
                }
                uninspectedInlineImages.add(reportPart(part, messageNumber, null));
                return Outcome.KEEP;
            }

            return attachment(part, type, messageNumber);

        }

        private void multipart(final MimePart part, final int messageNumber) throws Exception {

            final MimeMultipart multipart = (MimeMultipart) part.getContent();
            final List<Integer> removed = new ArrayList<>();

            for (int i = 0; i < multipart.getCount(); i++) {
                if (content((MimeBodyPart) multipart.getBodyPart(i), messageNumber) == Outcome.REMOVE) {
                    removed.add(i);
                }
            }

            Collections.reverse(removed);
            for (final int index : removed) {
                multipart.removeBodyPart(index);
            }

            // A multipart with no parts cannot be written.
            if (multipart.getCount() == 0) {
                final MimeBodyPart empty = new MimeBodyPart();
                empty.setText("", StandardCharsets.UTF_8.name());
                multipart.addBodyPart(empty);
            }

            part.setContent(multipart);

        }

        // Replaces a signed part with the content it signs, since redacting invalidates the signature.
        private void unwrapSigned(final MimePart part, final int messageNumber) throws Exception {

            final MimeMultipart signed = (MimeMultipart) part.getContent();

            for (int i = 1; i < signed.getCount(); i++) {
                droppedSignatures.add(reportPart(signed.getBodyPart(i), messageNumber, null));
            }

            final MimeBodyPart content = (MimeBodyPart) signed.getBodyPart(0);
            final Object inner = content.getContent();

            if (inner instanceof MimeMultipart multipart) {
                part.setContent(multipart);
            } else {
                part.setDataHandler(content.getDataHandler());
                part.setHeader("Content-Type", content.getContentType());
            }

            for (final String name : List.of("Content-Disposition", "Content-ID", "Content-Description")) {
                final String value = content.getHeader(name, null);
                if (value != null) {
                    part.setHeader(name, value);
                }
            }

        }

        private void bodyText(final MimePart part, final String type, final int messageNumber) throws Exception {

            final boolean html = "text/html".equals(type);
            String text = readText(part, type);

            if (html && !removedContentIds.isEmpty()) {
                text = neutralize(text);
            }

            final Field field = field(messageNumber, html ? "Body (HTML)" : "Body (text)", html, text);

            if (field.output().equals(readText(part, type))) {
                return;
            }

            if ("text/plain".equals(type) || html) {
                final ContentType contentType = new ContentType(part.getContentType());
                contentType.setParameter("charset", StandardCharsets.UTF_8.name());
                part.setContent(field.output(), contentType.toString());
            } else {
                final String contentType = part.getContentType();
                part.setDataHandler(new DataHandler(new ByteArrayDataSource(
                        field.output().getBytes(StandardCharsets.UTF_8), contentType)));
                part.setHeader("Content-Type", contentType);
            }

        }

        // Points each reference to a removed inline image at about:blank, so it does not dangle.
        private String neutralize(final String html) {
            String result = html;
            for (final String contentId : removedContentIds) {
                result = Pattern.compile("cid:" + Pattern.quote(contentId), Pattern.CASE_INSENSITIVE)
                        .matcher(result).replaceAll(Matcher.quoteReplacement(NEUTRALIZED_REFERENCE));
            }
            return result;
        }

        private Outcome attachment(final MimePart part, final String type, final int messageNumber) throws Exception {

            switch (settings.getAttachments()) {

                case REMOVE -> {
                    removedAttachments.add(reportPart(part, messageNumber, EmailReport.RemovalReason.POLICY));
                    return Outcome.REMOVE;
                }

                case REDACT -> {
                    final byte[] document = rawContent(part).readAllBytes();
                    final MimeType documentType = documentType(type, part.getFileName(), document);
                    if (documentType == null) {
                        removedAttachments.add(reportPart(part, messageNumber, EmailReport.RemovalReason.UNSUPPORTED_TYPE));
                        return Outcome.REMOVE;
                    }
                    final RedactedAttachment redacted;
                    try {
                        redacted = attachmentRedactor.redact(name(part), document, documentType, declaredCharset(part));
                    } catch (final EncryptedEmailException e) {
                        throw e;
                    } catch (final Exception e) {
                        removedAttachments.add(reportPart(part, messageNumber, EmailReport.RemovalReason.REDACTION_FAILED));
                        return Outcome.REMOVE;
                    }
                    final String contentType = labeled(part.getContentType(), redacted.charset());
                    part.setDataHandler(new DataHandler(new ByteArrayDataSource(redacted.document(), contentType)));
                    part.setHeader("Content-Type", contentType);
                    return Outcome.KEEP;
                }

                default -> {
                    uninspectedAttachments.add(reportPart(part, messageNumber, null));
                    return Outcome.KEEP;
                }

            }

        }

        private static EmailReport.Part reportPart(final Part part, final int messageNumber,
                                                   final EmailReport.RemovalReason reason) throws MessagingException {
            return new EmailReport.Part(fileName(part), baseType(part), messageNumber, reason);
        }

    }

    /**
     * The name a report or finding gives an attachment: its filename, or its media type when it has none.
     * @param part The attachment.
     * @return The name.
     * @throws MessagingException Thrown if the part cannot be read.
     */
    static String name(final Part part) throws MessagingException {
        final String fileName = fileName(part);
        return fileName == null ? baseType(part) : fileName;
    }

    private static String fileName(final Part part) {
        try {
            final String fileName = part.getFileName();
            return fileName == null ? null : MimeUtility.decodeText(fileName);
        } catch (final MessagingException | UnsupportedEncodingException e) {
            return null;
        }
    }

    // A content type with the charset a text attachment was written in, when it declares none.
    private static String labeled(final String contentType, final Charset charset) {
        if (charset == null) {
            return contentType;
        }
        try {
            final ContentType type = new ContentType(contentType);
            // Only a text type takes a charset; mail would otherwise add UTF-8 to one without it.
            if (!"text".equalsIgnoreCase(type.getPrimaryType()) || type.getParameter("charset") != null) {
                return contentType;
            }
            type.setParameter("charset", charset.name());
            return type.toString();
        } catch (final ParseException e) {
            return contentType;
        }
    }

    // The charset a part's content type declares, or null when it declares none. One Java cannot use is refused.
    private static Charset declaredCharset(final Part part) throws MessagingException {
        final String charset;
        try {
            charset = new ContentType(part.getContentType()).getParameter("charset");
        } catch (final ParseException | NullPointerException e) {
            return null;
        }
        return charset == null || charset.isBlank() ? null : Charset.forName(charset.strip());
    }

    /*
     * An attachment's type, from its media type, its filename, or its content; otherwise null. A plain-text
     * attachment is one by its media type: a part only gets here as an attachment, since body text is handled first.
     */
    static MimeType documentType(final String type, final String fileName, final byte[] document) {

        final String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);

        if (MimeType.APPLICATION_PDF.getValue().equals(type) || name.endsWith(".pdf")) {
            return MimeType.APPLICATION_PDF;
        }
        if (MimeType.APPLICATION_DOCX.getValue().equals(type) || name.endsWith(".docx")) {
            return MimeType.APPLICATION_DOCX;
        }
        if (MimeType.APPLICATION_XLSX.getValue().equals(type) || name.endsWith(".xlsx")) {
            return MimeType.APPLICATION_XLSX;
        }
        if (MimeType.TEXT_CSV.getValue().equals(type) || name.endsWith(".csv")) {
            return MimeType.TEXT_CSV;
        }
        if (MimeType.APPLICATION_RTF.getValue().equals(type) || "text/rtf".equals(type) || name.endsWith(".rtf")) {
            return MimeType.APPLICATION_RTF;
        }
        if (MimeType.TEXT_PLAIN.getValue().equals(type)) {
            return MimeType.TEXT_PLAIN;
        }

        // A generic type with no telling name: look at the content.
        if ("application/octet-stream".equals(type) && name.lastIndexOf('.') < 0) {
            if (document.length >= 5 && new String(document, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
                return MimeType.APPLICATION_PDF;
            }
            final OoxmlDocumentType ooxml = OoxmlDocumentType.of(document);
            if (ooxml != null) {
                return ooxml.mimeType();
            }
            if (document.length >= 5 && new String(document, 0, 5, StandardCharsets.US_ASCII).equals("{\\rtf")) {
                return MimeType.APPLICATION_RTF;
            }
        }

        return null;

    }

    private static boolean isAttachment(final Part part) throws MessagingException {
        return Part.ATTACHMENT.equalsIgnoreCase(disposition(part)) || part.getFileName() != null;
    }

    private static boolean isBodyText(final Part part, final String type) throws MessagingException {
        return ("text/html".equals(type) || PLAIN_TEXT_TYPES.contains(type)) && !isAttachment(part);
    }

    // An image shown in the body, as opposed to an image attached as a file.
    private static boolean isInlineImage(final Part part, final String type) throws MessagingException {
        if (!type.startsWith("image/") || Part.ATTACHMENT.equalsIgnoreCase(disposition(part))) {
            return false;
        }
        return Part.INLINE.equalsIgnoreCase(disposition(part))
                || (part instanceof MimePart mimePart && mimePart.getContentID() != null);
    }

    private static String disposition(final Part part) {
        try {
            return part.getDisposition();
        } catch (final MessagingException e) {
            return null;
        }
    }

    private static String baseType(final Part part) throws MessagingException {
        try {
            return new ContentType(part.getContentType()).getBaseType().toLowerCase(Locale.ROOT);
        } catch (final ParseException | NullPointerException e) {
            return "text/plain";
        }
    }

    // A part's content with its transfer encoding decoded.
    private static InputStream rawContent(final Part part) throws IOException, MessagingException {
        return part.getInputStream();
    }

    private static String readText(final Part part, final String type) throws IOException, MessagingException {
        if ("text/plain".equals(type) || "text/html".equals(type)) {
            try {
                final Object content = part.getContent();
                if (content instanceof String text) {
                    return text;
                }
            } catch (final UnsupportedEncodingException e) {
                // An unknown charset: read the bytes as UTF-8 below.
            }
        }
        try (InputStream in = part.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String decodeHeader(final String value) {
        final String unfolded = MimeUtility.unfold(value);
        try {
            return MimeUtility.decodeText(unfolded);
        } catch (final UnsupportedEncodingException e) {
            return unfolded;
        }
    }

    private static String stripBrackets(final String contentId) {
        final String id = contentId.strip();
        return id.startsWith("<") && id.endsWith(">") ? id.substring(1, id.length() - 1) : id;
    }

}
