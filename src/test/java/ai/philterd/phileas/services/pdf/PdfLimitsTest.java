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
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.filters.filtering.PdfFilterService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PdfLimitsTest {

    private static byte[] pdf(final int pages, final float widthPoints, final float heightPoints) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) {
                document.addPage(new PDPage(new PDRectangle(widthPoints, heightPoints)));
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private static PdfRedactionOptions options(final int dpi) {
        return new PdfRedactionOptions(dpi, 0.5F, 1.0F, false);
    }

    @Test
    public void refusesAHugePageBeforeRenderingIt() throws Exception {
        // 200 x 200 inches renders to about 900 million pixels at 150 DPI; the file itself is tiny.
        final byte[] input = pdf(1, 200 * 72, 200 * 72);
        assertTrue(input.length < 10_000);
        final PdfLimitExceededException e = assertThrows(PdfLimitExceededException.class,
                () -> new PdfRedactor(new Policy(), List.of(), options(150)).process(input, MimeType.APPLICATION_PDF));
        assertTrue(e.getMessage().contains("Page 1"), e.getMessage());
    }

    @Test
    public void refusesAHighDpiOnAnOrdinaryPage() throws Exception {
        final byte[] input = pdf(1, PDRectangle.LETTER.getWidth(), PDRectangle.LETTER.getHeight());
        assertThrows(PdfLimitExceededException.class,
                () -> new PdfRedactor(new Policy(), List.of(), options(2400)).process(input, MimeType.IMAGE_JPEG));
    }

    @Test
    public void refusesANonPositiveDpi() throws Exception {
        final byte[] input = pdf(1, PDRectangle.LETTER.getWidth(), PDRectangle.LETTER.getHeight());
        assertThrows(PdfLimitExceededException.class,
                () -> new PdfRedactor(new Policy(), List.of(), options(0)).process(input, MimeType.APPLICATION_PDF));
    }

    @Test
    public void refusesTooManyPages() throws Exception {
        final byte[] input = pdf(3, PDRectangle.LETTER.getWidth(), PDRectangle.LETTER.getHeight());
        final PdfRedactionOptions options = options(72);
        options.setMaxPages(2);
        assertThrows(PdfLimitExceededException.class,
                () -> new PdfRedactor(new Policy(), List.of(), options).process(input, MimeType.APPLICATION_PDF));
        assertThrows(PdfLimitExceededException.class, () -> new PdfTextExtractor(2).getLines(input));
    }

    @Test
    public void zeroSetsNoLimit() throws Exception {
        final byte[] input = pdf(3, 50 * 72, 50 * 72);
        final PdfRedactionOptions options = options(36);
        options.setMaxPages(0);
        options.setMaxPixelsPerPage(0);
        assertNotNull(new PdfRedactor(new Policy(), List.of(), options).process(input, MimeType.APPLICATION_PDF));
    }

    @Test
    public void redactsAnOrdinaryDocumentWithinTheDefaults() throws Exception {
        final byte[] input = pdf(2, PDRectangle.LETTER.getWidth(), PDRectangle.LETTER.getHeight());
        assertNotNull(new PdfRedactor(new Policy(), List.of(), options(150)).process(input, MimeType.APPLICATION_PDF));
    }

    @Test
    public void theFilterServiceAppliesTheConfiguredLimits() throws Exception {
        final Properties properties = new Properties();
        properties.setProperty("pdf.max.pages", "1");
        final PdfFilterService service = new PdfFilterService(new PhileasConfiguration(properties), null);
        final byte[] input = pdf(2, PDRectangle.LETTER.getWidth(), PDRectangle.LETTER.getHeight());
        assertThrows(PdfLimitExceededException.class, () -> service.apply(new Policy(), input, List.of(), MimeType.APPLICATION_PDF));
    }

}
