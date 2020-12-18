package com.mtnfog;

import com.mtnfog.phileas.model.enums.MimeType;
import com.mtnfog.phileas.model.services.Redacter;
import com.mtnfog.services.pdf.PdfRedacter;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class PdfRedacterTest {

    private static final Logger LOGGER = LogManager.getLogger(PdfRedacterTest.class);

    @Test
    public void testPDF() throws IOException {

        final Set<String> terms = Set.copyOf(Arrays.asList("Wendy", "Bankruptcy"));

        final String filename = "12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final Redacter pdfRedacter = new PdfRedacter(terms);
        final byte[] redacted = pdfRedacter.process(document, MimeType.APPLICATION_PDF);

        final File outputFile = File.createTempFile("output", ".pdf");
        outputFile.deleteOnExit();

        final String output = outputFile.getAbsolutePath();
        LOGGER.info("Writing redacted PDF to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

        try (InputStream md5is = Files.newInputStream(Paths.get(output))) {
            Assertions.assertEquals("7edeabef20f588253018ff90f93a32b7", org.apache.commons.codec.digest.DigestUtils.md5Hex(md5is));
        }

    }

    @Test
    public void testJpeg() throws IOException {

        final Set<String> terms = Set.copyOf(Arrays.asList("Wendy", "Bankruptcy"));

        final String filename = "12-12110 K.pdf";
        final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        final byte[] document = IOUtils.toByteArray(is);

        final Redacter pdfRedacter = new PdfRedacter(terms);
        final byte[] redacted = pdfRedacter.process(document, MimeType.IMAGE_JPEG);

        final File outputFile = File.createTempFile("output", ".jpeg");
        outputFile.deleteOnExit();

        final String output = outputFile.getAbsolutePath();
        LOGGER.info("Writing redacted JPEG to {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, redacted);

        try (InputStream md5is = Files.newInputStream(Paths.get(output))) {
            Assertions.assertEquals("f54255413a4f58193d5114a09a9165f3", org.apache.commons.codec.digest.DigestUtils.md5Hex(md5is));
        }

    }

}