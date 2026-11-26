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
package ai.philterd.phileas.services.pdf;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.LeftoverCheckResult;
import ai.philterd.phileas.model.filtering.LeftoverFinding;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.config.LeftoverCheck;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.FilterOptions;
import ai.philterd.phileas.services.filters.filtering.PdfFilterService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Properties;

public class PdfLeftoverCheckTest {

    // The policy redacts the SSN, so the first page is flattened to an image; the second has nothing it redacts.
    private static final String[] PAGES = {"His SSN is 123-45-6789.", "Write to test@example.com today."};

    private static PdfFilterService service() {
        return new PdfFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);
    }

    private static Policy policy(final boolean preserveUnredactedPages, final String using) {
        return new Gson().fromJson("{\"identifiers\":{\"ssn\":{}},\"config\":{"
                + "\"pdf\":{\"preserveUnredactedPages\":" + preserveUnredactedPages + "},"
                + "\"review\":{\"leftoverCheck\":{\"mode\":\"onRequest\",\"using\":\"" + using + "\"}}}}", Policy.class);
    }

    @Test
    public void findsWhatIsLeftOnPagesKeptAsText() throws Exception {

        final BinaryDocumentFilterResult result = service().filter(policy(true, "broad"), new DefaultContextService(),
                new InMemoryVectorService(), "context", document(), MimeType.APPLICATION_PDF,
                FilterOptions.defaults().withLeftoverCheck(true).withLeftoverCheckMatchedText(true));

        final LeftoverCheckResult check = result.getLeftoverCheck();
        Assertions.assertTrue(check.isPerformed());

        final LeftoverFinding email = check.getFindings().stream()
                .filter(finding -> finding.getFilterType() == FilterType.EMAIL_ADDRESS).findFirst().orElseThrow();
        Assertions.assertEquals(1, email.getPageNumber());
        Assertions.assertEquals("test@example.com", email.getMatchedText());

        // The redacted page is an image, so its SSN is not read back.
        Assertions.assertTrue(check.getFindings().stream().noneMatch(finding -> finding.getFilterType() == FilterType.SSN));

    }

    @Test
    public void samePolicyDoesNotReportATypeThePolicyDoesNotInclude() throws Exception {

        final BinaryDocumentFilterResult result = service().filter(policy(true, "samePolicy"), new DefaultContextService(),
                new InMemoryVectorService(), "context", document(), MimeType.APPLICATION_PDF,
                FilterOptions.defaults().withLeftoverCheck(true));

        Assertions.assertTrue(result.getLeftoverCheck().isPerformed());
        Assertions.assertEquals(0, result.getLeftoverCheck().getFindingCount());

    }

    @Test
    public void anOutputWithNoTextIsNotChecked() throws Exception {

        final FilterOptions requested = FilterOptions.defaults().withLeftoverCheck(true);

        // Every page flattened to an image.
        final BinaryDocumentFilterResult flattened = service().filter(policy(false, "broad"), new DefaultContextService(),
                new InMemoryVectorService(), "context", document(), MimeType.APPLICATION_PDF, requested);
        Assertions.assertFalse(flattened.getLeftoverCheck().isPerformed());
        Assertions.assertEquals(LeftoverCheckResult.Reason.UNREADABLE_OUTPUT, flattened.getLeftoverCheck().getReason());

        // Images rather than a PDF.
        final BinaryDocumentFilterResult images = service().filter(policy(true, "broad"), new DefaultContextService(),
                new InMemoryVectorService(), "context", document(), MimeType.IMAGE_JPEG, requested);
        Assertions.assertFalse(images.getLeftoverCheck().isPerformed());
        Assertions.assertEquals(LeftoverCheckResult.Reason.UNREADABLE_OUTPUT, images.getLeftoverCheck().getReason());

        final JsonObject json = new Gson().toJsonTree(flattened).getAsJsonObject().getAsJsonObject("leftoverCheck");
        Assertions.assertFalse(json.get("performed").getAsBoolean(), json.toString());
        Assertions.assertEquals("unreadableOutput", json.get("reason").getAsString(), json.toString());

    }

    @Test
    public void aCheckThatFailsIsReportedAsNotPerformed() throws Exception {
        final PdfFilterService failing = new PdfFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null) {
            @Override
            protected List<Span> leftoverSpans(final Policy p, final LeftoverCheck.Using using, final String text) {
                throw new IllegalStateException("check failed");
            }
        };

        final BinaryDocumentFilterResult result = failing.filter(policy(true, "broad"), new DefaultContextService(),
                new InMemoryVectorService(), "context", document(), MimeType.APPLICATION_PDF,
                FilterOptions.defaults().withLeftoverCheck(true));

        // The redaction stands: the first page, which had the SSN, was flattened, so only the second has text.
        Assertions.assertEquals(List.of(PAGES[1]), new PdfTextExtractor().getLines(result.getDocument()).stream()
                .map(PdfLine::getText).map(String::strip).toList());
        Assertions.assertFalse(result.getLeftoverCheck().isPerformed());
        Assertions.assertEquals(LeftoverCheckResult.Reason.CHECK_FAILED, result.getLeftoverCheck().getReason());
    }

    @Test
    public void notRunUnlessRequested() throws Exception {

        final BinaryDocumentFilterResult result = service().filter(policy(true, "broad"), "context", document(),
                MimeType.APPLICATION_PDF);

        Assertions.assertFalse(result.getLeftoverCheck().isPerformed());
        Assertions.assertEquals(LeftoverCheckResult.Reason.NOT_REQUESTED, result.getLeftoverCheck().getReason());

    }

    @Test
    public void findingsHaveTheirLinesAndOffsetsIntoTheJoinedLines() throws Exception {

        // Nothing here is redacted, so every page is kept as text.
        final byte[] input = document(new String[]{"Call 212-555-0187 now.", "Mail test@example.com later."},
                new String[]{"Visit 10.1.2.3 today."});

        final BinaryDocumentFilterResult result = service().filter(policy(true, "broad"), new DefaultContextService(),
                new InMemoryVectorService(), "context", input, MimeType.APPLICATION_PDF,
                FilterOptions.defaults().withLeftoverCheck(true).withLeftoverCheckMatchedText(true));

        final List<PdfLine> lines = new PdfTextExtractor().getLines(result.getDocument());
        final String joined = String.join("", lines.stream().map(PdfLine::getText).toList());

        final List<LeftoverFinding> findings = result.getLeftoverCheck().getFindings();
        Assertions.assertEquals(List.of("212-555-0187|0|1", "test@example.com|0|2", "10.1.2.3|1|3"), findings.stream()
                .filter(finding -> finding.getFilterType() == FilterType.PHONE_NUMBER
                        || finding.getFilterType() == FilterType.EMAIL_ADDRESS || finding.getFilterType() == FilterType.IP_ADDRESS)
                .map(finding -> finding.getMatchedText() + "|" + finding.getPageNumber() + "|" + finding.getLineNumber())
                .toList());

        for (final LeftoverFinding finding : findings) {
            Assertions.assertEquals(finding.getMatchedText(), joined.substring(finding.getCharacterStart(), finding.getCharacterEnd()));
        }

    }

    @Test
    public void eachPageIsCheckedWithItsPolicyAndTheDocumentsUsing() throws Exception {

        // The page policy's identifier ignores its value, so the redaction leaves it and both pages stay text. The
        // check drops the identifier's ignore list, so it reports the value on the page it applies to, and only there.
        final Policy pagePolicy = new Gson().fromJson("{\"identifiers\":{\"identifiers\":[{\"pattern\":\"CASE-[0-9]{4}\","
                + "\"classification\":\"case-number\",\"ignored\":[\"CASE-1234\"]}]}}", Policy.class);
        final byte[] input = document(new String[]{"Filed as CASE-1234."}, new String[]{"Filed as CASE-1234."});

        final BinaryDocumentFilterResult result = service().filter(policy(true, "samePolicy"), new DefaultContextService(),
                new InMemoryVectorService(), "context", input, MimeType.APPLICATION_PDF,
                new PdfPagePolicies().assign("2", pagePolicy, "cases"),
                FilterOptions.defaults().withLeftoverCheck(true).withLeftoverCheckMatchedText(true));

        final LeftoverCheckResult check = result.getLeftoverCheck();
        Assertions.assertTrue(check.isPerformed());
        Assertions.assertEquals(List.of("case-number|1|CASE-1234"), check.getFindings().stream()
                .map(finding -> finding.getClassification() + "|" + finding.getPageNumber() + "|" + finding.getMatchedText())
                .toList());

    }

    private static byte[] document() throws Exception {
        return document(new String[][]{{PAGES[0]}, {PAGES[1]}});
    }

    // Each argument is a page, and each of its strings a line.
    private static byte[] document(final String[]... pages) throws Exception {

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

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
