package io.philterd;

import io.philterd.phileas.model.enums.FilterType;
import io.philterd.phileas.model.enums.MimeType;
import io.philterd.phileas.model.objects.PdfRedactionOptions;
import io.philterd.phileas.model.objects.RedactionOptions;
import io.philterd.phileas.model.objects.Span;
import io.philterd.phileas.model.profile.FilterProfile;
import io.philterd.phileas.model.profile.graphical.BoundingBox;
import io.philterd.phileas.model.services.AlertService;
import io.philterd.phileas.model.services.Redacter;
import io.philterd.services.pdf.PdfRedacter;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class PdfRedacterTest {

    private static final Logger LOGGER = LogManager.getLogger(PdfRedacterTest.class);

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void testPDF1() throws IOException {

        final Span span1 = Span.make(0, 1, FilterType.PERSON, "ctx", "docid", 0.25, "Wendy", "repl", null, false, null);
        final Span span2 = Span.make(0, 1, FilterType.PERSON, "ctx", "docid", 0.25, "Bankruptcy", "repl", null, false, null);
        final Set<Span> spans = Set.copyOf(Arrays.asList(span1, span2));

        final String filename = "12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final FilterProfile filterProfile = new FilterProfile();
        final PdfRedactionOptions redactionOptions = new PdfRedactionOptions();

        final List<BoundingBox> boundingBoxes = Collections.emptyList();
        final Redacter pdfRedacter = new PdfRedacter(filterProfile, spans, redactionOptions, boundingBoxes);

        final byte[] redacted = pdfRedacter.process(document, MimeType.APPLICATION_PDF);

        final File outputFile = File.createTempFile("output", ".pdf");
        outputFile.deleteOnExit();

        LOGGER.info("Writing redacted PDF to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

    }

    @Test
    public void testPDF2() throws IOException {

        final Span span1 = Span.make(0, 1, FilterType.DATE, "ctx", "docid", 0.25, "July 3, 2012", "||||", null, false, null);
        final Set<Span> spans = Set.copyOf(Arrays.asList(span1));

        final String filename = "12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final FilterProfile filterProfile = new FilterProfile();
        final PdfRedactionOptions redactionOptions = new PdfRedactionOptions();

        final List<BoundingBox> boundingBoxes = Collections.emptyList();
        final Redacter pdfRedacter = new PdfRedacter(filterProfile, spans, redactionOptions, boundingBoxes);

        final byte[] redacted = pdfRedacter.process(document, MimeType.APPLICATION_PDF);

        final File outputFile = File.createTempFile("output", ".pdf");
        outputFile.deleteOnExit();

        LOGGER.info("Writing redacted PDF to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

    }

    @Test
    public void testJpeg1() throws IOException {

        final Span span1 = Span.make(0, 1, FilterType.PERSON, "ctx", "docid", 0.25, "Wendy", "repl", null, false, null);
        final Span span2 = Span.make(0, 1, FilterType.PERSON, "ctx", "docid", 0.25, "Bankruptcy", "repl", null, false, null);
        final Set<Span> spans = Set.copyOf(Arrays.asList(span1, span2));

        final String filename = "12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final FilterProfile filterProfile = new FilterProfile();
        final PdfRedactionOptions redactionOptions = new PdfRedactionOptions();

        final List<BoundingBox> boundingBoxes = Collections.emptyList();
        final Redacter pdfRedacter = new PdfRedacter(filterProfile, spans, redactionOptions, boundingBoxes);
        final byte[] redacted = pdfRedacter.process(document, MimeType.IMAGE_JPEG);

        final File outputFile = File.createTempFile("output", ".zip");
        //outputFile.deleteOnExit();

        LOGGER.info("Writing redacted JPEG to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

    }

    @Test
    public void testJpeg2() throws IOException {

        final Span span1 = Span.make(0, 1, FilterType.DATE, "ctx", "docid", 0.25, "July 3, 2012", "||||", null, false, null);
        final Set<Span> spans = Set.copyOf(Arrays.asList(span1));

        final String filename = "12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final FilterProfile filterProfile = new FilterProfile();
        final PdfRedactionOptions redactionOptions = new PdfRedactionOptions();

        final List<BoundingBox> boundingBoxes = Collections.emptyList();
        final Redacter pdfRedacter = new PdfRedacter(filterProfile, spans, redactionOptions, boundingBoxes);
        final byte[] redacted = pdfRedacter.process(document, MimeType.IMAGE_JPEG);

        final File outputFile = File.createTempFile("output", ".zip");
        outputFile.deleteOnExit();

        LOGGER.info("Writing redacted JPEG to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

    }

    @Test
    public void textPdfBoundingBox1() throws IOException {

        final String filename = "12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final FilterProfile filterProfile = new FilterProfile();
        final PdfRedactionOptions redactionOptions = new PdfRedactionOptions();

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

        final Redacter pdfRedacter = new PdfRedacter(filterProfile, Collections.emptySet(), redactionOptions, boundingBoxes);
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

        final FilterProfile filterProfile = new FilterProfile();
        final PdfRedactionOptions redactionOptions = new PdfRedactionOptions();

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

        final Redacter pdfRedacter = new PdfRedacter(filterProfile, Collections.emptySet(), redactionOptions, boundingBoxes);
        final byte[] redacted = pdfRedacter.process(document, MimeType.APPLICATION_PDF);

        final File outputFile = File.createTempFile("output", ".pdf");
        outputFile.deleteOnExit();

        LOGGER.info("Writing redacted PDF to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

    }

    @Test
    public void testPdfSpansAndBoundingBoxes() throws IOException {

        final Span span1 = Span.make(0, 1, FilterType.PERSON, "ctx", "docid", 0.25, "Wendy", "repl", null, false, null);
        final Span span2 = Span.make(0, 1, FilterType.PERSON, "ctx", "docid", 0.25, "Bankruptcy", "repl", null, false, null);
        final Set<Span> spans = Set.copyOf(Arrays.asList(span1, span2));

        final String filename = "12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final FilterProfile filterProfile = new FilterProfile();
        final PdfRedactionOptions redactionOptions = new PdfRedactionOptions();

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

        final Redacter pdfRedacter = new PdfRedacter(filterProfile, spans, redactionOptions, boundingBoxes);
        final byte[] redacted = pdfRedacter.process(document, MimeType.APPLICATION_PDF);

        final File outputFile = File.createTempFile("output", ".pdf");
        outputFile.deleteOnExit();

        LOGGER.info("Writing redacted PDF to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

    }

}