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

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.EmailReport;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.LeftoverCheckResult;
import ai.philterd.phileas.model.filtering.LeftoverFinding;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.EmailFilterService;
import ai.philterd.phileas.services.filters.filtering.FilterOptions;
import ai.philterd.phileas.services.rtf.RtfFormatException;
import com.google.gson.Gson;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static ai.philterd.phileas.services.email.EmailFixtures.allText;
import static ai.philterd.phileas.services.email.EmailFixtures.attachment;
import static ai.philterd.phileas.services.email.EmailFixtures.bytes;
import static ai.philterd.phileas.services.email.EmailFixtures.multipart;
import static ai.philterd.phileas.services.email.EmailFixtures.nested;
import static ai.philterd.phileas.services.email.EmailFixtures.parse;
import static ai.philterd.phileas.services.email.EmailFixtures.text;

public class EmailFilterServiceTest {

    private static final String SSN = "123-45-6789";
    private static final String EMAIL = "pat@example.com";
    private static final String OTHER_EMAIL = "lee@example.org";

    private static final Map<String, List<String>> GROUPS = Map.of(
            "removeTechnical", List.of("Received", "Return-Path", "Message-ID", "User-Agent", "X-Mailer", "X-MimeOLE",
                    "DKIM-Signature", "Authentication-Results", "Received-SPF", "Autocrypt", "X-Custom-Header", "ARC-Seal"),
            "removeIdentity", List.of("Bcc", "Reply-To", "Sender", "Resent-From", "Resent-Date"),
            "removeDate", List.of("Date"),
            "removeSubject", List.of("Subject"));

    // Headers no setting removes.
    private static final List<String> UNTOUCHED = List.of("Thread-Topic", "Keywords", "MIME-Version", "Importance");

    private static EmailFilterService service() {
        return new EmailFilterService(new PhileasConfiguration(new Properties()), new DefaultContextService(),
                new InMemoryVectorService(), null);
    }

    private static Policy policy(final String email) {
        return policy(email, "");
    }

