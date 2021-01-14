package com.mtnfog.services.pdf;

import com.mtnfog.phileas.model.enums.MimeType;
import com.mtnfog.phileas.model.objects.RedactionOptions;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.services.Redacter;
import com.mtnfog.services.pdf.model.RedactedRectangle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
    private final RedactionOptions redactionOptions;

    private static final Map<String, PDColor> COLORS = new LinkedHashMap<>();

    static {
        COLORS.put("black", new PDColor(new float[]{0, 0, 0}, PDDeviceRGB.INSTANCE));
        COLORS.put("red", new PDColor(new float[]{0, 0, 0}, PDDeviceRGB.INSTANCE));
        COLORS.put("yellow", new PDColor(new float[]{0, 0, 0}, PDDeviceRGB.INSTANCE));
    }

    public PdfRedacter(FilterProfile filterProfile, Set<Span> spans, RedactionOptions redactionOptions) throws IOException {

        this.filterProfile = filterProfile;
        this.spans = spans;
        this.redactionOptions = redactionOptions;

    }

    @Override
    public byte[] process(byte[] document, MimeType outputType) throws IOException {

        final PDDocument pdDocument = PDDocument.load(document);

        setSortByPosition(true);
        setStartPage(0);
        setEndPage(pdDocument.getNumberOfPages());

        final Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
        writeText(pdDocument, dummy);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        if(outputType == MimeType.APPLICATION_PDF) {

            pdDocument.save(outputStream);
            pdDocument.close();

        } else if(outputType == MimeType.IMAGE_JPEG) {

            final PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);

            final ZipOutputStream zipOut = new ZipOutputStream(outputStream);

            for (int x = 0; x < pdDocument.getNumberOfPages(); x++) {

                LOGGER.debug("Creating iamge from PDF page " + x);
                final BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(x,600);

                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "jpg", baos);
                baos.close();

                ZipEntry zipEntry = new ZipEntry("page-" + x + ".jpeg");
                zipEntry.setSize(baos.size());

                zipOut.putNextEntry(zipEntry);
                zipOut.write(baos.toByteArray());
                zipOut.closeEntry();

            }

            zipOut.close();
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

                final String term = span.getText();

                // Set index to 0 to do the whole line
                final int index = text.indexOf(span.getText());

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

        }

    }

}