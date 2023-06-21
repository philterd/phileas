/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.services.pdf;

import ai.philterd.phileas.model.enums.MimeType;
import ai.philterd.phileas.model.objects.PdfRedactionOptions;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.profile.FilterProfile;
import ai.philterd.phileas.model.profile.graphical.BoundingBox;
import ai.philterd.phileas.model.services.Redacter;
import ai.philterd.services.pdf.model.RedactedRectangle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Redacts a list of given terms in a PDF document.
 */
public class PdfRedacter extends PDFTextStripper implements Redacter {

    private static final Logger LOGGER = LogManager.getLogger(PdfRedacter.class);

    private Map<Integer, List<RedactedRectangle>> rectangles = new HashMap<>();

    private FilterProfile filterProfile;
    private final Set<Span> spans;
    private final PdfRedactionOptions pdfRedactionOptions;
    private final List<BoundingBox> boundingBoxes;

    private static final Map<String, PDColor> COLORS = new LinkedHashMap<>();

    static {
        COLORS.put("black", new PDColor(new float[]{0, 0, 0}, PDDeviceRGB.INSTANCE));
        COLORS.put("red", new PDColor(new float[]{255, 0, 0}, PDDeviceRGB.INSTANCE));
        COLORS.put("yellow", new PDColor(new float[]{1, 1, 100 / 255F}, PDDeviceRGB.INSTANCE));
    }

    public PdfRedacter(FilterProfile filterProfile,
                       Set<Span> spans, PdfRedactionOptions pdfRedactionOptions,
                       List<BoundingBox> boundingBoxes) throws IOException {

        this.filterProfile = filterProfile;
        this.spans = spans;
        this.pdfRedactionOptions = pdfRedactionOptions;
        this.boundingBoxes = boundingBoxes;

    }

    @Override
    public byte[] process(byte[] document, MimeType outputMimeType) throws IOException {

        final PDDocument pdDocument = PDDocument.load(document);

        setSortByPosition(true);
        setStartPage(0);
        setEndPage(pdDocument.getNumberOfPages());

        // PHL-244: Redact the bounding boxes in the output stream.
        for(final BoundingBox boundingBox : boundingBoxes) {

            final PDPage page = pdDocument.getPage(boundingBox.getPage() - 1);
            final PDPageContentStream contentStream = new PDPageContentStream(pdDocument, page, PDPageContentStream.AppendMode.APPEND, true);

            // TODO: Should there be a default redaction color defined as a constant?
            contentStream.setNonStrokingColor(COLORS.getOrDefault(boundingBox.getColor(), COLORS.get("black")));
            contentStream.addRect(boundingBox.getX(), boundingBox.getY(), boundingBox.getW(), boundingBox.getH());
            contentStream.fill();
            contentStream.close();

        }

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Don't use the disk for caching.
        ImageIO.setUseCache(false);

        final PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);

        if(outputMimeType == MimeType.IMAGE_JPEG) {

            final ZipOutputStream zipOut = new ZipOutputStream(outputStream);

            for (int x = 0; x < pdDocument.getNumberOfPages(); x++) {

                LOGGER.debug("Creating image from PDF page " + x);
                final BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(x, pdfRedactionOptions.getDpi());

                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                final ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
                final ImageWriteParam imageWriteParam = writer.getDefaultWriteParam();
                imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                imageWriteParam.setCompressionQuality(pdfRedactionOptions.getCompressionQuality());
                writer.setOutput(ImageIO.createImageOutputStream(byteArrayOutputStream));
                writer.write(null, new IIOImage(bufferedImage, null, null), imageWriteParam);

                // Add the image to the zip file to be returned.
                ZipEntry zipEntry = new ZipEntry("page-" + x + ".png");
                zipEntry.setSize(byteArrayOutputStream.size());

                zipOut.putNextEntry(zipEntry);
                zipOut.write(byteArrayOutputStream.toByteArray());
                zipOut.closeEntry();

            }

            zipOut.close();
            pdDocument.close();

        } else if(outputMimeType == MimeType.APPLICATION_PDF) {

            pdfRenderer.setSubsamplingAllowed(true);

            final PDDocument outputPdfDocument = new PDDocument();

            for (int x = 0; x < pdDocument.getNumberOfPages(); x++) {

                final float scale = pdfRedactionOptions.getScale();

                LOGGER.debug("Creating image from redacted PDF page " + x);

                final BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(x, pdfRedactionOptions.getDpi(), ImageType.RGB);

                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                final ImageWriter writer = ImageIO.getImageWritersByFormatName("JPEG").next();
                final ImageWriteParam imageWriteParam = writer.getDefaultWriteParam();
                imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                imageWriteParam.setCompressionQuality(pdfRedactionOptions.getCompressionQuality());
                writer.setOutput(ImageIO.createImageOutputStream(byteArrayOutputStream));
                writer.write(null, new IIOImage(bufferedImage, null, null), imageWriteParam);
                writer.dispose();

                final byte[] bytes = byteArrayOutputStream.toByteArray();
                final InputStream is = new ByteArrayInputStream(bytes);
                final BufferedImage newBi = ImageIO.read(is);
                is.close();

                final PDImageXObject pdImage = JPEGFactory.createFromImage(outputPdfDocument, newBi);

                final PDRectangle pdRectangle = new PDRectangle(pdImage.getWidth() * scale, pdImage.getHeight() * scale);
                final PDPage page = new PDPage(pdRectangle);
                outputPdfDocument.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(outputPdfDocument, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    contentStream.drawImage(pdImage, 0, 0, pdImage.getWidth() * scale, pdImage.getHeight() * scale);
                }

            }

            outputPdfDocument.save(outputStream);
            outputPdfDocument.close();

            pdDocument.close();

        } else {
            throw new IllegalArgumentException("Invalid output mime type.");
        }

