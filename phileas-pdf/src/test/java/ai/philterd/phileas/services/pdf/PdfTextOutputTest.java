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
package ai.philterd.phileas.services.pdf;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.LeftoverCheckResult;
import ai.philterd.phileas.model.filtering.LeftoverFinding;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextSegment;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.graphical.BoundingBox;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.FilterOptions;
import ai.philterd.phileas.services.filters.filtering.PdfFilterService;
import com.google.gson.Gson;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PdfTextOutputTest {

    private static final FilterOptions TEXT = FilterOptions.defaults().withOutput(FilterOptions.Output.TEXT);

    private static final String SSN_AND_EMAIL = "{\"identifiers\":{\"ssn\":{},\"emailAddress\":{}}}";

    private static PdfFilterService service() {
        return new PdfFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);
    }

    private static Policy policy(final String json) {
        return new Gson().fromJson(json, Policy.class);
    }

    private static BinaryDocumentFilterResult filter(final PdfFilterService service, final Policy policy,
                                                     final byte[] input, final FilterOptions options) throws Exception {
        return service.filter(policy, new DefaultContextService(), new InMemoryVectorService(), "context", input,
                MimeType.APPLICATION_PDF, options);
    }

    @Test
    public void returnsEachPagesRedactedTextInPlaceOfTheDocument() throws Exception {

        final byte[] input = document(new String[]{"His SSN is 123-45-6789."}, new String[]{"Write to test@example.com today."});

        final BinaryDocumentFilterResult result = filter(service(), policy(SSN_AND_EMAIL), input, TEXT);

        Assertions.assertNull(result.getDocument());
        Assertions.assertEquals(List.of(
                new TextSegment("page 1", "His SSN is {{{REDACTED-ssn}}}.", TextSegment.Status.TEXT),
                new TextSegment("page 2", "Write to {{{REDACTED-email-address}}} today.", TextSegment.Status.TEXT)),
                result.getText().getSegments());
        Assertions.assertEquals(2, result.getExplanation().appliedSpans().size());

    }

    @Test
    public void theDocumentIsStillTheDefault() throws Exception {

        final BinaryDocumentFilterResult result = filter(service(), policy(SSN_AND_EMAIL),
                document(new String[]{"His SSN is 123-45-6789."}), FilterOptions.defaults());

        Assertions.assertNotNull(result.getDocument());
        Assertions.assertNull(result.getText());

    }

    @Test
    public void joinsAPagesLinesWithLineBreaks() throws Exception {

        final byte[] input = document(new String[]{"First line.", "His SSN is 123-45-6789.", "Last line."});

        final BinaryDocumentFilterResult result = filter(service(), policy(SSN_AND_EMAIL), input, TEXT);

        Assertions.assertEquals("First line.\nHis SSN is {{{REDACTED-ssn}}}.\nLast line.",
                result.getText().getSegments().get(0).getText());

    }

    @Test
    public void wordsPlacedSeparatelyOnALineAreJoinedWithASpace() throws Exception {

        // Each word drawn on its own, as many PDF writers do, so the extractor reads them as separate pieces.
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            final PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float x = 72;
                for (final String word : new String[]{"Mail", "test@example.com", "today."}) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(x, 700);
                    content.showText(word);
                    content.endText();
                    x += word.length() * 7 + 6;
                }
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 680);
                content.showText("Next line.");
                content.endText();
            }
            document.save(out);
        }

        final BinaryDocumentFilterResult result = filter(service(), policy(SSN_AND_EMAIL), out.toByteArray(), TEXT);

        Assertions.assertEquals("Mail {{{REDACTED-email-address}}} today.\nNext line.",
                result.getText().getSegments().get(0).getText());

    }

    @Test
    public void aPageWithNoTextIsAnEmptySegmentListedAsWithoutText() throws Exception {

        final byte[] input = document(new String[]{"His SSN is 123-45-6789."}, new String[]{}, new String[]{"Last page."});

        final BinaryDocumentFilterResult result = filter(service(), policy(SSN_AND_EMAIL), input, TEXT);

        Assertions.assertEquals(new TextSegment("page 2", "", TextSegment.Status.NO_TEXT), result.getText().getSegments().get(1));
        Assertions.assertEquals(List.of("page 2"), result.getText().getSegmentsWithoutText());
        Assertions.assertEquals("His SSN is {{{REDACTED-ssn}}}.\n\nLast page.", result.getText().join("\n\n"));

    }

    @Test
    public void doesNotRenderTheDocument() throws Exception {

        final PdfFilterService service = new PdfFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null) {
            @Override
            protected byte[] render(final Policy policy, final List<BoundingBox> boundingBoxes, final List<Span> spans,
                                    final PdfRedactionOptions options, final byte[] input, final MimeType outputMimeType) {
                throw new IllegalStateException("rendered");
            }
        };

        final byte[] input = document(new String[]{"His SSN is 123-45-6789."});

        Assertions.assertNotNull(filter(service, policy(SSN_AND_EMAIL), input, TEXT).getText());
        Assertions.assertThrows(IllegalStateException.class,
                () -> filter(service, policy(SSN_AND_EMAIL), input, FilterOptions.defaults()));

    }

    @Test
    public void detectsOnceForEachLineAsTheDocumentRedactionDoes() throws Exception {

        final String policy = "{\"identifiers\":{\"pheyes\":[{\"phEyeConfiguration\":"
                + "{\"endpoint\":\"http://localhost:18080\",\"labels\":[\"Person\"]}}]}}";
        final byte[] input = document(new String[]{"Ann was seen today.", "Ann called back."}, new String[]{"Ann left."});

        final AtomicInteger textCalls = new AtomicInteger();
        final BinaryDocumentFilterResult text = filter(new PdfFilterService(new PhileasConfiguration(new Properties()),
                        new DefaultContextService(), new InMemoryVectorService(), phEyeFindingAnn(textCalls)),
                policy(policy), input, TEXT);

        final AtomicInteger documentCalls = new AtomicInteger();
        filter(new PdfFilterService(new PhileasConfiguration(new Properties()), new DefaultContextService(),
                new InMemoryVectorService(), phEyeFindingAnn(documentCalls)), policy(policy), input, FilterOptions.defaults());

        Assertions.assertEquals(3, textCalls.get());
        Assertions.assertEquals(documentCalls.get(), textCalls.get());
        Assertions.assertEquals("{{{REDACTED-pheye}}} was seen today.\n{{{REDACTED-pheye}}} called back.",
                text.getText().getSegments().get(0).getText());

    }

    @Test
    public void theTextHasEachSpansReplacement() throws Exception {

        final String policy = "{\"identifiers\":{\"ssn\":{\"ssnFilterStrategies\":[{\"strategy\":\"RANDOM_REPLACE\"}]},"
                + "\"emailAddress\":{\"emailAddressFilterStrategies\":[{\"strategy\":\"RANDOM_REPLACE\"}]}}}";
        final byte[] input = document(new String[]{"His SSN is 123-45-6789."}, new String[]{"Write to test@example.com today."});

        final BinaryDocumentFilterResult result = filter(service(), policy(policy), input, TEXT);

        Assertions.assertEquals(2, result.getExplanation().appliedSpans().size());
        for (final Span span : result.getExplanation().appliedSpans()) {
            final String pageText = result.getText().getSegments().get(span.getPageNumber()).getText();
            Assertions.assertTrue(pageText.contains(span.getReplacement()), pageText);
            Assertions.assertFalse(pageText.contains(span.getText()), pageText);
        }

    }

    @Test
    public void aPageWhosePolicyHasNoFiltersIsLeftOut() throws Exception {

        final byte[] input = document(new String[]{"His SSN is 123-45-6789."}, new String[]{"Her SSN is 987-65-4320."});

        final BinaryDocumentFilterResult result = service().filter(policy(SSN_AND_EMAIL), new DefaultContextService(),
                new InMemoryVectorService(), "context", input, MimeType.APPLICATION_PDF,
                new PdfPagePolicies().assign("2", policy("{\"identifiers\":{}}"), "unscanned"), TEXT);

        Assertions.assertEquals(TextSegment.Status.TEXT, result.getText().getSegments().get(0).getStatus());
        Assertions.assertEquals(new TextSegment("page 2", "", TextSegment.Status.NOT_SCANNED), result.getText().getSegments().get(1));
        Assertions.assertTrue(result.getText().getSegmentsWithoutText().isEmpty());

    }

    @Test
    public void textUnderABoundingBoxIsLeftOut() throws Exception {

        // The first line's baseline is at y=700 and the second's at y=680, both from x=72.
        final byte[] input = document(new String[]{"Covered line.", "Write to test@example.com today."});

        // The whole first line.
        final BinaryDocumentFilterResult line = filter(service(), policy(withBox(72, 695, 400, 15)), input, TEXT);
        Assertions.assertEquals("\nWrite to {{{REDACTED-email-address}}} today.", line.getText().getSegments().get(0).getText());

        // The start of the second line only.
        final String partial = filter(service(), policy(withBox(60, 676, 30, 14)), input, TEXT)
                .getText().getSegments().get(0).getText();
        Assertions.assertTrue(partial.startsWith("Covered line.\n"), partial);
        Assertions.assertFalse(partial.contains("Write"), partial);
        Assertions.assertTrue(partial.endsWith("{{{REDACTED-email-address}}} today."), partial);

        // A box on another page leaves this page as it is.
        final String otherPage = filter(service(), policy(withBox(72, 695, 400, 15).replace("\"page\":1", "\"page\":2")),
                document(new String[]{"Covered line."}, new String[]{"Second page."}), TEXT).getText().getSegments().get(0).getText();
        Assertions.assertEquals("Covered line.", otherPage);

    }

    @Test
    public void metadataIsLeftOutUnlessAskedFor() throws Exception {

        final byte[] input = document(Map.of("Title", "Quarterly notes", "Subject", "Contact test@example.com"),
                new String[]{"Nothing here."});

        final BinaryDocumentFilterResult without = filter(service(), policy(SSN_AND_EMAIL), input, TEXT);
        Assertions.assertEquals(List.of("page 1"), without.getText().getSegments().stream().map(TextSegment::getLabel).toList());

        final BinaryDocumentFilterResult with = filter(service(), policy(SSN_AND_EMAIL), input, TEXT.withTextMetadata(true));
        final TextSegment metadata = with.getText().getSegments().get(1);
        Assertions.assertEquals("metadata", metadata.getLabel());
        Assertions.assertEquals(TextSegment.Status.TEXT, metadata.getStatus());
        Assertions.assertTrue(metadata.getText().contains("Title: Quarterly notes"), metadata.getText());
        Assertions.assertTrue(metadata.getText().contains("Subject: Contact {{{REDACTED-email-address}}}"), metadata.getText());
        Assertions.assertFalse(metadata.getText().contains("test@example.com"), metadata.getText());

        // A document with no metadata has an empty metadata segment.
        final BinaryDocumentFilterResult none = filter(service(), policy(SSN_AND_EMAIL),
                document(new String[]{"Nothing here."}), TEXT.withTextMetadata(true));
        Assertions.assertEquals(new TextSegment("metadata", "", TextSegment.Status.NO_TEXT), none.getText().getSegments().get(1));

    }

    @Test
    public void theLeftoverCheckRunsOverTheText() throws Exception {

        final Policy policy = policy("{\"identifiers\":{\"ssn\":{}},\"config\":{\"review\":{\"leftoverCheck\":"
                + "{\"mode\":\"onRequest\",\"using\":\"broad\"}}}}");
        final byte[] input = document(Map.of("Subject", "Mail test@example.org"),
                new String[]{"His SSN is 123-45-6789."}, new String[]{"First line.", "Write to test@example.com today."});

        final BinaryDocumentFilterResult result = filter(service(), policy, input,
                TEXT.withTextMetadata(true).withLeftoverCheck(true).withLeftoverCheckMatchedText(true));

        final LeftoverCheckResult check = result.getLeftoverCheck();
        Assertions.assertTrue(check.isPerformed());

        // The SSN's replacement is not reported, and each email address is placed in its segment.
        Assertions.assertTrue(check.getFindings().stream().noneMatch(finding -> finding.getFilterType() == FilterType.SSN));
        final List<LeftoverFinding> emails = check.getFindings().stream()
                .filter(finding -> finding.getFilterType() == FilterType.EMAIL_ADDRESS).toList();
        Assertions.assertEquals(List.of("page 2|1|3|test@example.com", "metadata|0|0|test@example.org"), emails.stream()
                .map(finding -> finding.getSegment() + "|" + finding.getPageNumber() + "|" + finding.getLineNumber()
                        + "|" + finding.getMatchedText()).toList());

        for (final LeftoverFinding finding : emails) {
            final String segmentText = result.getText().getSegments().stream()
                    .filter(segment -> segment.getLabel().equals(finding.getSegment())).findFirst().orElseThrow().getText();
            Assertions.assertEquals(finding.getMatchedText(),
                    segmentText.substring(finding.getCharacterStart(), finding.getCharacterEnd()));
        }

    }

    @Test
    public void aTextOutputWithNoTextIsNotChecked() throws Exception {

        final Policy policy = policy("{\"identifiers\":{\"ssn\":{}},\"config\":{\"review\":{\"leftoverCheck\":"
                + "{\"mode\":\"always\"}}}}");

        final BinaryDocumentFilterResult result = filter(service(), policy, document(new String[]{}), TEXT);

        Assertions.assertFalse(result.getLeftoverCheck().isPerformed());
        Assertions.assertEquals(LeftoverCheckResult.Reason.UNREADABLE_OUTPUT, result.getLeftoverCheck().getReason());

    }

    @Test
    public void aValueOnSeveralPagesGetsOneDocumentScopedReplacement() throws Exception {

        final Policy policy = policy("{\"identifiers\":{\"phoneNumber\":{\"phoneNumberFilterStrategies\":"
                + "[{\"strategy\":\"RANDOM_REPLACE\"}]}}}");
        final byte[] input = document(new String[]{"Call 212-555-0187 now."}, new String[]{"Or 212-555-0187 later."});

        for (final FilterOptions options : new FilterOptions[]{FilterOptions.defaults(), TEXT}) {
            final List<Span> spans = filter(service(), policy, input, options).getExplanation().appliedSpans();
            Assertions.assertEquals(2, spans.size());
            Assertions.assertEquals(spans.get(0).getReplacement(), spans.get(1).getReplacement());
        }

    }

    @Test
    public void theTextIsNotSerialized() throws Exception {

        final BinaryDocumentFilterResult result = filter(service(), policy(SSN_AND_EMAIL),
                document(new String[]{"Write to test@example.com today."}), TEXT);

        Assertions.assertFalse(result.toString().contains("today"), result.toString());

    }

    private static String withBox(final float x, final float y, final float w, final float h) {
        return "{\"identifiers\":{\"emailAddress\":{}},\"graphical\":{\"boundingBoxes\":[{\"x\":" + x + ",\"y\":" + y
                + ",\"w\":" + w + ",\"h\":" + h + ",\"page\":1}]}}";
    }

    // Answers every PhEye request with "Ann" at the start of the text, and counts the detection requests.
    private static HttpClient phEyeFindingAnn(final AtomicInteger calls) throws Exception {
        final HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            // Detection posts the text; other requests are not detection.
            if (invocation.getArgument(0) instanceof HttpPost) {
                calls.incrementAndGet();
            }
            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            when(response.getCode()).thenReturn(200);
            when(response.getEntity()).thenReturn(new StringEntity(
                    "[{\"start\": 0, \"end\": 3, \"label\": \"Person\", \"text\": \"Ann\", \"score\": 0.9}]"));
            return handler.handleResponse(response);
        });
        return httpClient;
    }

    private static byte[] document(final String[]... pages) throws Exception {
        return document(Map.of(), pages);
    }

    // Each page argument is a page, and each of its strings a line.
    private static byte[] document(final Map<String, String> information, final String[]... pages) throws Exception {

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            final PDDocumentInformation info = document.getDocumentInformation();
            information.forEach(info::setCustomMetadataValue);

            for (final String[] lines : pages) {
                final PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(72, 700);
                    for (final String line : lines) {
                        content.showText(line);
                        content.newLineAtOffset(0, -20);
                    }
                    content.endText();
                }
            }

            document.save(out);
            return out.toByteArray();

        }

    }

}
