package com.mtnfog;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.MimeType;
import com.mtnfog.phileas.model.objects.RedactionOptions;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.Redacter;
import com.mtnfog.services.pdf.PdfRedacter;
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
import java.util.Set;

public class PdfRedacterTest {

    private static final Logger LOGGER = LogManager.getLogger(PdfRedacterTest.class);

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void testPDF1() throws IOException {

        final Span span1 = Span.make(0, 1, FilterType.NER_ENTITY, "ctx", "docid", 0.25, "Wendy", "repl", null, false, null);
        final Span span2 = Span.make(0, 1, FilterType.NER_ENTITY, "ctx", "docid", 0.25, "Bankruptcy", "repl", null, false, null);
        final Set<Span> spans = Set.copyOf(Arrays.asList(span1, span2));

        final String filename = "12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final FilterProfile filterProfile = new FilterProfile();
        final RedactionOptions redactionOptions = new RedactionOptions();

        final Redacter pdfRedacter = new PdfRedacter(filterProfile, spans, redactionOptions);
        final byte[] redacted = pdfRedacter.process(document, MimeType.APPLICATION_PDF);

        final File outputFile = File.createTempFile("output", ".pdf");
        outputFile.deleteOnExit();

        LOGGER.info("Writing redacted PDF to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

        // TODO: How to assert? MD5 gives a different value each time.

    }

    @Test
    public void testPDF2() throws IOException {

        final Span span1 = Span.make(0, 1, FilterType.DATE, "ctx", "docid", 0.25, "July 3, 2012", "||||", null, false, null);
        final Set<Span> spans = Set.copyOf(Arrays.asList(span1));

        final String filename = "12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final FilterProfile filterProfile = new FilterProfile();
        final RedactionOptions redactionOptions = new RedactionOptions();

        final Redacter pdfRedacter = new PdfRedacter(filterProfile, spans, redactionOptions);
        final byte[] redacted = pdfRedacter.process(document, MimeType.APPLICATION_PDF);

        final File outputFile = File.createTempFile("output", ".pdf");
        outputFile.deleteOnExit();

        LOGGER.info("Writing redacted PDF to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

        // TODO: How to assert? MD5 gives a different value each time.

    }

    @Test
    public void testJpeg1() throws IOException {

        final Span span1 = Span.make(0, 1, FilterType.NER_ENTITY, "ctx", "docid", 0.25, "Wendy", "repl", null, false, null);
        final Span span2 = Span.make(0, 1, FilterType.NER_ENTITY, "ctx", "docid", 0.25, "Bankruptcy", "repl", null, false, null);
        final Set<Span> spans = Set.copyOf(Arrays.asList(span1, span2));

        final String filename = "12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final FilterProfile filterProfile = new FilterProfile();
        final RedactionOptions redactionOptions = new RedactionOptions();

        final Redacter pdfRedacter = new PdfRedacter(filterProfile, spans, redactionOptions);
        final byte[] redacted = pdfRedacter.process(document, MimeType.IMAGE_JPEG);

        final File outputFile = File.createTempFile("output", ".zip");
        outputFile.deleteOnExit();

        LOGGER.info("Writing redacted JPEG to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

        // TODO: How to assert? MD5 gives a different value each time.

    }

    @Test
    public void testJpeg2() throws IOException {

        final Span span1 = Span.make(0, 1, FilterType.DATE, "ctx", "docid", 0.25, "July 3, 2012", "||||", null, false, null);
        final Set<Span> spans = Set.copyOf(Arrays.asList(span1));

        final String filename = "12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final FilterProfile filterProfile = new FilterProfile();
        final RedactionOptions redactionOptions = new RedactionOptions();

        final Redacter pdfRedacter = new PdfRedacter(filterProfile, spans, redactionOptions);
        final byte[] redacted = pdfRedacter.process(document, MimeType.IMAGE_JPEG);

        final File outputFile = File.createTempFile("output", ".zip");
        outputFile.deleteOnExit();

        LOGGER.info("Writing redacted JPEG to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

        // TODO: How to assert? MD5 gives a different value each time.

    }

}