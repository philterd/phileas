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
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.junit.jupiter.api.Assertions;
import java.io.ByteArrayInputStream;
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

    /** A one-page PDF whose content stream is {@code decodedBytes} of spaces, Flate-compressed to a few kilobytes. */
    private static byte[] compressionBomb(final int decodedBytes) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final PDPage page = new PDPage(PDRectangle.LETTER);
            final PDStream contents = new PDStream(document, new ByteArrayInputStream(new byte[decodedBytes]), COSName.FLATE_DECODE);
            page.setContents(contents);
            document.addPage(page);
            document.save(out);
            return out.toByteArray();
        }
    }

    /** An image stream that declares {@code width} by {@code height} pixels but holds only a few bytes. */
    private static COSStream declaredImage(final PDDocument document, final int width, final int height) throws IOException {
        final COSStream image = document.getDocument().createCOSStream();
        image.setItem(COSName.TYPE, COSName.XOBJECT);
        image.setItem(COSName.SUBTYPE, COSName.IMAGE);
        image.setInt(COSName.WIDTH, width);
        image.setInt(COSName.HEIGHT, height);
        image.setInt(COSName.BITS_PER_COMPONENT, 8);
        image.setItem(COSName.COLORSPACE, COSName.DEVICEGRAY);
        try (java.io.OutputStream data = image.createOutputStream()) {
            data.write(new byte[16]);
        }
        return image;
    }

    private static COSDictionary xobjects(final String name, final COSStream stream) {
        final COSDictionary xobjects = new COSDictionary();
        xobjects.setItem(COSName.getPDFName(name), stream);
        final COSDictionary resources = new COSDictionary();
        resources.setItem(COSName.XOBJECT, xobjects);
        return resources;
    }

    private static byte[] save(final PDDocument document) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.save(out);
            return out.toByteArray();
        }
    }

    @Test
    public void refusesACompressedStreamThatExpandsPastTheLimit() throws Exception {
        final byte[] input = compressionBomb(3_000_000);
        Assertions.assertTrue(input.length < 50_000, "the file is small: " + input.length);
        final PdfRedactionOptions options = options(72);
        options.setMaxStreamBytes(1_000_000);
        final PdfLimitExceededException e = assertThrows(PdfLimitExceededException.class,
                () -> new PdfRedactor(new Policy(), List.of(), options).process(input, MimeType.APPLICATION_PDF));
        assertTrue(e.getMessage().contains("decompresses"), e.getMessage());
        assertThrows(PdfLimitExceededException.class, () -> new PdfTextExtractor(0, 0, 1_000_000).getLines(input));
        // Under the limit it is an ordinary page.
        assertNotNull(new PdfTextExtractor(0, 0, 4_000_000).getLines(input));
    }

    @Test
    public void refusesAnImageThatDeclaresMorePixelsThanTheLimit() throws Exception {
        try (PDDocument document = new PDDocument()) {
            final PDPage page = new PDPage(PDRectangle.LETTER);
            page.setResources(new PDResources(xobjects("Im1", declaredImage(document, 100_000, 100_000))));
            document.addPage(page);
            final byte[] input = save(document);
            final PdfLimitExceededException e = assertThrows(PdfLimitExceededException.class,
                    () -> new PdfRedactor(new Policy(), List.of(), options(72)).process(input, MimeType.APPLICATION_PDF));
            assertTrue(e.getMessage().contains("image"), e.getMessage());
        }
    }

    @Test
    public void findsAnImageInsideAForm() throws Exception {
        try (PDDocument document = new PDDocument()) {
            final COSStream form = document.getDocument().createCOSStream();
            form.setItem(COSName.TYPE, COSName.XOBJECT);
            form.setItem(COSName.SUBTYPE, COSName.FORM);
            form.setItem(COSName.RESOURCES, xobjects("Im1", declaredImage(document, 100_000, 100_000)));
            final PDPage page = new PDPage(PDRectangle.LETTER);
            page.setResources(new PDResources(xobjects("Fm1", form)));
            document.addPage(page);
            final byte[] input = save(document);
            assertThrows(PdfLimitExceededException.class,
                    () -> new PdfTextExtractor(0, PdfLimits.DEFAULT_MAX_PIXELS_PER_PAGE, 0).getLines(input));
        }
    }

    @Test
    public void findsAnImageInResourcesInheritedFromThePageTree() throws Exception {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.LETTER));
            document.getPages().getCOSObject().setItem(COSName.RESOURCES, xobjects("Im1", declaredImage(document, 100_000, 100_000)));
            final byte[] input = save(document);
            assertThrows(PdfLimitExceededException.class,
                    () -> new PdfTextExtractor(0, PdfLimits.DEFAULT_MAX_PIXELS_PER_PAGE, 0).getLines(input));
        }
    }

    @Test
    public void aBadZlibHeaderDoesNotHideACompressionBomb() throws Exception {
        // PDFBox skips the two header bytes and inflates the rest, so a bomb whose header is not valid zlib still
        // decompresses in full there and must be counted the same way.
        final java.util.zip.Deflater deflater = new java.util.zip.Deflater();
        deflater.setInput(new byte[3_000_000]);
        deflater.finish();
        final ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        final byte[] chunk = new byte[8192];
        while (!deflater.finished()) {
            compressed.write(chunk, 0, deflater.deflate(chunk));
        }
        final byte[] data = compressed.toByteArray();
        data[0] = 0;
        data[1] = 0;

        try (PDDocument document = new PDDocument()) {
            final COSStream contents = document.getDocument().createCOSStream();
            contents.setItem(COSName.FILTER, COSName.FLATE_DECODE);
            try (java.io.OutputStream raw = contents.createRawOutputStream()) {
                raw.write(data);
            }
            final PDPage page = new PDPage(PDRectangle.LETTER);
            page.getCOSObject().setItem(COSName.CONTENTS, contents);
            document.addPage(page);
            try (java.io.InputStream decoded = contents.createInputStream()) {
                Assertions.assertEquals(3_000_000, decoded.readAllBytes().length, "PDFBox decodes it in full");
            }
            final byte[] input = save(document);
            assertThrows(PdfLimitExceededException.class, () -> new PdfTextExtractor(0, 0, 1_000_000).getLines(input));
        }
    }

    @Test
    public void zeroTurnsTheContentChecksOff() throws Exception {
        final byte[] input = compressionBomb(3_000_000);
        assertNotNull(new PdfTextExtractor(0, 0, 0).getLines(input));
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
