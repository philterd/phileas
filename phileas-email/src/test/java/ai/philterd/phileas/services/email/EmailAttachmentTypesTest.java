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
package ai.philterd.phileas.services.email;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.EmailReport;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.LeftoverFinding;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.RedactionWarning;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.config.LeftoverCheck;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.BinaryFilterService;
import ai.philterd.phileas.services.filters.filtering.CsvFilterService;
import ai.philterd.phileas.services.filters.filtering.EmailFilterService;
import ai.philterd.phileas.services.filters.filtering.FilterOptions;
import com.google.gson.Gson;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static ai.philterd.phileas.services.email.EmailFixtures.attachment;
import static ai.philterd.phileas.services.email.EmailFixtures.bytes;
import static ai.philterd.phileas.services.email.EmailFixtures.multipart;
import static ai.philterd.phileas.services.email.EmailFixtures.parse;
import static ai.philterd.phileas.services.email.EmailFixtures.text;

public class EmailAttachmentTypesTest {

    private static final String EMAIL = "pat@example.com";
    private static final String SSN = "123-45-6789";

    private static final Charset LATIN_1 = StandardCharsets.ISO_8859_1;

    private static Policy policy(final String attachments, final String extraConfig) {
        return policy(attachments, extraConfig, "");
    }

    private static Policy policy(final String attachments, final String extraConfig, final String topLevel) {
        return new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}}" + topLevel
                + ",\"config\":{\"email\":{\"attachments\":\"" + attachments + "\"}"
                + (extraConfig.isEmpty() ? "" : "," + extraConfig) + "}}", Policy.class);
    }

    private static EmailFilterService service() {
        return service(new Properties());
    }

    private static EmailFilterService service(final Properties properties) {
        return new EmailFilterService(new PhileasConfiguration(properties), new DefaultContextService(),
                new InMemoryVectorService(), null);
    }

    private static BinaryDocumentFilterResult filter(final EmailFilterService service, final Policy policy,
                                                     final MimeMessage message, final FilterOptions options) throws Exception {
        return service.filter(policy, new DefaultContextService(), new InMemoryVectorService(), "ctx", bytes(message),
                MimeType.MESSAGE_RFC822, options);
    }

    private static BinaryDocumentFilterResult filter(final Policy policy, final MimeMessage message) throws Exception {
        return filter(service(), policy, message, FilterOptions.defaults());
    }

    private static MimeMessage message(final MimeBodyPart... attachments) throws Exception {
        final MimeBodyPart[] parts = new MimeBodyPart[attachments.length + 1];
        parts[0] = text("Body " + EMAIL + ".", "plain");
        System.arraycopy(attachments, 0, parts, 1, attachments.length);
        final MimeMessage message = new MimeMessage(EmailFixtures.SESSION);
        message.setSubject("Files");
        message.setFrom("sender@example.net");
        message.setRecipients(Message.RecipientType.TO, "recipient@example.net");
        message.setContent((Multipart) multipart("mixed", parts).getContent());
        message.saveChanges();
        return message;
    }

    // The message's attachments after redaction, as parts.
    private static List<BodyPart> attachments(final BinaryDocumentFilterResult result) throws Exception {
        final Multipart multipart = (Multipart) parse(result.getDocument()).getContent();
        final List<BodyPart> out = new ArrayList<>();
        for (int i = 0; i < multipart.getCount(); i++) {
            if (multipart.getBodyPart(i).getFileName() != null) {
                out.add(multipart.getBodyPart(i));
            }
        }
        return out;
    }

    private static byte[] content(final BodyPart part) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        part.getInputStream().transferTo(out);
        return out.toByteArray();
    }

    private static List<String> removed(final BinaryDocumentFilterResult result) {
        return result.getEmail().getRemovedAttachments().stream()
                .map(part -> part.getFileName() + "|" + part.getReason()).toList();
    }

    // --- CSV -------------------------------------------------------------------------------------------

    @Test
    public void aCsvAttachmentIsRedactedFieldByFieldWithThePolicysCsvSettingsAndColumnRules() throws Exception {

        final String tabular = ",\"tabular\":{\"sheets\":[{\"columns\":[{\"header\":\"Keep\",\"action\":\"ignore\"}]}]}";
        final byte[] csv = ("Keep;Email\n" + EMAIL + ";" + EMAIL + "\n").getBytes(StandardCharsets.UTF_8);

        final BinaryDocumentFilterResult result = filter(policy("redact", "\"csv\":{\"delimiter\":\";\"}", tabular),
                message(attachment("people.csv", "text/csv", csv)));

        final BodyPart part = attachments(result).get(0);
        Assertions.assertEquals("people.csv", part.getFileName());
        Assertions.assertTrue(part.getContentType().startsWith("text/csv"), part.getContentType());
        // The ignored column is kept; the other is redacted.
        Assertions.assertEquals("Keep;Email\n" + EMAIL + ";{{{REDACTED-email-address}}}\n",
                new String(content(part), StandardCharsets.UTF_8));
        Assertions.assertTrue(result.getEmail().getRemovedAttachments().isEmpty());

    }

    @Test
    public void aCsvAttachmentIsReadAndWrittenInItsDeclaredCharsetOrElseThePolicysEncoding() throws Exception {

        // CRLF line endings, as CSV uses, which mail keeps for a text part.
        final String csv = "Name,Email\r\nCaf\u00e9," + EMAIL + "\r\n";
        final String redacted = "Name,Email\r\nCaf\u00e9,{{{REDACTED-email-address}}}\r\n";

        // Declared Latin-1, with the policy's encoding left at UTF-8, where these bytes are not valid.
        final BinaryDocumentFilterResult declared = filter(policy("redact", ""),
                message(attachment("people.csv", "text/csv; charset=ISO-8859-1", csv.getBytes(LATIN_1))));
        Assertions.assertArrayEquals(redacted.getBytes(LATIN_1), content(attachments(declared).get(0)));

        // Undeclared: the policy's encoding, and the written part is labeled with it.
        final BinaryDocumentFilterResult undeclared = filterRaw(policy("redact", "\"csv\":{\"encoding\":\"ISO-8859-1\"}"),
                raw("text/csv; name=people.csv", "people.csv", csv.getBytes(LATIN_1)));
        final BodyPart part = attachments(undeclared).get(0);
        Assertions.assertArrayEquals(redacted.getBytes(LATIN_1), content(part));
        Assertions.assertTrue(part.getContentType().toLowerCase().contains("charset=iso-8859-1"), part.getContentType());

    }

    // --- RTF -------------------------------------------------------------------------------------------

    @Test
    public void anRtfAttachmentIsRedactedWithThePolicysWordSettings() throws Exception {

        final String rtf = "{\\rtf1\\ansi{\\fonttbl{\\f0 Arial;}}\\pard Mail " + EMAIL
                + "{\\*\\annotation\\pard Ask " + SSN + "}.\\par}";

        for (final MimeBodyPart part : new MimeBodyPart[]{
                attachment("letter.rtf", "application/rtf", rtf.getBytes(LATIN_1)),
                unnamed("application/octet-stream", rtf.getBytes(LATIN_1))}) {

            final BinaryDocumentFilterResult result = filter(policy("redact", ""), message(part));

            final String out = new String(content(attachedOrUnnamed(result)), LATIN_1);
            Assertions.assertFalse(out.contains(EMAIL), out);
            // config.word's default removes the comment, with its SSN.
            Assertions.assertFalse(out.contains(SSN), out);
            Assertions.assertFalse(out.contains("annotation"), out);
            Assertions.assertTrue(result.getEmail().getRemovedAttachments().isEmpty(), removed(result).toString());

        }

    }

    @Test
    public void aTextRtfPartIsLabeledWithTheCharsetItIsWrittenIn() throws Exception {

        // As a text type, mail would label the part UTF-8 when it declares no charset; it is written byte per character.
        final String rtf = "{\\rtf1\\ansi\\pard Caf\\'e9 " + EMAIL + "\\par}";
        final BodyPart text = attachments(filterRaw(policy("redact", ""),
                raw("text/rtf; name=letter.rtf", "letter.rtf", rtf.getBytes(LATIN_1)))).get(0);
        Assertions.assertTrue(text.getContentType().toLowerCase().contains("charset=iso-8859-1"), text.getContentType());
        Assertions.assertFalse(new String(content(text), LATIN_1).contains(EMAIL));

        // Not a text type, so no charset is added.
        final BodyPart application = attachments(filterRaw(policy("redact", ""),
                raw("application/rtf; name=letter.rtf", "letter.rtf", rtf.getBytes(LATIN_1)))).get(0);
        Assertions.assertFalse(application.getContentType().toLowerCase().contains("charset"), application.getContentType());

    }

    // --- plain text -------------------------------------------------------------------------------------

    @Test
    public void aPlainTextAttachmentIsRedactedInItsDeclaredCharsetOrElseUtf8() throws Exception {

        final String text = "Caf\u00e9 notes: " + EMAIL;
        final String redacted = "Caf\u00e9 notes: {{{REDACTED-email-address}}}";

        final BinaryDocumentFilterResult declared = filter(policy("redact", ""),
                message(attachment("notes.txt", "text/plain; charset=ISO-8859-1", text.getBytes(LATIN_1))));
        final BodyPart part = attachments(declared).get(0);
        Assertions.assertEquals("notes.txt", part.getFileName());
        Assertions.assertArrayEquals(redacted.getBytes(LATIN_1), content(part));

        final BinaryDocumentFilterResult undeclared = filterRaw(policy("redact", ""),
                raw("text/plain; name=notes.txt", "notes.txt", text.getBytes(StandardCharsets.UTF_8)));
        Assertions.assertArrayEquals(redacted.getBytes(StandardCharsets.UTF_8), content(attachments(undeclared).get(0)));
        Assertions.assertTrue(attachments(undeclared).get(0).getContentType().toLowerCase().contains("charset=utf-8"));

        // The body is still redacted as body text, not as an attachment.
        final String body = (String) ((Multipart) parse(declared.getDocument()).getContent()).getBodyPart(0).getContent();
        Assertions.assertEquals("Body {{{REDACTED-email-address}}}.", body);

    }

    // --- refused ----------------------------------------------------------------------------------------

    @Test
    public void anAttachmentItsServiceRefusesIsRemovedAndReported() throws Exception {

        final Properties oneRow = new Properties();
        oneRow.setProperty("csv.max.rows", "1");

        final MimeMessage message = message(
                attachment("broken.rtf", "application/rtf", "{\\rtf1 never closed".getBytes(LATIN_1)),
                attachment("big.csv", "text/csv", ("a\n" + EMAIL + "\n").getBytes(StandardCharsets.UTF_8)),
                attachment("bad.txt", "text/plain; charset=UTF-8", new byte[]{'h', 'i', (byte) 0xFF}));

        final BinaryDocumentFilterResult result = filter(service(oneRow), policy("redact", ""), message, FilterOptions.defaults());

        Assertions.assertEquals(List.of("broken.rtf|REDACTION_FAILED", "big.csv|REDACTION_FAILED", "bad.txt|REDACTION_FAILED"),
                removed(result));
        Assertions.assertTrue(attachments(result).isEmpty());

    }

    // --- leftover check ---------------------------------------------------------------------------------

    @Test
    public void theLeftoverCheckScansTheseAttachments() throws Exception {

        // The policy redacts addresses only, so the check, using broad, reports the SSN left in each attachment.
        final Policy policy = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{}},\"config\":{\"email\":"
                + "{\"attachments\":\"redact\"},\"review\":{\"leftoverCheck\":{\"mode\":\"always\",\"using\":\"broad\"}}}}",
                Policy.class);
        final MimeMessage message = message(
                attachment("people.csv", "text/csv", ("SSN\n" + SSN + "\n").getBytes(StandardCharsets.UTF_8)),
                attachment("letter.rtf", "application/rtf", ("{\\rtf1\\ansi\\pard SSN " + SSN + "\\par}").getBytes(LATIN_1)),
                attachment("notes.txt", "text/plain", ("SSN " + SSN).getBytes(StandardCharsets.UTF_8)));

        final BinaryDocumentFilterResult result = filter(service(), policy, message,
                FilterOptions.defaults().withLeftoverCheckMatchedText(true));

        Assertions.assertTrue(result.getLeftoverCheck().isPerformed());
        Assertions.assertEquals(List.of("people.csv", "letter.rtf", "notes.txt"), result.getLeftoverCheck().getFindings().stream()
                .filter(finding -> finding.getFilterType() == FilterType.SSN).map(LeftoverFinding::getAttachment).toList());
        Assertions.assertTrue(result.getLeftoverCheck().getUncheckedAttachments().isEmpty());

        // A check that cannot read an attachment again lists it as unchecked.
        final EmailFilterService failing = new EmailFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null) {
            private final CsvFilterService csv = new CsvFilterService(new PhileasConfiguration(new Properties()), null) {
                @Override
                protected List<Span> leftoverSpans(final Policy p, final LeftoverCheck.Using using, final String text) {
                    throw new IllegalStateException("check failed");
                }
            };

            @Override
            protected BinaryFilterService attachmentService(final MimeType type) {
                return type == MimeType.TEXT_CSV ? csv : super.attachmentService(type);
            }
        };
        final BinaryDocumentFilterResult unchecked = filter(failing, policy, message, FilterOptions.defaults());
        Assertions.assertEquals(List.of("people.csv"), unchecked.getLeftoverCheck().getUncheckedAttachments());

    }

    @Test
    public void anAttachmentsColumnRuleWarningIsOnTheMessagesResult() throws Exception {

        // The rule's header matches no column of either attachment, so each reports it, and the message reports it once.
        final String tabular = ",\"tabular\":{\"sheets\":[{\"unlistedColumns\":\"ignore\",\"columns\":["
                + "{\"header\":\"Social Security\",\"action\":\"redact\",\"strategies\":[{}]}]}]}";
        final MimeMessage message = message(
                attachment("people.csv", "text/csv", ("SSN\r\n" + SSN + "\r\n").getBytes(StandardCharsets.UTF_8)),
                attachment("sheet.xlsx", MimeType.APPLICATION_XLSX.getValue(), EmailFixtures.xlsx("Write to " + EMAIL)));
        final RedactionWarning expected = RedactionWarning.unmatchedHeader("Social Security", null).asEmbedded();

        for (final FilterOptions options : new FilterOptions[]{FilterOptions.defaults(),
                FilterOptions.defaults().withOutput(FilterOptions.Output.TEXT)}) {
            final BinaryDocumentFilterResult result = filter(service(), policy("redact", "", tabular), message, options);
            Assertions.assertEquals(List.of(expected), result.getWarnings());
        }

        // A kept attachment is not redacted, so its rules are not applied and it has no warnings.
        Assertions.assertTrue(filter(policy("keep", "", tabular), message).getWarnings().isEmpty());

    }

    // --- unchanged ---------------------------------------------------------------------------------------

    @Test
    public void keepAndRemoveAreUnchangedAndOtherTypesAreStillRemoved() throws Exception {

        final List<MimeBodyPart> parts = List.of(
                attachment("people.csv", "text/csv", ("Email\n" + EMAIL + "\n").getBytes(StandardCharsets.UTF_8)),
                attachment("letter.rtf", "application/rtf", "{\\rtf1 Hi.}".getBytes(LATIN_1)),
                attachment("notes.txt", "text/plain", "Hi.".getBytes(StandardCharsets.UTF_8)));

        final BinaryDocumentFilterResult kept = filter(policy("keep", ""), message(parts.toArray(new MimeBodyPart[0])));
        Assertions.assertEquals(List.of("people.csv", "letter.rtf", "notes.txt"), kept.getEmail().getUninspectedAttachments()
                .stream().map(EmailReport.Part::getFileName).toList());
        Assertions.assertTrue(new String(content(attachments(kept).get(0)), StandardCharsets.UTF_8).contains(EMAIL));

        final BinaryDocumentFilterResult removed = filter(policy("remove", ""), message(parts.toArray(new MimeBodyPart[0])));
        Assertions.assertTrue(attachments(removed).isEmpty());

        final BinaryDocumentFilterResult others = filter(policy("redact", ""), message(
                attachment("photo.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G'}),
                attachment("archive.zip", "application/zip", new byte[]{'P', 'K', 3, 4}),
                attachment("old.doc", "application/msword", new byte[]{1, 2, 3}),
                attachment("old.xls", "application/vnd.ms-excel", new byte[]{1, 2, 3})));
        Assertions.assertEquals(List.of("photo.png|UNSUPPORTED_TYPE", "archive.zip|UNSUPPORTED_TYPE",
                "old.doc|UNSUPPORTED_TYPE", "old.xls|UNSUPPORTED_TYPE"), removed(others));

    }

    // --- text output --------------------------------------------------------------------------------------

    @Test
    public void theseAttachmentsAreTheirOwnSegmentsOfTextOutput() throws Exception {

        final BinaryDocumentFilterResult result = filter(service(), policy("redact", ""), message(
                attachment("people.csv", "text/csv", ("Email\n" + EMAIL + "\n").getBytes(StandardCharsets.UTF_8)),
                attachment("letter.rtf", "application/rtf", ("{\\rtf1\\ansi\\pard Mail " + EMAIL + "\\par}").getBytes(LATIN_1)),
                attachment("notes.txt", "text/plain", ("Notes " + EMAIL).getBytes(StandardCharsets.UTF_8))),
                FilterOptions.defaults().withOutput(FilterOptions.Output.TEXT));

        final String all = result.getText().getSegments().stream()
                .map(segment -> segment.getLabel() + "=" + segment.getText()).reduce("", (a, b) -> a + "\n" + b);
        Assertions.assertTrue(all.contains("attachment: people.csv / rows=Email: {{{REDACTED-email-address}}}"), all);
        Assertions.assertTrue(all.contains("attachment: letter.rtf / body=Mail {{{REDACTED-email-address}}}"), all);
        Assertions.assertTrue(all.contains("attachment: notes.txt / text=Notes {{{REDACTED-email-address}}}"), all);
        Assertions.assertFalse(all.contains(EMAIL), all);

    }

    private static BinaryDocumentFilterResult filterRaw(final Policy policy, final byte[] eml) throws Exception {
        return service().filter(policy, new DefaultContextService(), new InMemoryVectorService(), "ctx", eml,
                MimeType.MESSAGE_RFC822, FilterOptions.defaults());
    }

    // A message written by hand, so its attachment declares exactly the content type given, with no charset added.
    private static byte[] raw(final String contentType, final String fileName, final byte[] content) {
        final String eml = "From: sender@example.net\r\nTo: recipient@example.net\r\nSubject: Files\r\n"
                + "MIME-Version: 1.0\r\nContent-Type: multipart/mixed; boundary=\"b\"\r\n\r\n"
                + "--b\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\nBody " + EMAIL + ".\r\n"
                + "--b\r\nContent-Type: " + contentType + "\r\nContent-Disposition: attachment; filename=" + fileName
                + "\r\nContent-Transfer-Encoding: base64\r\n\r\n"
                + java.util.Base64.getMimeEncoder().encodeToString(content) + "\r\n--b--\r\n";
        return eml.getBytes(StandardCharsets.US_ASCII);
    }

    // An attachment with no filename or disposition is only an attachment by its type.
    private static MimeBodyPart unnamed(final String type, final byte[] content) throws Exception {
        final MimeBodyPart part = new MimeBodyPart();
        part.setDataHandler(new jakarta.activation.DataHandler(new jakarta.mail.util.ByteArrayDataSource(content, type)));
        return part;
    }

    // The first part after the body: a named attachment or the unnamed one.
    private static BodyPart attachedOrUnnamed(final BinaryDocumentFilterResult result) throws Exception {
        return ((Multipart) parse(result.getDocument()).getContent()).getBodyPart(1);
    }

}
