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

import ai.philterd.phileas.services.rtf.RtfTextExtractor;
import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.poi.hsmf.MAPIMessage;
import org.apache.poi.hsmf.datatypes.AttachmentChunks;
import org.apache.poi.hsmf.datatypes.ByteChunk;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.hsmf.datatypes.PropertyValue;
import org.apache.poi.hsmf.datatypes.RecipientChunks;
import org.apache.poi.hsmf.datatypes.StringChunk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads an Outlook {@code .msg} into a MIME message, which is redacted and written out as {@code .eml}. The
 * subject, sender, recipients (To, Cc, and Bcc), date, bodies, and attachments are carried over, and an embedded
 * message becomes a nested {@code message/rfc822} part. A message whose only body is RTF has that body read as
 * plain text so it is not dropped. The transport headers an Outlook message may hold are not carried over.
 */
final class MsgConverter {

    // OLE2 compound file signature.
    private static final byte[] OLE2 = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};

    private static final String UNKNOWN_ADDRESS = "unknown@invalid";

    private MsgConverter() {
    }

    /**
     * Whether the input is an OLE2 compound file, as an Outlook message is.
     * @param input The input.
     * @return <code>true</code> if it starts with the OLE2 signature.
     */
    static boolean isOle2(final byte[] input) {
        return input.length >= OLE2.length && Arrays.equals(Arrays.copyOf(input, OLE2.length), OLE2);
    }

    /**
     * Reads an Outlook message.
     * @param session The mail session.
     * @param input The {@code .msg} bytes.
     * @return The message as MIME.
     * @throws IOException Thrown if the input is not an Outlook message.
     * @throws MessagingException Thrown if the MIME message cannot be built.
     */
    static RedactableMessage convert(final Session session, final byte[] input) throws IOException, MessagingException {
        final MAPIMessage msg;
        try {
            msg = new MAPIMessage(new ByteArrayInputStream(input));
        } catch (final RuntimeException e) {
            throw new IOException("The input is not an Outlook message.", e);
        }
        return convert(session, msg);
    }

    private static RedactableMessage convert(final Session session, final MAPIMessage msg) throws IOException, MessagingException {

        msg.setReturnNullOnMissingChunk(true);
        if (!msg.has7BitEncodingStrings()) {
            msg.guess7BitEncoding();
        }

        final RedactableMessage message = new RedactableMessage(session);

        final String subject = read(() -> msg.getSubject());
        if (!isBlank(subject)) {
            message.setSubject(subject, StandardCharsets.UTF_8.name());
        }

        final StringChunk fromChunk = msg.getMainChunks().getEmailFromChunk();
        final InternetAddress from = address(read(() -> msg.getDisplayFrom()), fromChunk == null ? null : fromChunk.getValue());
        if (from != null) {
            message.setFrom(from);
        }

        final Set<String> ccNames = names(read(() -> msg.getDisplayCC()));
        final Set<String> bccNames = names(read(() -> msg.getDisplayBCC()));

        final List<InternetAddress> to = new ArrayList<>();
        final List<InternetAddress> cc = new ArrayList<>();
        final List<InternetAddress> bcc = new ArrayList<>();

        for (final RecipientChunks recipient : msg.getRecipientDetailsChunks()) {
            final InternetAddress address = address(read(recipient::getRecipientName), read(recipient::getRecipientEmailAddress));
            if (address == null) {
                continue;
            }
            switch (recipientType(recipient, ccNames, bccNames)) {
                case 2 -> cc.add(address);
                case 3 -> bcc.add(address);
                default -> to.add(address);
            }
        }

        setRecipients(message, Message.RecipientType.TO, to);
        setRecipients(message, Message.RecipientType.CC, cc);
        setRecipients(message, Message.RecipientType.BCC, bcc);

        final Calendar date = read(() -> msg.getMessageDate());
        if (date != null) {
            message.setSentDate(date.getTime());
        }

        final List<MimeBodyPart> bodies = new ArrayList<>();

        final String text = read(() -> msg.getTextBody());
        final String html = read(() -> msg.getHtmlBody());

        if (!isBlank(text)) {
            bodies.add(textPart(text, "plain"));
        }
        if (!isBlank(html)) {
            bodies.add(textPart(html, "html"));
        }
        if (bodies.isEmpty()) {
            final String rtf = read(() -> msg.getRtfBody());
            if (!isBlank(rtf)) {
                bodies.add(textPart(RtfTextExtractor.extract(rtf), "plain"));
            }
        }

        final List<MimeBodyPart> attachments = new ArrayList<>();
        for (final AttachmentChunks attachment : msg.getAttachmentFiles()) {
            final MimeBodyPart part = attachmentPart(session, attachment);
            if (part != null) {
                attachments.add(part);
            }
        }

        final MimeBodyPart body;
        if (bodies.isEmpty()) {
            body = textPart("", "plain");
        } else if (bodies.size() == 1) {
            body = bodies.get(0);
        } else {
            final MimeMultipart alternative = new MimeMultipart("alternative");
            for (final MimeBodyPart part : bodies) {
                alternative.addBodyPart(part);
            }
            body = new MimeBodyPart();
            body.setContent(alternative);
        }

        if (attachments.isEmpty()) {
            if (body.getContent() instanceof MimeMultipart multipart) {
                message.setContent(multipart);
            } else {
                message.setText((String) body.getContent(), StandardCharsets.UTF_8.name(),
                        body.isMimeType("text/html") ? "html" : "plain");
            }
        } else {
            final MimeMultipart mixed = new MimeMultipart("mixed");
            mixed.addBodyPart(body);
            for (final MimeBodyPart part : attachments) {
                mixed.addBodyPart(part);
            }
            message.setContent(mixed);
        }

        message.saveChanges();
        return message;

    }

    private static MimeBodyPart attachmentPart(final Session session, final AttachmentChunks attachment)
            throws IOException, MessagingException {

        final String name = firstNonBlank(value(attachment.getAttachLongFileName()), value(attachment.getAttachFileName()),
                value(attachment.getAttachDisplayName()));

        final MimeBodyPart part = new MimeBodyPart();

        if (attachment.isEmbeddedMessage()) {
            final MAPIMessage embedded = attachment.getEmbeddedMessage();
            if (embedded == null) {
                return null;
            }
            part.setContent(convert(session, embedded), "message/rfc822");
            part.setDisposition(Part.ATTACHMENT);
            if (name != null) {
                part.setFileName(name);
            }
            return part;
        }

        final ByteChunk data = attachment.getAttachData();
        byte[] bytes = data == null ? null : data.getValue();
        if (bytes == null) {
            bytes = attachment.getEmbeddedAttachmentObject();
        }
        if (bytes == null) {
            return null;
        }

        final String mimeType = firstNonBlank(value(attachment.getAttachMimeTag()), "application/octet-stream");
        part.setDataHandler(new DataHandler(new ByteArrayDataSource(bytes, mimeType)));
        part.setHeader("Content-Type", mimeType);

        final String contentId = value(attachment.getAttachContentId());
        if (contentId != null && mimeType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            part.setDisposition(Part.INLINE);
            part.setContentID("<" + contentId.replaceAll("^<|>$", "") + ">");
        } else {
            part.setDisposition(Part.ATTACHMENT);
        }
        if (name != null) {
            part.setFileName(name);
        }

        return part;

    }

    // MAPI PR_RECIPIENT_TYPE: 1 To, 2 Cc, 3 Bcc. Without it, the display lists say which a recipient is.
    private static int recipientType(final RecipientChunks recipient, final Set<String> ccNames, final Set<String> bccNames) {

        final List<PropertyValue> values = recipient.getProperties().get(MAPIProperty.RECIPIENT_TYPE);
        if (values != null && !values.isEmpty() && values.get(0).getValue() instanceof Number number) {
            return number.intValue();
        }

        final String name = read(recipient::getRecipientName);
        if (name != null && bccNames.contains(name.strip())) {
            return 3;
        }
        if (name != null && ccNames.contains(name.strip())) {
            return 2;
        }
        return 1;

    }

    private static void setRecipients(final RedactableMessage message, final Message.RecipientType type,
                                      final List<InternetAddress> addresses) throws MessagingException {
        if (!addresses.isEmpty()) {
            message.setRecipients(type, addresses.toArray(new InternetAddress[0]));
        }
    }

    private static InternetAddress address(final String name, final String email) {
        if (isBlank(email) && isBlank(name)) {
            return null;
        }
        try {
            final InternetAddress address = new InternetAddress();
            address.setAddress(isBlank(email) ? UNKNOWN_ADDRESS : email.strip());
            if (!isBlank(name) && !name.strip().equals(address.getAddress())) {
                address.setPersonal(name.strip(), StandardCharsets.UTF_8.name());
            }
            return address;
        } catch (final UnsupportedEncodingException e) {
            return null;
        }
    }

    private static MimeBodyPart textPart(final String text, final String subtype) throws MessagingException {
        final MimeBodyPart part = new MimeBodyPart();
        part.setText(text, StandardCharsets.UTF_8.name(), subtype);
        return part;
    }

    private static Set<String> names(final String displayList) {
        if (isBlank(displayList)) {
            return Set.of();
        }
        return Arrays.stream(displayList.split(";")).map(String::strip).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
    }

    private interface Reader<T> {
        T read() throws Exception;
    }

    // A chunk that is missing or unreadable is treated as absent.
    private static <T> T read(final Reader<T> reader) {
        try {
            return reader.read();
        } catch (final Exception e) {
            return null;
        }
    }

    private static String value(final StringChunk chunk) {
        if (chunk == null) {
            return null;
        }
        final String value = chunk.getValue();
        return isBlank(value) ? null : value.strip();
    }

    private static String firstNonBlank(final String... values) {
        for (final String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

}
