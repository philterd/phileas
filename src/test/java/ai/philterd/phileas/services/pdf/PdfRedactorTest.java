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

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Graphical;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.graphical.BoundingBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class PdfRedactorTest {

    private static final Logger LOGGER = LogManager.getLogger(PdfRedactorTest.class);

    private static final String SMALL_PDF = "12-12110 K.pdf";
    private static final String LARGE_PDF = "33011-pdf-118-pages.pdf";

    // Render at a modest DPI for the pixel-based assertions to keep the tests fast.
    private static final int RENDER_DPI = 72;

    /**
     * The core security property: a redacted PDF must not expose any recoverable text. The output
     * is rasterized, so the entire text layer (and therefore every PII value) is gone. The spans
     * are built from words that genuinely appear in the document, so a regression that emitted the
     * original PDF (or otherwise kept the text layer) would leave those words readable and fail.
     */
    @Test
    public void redactedPdfHasNoRecoverableText() throws IOException {

        final byte[] document = load(SMALL_PDF);

        final List<PdfLine> lines = new PdfTextExtractor().getLines(document);
        final Span span1 = matchingSpan(richLine(lines, 0));
        final Span span2 = matchingSpan(richLine(lines, 1));
        final String term1 = span1.getText();
        final String term2 = span2.getText();

        final String originalText = extractText(document);
        Assertions.assertTrue(originalText.contains(term1), "precondition: original PDF should contain '" + term1 + "'");
        Assertions.assertTrue(originalText.contains(term2), "precondition: original PDF should contain '" + term2 + "'");

        final PdfRedactor redactor = new PdfRedactor(new Policy(), List.of(span1, span2), lowDpiOptions());
        final byte[] redacted = redactor.process(document, MimeType.APPLICATION_PDF);

        Assertions.assertNotNull(redacted);
        Assertions.assertTrue(redacted.length > 0, "redacted output should not be empty");
        Assertions.assertEquals(pageCount(document), pageCount(redacted), "page count should be preserved");

        final String redactedText = extractText(redacted).trim();
        Assertions.assertTrue(redactedText.isEmpty(),
                "redacted PDF must not expose any recoverable text, but found: " + redactedText);
        Assertions.assertFalse(redactedText.contains(term1), "PII term leaked into redacted output: " + term1);
        Assertions.assertFalse(redactedText.contains(term2), "PII term leaked into redacted output: " + term2);
    }

    /**
     * Proves redaction actually covers the matched text (not merely that rasterization removed the
     * text layer): redacting a span draws a filled rectangle, so the page gains a region of dark
     * pixels compared with the same page rendered without any redaction.
     */
    @Test
    public void redactionDrawsABoxOverMatchedText() throws IOException {

        final byte[] document = load(SMALL_PDF);
        final Span span = matchingSpan(richLine(new PdfTextExtractor().getLines(document), 0));

        final byte[] withRedaction = new PdfRedactor(new Policy(), List.of(span), lowDpiOptions())
                .process(document, MimeType.APPLICATION_PDF);
        final byte[] withoutRedaction = new PdfRedactor(new Policy(), List.of(), lowDpiOptions())
                .process(document, MimeType.APPLICATION_PDF);

        final long darkWith = darkPixels(withRedaction, 0);
        final long darkWithout = darkPixels(withoutRedaction, 0);

        LOGGER.info("Dark pixels with redaction: {}, without: {}", darkWith, darkWithout);
        Assertions.assertTrue(darkWith > darkWithout + 100,
                "a redaction rectangle should add dark pixels to the page (with=" + darkWith + ", without=" + darkWithout + ")");
    }

    /**
     * The graphical bounding-box redaction path should also fill a region of the page.
     */
    @Test
    public void boundingBoxRedactionDrawsABox() throws IOException {

        final byte[] document = load(SMALL_PDF);

        final BoundingBox boundingBox = new BoundingBox();
        boundingBox.setX(100);
        boundingBox.setY(100);
        boundingBox.setW(400);
        boundingBox.setH(200);
        boundingBox.setPage(1);

        final Policy policy = new Policy();
        final Graphical graphical = new Graphical();
        graphical.setBoundingBoxes(List.of(boundingBox));
        policy.setGraphical(graphical);

        final byte[] withBox = new PdfRedactor(policy, List.of(), lowDpiOptions())
                .process(document, MimeType.APPLICATION_PDF);
        final byte[] withoutBox = new PdfRedactor(new Policy(), List.of(), lowDpiOptions())
                .process(document, MimeType.APPLICATION_PDF);

        final long darkWith = darkPixels(withBox, 0);
        final long darkWithout = darkPixels(withoutBox, 0);

        LOGGER.info("Dark pixels with bounding box: {}, without: {}", darkWith, darkWithout);
        Assertions.assertTrue(darkWith > darkWithout + 100,
                "a filled bounding box should add dark pixels to the page (with=" + darkWith + ", without=" + darkWithout + ")");
    }

    /**
     * JPEG output is a zip containing one readable image per page.
     */
    @Test
    public void jpegOutputProducesOneImagePerPage() throws IOException {

        final byte[] document = load(SMALL_PDF);
        final int pages = pageCount(document);

        final Span span = matchingSpan(richLine(new PdfTextExtractor().getLines(document), 0));
        final byte[] redacted = new PdfRedactor(new Policy(), List.of(span), lowDpiOptions())
                .process(document, MimeType.IMAGE_JPEG);

        int entries = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(redacted))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                final byte[] imageBytes = zip.readAllBytes();
                Assertions.assertTrue(imageBytes.length > 0, "image entry should not be empty: " + entry.getName());
                final BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
                Assertions.assertNotNull(image, "each zip entry should be a readable image: " + entry.getName());
                entries++;
            }
        }

        Assertions.assertEquals(pages, entries, "there should be one image per page");
    }

    /**
     * A large multi-page document is rasterized in full: page count is preserved and no text is
     * recoverable from the output.
     */
    @Test
    public void preservesPageCountForLargeDocument() throws IOException {

        final byte[] document = load(LARGE_PDF);

        final PdfRedactionOptions options = new PdfRedactionOptions();
        options.setDpi(36);
        options.setScale(0.25f);

        final byte[] redacted = new PdfRedactor(new Policy(), List.of(), options)
                .process(document, MimeType.APPLICATION_PDF);

        Assertions.assertEquals(pageCount(document), pageCount(redacted), "page count should be preserved");
        Assertions.assertTrue(extractText(redacted).trim().isEmpty(),
                "rasterized output must expose no recoverable text");
    }

    @Test
    public void invalidOutputMimeTypeThrows() throws IOException {
        final byte[] document = load(SMALL_PDF);
        final PdfRedactor redactor = new PdfRedactor(new Policy(), List.of(), lowDpiOptions());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> redactor.process(document, MimeType.TEXT_PLAIN));
    }

    @Test
    public void testAddReplacementTextToRect() throws IOException {

        var contentStream = Mockito.mock(PDPageContentStream.class);

        final Span span1 = Span.make(0, 1, FilterType.AGE, "ctx", 0.25, "Wendy", "repl", null, false, true, null, 0);
        final Span span2 = Span.make(0, 1, FilterType.AGE, "ctx", 0.25, "Bankruptcy", "repl", null, false, true, null, 0);
        final List<Span> spanList = List.of(span1, span2);

        final Policy policy = new Policy();
        policy.getConfig().getPdf().setShowReplacement(true);
        final PdfRedactionOptions pdfRedactionOptions = new PdfRedactionOptions();

        final PdfRedactor pdfRedactor = new PdfRedactor(policy, spanList, pdfRedactionOptions);

        final RedactedRectangle redactedRectangle = new RedactedRectangle(PDRectangle.LETTER, span1);
        pdfRedactor.addReplacementTextToRect(redactedRectangle, contentStream);

        verify(contentStream).beginText();
        verify(contentStream).setNonStrokingColor(
                argThat((PDColor color) -> {
                    return (
                            Arrays.equals(color.getComponents(), new float[]{255, 255, 255})
                                    && color.getColorSpace() == PDDeviceRGB.INSTANCE
                    );
                })
        );
        verify(contentStream).setFont(argThat((PDType1Font font) -> font.getName().equals(Standard14Fonts.FontName.HELVETICA.getName())), eq(12.0f));
        verify(contentStream).newLineAtOffset(295.998f, 394.758f);
        verify(contentStream).showText("repl");
        verify(contentStream).endText();

    }

    // ---- helpers ----

    private byte[] load(final String resource) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
            Assertions.assertNotNull(is, "missing test resource: " + resource);
            return is.readAllBytes();
        }
    }

    private static PdfRedactionOptions lowDpiOptions() {
        final PdfRedactionOptions options = new PdfRedactionOptions();
        options.setDpi(RENDER_DPI);
        return options;
    }

    private static String extractText(final byte[] pdf) throws IOException {
        final StringBuilder sb = new StringBuilder();
        for (final PdfLine line : new PdfTextExtractor().getLines(pdf)) {
            sb.append(line.getText()).append('\n');
        }
        return sb.toString();
    }

    private static int pageCount(final byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return doc.getNumberOfPages();
        }
    }

    /** The first line on the given page that contains a usable (>= 5 character, purely alphabetic) word. */
    private static PdfLine richLine(final List<PdfLine> lines, final int page) {
        return lines.stream()
                .filter(l -> l.getPageNumber() == page)
                .filter(l -> longestAlphabeticToken(l.getText()).length() >= 5)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no usable line found on page " + page));
    }

    /** Builds a span whose line hash matches the given line, so the redactor will redact its longest word. */
    private static Span matchingSpan(final PdfLine line) {
        final String term = longestAlphabeticToken(line.getText());
        final Span span = Span.make(0, term.length(), FilterType.AGE, "ctx", 0.25, term, "{{{REDACTED}}}",
                null, false, true, null, 0);
        span.setLineHash(line.getLineHash());
        return span;
    }

    /** Longest whitespace-delimited, purely alphabetic token (guaranteed to be a substring of the line). */
    private static String longestAlphabeticToken(final String text) {
        String longest = "";
        for (final String token : text.split("\\s+")) {
            if (token.matches("[A-Za-z]+") && token.length() > longest.length()) {
                longest = token;
            }
        }
        return longest;
    }

    private static long darkPixels(final byte[] pdf, final int pageIndex) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            final BufferedImage image = new PDFRenderer(doc).renderImageWithDPI(pageIndex, RENDER_DPI, ImageType.RGB);
            long count = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    final int rgb = image.getRGB(x, y);
                    final int r = (rgb >> 16) & 0xFF;
                    final int g = (rgb >> 8) & 0xFF;
                    final int b = rgb & 0xFF;
                    if (r < 60 && g < 60 && b < 60) {
                        count++;
                    }
                }
            }
            return count;
        }
    }

}
