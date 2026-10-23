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
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.filtering.PdfFilterService;
import com.google.gson.Gson;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

/**
 * A redacted page keeps the size of the input page, whatever the policy's {@code scale} and {@code dpi}.
 * See issue #451.
 */
public class PdfPageSizeTest {

    private static final String SSN = "Patient SSN 123-45-6789 on file.";
    private static final String NOTHING = "Nothing sensitive here.";

    /** A4, US Letter, rotated pages (-90 is read as 270), and a Letter page whose CropBox is smaller than its MediaBox. */
    private static final List<InputPage> PAGES = List.of(
            new InputPage(PDRectangle.A4, null, 0, SSN),
            new InputPage(PDRectangle.LETTER, null, 0, SSN),
            new InputPage(PDRectangle.A4, null, 0, NOTHING),
            new InputPage(PDRectangle.LETTER, null, 0, NOTHING),
            new InputPage(PDRectangle.A4, null, 90, SSN),
            new InputPage(PDRectangle.LETTER, null, -90, SSN),
            new InputPage(PDRectangle.LETTER, new PDRectangle(36, 400, 400, 380), 0, SSN));

    private static Stream<Arguments> settings() {
        final List<Arguments> arguments = new ArrayList<>();
        for (final boolean preserveUnredactedPages : List.of(false, true)) {
            for (final float scale : List.of(0.25f, 1.0f, 2.0f)) {
                for (final int dpi : List.of(72, 150)) {
                    arguments.add(Arguments.of(preserveUnredactedPages, scale, dpi));
                }
            }
        }
        return arguments.stream();
    }