    private static Policy policy(final String email, final String extraConfig) {
        return new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}},\"config\":{"
                + (email == null ? "" : "\"email\":" + email) + (email != null && !extraConfig.isEmpty() ? "," : "")
                + extraConfig + "}}", Policy.class);
    }

    private static BinaryDocumentFilterResult filter(final Policy policy, final byte[] input) throws Exception {
        return service().filter(policy, "context", input, MimeType.MESSAGE_RFC822);
    }

    private static MimeMessage message(final String subject, final Part body) throws Exception {
        final MimeMessage message = new MimeMessage(EmailFixtures.SESSION);
        message.setSubject(subject);
        message.setFrom("Pat Sender <" + EMAIL + ">");
        message.setRecipients(Message.RecipientType.TO, "Lee Recipient <" + OTHER_EMAIL + ">");
        message.setRecipients(Message.RecipientType.CC, "copy@example.net");
        if (body instanceof MimeBodyPart part && part.getContent() instanceof Multipart multipart) {
            message.setContent(multipart);
        } else {
            message.setText(body == null ? "" : (String) ((MimeBodyPart) body).getContent(), "UTF-8");
        }
        return message;
    }

    private static MimeMessage withEveryHeader(final MimeMessage message) throws Exception {
        for (final List<String> group : GROUPS.values()) {
            for (final String name : group) {
                if (!"Subject".equals(name)) {
                    message.addHeader(name, "value-of-" + name.toLowerCase());
                }
            }
        }
        for (final String name : UNTOUCHED) {
            if (!"MIME-Version".equals(name)) {
                message.addHeader(name, "kept-" + name.toLowerCase());
            }
        }
        return message;
    }

    // --- Text fields -------------------------------------------------------------------------------

    @Test
    public void redactsSubjectAddressesAndBodies() throws Exception {

        final String html = "<p>Write to <a href=\"mailto:" + OTHER_EMAIL + "\">us</a>, or pat&#64;example.com, SSN "
                + SSN + ".</p>";
        final MimeMessage input = message("SSN " + SSN, multipart("alternative",
                text("Plain body for " + EMAIL + " with SSN " + SSN + ".", "plain"), text(html, "html")));

        final BinaryDocumentFilterResult result = filter(policy(null), bytes(input));
        final MimeMessage output = parse(result.getDocument());
        final String all = allText(output);

        Assertions.assertFalse(all.contains(SSN), all);
        Assertions.assertFalse(all.contains(EMAIL), all);
        Assertions.assertFalse(all.contains(OTHER_EMAIL), all);
        Assertions.assertFalse(all.contains("pat&#64;example.com"), all);
        Assertions.assertTrue(all.contains("{{{REDACTED-ssn}}}"), all);
        Assertions.assertTrue(all.contains("Plain body for"), all);
        Assertions.assertTrue(output.getSubject().startsWith("SSN {{{REDACTED-ssn}}}"));
        Assertions.assertTrue(output.getHeader("Cc")[0].contains("{{{REDACTED-email-address}}}"));

        // Spans are numbered by field: Subject, From, To, Cc, then the two bodies.
        Assertions.assertTrue(result.getExplanation().appliedSpans().stream().anyMatch(span -> span.getParagraphNumber() == 0));
        Assertions.assertTrue(result.getExplanation().appliedSpans().stream().anyMatch(span -> span.getParagraphNumber() == 5));
        Assertions.assertNotNull(result.getEmail());
        Assertions.assertFalse(result.getEmail().hasNotices());

    }

    @Test
    public void redactsEncodedHeadersAndDeliveryStatusParts() throws Exception {

        final String subject = "=?UTF-8?B?" + java.util.Base64.getEncoder().encodeToString(
                ("Caf\u00e9 SSN " + SSN).getBytes(StandardCharsets.UTF_8)) + "?=";
        final String eml = "From: postmaster@example.net\r\nTo: " + EMAIL + "\r\nSubject: " + subject + "\r\n"
                + "MIME-Version: 1.0\r\nContent-Type: multipart/report; report-type=delivery-status; boundary=\"r\"\r\n\r\n"
                + "--r\r\nContent-Type: text/plain\r\n\r\nDelivery failed.\r\n"
                + "--r\r\nContent-Type: message/delivery-status\r\n\r\nReporting-MTA: dns; mail.example.net\r\n\r\n"
                + "Final-Recipient: rfc822; " + OTHER_EMAIL + "\r\nAction: failed\r\nStatus: 5.1.1\r\n"
                + "--r\r\nContent-Type: text/rfc822-headers\r\n\r\nFrom: " + EMAIL + "\r\nSubject: SSN " + SSN + "\r\n"
                + "--r--\r\n";

        final BinaryDocumentFilterResult result = filter(policy(null), eml.getBytes(StandardCharsets.UTF_8));
        final MimeMessage output = parse(result.getDocument());
        final String all = allText(output);

        Assertions.assertEquals("Caf\u00e9 SSN {{{REDACTED-ssn}}}", output.getSubject());
        Assertions.assertFalse(all.contains(OTHER_EMAIL), all);
        Assertions.assertFalse(all.contains(EMAIL), all);
        Assertions.assertFalse(all.contains(SSN), all);
        Assertions.assertTrue(all.contains("Final-Recipient: rfc822; {{{REDACTED-email-address}}}"), all);
        Assertions.assertTrue(result.getEmail().getUninspectedAttachments().isEmpty());

        // The report is serialized with the result.
        Assertions.assertTrue(result.toString().contains("\"email\":{\"uninspectedAttachments\":[]"), result.toString());

    }

    @Test
    public void redactsANestedMessageWithTheSameSettings() throws Exception {

        final MimeMessage forwarded = withEveryHeader(message("Forwarded SSN " + SSN,
                text("Inner body " + EMAIL + ".", "plain")));
        final MimeMessage input = message("Outer", multipart("mixed", text("See below.", "plain"), nested(forwarded)));

        final MimeMessage output = parse(filter(policy("{\"headers\":{\"removeSubject\":false}}"), bytes(input)).getDocument());
        final MimeMessage inner = innerMessage(output);

        Assertions.assertTrue(inner.getSubject().contains("{{{REDACTED-ssn}}}"), inner.getSubject());
        final String all = allText(inner);
        Assertions.assertFalse(all.contains(EMAIL), all);
        Assertions.assertFalse(all.contains(SSN), all);
        Assertions.assertNull(inner.getHeader("Received"));
        Assertions.assertNull(inner.getHeader("Bcc"));
        Assertions.assertNotNull(inner.getHeader("Date"));

    }

    @Test
    public void eachHeaderSettingRemovesExactlyItsGroup() throws Exception {

        for (final String setting : GROUPS.keySet()) {

            final StringBuilder headers = new StringBuilder("{");
            for (final String other : GROUPS.keySet()) {
                headers.append('"').append(other).append("\":").append(other.equals(setting)).append(',');
            }
            headers.setLength(headers.length() - 1);
            headers.append('}');

            final MimeMessage forwarded = withEveryHeader(message("Inner subject", text("Inner.", "plain")));
            final MimeMessage input = withEveryHeader(message("Outer subject",
                    multipart("mixed", text("Outer.", "plain"), nested(forwarded))));

            final MimeMessage output = parse(filter(policy("{\"headers\":" + headers + "}"), bytes(input)).getDocument());

            for (final MimeMessage message : List.of(output, innerMessage(output))) {
                for (final Map.Entry<String, List<String>> group : GROUPS.entrySet()) {
                    for (final String name : group.getValue()) {
                        final boolean removed = group.getKey().equals(setting);
                        Assertions.assertEquals(removed, message.getHeader(name) == null,
                                setting + ": " + name + (removed ? " was kept" : " was removed"));
                    }
                }
                for (final String name : UNTOUCHED) {
                    Assertions.assertNotNull(message.getHeader(name), setting + " removed " + name);
                }
                Assertions.assertEquals("kept-keywords", message.getHeader("Keywords")[0]);
            }

        }

    }

    @Test
    public void removeSubjectRemovesItInNestedMessagesToo() throws Exception {

        final MimeMessage forwarded = message("Inner " + SSN, text("Inner.", "plain"));
        final MimeMessage input = message("Outer " + SSN, multipart("mixed", text("Outer.", "plain"), nested(forwarded)));

        final MimeMessage output = parse(filter(policy("{\"headers\":{\"removeSubject\":true}}"), bytes(input)).getDocument());

        Assertions.assertNull(output.getHeader("Subject"));
        Assertions.assertNull(innerMessage(output).getHeader("Subject"));

    }

    @Test
    public void addsNoDateOrMessageIdTheInputDidNotHave() throws Exception {

        final MimeMessage input = message("Hello", text("Body.", "plain"));
        final byte[] eml = new String(bytes(input), StandardCharsets.UTF_8)
                .replaceAll("(?m)^(Date|Message-ID):.*\\r?\\n", "").getBytes(StandardCharsets.UTF_8);
        Assertions.assertNull(parse(eml).getHeader("Date"));

        final MimeMessage output = parse(filter(policy("{\"headers\":{\"removeTechnical\":false}}"), eml).getDocument());

        Assertions.assertNull(output.getHeader("Date"));
        Assertions.assertNull(output.getHeader("Message-ID"));

    }

    @Test
    public void keepsTheMessageIdWhenTechnicalHeadersAreKept() throws Exception {

        final byte[] eml = new String(bytes(message("Hello", text("Body.", "plain"))), StandardCharsets.UTF_8)
                .replaceAll("(?m)^Message-ID:.*$", "Message-ID: value-of-message-id").getBytes(StandardCharsets.UTF_8);
        final MimeMessage output = parse(filter(policy("{\"headers\":{\"removeTechnical\":false}}"), eml).getDocument());

        Assertions.assertEquals("value-of-message-id", output.getHeader("Message-ID")[0]);

    }

    // --- Attachments -------------------------------------------------------------------------------

    private static MimeMessage withAttachments() throws Exception {
        return message("Files", multipart("mixed", text("Attached.", "plain"),
                attachment("report.pdf", "application/pdf", EmailFixtures.pdf("SSN " + SSN)),
                attachment("letter.docx", "application/octet-stream", EmailFixtures.docx("Write to " + EMAIL)),
                attachment("sheet.xlsx", MimeType.APPLICATION_XLSX.getValue(), EmailFixtures.xlsx("Write to " + EMAIL)),
                attachment("archive.zip", "application/zip", "not inspected".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    public void keepsAttachmentsAndReportsThemUninspected() throws Exception {

        final BinaryDocumentFilterResult result = filter(policy(null), bytes(withAttachments()));

        final List<String> names = result.getEmail().getUninspectedAttachments().stream()
                .map(EmailReport.Part::getFileName).toList();
        Assertions.assertEquals(List.of("report.pdf", "letter.docx", "sheet.xlsx", "archive.zip"), names);
        Assertions.assertEquals(4, attachments(parse(result.getDocument())).size());

    }

    @Test
    public void removesAttachmentsAndTheirNames() throws Exception {

        final BinaryDocumentFilterResult result = filter(policy("{\"attachments\":\"remove\"}"), bytes(withAttachments()));
        final String eml = new String(result.getDocument(), StandardCharsets.UTF_8);

        Assertions.assertTrue(attachments(parse(result.getDocument())).isEmpty());
        for (final String name : List.of("report.pdf", "letter.docx", "sheet.xlsx", "archive.zip")) {
            Assertions.assertFalse(eml.contains(name), name);
        }
        Assertions.assertFalse(result.getEmail().hasNotices());
        Assertions.assertTrue(allText(parse(result.getDocument())).contains("Attached."));

    }

    @Test
    public void redactsPdfWordAndExcelAttachmentsAndRemovesTheRest() throws Exception {

        final BinaryDocumentFilterResult result = filter(policy("{\"attachments\":\"redact\"}"), bytes(withAttachments()));
        final List<MimeBodyPart> attachments = attachments(parse(result.getDocument()));

        Assertions.assertEquals(List.of("report.pdf", "letter.docx", "sheet.xlsx"),
                attachments.stream().map(EmailFilterServiceTest::fileName).toList());

        try (var pdf = Loader.loadPDF(content(attachments.get(0)))) {
            Assertions.assertFalse(new PDFTextStripper().getText(pdf).contains(SSN));
        }
        final String docx = EmailFixtures.packageXml(content(attachments.get(1)));
        Assertions.assertFalse(docx.contains(EMAIL), docx);
        Assertions.assertTrue(docx.contains("{{{REDACTED-email-address}}}"), docx);
        final String xlsx = EmailFixtures.packageXml(content(attachments.get(2)));
        Assertions.assertFalse(xlsx.contains(EMAIL), xlsx);

        final List<EmailReport.Part> removed = result.getEmail().getRemovedAttachments();
        Assertions.assertEquals(1, removed.size());
        Assertions.assertEquals("archive.zip", removed.get(0).getFileName());
        Assertions.assertEquals(EmailReport.RemovalReason.UNSUPPORTED_TYPE, removed.get(0).getReason());
        Assertions.assertTrue(result.getEmail().getUninspectedAttachments().isEmpty());

    }

    @Test
    public void removeAlsoRemovesAForwardedMessageAttachedAsAFile() throws Exception {

        final MimeBodyPart forwarded = nested(message("Inner", text("Inner.", "plain")));
        forwarded.setDisposition(Part.ATTACHMENT);
        final MimeMessage input = message("Outer", multipart("mixed", text("Outer.", "plain"), forwarded));

        final MimeMessage output = parse(filter(policy("{\"attachments\":\"remove\"}"), bytes(input)).getDocument());

        Assertions.assertFalse(allText(output).contains("Inner."));
        Assertions.assertTrue(allText(output).contains("Outer."));

    }

    @Test
    public void recognizesAnUnnamedGenericAttachmentByItsContent() throws Exception {

        final MimeBodyPart unnamed = new MimeBodyPart();
        unnamed.setDataHandler(new jakarta.activation.DataHandler(new jakarta.mail.util.ByteArrayDataSource(
                EmailFixtures.docx("Write to " + EMAIL), "application/octet-stream")));
        unnamed.setDisposition(Part.ATTACHMENT);
        final MimeMessage input = message("Files", multipart("mixed", text("Attached.", "plain"), unnamed));

        final BinaryDocumentFilterResult result = filter(policy("{\"attachments\":\"redact\"}"), bytes(input));
        final List<MimeBodyPart> attachments = attachments(parse(result.getDocument()));

        Assertions.assertEquals(1, attachments.size());
        Assertions.assertFalse(EmailFixtures.packageXml(content(attachments.get(0))).contains(EMAIL));
        Assertions.assertTrue(result.getEmail().getRemovedAttachments().isEmpty());

    }

    @Test
    public void matchesHeaderNamesWithoutRegardToCase() throws Exception {

        final String eml = "From: a@example.com\r\nsubject: SSN " + SSN + "\r\nx-mailer: Example Mail\r\n"
                + "bcc: hidden@example.net\r\nreceived: from mail.example.net\r\n\r\nBody.\r\n";

        final MimeMessage output = parse(filter(policy(null), eml.getBytes(StandardCharsets.UTF_8)).getDocument());
        final String all = allText(output);

        Assertions.assertFalse(all.toLowerCase().contains("x-mailer"), all);
        Assertions.assertFalse(all.toLowerCase().contains("bcc"), all);
        Assertions.assertFalse(all.toLowerCase().contains("received"), all);
        Assertions.assertEquals("SSN {{{REDACTED-ssn}}}", output.getSubject());

    }

    @Test
    public void removesAnAttachmentThatCannotBeRedacted() throws Exception {

        final MimeMessage input = message("Files", multipart("mixed", text("Attached.", "plain"),
                attachment("damaged.pdf", "application/pdf", "%PDF-not really".getBytes(StandardCharsets.US_ASCII))));

        final BinaryDocumentFilterResult result = filter(policy("{\"attachments\":\"redact\"}"), bytes(input));

        Assertions.assertTrue(attachments(parse(result.getDocument())).isEmpty());
        Assertions.assertEquals(EmailReport.RemovalReason.REDACTION_FAILED,
                result.getEmail().getRemovedAttachments().get(0).getReason());

    }

    // --- Signed and encrypted ----------------------------------------------------------------------

    @Test
    public void dropsTheSignatureOfASignedMessageAndReportsIt() throws Exception {

        for (final String[] signature : List.of(
                new String[]{"application/pkcs7-signature", "application/pkcs7-signature"},
                new String[]{"application/pgp-signature", "application/pgp-signature"})) {

            final MimeBodyPart signed = multipart("signed; protocol=\"" + signature[0] + "\"",
                    text("Signed body " + EMAIL + ".", "plain"),
                    attachment("smime.p7s", signature[1], new byte[]{1, 2, 3}));
            final MimeMessage input = message("Signed", signed);

            final BinaryDocumentFilterResult result = filter(policy(null), bytes(input));
            final MimeMessage output = parse(result.getDocument());

            Assertions.assertFalse(output.isMimeType("multipart/signed"), output.getContentType());
            Assertions.assertFalse(new String(result.getDocument(), StandardCharsets.UTF_8).contains(signature[1]));
            Assertions.assertFalse(allText(output).contains(EMAIL));
            Assertions.assertTrue(allText(output).contains("Signed body"));
            Assertions.assertEquals(1, result.getEmail().getDroppedSignatures().size());
            Assertions.assertEquals(signature[1], result.getEmail().getDroppedSignatures().get(0).getContentType());

        }

    }

    @Test
    public void refusesAnEncryptedMessage() throws Exception {

        final MimeMessage smime = message("Encrypted", null);
        smime.setDataHandler(new jakarta.activation.DataHandler(new jakarta.mail.util.ByteArrayDataSource(
                new byte[]{1, 2, 3}, "application/pkcs7-mime; smime-type=enveloped-data")));

        final MimeMessage pgp = message("Encrypted", multipart("encrypted; protocol=\"application/pgp-encrypted\"",
                attachment("version.asc", "application/pgp-encrypted", "Version: 1".getBytes(StandardCharsets.US_ASCII)),
                attachment("encrypted.asc", "application/octet-stream", "data".getBytes(StandardCharsets.US_ASCII))));

        final MimeMessage inline = message("Encrypted", text("-----BEGIN PGP MESSAGE-----\nhQEMA\n-----END PGP MESSAGE-----", "plain"));

        final MimeMessage nestedEncrypted = message("Outer", multipart("mixed", text("See below.", "plain"), nested(smime)));

        for (final MimeMessage input : List.of(smime, pgp, inline, nestedEncrypted)) {
            final byte[] eml = bytes(input);
            Assertions.assertThrows(EncryptedEmailException.class, () -> filter(policy(null), eml));
        }

    }

    // --- Inline images -----------------------------------------------------------------------------

    private static MimeMessage withInlineImage() throws Exception {
        final MimeBodyPart image = new MimeBodyPart();
        image.setDataHandler(new jakarta.activation.DataHandler(new jakarta.mail.util.ByteArrayDataSource(
                new byte[]{(byte) 0x89, 'P', 'N', 'G'}, "image/png")));
        image.setContentID("<image001.png@01D9A1B2.C3D4E5F0>");
        image.setDisposition(Part.INLINE);
        return message("Picture", multipart("related",
                text("<p>Logo <img src=\"cid:image001.png@01D9A1B2.C3D4E5F0\"></p>", "html"), image));
    }

    @Test
    public void removesInlineImagesAndNeutralizesTheirReferences() throws Exception {

        final BinaryDocumentFilterResult result = filter(policy("{\"inlineImages\":\"remove\"}"), bytes(withInlineImage()));
        final MimeMessage output = parse(result.getDocument());
        final String all = allText(output);

        Assertions.assertFalse(all.contains("cid:"), all);
        Assertions.assertTrue(all.contains("about:blank"), all);
        Assertions.assertFalse(new String(result.getDocument(), StandardCharsets.UTF_8).contains("image/png"));
        Assertions.assertFalse(result.getEmail().hasNotices());

    }

    @Test
    public void keepsInlineImagesAndReportsThemUninspected() throws Exception {

        final BinaryDocumentFilterResult result = filter(policy(null), bytes(withInlineImage()));

        Assertions.assertEquals(1, result.getEmail().getUninspectedInlineImages().size());
        Assertions.assertEquals("image/png", result.getEmail().getUninspectedInlineImages().get(0).getContentType());
        Assertions.assertTrue(allText(parse(result.getDocument())).contains("cid:image001.png@01D9A1B2.C3D4E5F0"));

    }

    // --- Outlook .msg ------------------------------------------------------------------------------

    private static final List<EmailFixtures.Recipient> RECIPIENTS = List.of(
            new EmailFixtures.Recipient("Lee Recipient", OTHER_EMAIL, 1),
            new EmailFixtures.Recipient("Copy Person", "copy@example.net", 2),
            new EmailFixtures.Recipient("Hidden Person", "hidden@example.net", 3));

    @Test
    public void redactsAnOutlookMessageWithAnRtfOnlyBody() throws Exception {

        final String rtf = "{\\rtf1\\ansi\\ansicpg1252\\deff0{\\fonttbl{\\f0 Arial;}}\\f0\\fs20 "
                + "Call about SSN " + SSN + "\\par Reply to " + EMAIL + "\\par}";
        final byte[] msg = EmailFixtures.msg("Case " + SSN, "Pat Sender", EMAIL, RECIPIENTS, null, rtf, true);

        final BinaryDocumentFilterResult result = service().filter(policy(null), "context", msg, MimeType.MESSAGE_RFC822);
        final MimeMessage output = parse(result.getDocument());
        final String all = allText(output);

        Assertions.assertTrue(all.contains("Call about SSN {{{REDACTED-ssn}}}"), all);
        Assertions.assertFalse(all.contains(SSN), all);
        Assertions.assertFalse(all.contains(EMAIL), all);
        Assertions.assertFalse(all.contains("fonttbl"), all);
        Assertions.assertTrue(output.getSubject().contains("{{{REDACTED-ssn}}}"));

    }

    @Test
    public void refusesAnOutlookMessageWhoseRtfOnlyBodyIsMalformed() throws Exception {
        final byte[] msg = EmailFixtures.msg("Case", "Pat Sender", EMAIL, RECIPIENTS, null, "{\\rtf1 unclosed", true);
        Assertions.assertThrows(RtfFormatException.class,
                () -> service().filter(policy(null), "context", msg, MimeType.MESSAGE_RFC822));
    }

    @Test
    public void carriesOutlookBccRecipientsSoTheIdentitySettingDecides() throws Exception {

        for (final boolean withTypes : List.of(true, false)) {

            final byte[] msg = EmailFixtures.msg("Hello", "Pat Sender", EMAIL, RECIPIENTS, "Body.", null, withTypes);

            final MimeMessage removed = parse(filter(policy(null), msg).getDocument());
            Assertions.assertNull(removed.getHeader("Bcc"));
            Assertions.assertFalse(allText(removed).contains("hidden@example.net"));
            Assertions.assertTrue(removed.getHeader("To")[0].contains("{{{REDACTED-email-address}}}"));
            Assertions.assertTrue(removed.getHeader("Cc")[0].contains("{{{REDACTED-email-address}}}"));

            final MimeMessage kept = parse(filter(policy("{\"headers\":{\"removeIdentity\":false}}"), msg).getDocument());
            Assertions.assertTrue(kept.getHeader("Bcc")[0].contains("hidden@example.net"), "with types " + withTypes);
            Assertions.assertFalse(kept.getHeader("To")[0].contains("hidden@example.net"));

        }

    }

    @Test
    public void carriesOverOutlookAttachmentsAndEmbeddedMessages() throws Exception {

        final EmailFixtures.MsgContent embedded = new EmailFixtures.MsgContent("Inner SSN " + SSN, "Inner Sender",
                "inner@example.com", List.of(new EmailFixtures.Recipient("Lee Recipient", OTHER_EMAIL, 1)),
                "Inner body " + EMAIL + ".", null, true, List.of());

        final byte[] msg = EmailFixtures.msg(new EmailFixtures.MsgContent("Outer", "Pat Sender", EMAIL, RECIPIENTS,
                "Outer body.", null, true, List.of(
                new EmailFixtures.MsgAttachment("report.pdf", "application/pdf", EmailFixtures.pdf("SSN " + SSN), null),
                new EmailFixtures.MsgAttachment("forwarded.msg", null, null, embedded))));

        final BinaryDocumentFilterResult result = filter(policy("{\"attachments\":\"redact\"}"), msg);
        final MimeMessage output = parse(result.getDocument());

        final List<MimeBodyPart> attachments = attachments(output);
        Assertions.assertEquals("report.pdf", fileName(attachments.get(0)));
        try (var pdf = Loader.loadPDF(content(attachments.get(0)))) {
            Assertions.assertFalse(new PDFTextStripper().getText(pdf).contains(SSN));
        }

        final MimeMessage inner = innerMessage(output);
        Assertions.assertTrue(inner.getSubject().contains("{{{REDACTED-ssn}}}"), inner.getSubject());
        final String all = allText(inner);
        Assertions.assertTrue(all.contains("Inner body {{{REDACTED-email-address}}}."), all);
        Assertions.assertFalse(all.contains("inner@example.com"), all);

    }

    // --- Leftover check ----------------------------------------------------------------------------

    @Test
    public void leftoverCheckCoversFieldsAndNamesAttachments() throws Exception {

        final MimeMessage input = message("Hello", multipart("mixed",
                text("Call 212-555-0187 about it.", "plain"),
                attachment("letter.docx", MimeType.APPLICATION_DOCX.getValue(), EmailFixtures.docx("Call 212-555-0143 today.")),
                attachment("scan.pdf", "application/pdf", EmailFixtures.pdf("SSN " + SSN))));

        final Policy policy = policy("{\"attachments\":\"redact\"}",
                "\"review\":{\"leftoverCheck\":{\"mode\":\"always\",\"using\":\"broad\"}}");

        final BinaryDocumentFilterResult result = service().filter(policy, new DefaultContextService(),
                new InMemoryVectorService(), "context", bytes(input), MimeType.MESSAGE_RFC822,
                FilterOptions.defaults().withLeftoverCheckMatchedText(true));

        final LeftoverCheckResult check = result.getLeftoverCheck();
        Assertions.assertTrue(check.isPerformed());

        final List<LeftoverFinding> phones = check.getFindings().stream()
                .filter(finding -> finding.getFilterType() == FilterType.PHONE_NUMBER).toList();

        final LeftoverFinding inBody = phones.stream().filter(finding -> finding.getAttachment() == null).findFirst().orElseThrow();
        Assertions.assertEquals("212-555-0187", inBody.getMatchedText());
        Assertions.assertEquals(4, inBody.getParagraphNumber());

        final LeftoverFinding inAttachment = phones.stream().filter(finding -> "letter.docx".equals(finding.getAttachment()))
                .findFirst().orElseThrow();
        Assertions.assertEquals("212-555-0143", inAttachment.getMatchedText());

        // The PDF's only page was redacted, so it was flattened and has no text to check.
        Assertions.assertEquals(List.of("scan.pdf"), check.getUncheckedAttachments());

        // Replacements the redaction made are not reported.
        Assertions.assertTrue(check.getFindings().stream().noneMatch(finding -> finding.getFilterType() == FilterType.EMAIL_ADDRESS));

    }

    // --- Service contract --------------------------------------------------------------------------

    @Test
    public void redactsOnlyToAnEml() throws Exception {
        final byte[] eml = bytes(message("Hello", text("Body.", "plain")));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service().filter(policy(null), "context", eml, MimeType.APPLICATION_PDF));
    }

    @Test
    public void refusesInputThatIsNotAnEmailMessage() throws Exception {
        for (final byte[] input : List.of(new byte[0], EmailFixtures.pdf("SSN " + SSN), EmailFixtures.docx("Text"),
                "Just some text.\nOn two lines.".getBytes(StandardCharsets.UTF_8),
                "Note: this is not mail\n\nBody.".getBytes(StandardCharsets.UTF_8))) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> filter(policy(null), input));
        }
    }

    @Test
    public void refusesAMessageNestedTooDeeply() throws Exception {
        for (final int depth : new int[]{EmailRedactor.MAX_DEPTH, 5000}) {
            final StringBuilder eml = new StringBuilder("From: a@example.com\r\nMIME-Version: 1.0\r\n");
            for (int i = 0; i < depth; i++) {
                eml.append("Content-Type: multipart/mixed; boundary=\"b").append(i).append("\"\r\n\r\n--b").append(i).append("\r\n");
            }
            eml.append("Content-Type: text/plain\r\n\r\nSSN ").append(SSN).append("\r\n");
            for (int i = depth - 1; i >= 0; i--) {
                eml.append("--b").append(i).append("--\r\n");
            }
            final byte[] input = eml.toString().getBytes(StandardCharsets.UTF_8);
            if (depth == EmailRedactor.MAX_DEPTH) {
                Assertions.assertFalse(new String(filter(policy(null), input).getDocument(), StandardCharsets.UTF_8).contains(SSN));
            } else {
                Assertions.assertThrows(IllegalArgumentException.class, () -> filter(policy(null), input));
            }
        }
    }

    @Test
    public void aReplacementCannotAddAHeader() throws Exception {

        final Policy policy = new Gson().fromJson("{\"identifiers\":{\"ssn\":{\"ssnFilterStrategies\":[{"
                + "\"strategy\":\"STATIC_REPLACE\",\"staticReplacement\":\"X\\r\\nBcc: added@example.com\"}]}}}", Policy.class);
        final byte[] eml = "From: a@example.com\r\nSubject: SSN 123-45-6789\r\n\r\nBody.\r\n".getBytes(StandardCharsets.UTF_8);

        final MimeMessage output = parse(filter(policy, eml).getDocument());

        Assertions.assertNull(output.getHeader("Bcc"));
        Assertions.assertEquals("SSN X  Bcc: added@example.com", output.getSubject());

    }

    @Test
    public void applyReappliesSpansByField() throws Exception {

        final byte[] eml = bytes(message("SSN " + SSN, multipart("alternative",
                text("Body " + EMAIL + ".", "plain"), text("<p>Body " + EMAIL + ".</p>", "html"))));

        final BinaryDocumentFilterResult result = filter(policy(null), eml);
        final byte[] applied = service().apply(policy(null), eml, result.getExplanation().appliedSpans(), MimeType.MESSAGE_RFC822);

        Assertions.assertEquals(allText(parse(result.getDocument())), allText(parse(applied)));

    }

    // --- Helpers -----------------------------------------------------------------------------------

    private static MimeMessage innerMessage(final MimeMessage message) throws Exception {
        final Multipart multipart = (Multipart) message.getContent();
        for (int i = 0; i < multipart.getCount(); i++) {
            if (multipart.getBodyPart(i).getContent() instanceof MimeMessage inner) {
                return inner;
            }
        }
        throw new AssertionError("no nested message");
    }

    private static List<MimeBodyPart> attachments(final MimeMessage message) throws Exception {
        final List<MimeBodyPart> attachments = new ArrayList<>();
        if (message.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                final MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                    attachments.add(part);
                }
            }
        }
        return attachments;
    }

    private static String fileName(final MimeBodyPart part) {
        try {
            return part.getFileName();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] content(final MimeBodyPart part) throws Exception {
        return part.getInputStream().readAllBytes();
    }

}
