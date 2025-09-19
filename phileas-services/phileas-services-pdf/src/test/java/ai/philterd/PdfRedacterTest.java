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
package ai.philterd;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.enums.MimeType;
import ai.philterd.phileas.model.objects.PdfRedactionOptions;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.policy.graphical.BoundingBox;
import ai.philterd.phileas.model.services.Redacter;
import ai.philterd.services.pdf.PdfRedacter;
import ai.philterd.services.pdf.model.RedactedRectangle;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class PdfRedacterTest {

    private static final Logger LOGGER = LogManager.getLogger(PdfRedacterTest.class);

    @Test
    public void testPDF1() throws IOException {
        
        final Span span1 = Span.make(0, 1, FilterType.AGE, "ctx", "docid", 0.25, "Bankruptcy", "repl", null, false, true, null, 0);
        final Span span2 = Span.make(0, 1, FilterType.AGE, "ctx", "docid", 0.25, "William", "repl", null, false, true, null, 0);
        final Set<Span> spans = Set.copyOf(Arrays.asList(span1, span2));

        final String filename = "33011-pdf-118-pages.pdf"; //"12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final Policy policy = new Policy();

        final PdfRedactionOptions pdfRedactionOptions = new PdfRedactionOptions();
        pdfRedactionOptions.setDpi(150);
        pdfRedactionOptions.setScale(1.0f);
        pdfRedactionOptions.setCompressionQuality(1.0f);

        final List<BoundingBox> boundingBoxes = Collections.emptyList();
        final Redacter pdfRedacter = new PdfRedacter(policy, spans, pdfRedactionOptions, boundingBoxes);

        final byte[] redacted = pdfRedacter.process(document, MimeType.APPLICATION_PDF);

        final File outputFile = File.createTempFile("output", ".pdf");
        //outputFile.deleteOnExit();

        LOGGER.info("Writing redacted PDF to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

        showFileSizes(new File("src/test/resources/" + filename).toPath(), outputFile.toPath());

    }

    @Test
    public void testPDF2() throws IOException {

        final Span span1 = Span.make(0, 1, FilterType.DATE, "ctx", "docid", 0.25, "July 3, 2012", "||||", null, false, true, null, 0);
        final Span span2 = Span.make(0, 1, FilterType.AGE, "ctx", "docid", 0.25, "Wendy", "repl", null, false, true, null, 0);
        final Set<Span> spans = Set.copyOf(Arrays.asList(span1, span2));

        final String filename = "12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final Policy policy = new Policy();
        policy.getConfig().getPdf().setShowReplacement(true);
        final PdfRedactionOptions pdfRedactionOptions = new PdfRedactionOptions();

        final List<BoundingBox> boundingBoxes = Collections.emptyList();
        final Redacter pdfRedacter = new PdfRedacter(policy, spans, pdfRedactionOptions, boundingBoxes);

        final byte[] redacted = pdfRedacter.process(document, MimeType.APPLICATION_PDF);

        final File outputFile = File.createTempFile("output", ".pdf");
        outputFile.deleteOnExit();

        LOGGER.info("Writing redacted PDF to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

        showFileSizes(new File("src/test/resources/12-12110 K.pdf").toPath(), outputFile.toPath());

    }

    @Test
    public void testJpeg1() throws IOException {

        final Span span1 = Span.make(0, 1, FilterType.AGE, "ctx", "docid", 0.25, "Wendy", "repl", null, false, true, null, 0);
        final Span span2 = Span.make(0, 1, FilterType.AGE, "ctx", "docid", 0.25, "Bankruptcy", "repl", null, false, true, null, 0);
        final Set<Span> spans = Set.copyOf(Arrays.asList(span1, span2));

        final String filename = "12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final Policy policy = new Policy();
        final PdfRedactionOptions pdfRedactionOptions = new PdfRedactionOptions();

        final List<BoundingBox> boundingBoxes = Collections.emptyList();
        final Redacter pdfRedacter = new PdfRedacter(policy, spans, pdfRedactionOptions, boundingBoxes);
        final byte[] redacted = pdfRedacter.process(document, MimeType.IMAGE_JPEG);

        final File outputFile = File.createTempFile("output", ".zip");
        //outputFile.deleteOnExit();

        LOGGER.info("Writing redacted JPEG to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

        showFileSizes(new File("src/test/resources/12-12110 K.pdf").toPath(), outputFile.toPath());

    }

    @Test
    public void testJpeg2() throws IOException {

        final Span span1 = Span.make(0, 1, FilterType.DATE, "ctx", "docid", 0.25, "July 3, 2012", "||||", null, false, true, null, 0);
        final Set<Span> spans = Set.copyOf(Arrays.asList(span1));

        final String filename = "12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final Policy policy = new Policy();
        final PdfRedactionOptions pdfRedactionOptions = new PdfRedactionOptions();

        final List<BoundingBox> boundingBoxes = Collections.emptyList();
        final Redacter pdfRedacter = new PdfRedacter(policy, spans, pdfRedactionOptions, boundingBoxes);
        final byte[] redacted = pdfRedacter.process(document, MimeType.IMAGE_JPEG);

        final File outputFile = File.createTempFile("output", ".zip");
        outputFile.deleteOnExit();

        LOGGER.info("Writing redacted JPEG to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

        showFileSizes(new File("src/test/resources/12-12110 K.pdf").toPath(), outputFile.toPath());

    }

    @Test
    public void textPdfBoundingBox1() throws IOException {

        final String filename = "12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final Policy policy = new Policy();
        final PdfRedactionOptions pdfRedactionOptions = new PdfRedactionOptions();

        final BoundingBox boundingBox1 = new BoundingBox();
        boundingBox1.setX(100);
        boundingBox1.setY(100);
        boundingBox1.setW(500);
        boundingBox1.setH(150);
        boundingBox1.setPage(1);

        final BoundingBox boundingBox2 = new BoundingBox();
        boundingBox2.setX(100);
        boundingBox2.setY(100);
        boundingBox2.setW(500);
        boundingBox2.setH(150);
        boundingBox2.setPage(2);

        final List<BoundingBox> boundingBoxes = Arrays.asList(boundingBox1, boundingBox2);

        final Redacter pdfRedacter = new PdfRedacter(policy, Collections.emptySet(), pdfRedactionOptions, boundingBoxes);
        final byte[] redacted = pdfRedacter.process(document, MimeType.APPLICATION_PDF);

        final File outputFile = File.createTempFile("output", ".pdf");
        outputFile.deleteOnExit();

        LOGGER.info("Writing redacted PDF to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

    }

    @Test
    public void textPdfBoundingBox2() throws IOException {

        final String filename = "12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final Policy policy = new Policy();
        final PdfRedactionOptions pdfRedactionOptions = new PdfRedactionOptions();

        final BoundingBox boundingBox1 = new BoundingBox();
        boundingBox1.setX(100);
        boundingBox1.setY(100);
        boundingBox1.setW(500);
        boundingBox1.setH(150);
        boundingBox1.setPage(1);
        boundingBox1.setColor("yellow");
        boundingBox1.setMimeType(MimeType.APPLICATION_PDF.toString());

        final BoundingBox boundingBox2 = new BoundingBox();
        boundingBox2.setX(100);
        boundingBox2.setY(50);
        boundingBox2.setW(250);
        boundingBox2.setH(75);
        boundingBox2.setPage(2);
        boundingBox2.setColor("red");
        boundingBox2.setMimeType(MimeType.APPLICATION_PDF.toString());

        final List<BoundingBox> boundingBoxes = Arrays.asList(boundingBox1, boundingBox2);

        final Redacter pdfRedacter = new PdfRedacter(policy, Collections.emptySet(), pdfRedactionOptions, boundingBoxes);
        final byte[] redacted = pdfRedacter.process(document, MimeType.APPLICATION_PDF);

        final File outputFile = File.createTempFile("output", ".pdf");
        outputFile.deleteOnExit();

        LOGGER.info("Writing redacted PDF to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

    }

    @Test
    public void testPdfSpansAndBoundingBoxes() throws IOException {

        final Span span1 = Span.make(0, 1, FilterType.AGE, "ctx", "docid", 0.25, "Wendy", "repl", null, false, true, null, 0);
        final Span span2 = Span.make(0, 1, FilterType.AGE, "ctx", "docid", 0.25, "Bankruptcy", "repl", null, false, true, null, 0);
        final Set<Span> spans = Set.copyOf(Arrays.asList(span1, span2));

        final String filename = "12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final Policy policy = new Policy();
        final PdfRedactionOptions pdfRedactionOptions = new PdfRedactionOptions();

        final BoundingBox boundingBox1 = new BoundingBox();
        boundingBox1.setX(100);
        boundingBox1.setY(100);
        boundingBox1.setW(500);
        boundingBox1.setH(150);
        boundingBox1.setPage(1);

        final BoundingBox boundingBox2 = new BoundingBox();
        boundingBox2.setX(100);
        boundingBox2.setY(100);
        boundingBox2.setW(500);
        boundingBox2.setH(150);
        boundingBox2.setPage(2);

        final List<BoundingBox> boundingBoxes = Arrays.asList(boundingBox1, boundingBox2);

        final Redacter pdfRedacter = new PdfRedacter(policy, spans, pdfRedactionOptions, boundingBoxes);
        final byte[] redacted = pdfRedacter.process(document, MimeType.APPLICATION_PDF);

        final File outputFile = File.createTempFile("output", ".pdf");
        outputFile.deleteOnExit();

        LOGGER.info("Writing redacted PDF to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

    }

    @Test
    public void testAddReplacementTextToRect() throws IOException {

        var contentStream = Mockito.mock(PDPageContentStream.class);

        final Span span1 = Span.make(0, 1, FilterType.AGE, "ctx", "docid", 0.25, "Wendy", "repl", null, false, true, null, 0);
        final Span span2 = Span.make(0, 1, FilterType.AGE, "ctx", "docid", 0.25, "Bankruptcy", "repl", null, false, true, null, 0);
        final Set<Span> spans = Set.copyOf(Arrays.asList(span1, span2));

        final Policy policy = new Policy();
        policy.getConfig().getPdf().setShowReplacement(true);
        final PdfRedactionOptions pdfRedactionOptions = new PdfRedactionOptions();

        final List<BoundingBox> boundingBoxes = Collections.emptyList();

        final PdfRedacter pdfRedacter = new PdfRedacter(policy, spans, pdfRedactionOptions, boundingBoxes);

        RedactedRectangle redactedRectangle = new RedactedRectangle(PDRectangle.LETTER, span1);
        pdfRedacter.addReplacementTextToRect(redactedRectangle, contentStream);

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

    private void showFileSizes(Path inputFile, Path outputFile) throws IOException {

        long inputFileBytes = Files.size(inputFile);
        long outputFileBytes = Files.size(outputFile);
        long difference = inputFileBytes - outputFileBytes;
        LOGGER.info("Input PDF: {} ({}), Redacted PDF: {} ({})", inputFileBytes, FileUtils.byteCountToDisplaySize(inputFileBytes), outputFileBytes, FileUtils.byteCountToDisplaySize(outputFileBytes));
        LOGGER.info("Size difference of {} ({})", difference, FileUtils.byteCountToDisplaySize(Math.abs(difference)));

    }

}