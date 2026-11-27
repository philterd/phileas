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
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.LeftoverCheckResult;
import ai.philterd.phileas.model.filtering.LeftoverFinding;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.TextSegment;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.csv.CsvRedactionOptions;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import com.google.gson.Gson;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CsvTextOutputTest {

    private static final FilterOptions TEXT = FilterOptions.defaults().withOutput(FilterOptions.Output.TEXT);

    private static final String EMAIL = "test@example.com";
    private static final String SSN = "123-45-6789";

    private static Policy policy(final String rest) {
        return new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}}"
                + (rest.isEmpty() ? "" : "," + rest) + "}", Policy.class);
    }

    private static BinaryDocumentFilterResult filter(final String rest, final byte[] csv, final CsvRedactionOptions options,
                                                     final FilterOptions filterOptions) throws Exception {
        return new CsvFilterService(new PhileasConfiguration(new Properties()), new DefaultContextService(),
                new InMemoryVectorService(), new SecureRandom(), null, options)
                .filter(policy(rest), new DefaultContextService(), new InMemoryVectorService(), "ctx", csv,
                        MimeType.TEXT_CSV, filterOptions);
    }

    private static BinaryDocumentFilterResult filter(final String rest, final String csv) throws Exception {
        return filter(rest, csv.getBytes(StandardCharsets.UTF_8), CsvRedactionOptions.defaults(), TEXT);
    }

    @Test
    public void rowsAreHeaderValueLinesInPlaceOfTheDocument() throws Exception {

        final BinaryDocumentFilterResult result = filter("", "Name,Email,SSN\r\nRow one," + EMAIL + "," + SSN
                + "\r\nRow two,,none\r\n");

        Assertions.assertNull(result.getDocument());
        Assertions.assertEquals(List.of(new TextSegment("rows", "Name: Row one\nEmail: {{{REDACTED-email-address}}}\n"
                + "SSN: {{{REDACTED-ssn}}}\n\nName: Row two\nSSN: none", TextSegment.Status.TEXT)),
                result.getText().getSegments());
        Assertions.assertEquals(2, result.getExplanation().appliedSpans().size());

    }

    @Test
    public void quotedFieldsAreTheirValues() throws Exception {

        final BinaryDocumentFilterResult result = filter("", "Note\n\"Mail " + EMAIL + ", \"\"soon\"\"\"\n\"two\nlines\"\n");

        Assertions.assertEquals("Note: Mail {{{REDACTED-email-address}}}, \"soon\"\n\nNote: two lines",
                result.getText().getSegments().get(0).getText());

    }

    @Test
    public void withoutAHeaderRowColumnsAreNumbered() throws Exception {

        final BinaryDocumentFilterResult result = filter("", ("Row one," + EMAIL + "\n").getBytes(StandardCharsets.UTF_8),
                CsvRedactionOptions.defaults().setHeaderRow(false), TEXT);

        Assertions.assertEquals("column 1: Row one\ncolumn 2: {{{REDACTED-email-address}}}",
                result.getText().getSegments().get(0).getText());

    }

    @Test
    public void columnRulesApplyAndIgnoredColumnsAreLeftOut() throws Exception {

        final String tabular = "\"tabular\":{\"sheets\":[{\"columns\":[{\"header\":\"Keep\",\"action\":\"ignore\"},"
                + "{\"header\":\"Contact\",\"action\":\"redact\",\"strategies\":[{\"strategy\":\"REDACT\"}]}]}]}";
        final String csv = "Keep,Contact,Comment\nA-1 " + EMAIL + "," + EMAIL + ",Mail " + EMAIL + "\n";

        final BinaryDocumentFilterResult text = filter(tabular, csv);
        final BinaryDocumentFilterResult document = filter(tabular, csv.getBytes(StandardCharsets.UTF_8),
                CsvRedactionOptions.defaults(), FilterOptions.defaults());

        final String contact = document.getExplanation().appliedSpans().stream()
                .filter(span -> span.getColumn() == 2).findFirst().orElseThrow().getReplacement();
        final String rows = text.getText().getSegments().get(0).getText();
        Assertions.assertEquals("Contact: " + contact + "\nComment: Mail {{{REDACTED-email-address}}}", rows);
        // The ignored column holds the address unredacted in the document, so the text leaves it out.
        Assertions.assertTrue(new String(document.getDocument(), StandardCharsets.UTF_8).contains("A-1 " + EMAIL));
        Assertions.assertFalse(rows.contains(EMAIL), rows);

    }

    @Test
    public void theEncodingIsDecoded() throws Exception {

        final byte[] little = ("\uFEFFName,Email\nCaf\u00e9," + EMAIL + "\n").getBytes(StandardCharsets.UTF_16LE);

        final BinaryDocumentFilterResult result = filter("\"config\":{\"csv\":{\"encoding\":\"UTF-16\"}}", little,
                CsvRedactionOptions.defaults(), TEXT);

        Assertions.assertEquals("Name: Caf\u00e9\nEmail: {{{REDACTED-email-address}}}", result.getText().getSegments().get(0).getText());

    }

    @Test
    public void theLeftoverCheckRunsOverTheText() throws Exception {

        final BinaryDocumentFilterResult result = new CsvFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), new SecureRandom(), null, CsvRedactionOptions.defaults())
                .filter(new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{}},\"config\":{\"review\":"
                                + "{\"leftoverCheck\":{\"mode\":\"always\",\"using\":\"broad\"}}}}", Policy.class),
                        new DefaultContextService(), new InMemoryVectorService(), "ctx",
                        ("Email,SSN\n" + EMAIL + "," + SSN + "\n").getBytes(StandardCharsets.UTF_8), MimeType.TEXT_CSV,
                        TEXT.withLeftoverCheckMatchedText(true));

        final List<LeftoverFinding> findings = result.getLeftoverCheck().getFindings();
        Assertions.assertTrue(findings.stream().noneMatch(f -> f.getFilterType() == FilterType.EMAIL_ADDRESS), findings.toString());
        final LeftoverFinding ssn = findings.stream().filter(f -> SSN.equals(f.getMatchedText())).findFirst().orElseThrow();
        Assertions.assertEquals("rows", ssn.getSegment());
        Assertions.assertEquals(SSN, result.getText().getSegments().get(0).getText()
                .substring(ssn.getCharacterStart(), ssn.getCharacterEnd()));

    }

    @Test
    public void aDocumentWithOnlyAHeaderHasNoTextAndIsNotChecked() throws Exception {

        final BinaryDocumentFilterResult result = filter("\"config\":{\"review\":{\"leftoverCheck\":{\"mode\":\"always\"}}}",
                "Name,Email\n");

        Assertions.assertEquals(List.of(new TextSegment("rows", "", TextSegment.Status.NO_TEXT)), result.getText().getSegments());
        Assertions.assertEquals(LeftoverCheckResult.Reason.UNREADABLE_OUTPUT, result.getLeftoverCheck().getReason());

    }

    @Test
    public void detectsOnceForEachFieldAsTheDocumentRedactionDoes() throws Exception {

        final Policy policy = new Gson().fromJson("{\"identifiers\":{\"pheyes\":[{\"phEyeConfiguration\":"
                + "{\"endpoint\":\"http://localhost:18080\",\"labels\":[\"Person\"]}}]}}", Policy.class);
        // A header row and two data rows with three fields with text: the header and each field is detected once.
        final byte[] csv = "Name,Note\nAnn Example,seen\nAnn Example,\n".getBytes(StandardCharsets.UTF_8);

        final AtomicInteger textCalls = new AtomicInteger();
        final BinaryDocumentFilterResult text = service(phEyeFindingAnn(textCalls)).filter(policy, new DefaultContextService(),
                new InMemoryVectorService(), "ctx", csv, MimeType.TEXT_CSV, TEXT);

        final AtomicInteger documentCalls = new AtomicInteger();
        service(phEyeFindingAnn(documentCalls)).filter(policy, new DefaultContextService(), new InMemoryVectorService(), "ctx",
                csv, MimeType.TEXT_CSV, FilterOptions.defaults());

        Assertions.assertEquals(5, textCalls.get());
        Assertions.assertEquals(documentCalls.get(), textCalls.get());
        // The stand-in model finds the first three characters of whatever it is sent, so only its calls are compared.
        Assertions.assertFalse(text.getText().join("\n").contains("Ann"), text.getText().join("\n"));

    }

    private static CsvFilterService service(final HttpClient httpClient) {
        return new CsvFilterService(new PhileasConfiguration(new Properties()), new DefaultContextService(),
                new InMemoryVectorService(), new SecureRandom(), httpClient, CsvRedactionOptions.defaults());
    }

    // Answers every PhEye request with "Ann" at the start of the text, and counts the detection requests.
    private static HttpClient phEyeFindingAnn(final AtomicInteger calls) throws Exception {
        final HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
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

    @Test
    public void theOutputTypeIsNotCheckedForText() throws Exception {
        Assertions.assertNotNull(new CsvFilterService(new PhileasConfiguration(new Properties()), new DefaultContextService(),
                new InMemoryVectorService(), new SecureRandom(), null, CsvRedactionOptions.defaults())
                .filter(policy(""), new DefaultContextService(), new InMemoryVectorService(), "ctx",
                        "Name\nA\n".getBytes(StandardCharsets.UTF_8), MimeType.TEXT_PLAIN, TEXT).getText());
    }

}
