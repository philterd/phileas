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
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.FilterOptions;
import ai.philterd.phileas.services.filters.filtering.PdfFilterService;
import com.google.gson.Gson;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
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

    }

    @Test
    public void notRunUnlessRequested() throws Exception {

        final BinaryDocumentFilterResult result = service().filter(policy(true, "broad"), "context", document(),
                MimeType.APPLICATION_PDF);

        Assertions.assertFalse(result.getLeftoverCheck().isPerformed());
        Assertions.assertEquals(LeftoverCheckResult.Reason.NOT_REQUESTED, result.getLeftoverCheck().getReason());

    }

    private static byte[] document() throws Exception {

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (final String line : PAGES) {
                final PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(72, 700);
                    content.showText(line);
                    content.endText();
                }
            }

            document.save(out);
            return out.toByteArray();

        }

    }

}