    @ParameterizedTest(name = "preserveUnredactedPages={0}, scale={1}, dpi={2}")
    @MethodSource("settings")
    public void redactedPagesKeepTheInputPageSize(final boolean preserveUnredactedPages, final float scale, final int dpi) throws Exception {

        final String json = "{\"config\":{\"pdf\":{\"preserveUnredactedPages\":" + preserveUnredactedPages
                + ",\"scale\":" + scale + ",\"dpi\":" + dpi + "}},\"identifiers\":{\"ssn\":{}}}";

        final PdfFilterService service = new PdfFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), mock(VectorService.class), null);

        final byte[] input = document();
        final byte[] output = service.filter(new Gson().fromJson(json, Policy.class), "ctx", input,
                MimeType.APPLICATION_PDF).getDocument();

        try (PDDocument in = Loader.loadPDF(input); PDDocument out = Loader.loadPDF(output)) {

            Assertions.assertEquals(PAGES.size(), out.getNumberOfPages());

            final PDFTextStripper stripper = new PDFTextStripper();
            final PDFRenderer renderer = new PDFRenderer(out);

            for (int x = 0; x < PAGES.size(); x++) {

                final PDPage inputPage = in.getPage(x);
                final PDPage outputPage = out.getPage(x);
                final String label = "page " + (x + 1);

                Assertions.assertArrayEquals(displayedSize(inputPage), displayedSize(outputPage), 0.001f, label);

                stripper.setStartPage(x + 1);
                stripper.setEndPage(x + 1);
                final String text = stripper.getText(out).trim();
                final boolean copied = preserveUnredactedPages && PAGES.get(x).text().equals(NOTHING);

                if (copied) {

                    // A copied page keeps its boxes and rotation, not only its displayed size.
                    Assertions.assertEquals(NOTHING, text, label);
                    Assertions.assertEquals(inputPage.getMediaBox().toString(), outputPage.getMediaBox().toString(), label);
                    Assertions.assertEquals(inputPage.getCropBox().toString(), outputPage.getCropBox().toString(), label);
                    Assertions.assertEquals(inputPage.getRotation(), outputPage.getRotation(), label);

                } else {

                    Assertions.assertEquals("", text, label + " is rasterized");

                    // The image has the policy's resolution, computed in float as PDFRenderer does, so a
                    // Letter page at 150 DPI is 1649 pixels high rather than 1650.
                    final float[] size = displayedSize(inputPage);
                    final float pixelsPerPoint = dpi / 72f;
                    final PDImageXObject image = onlyImage(outputPage);
                    Assertions.assertEquals((int) Math.floor(size[0] * pixelsPerPoint), image.getWidth(), label);
                    Assertions.assertEquals((int) Math.floor(size[1] * pixelsPerPoint), image.getHeight(), label);

                    // The image is drawn at the page's size from its origin, and the gray background reaches
                    // every corner of the rendered page.
                    Assertions.assertArrayEquals(new float[]{size[0], 0, 0, size[1], 0, 0}, imageMatrix(outputPage), 0.001f, label);

                    final BufferedImage rendered = renderer.renderImageWithDPI(x, 72);
                    final int w = rendered.getWidth();
                    final int h = rendered.getHeight();
                    for (final int[] corner : new int[][]{{2, 2}, {w - 3, 2}, {2, h - 3}, {w - 3, h - 3}}) {
                        Assertions.assertTrue(isGray(rendered.getRGB(corner[0], corner[1])),
                                label + " corner " + corner[0] + "," + corner[1]);
                    }

                }

            }

        }

    }

    /** The page's size as a viewer shows it: its CropBox, with width and height swapped for a quarter turn. */
    private static float[] displayedSize(final PDPage page) {
        final PDRectangle cropBox = page.getCropBox();
        final int rotation = page.getRotation();
        return rotation == 90 || rotation == 270
                ? new float[]{cropBox.getHeight(), cropBox.getWidth()}
                : new float[]{cropBox.getWidth(), cropBox.getHeight()};
    }

    private static PDImageXObject onlyImage(final PDPage page) throws Exception {
        final List<PDImageXObject> images = new ArrayList<>();
        for (final COSName name : page.getResources().getXObjectNames()) {
            final PDXObject xObject = page.getResources().getXObject(name);
            if (xObject instanceof PDImageXObject image) {
                images.add(image);
            }
        }
        Assertions.assertEquals(1, images.size());
        return images.get(0);
    }

    /** The operands of the {@code cm} that places the page's image, as drawImage writes it. */
    private static float[] imageMatrix(final PDPage page) throws Exception {
        final PDFStreamParser parser = new PDFStreamParser(page);
        final List<Object> tokens = parser.parse();
        float[] matrix = null;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i) instanceof Operator operator && "cm".equals(operator.getName())) {
                matrix = new float[6];
                for (int j = 0; j < 6; j++) {
                    matrix[j] = ((COSNumber) tokens.get(i - 6 + j)).floatValue();
                }
            }
            if (tokens.get(i) instanceof Operator operator && "Do".equals(operator.getName())) {
                break;
            }
        }
        Assertions.assertNotNull(matrix);
        return matrix;
    }

    private static boolean isGray(final int rgb) {
        final int r = (rgb >> 16) & 0xFF;
        final int g = (rgb >> 8) & 0xFF;
        final int b = rgb & 0xFF;
        return Math.abs(r - 128) < 24 && Math.abs(g - 128) < 24 && Math.abs(b - 128) < 24;
    }

    private static byte[] document() throws Exception {

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (final InputPage inputPage : PAGES) {

                final PDPage page = new PDPage(inputPage.mediaBox());
                if (inputPage.cropBox() != null) {
                    page.setCropBox(inputPage.cropBox());
                }
                page.setRotation(inputPage.rotation());
                document.addPage(page);

                final PDRectangle visible = page.getCropBox();

                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.setNonStrokingColor(0.5f, 0.5f, 0.5f);
                    content.addRect(0, 0, inputPage.mediaBox().getWidth(), inputPage.mediaBox().getHeight());
                    content.fill();
                    content.beginText();
                    content.setNonStrokingColor(0f, 0f, 0f);
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(visible.getLowerLeftX() + 20, visible.getUpperRightY() - 60);
                    content.showText(inputPage.text());
                    content.endText();
                }

            }

            document.save(out);
            return out.toByteArray();

        }

    }

    private record InputPage(PDRectangle mediaBox, PDRectangle cropBox, int rotation, String text) {
    }

}