        return outputStream.toByteArray();

    }

    @Override
    protected void endDocument(PDDocument doc) throws IOException {

        final int buffer = 10;

        for(int pageNumber : rectangles.keySet()) {

            final PDPage page = document.getPage(pageNumber);
            final PDPageContentStream contentStream = new PDPageContentStream(doc, page, true, true, true);

            for(final RedactedRectangle rectangle : rectangles.get(pageNumber)) {

                contentStream.addRect(
                        rectangle.getPdRectangle().getLowerLeftX(),
                        rectangle.getPdRectangle().getLowerLeftY() - 3,
                        rectangle.getPdRectangle().getWidth(),
                        rectangle.getPdRectangle().getHeight() + buffer);

            }

            // Get the color based on the filter.
            final PDColor pdColor = COLORS.getOrDefault(filterProfile.getConfig().getPdf().getRedactionColor(), COLORS.get("black"));
            contentStream.setNonStrokingColor(pdColor);
            contentStream.setRenderingMode(RenderingMode.FILL);
            contentStream.fill();
            contentStream.close();

        }

    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {

        float
                posXInit  = 0,
                posXEnd   = 0,
                posYInit  = 0,
                posYEnd   = 0,
                width     = 0,
                height    = 0,
                fontHeight = 0;

        for(final Span span : spans) {

            if (text.contains(span.getText())) {

                try {

                    final String term = span.getText();

                    // Set index to 0 to do the whole line
                    final List<Integer> indexes = findIndexes(text, span);

                    for(final int index : indexes) {

                        posXInit = textPositions.get(index).getXDirAdj();
                        posXEnd = textPositions.get(index + term.length()).getXDirAdj() + textPositions.get(index + term.length()).getWidth();
                        //posYInit = textPositions.get(index).getPageHeight() - textPositions.get(index).getYDirAdj();
                        posYEnd = textPositions.get(index).getPageHeight() - textPositions.get(index + term.length()).getYDirAdj();
                        //width = textPositions.get(index).getWidthDirAdj();
                        height = textPositions.get(index).getHeightDir();

                        // quadPoints is array of x,y coordinates in Z-like order (top-left, top-right, bottom-left,bottom-right)
                        // of the area to be highlighted

                        //final int buffer = 5;

                        /*final float quadPoints[] = {
                        posXInit, posYEnd + height + buffer,
                        posXEnd, posYEnd + height + buffer,
                        posXInit, posYInit - buffer,
                        posXEnd, posYEnd - buffer
                        };*/

                        //final List<PDAnnotation> annotations = document.getPage(this.getCurrentPageNo() - 1).getAnnotations();
                        //final PDAnnotationTextMarkup highlight = new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);

                        final PDRectangle position = new PDRectangle();
                        position.setLowerLeftX(posXInit);
                        position.setLowerLeftY(posYEnd);
                        position.setUpperRightX(posXEnd);
                        position.setUpperRightY(posYEnd + height);

                        rectangles.putIfAbsent(this.getCurrentPageNo() - 1, new LinkedList<>());

                        final RedactedRectangle redactedRectangle = new RedactedRectangle(position, span);
                        rectangles.get(this.getCurrentPageNo() - 1).add(redactedRectangle);

                        /*highlight.setRectangle(position);
                        highlight.setQuadPoints(quadPoints);
                        highlight.setConstantOpacity(100);
                        highlight.setHidden(false);
                        highlight.setNoView(false);

                        final PDColor yellow = new PDColor(new float[]{1, 1, 1 / 255F}, PDDeviceRGB.INSTANCE);
                        highlight.setColor(yellow);
                        annotations.add(highlight);*/

                    }

                } catch (Exception ex) {
                    // TODO: Need to figure out why this sometimes fail.
                    LOGGER.warn("Problem parsing PDF span: " + ex.getMessage());
                }

            }

        }

    }

    /**
     * Find all indexes of a span in a string.
     */
    private List<Integer> findIndexes(String text, Span span) {

        final List<Integer> indexes = new ArrayList<>();

        int index = 0;

        while(index != -1){

            index = text.indexOf(span.getText(), index);

            if (index != -1) {
                indexes.add(index);
                index++;
            }

        }

        return indexes;

    }

}