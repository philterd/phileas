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
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.LeftoverCheckResult;
import ai.philterd.phileas.model.filtering.LeftoverFinding;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.TextSegment;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.EmailFilterService;
import ai.philterd.phileas.services.filters.filtering.FilterOptions;
import com.google.gson.Gson;
import jakarta.mail.Message;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static ai.philterd.phileas.services.email.EmailFixtures.attachment;
import static ai.philterd.phileas.services.email.EmailFixtures.bytes;
import static ai.philterd.phileas.services.email.EmailFixtures.multipart;
import static ai.philterd.phileas.services.email.EmailFixtures.nested;
import static ai.philterd.phileas.services.email.EmailFixtures.text;

public class EmailTextOutputTest {

    private static final FilterOptions TEXT = FilterOptions.defaults().withOutput(FilterOptions.Output.TEXT);

    private static final String SSN = "123-45-6789";
    private static final String EMAIL = "pat@example.com";
    private static final String OTHER_EMAIL = "lee@example.org";
    private static final String PHONE = "212-555-0187";

    private static final String REDACT_ATTACHMENTS = "{\"attachments\":\"redact\"}";

    private static Policy policy(final String email, final String review) {
        return new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}},\"config\":{"
                + (email == null ? "" : "\"email\":" + email) + (email != null && review != null ? "," : "")
                + (review == null ? "" : "\"review\":" + review) + "}}", Policy.class);
    }

    private static BinaryDocumentFilterResult filter(final Policy policy, final MimeMessage message,
                                                     final FilterOptions options) throws Exception {
        return new EmailFilterService(new PhileasConfiguration(new Properties()), new DefaultContextService(),
                new InMemoryVectorService(), null).filter(policy, new DefaultContextService(), new InMemoryVectorService(),
                "ctx", bytes(message), MimeType.MESSAGE_RFC822, options);
    }

    private static MimeMessage message(final String subject, final MimeBodyPart body) throws Exception {
        final MimeMessage message = new MimeMessage(EmailFixtures.SESSION);
        message.setSubject(subject);
        message.setFrom("Pat Sender <" + EMAIL + ">");
        message.setRecipients(Message.RecipientType.TO, "Lee Recipient <" + OTHER_EMAIL + ">");
        if (body.getContent() instanceof jakarta.mail.Multipart multipart) {
            message.setContent(multipart);
        } else {
            message.setContent(body.getContent(), body.getContentType());
        }
        message.saveChanges();
        return message;
    }

    private static TextSegment segment(final BinaryDocumentFilterResult result, final String label) {
        return result.getText().getSegments().stream().filter(s -> s.getLabel().equals(label)).findFirst()
                .orElseThrow(() -> new AssertionError(label + " in " + result.getText()));
    }

    private static List<String> labels(final BinaryDocumentFilterResult result) {
        return result.getText().getSegments().stream().map(TextSegment::getLabel).toList();
    }

    @Test
    public void headersAndBodyInPlaceOfTheMessage() throws Exception {

        final BinaryDocumentFilterResult result = filter(policy(null, null),
                message("Re: " + SSN, text("Call " + EMAIL + " today.\r\nThanks.", "plain")), TEXT);

        Assertions.assertNull(result.getDocument());
        Assertions.assertEquals(List.of("headers", "body"), labels(result));
        final String headers = segment(result, "headers").getText();
        Assertions.assertTrue(headers.contains("Subject: Re: {{{REDACTED-ssn}}}"), headers);
        Assertions.assertTrue(headers.contains("From: Pat Sender <{{{REDACTED-email-address}}}>"), headers);
        Assertions.assertTrue(headers.contains("To: Lee Recipient <{{{REDACTED-email-address}}}>"), headers);
        Assertions.assertEquals("Call {{{REDACTED-email-address}}} today.\nThanks.", segment(result, "body").getText());
        Assertions.assertNotNull(result.getEmail());

    }

    @Test
    public void aRemovedHeaderIsNotIncluded() throws Exception {

        final BinaryDocumentFilterResult result = filter(policy("{\"headers\":{\"removeSubject\":true}}", null),
                message("Re: " + SSN, text("Body.", "plain")), TEXT);

        Assertions.assertFalse(segment(result, "headers").getText().contains("Subject"));

    }

    @Test
    public void thePlainBodyIsUsedWhenThereIsOne() throws Exception {

        final BinaryDocumentFilterResult result = filter(policy(null, null), message("Hello", multipart("alternative",
                text("Plain " + EMAIL, "plain"), text("<p>HTML " + EMAIL + "</p>", "html"))), TEXT);

        Assertions.assertEquals("Plain {{{REDACTED-email-address}}}", segment(result, "body").getText());

    }

    @Test
    public void anHtmlOnlyBodyIsItsVisibleText() throws Exception {

        final String html = "<html><head><title>T</title><style>p { color: red; }</style></head><body>"
                + "<p>Hi <b>Pat</b>,</p>\n  <p>Mail pat&#64;example.com <br>or call.</p><script>var x = 1;</script>"
                + "<table><tr><td>A</td><td>B</td></tr></table></body></html>";

        final MimeMessage message = message("Hello", text("", "plain"));
        message.setText(html, "UTF-8", "html");
        message.saveChanges();

        final BinaryDocumentFilterResult result = filter(policy(null, null), message, TEXT);

        Assertions.assertEquals("Hi Pat,\nMail {{{REDACTED-email-address}}}\nor call.\nA B", segment(result, "body").getText());

    }

    @Test
    public void redactedAttachmentsAreTheirOwnSegments() throws Exception {

        final MimeMessage message = message("Files", multipart("mixed", text("Attached.", "plain"),
                attachment("report.pdf", "application/pdf", EmailFixtures.pdf("SSN " + SSN)),
                attachment("letter.docx", "application/octet-stream", EmailFixtures.docx("Write to " + EMAIL)),
                attachment("sheet.xlsx", MimeType.APPLICATION_XLSX.getValue(), EmailFixtures.xlsx("Write to " + EMAIL)),
                attachment("archive.zip", "application/zip", "not inspected".getBytes(StandardCharsets.UTF_8))));

        final BinaryDocumentFilterResult result = filter(policy(REDACT_ATTACHMENTS, null), message, TEXT);

        Assertions.assertEquals(List.of("headers", "body", "attachment: report.pdf / page 1", "attachment: letter.docx / body",
                "attachment: sheet.xlsx / sheet: Sheet1"), labels(result));
        Assertions.assertEquals("SSN {{{REDACTED-ssn}}}", segment(result, "attachment: report.pdf / page 1").getText().strip());
        Assertions.assertEquals("Write to {{{REDACTED-email-address}}}", segment(result, "attachment: letter.docx / body").getText());
        final String all = result.getText().join("\n");
        Assertions.assertFalse(all.contains(SSN) || all.contains(EMAIL) || all.contains(OTHER_EMAIL), all);
        Assertions.assertFalse(all.contains("not inspected"), all);

        // Kept attachments are not scanned, so they are not in the text, and the report still lists them.
        final BinaryDocumentFilterResult kept = filter(policy(null, null), message, TEXT);
        Assertions.assertEquals(List.of("headers", "body"), labels(kept));
        Assertions.assertEquals(4, kept.getEmail().getUninspectedAttachments().size());

    }

    @Test
    public void aNestedMessageHasItsOwnSegments() throws Exception {

        final MimeMessage inner = message("Inner " + SSN, text("Inner body " + EMAIL, "plain"));
        final BinaryDocumentFilterResult result = filter(policy(null, null),
                message("Outer", multipart("mixed", text("Outer body.", "plain"), nested(inner))), TEXT);

        Assertions.assertEquals("Outer body.", segment(result, "body").getText());
        Assertions.assertTrue(segment(result, "attached message 1 / headers").getText().contains("Subject: Inner {{{REDACTED-ssn}}}"));
        Assertions.assertEquals("Inner body {{{REDACTED-email-address}}}", segment(result, "attached message 1 / body").getText());

    }

    @Test
    public void theLeftoverCheckCoversTheFieldsAndTheAttachments() throws Exception {

        final MimeMessage message = message("Files", multipart("mixed", text("Call " + PHONE + ".", "plain"),
                attachment("letter.docx", "application/octet-stream", EmailFixtures.docx("Or " + PHONE))));

        final BinaryDocumentFilterResult result = filter(policy(REDACT_ATTACHMENTS,
                "{\"leftoverCheck\":{\"mode\":\"always\",\"using\":\"broad\"}}"), message, TEXT.withLeftoverCheckMatchedText(true));

        final LeftoverCheckResult check = result.getLeftoverCheck();
        Assertions.assertTrue(check.isPerformed());
        Assertions.assertTrue(check.getFindings().stream().noneMatch(f -> f.getFilterType() == FilterType.EMAIL_ADDRESS),
                check.getFindings().toString());
        final List<String> phones = check.getFindings().stream().filter(f -> PHONE.equals(f.getMatchedText()))
                .map(f -> f.getSegment() + "|" + f.getAttachment()).toList();
        Assertions.assertEquals(List.of("body|null", "body|letter.docx"), phones);

        final LeftoverFinding inBody = check.getFindings().stream()
                .filter(f -> PHONE.equals(f.getMatchedText()) && f.getAttachment() == null).findFirst().orElseThrow();
        Assertions.assertEquals(PHONE, segment(result, "body").getText().substring(inBody.getCharacterStart(), inBody.getCharacterEnd()));

    }

    @Test
    public void anHtmlBodysReplacementsAreKnownToTheLeftoverCheck() throws Exception {

        // A random stand-in for the address is an address too, so only its place in the text keeps it from a finding.
        final Policy policy = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{\"emailAddressFilterStrategies\":"
                + "[{\"strategy\":\"RANDOM_REPLACE\"}]}},\"config\":{\"review\":{\"leftoverCheck\":"
                + "{\"mode\":\"always\",\"using\":\"broad\"}}}}", Policy.class);
        final MimeMessage message = message("Hello", text("", "plain"));
        message.setText("<div>Mail <b>" + EMAIL + "</b></div><p>Call " + PHONE + "</p>", "UTF-8", "html");
        message.saveChanges();

        final BinaryDocumentFilterResult result = filter(policy, message, TEXT.withLeftoverCheckMatchedText(true));

        final String body = segment(result, "body").getText();
        Assertions.assertFalse(body.contains(EMAIL), body);
        final List<LeftoverFinding> findings = result.getLeftoverCheck().getFindings();
        Assertions.assertTrue(findings.stream().noneMatch(f -> f.getFilterType() == FilterType.EMAIL_ADDRESS), findings.toString());
        Assertions.assertTrue(findings.stream().anyMatch(f -> PHONE.equals(f.getMatchedText()) && "body".equals(f.getSegment())),
                findings.toString());

    }

    @Test
    public void detectsOnceForEachFieldAndAttachmentAsTheDocumentRedactionDoes() throws Exception {

        final Policy policy = new Gson().fromJson("{\"identifiers\":{\"pheyes\":[{\"phEyeConfiguration\":"
                + "{\"endpoint\":\"http://localhost:18080\",\"labels\":[\"Person\"]}}]},\"config\":{\"email\":"
                + REDACT_ATTACHMENTS + "}}", Policy.class);
        final MimeMessage message = message("Ann Example", multipart("mixed", text("Ann called.", "plain"),
                attachment("letter.docx", "application/octet-stream", EmailFixtures.docx("Ann wrote."))));

        final PhEyeStub textCalls = new PhEyeStub();
        new EmailFilterService(new PhileasConfiguration(new Properties()), new DefaultContextService(),
                new InMemoryVectorService(), textCalls).filter(policy, new DefaultContextService(), new InMemoryVectorService(),
                "ctx", bytes(message), MimeType.MESSAGE_RFC822, TEXT);

        final PhEyeStub documentCalls = new PhEyeStub();
        new EmailFilterService(new PhileasConfiguration(new Properties()), new DefaultContextService(),
                new InMemoryVectorService(), documentCalls).filter(policy, new DefaultContextService(), new InMemoryVectorService(),
                "ctx", bytes(message), MimeType.MESSAGE_RFC822, FilterOptions.defaults());

        // The subject, sender, recipient, body, and the attachment's paragraph.
        Assertions.assertEquals(5, textCalls.posts.get(), textCalls.texts.toString());
        Assertions.assertEquals(documentCalls.texts, textCalls.texts);

    }

    // A PhEye service that finds "Ann" at the start of whatever it is sent, recording each detection request's body.
    private static final class PhEyeStub implements HttpClient {

        private final AtomicInteger posts = new AtomicInteger();
        private final List<String> texts = new ArrayList<>();

        @Override
        public <T> T execute(final ClassicHttpRequest request, final HttpClientResponseHandler<? extends T> handler)
                throws IOException {
            if (request instanceof HttpPost) {
                posts.incrementAndGet();
                try {
                    texts.add(EntityUtils.toString(request.getEntity()));
                } catch (final ParseException e) {
                    throw new IOException(e);
                }
            }
            final BasicClassicHttpResponse response = new BasicClassicHttpResponse(200);
            response.setEntity(new StringEntity("[{\"start\": 0, \"end\": 3, \"label\": \"Person\", \"text\": \"Ann\", "
                    + "\"score\": 0.9}]", ContentType.APPLICATION_JSON));
            try {
                return handler.handleResponse(response);
            } catch (final HttpException e) {
                throw new IOException(e);
            }
        }

        @Override
        public HttpResponse execute(final ClassicHttpRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public HttpResponse execute(final ClassicHttpRequest request, final HttpContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ClassicHttpResponse execute(final HttpHost target, final ClassicHttpRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public HttpResponse execute(final HttpHost target, final ClassicHttpRequest request, final HttpContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T execute(final ClassicHttpRequest request, final HttpContext context,
                             final HttpClientResponseHandler<? extends T> handler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T execute(final HttpHost target, final ClassicHttpRequest request,
                             final HttpClientResponseHandler<? extends T> handler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T execute(final HttpHost target, final ClassicHttpRequest request, final HttpContext context,
                             final HttpClientResponseHandler<? extends T> handler) {
            throw new UnsupportedOperationException();
        }

    }

    @Test
    public void theOutputTypeIsNotCheckedForText() throws Exception {
        Assertions.assertNotNull(new EmailFilterService(new PhileasConfiguration(new Properties()), new DefaultContextService(),
                new InMemoryVectorService(), null).filter(policy(null, null), new DefaultContextService(), new InMemoryVectorService(),
                "ctx", bytes(message("Hello", text("Body.", "plain"))), MimeType.TEXT_PLAIN, TEXT).getText());
    }

}
