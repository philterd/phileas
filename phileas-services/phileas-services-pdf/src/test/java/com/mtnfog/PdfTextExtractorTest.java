package com.mtnfog;

import com.mtnfog.services.pdf.PdfTextExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class PdfTextExtractorTest {

    private static final Logger LOGGER = LogManager.getLogger(PdfTextExtractorTest.class);

    @Test
    public void test() throws IOException {

        final InputStream is = getClass().getClassLoader().getResourceAsStream("12-12110 K.pdf");
        final byte[] document = IOUtils.toByteArray(is);
        is.close();

        final PdfTextExtractor pdfTextExtractor = new PdfTextExtractor();
        final List<String> lines = pdfTextExtractor.getLines(document);

        LOGGER.info("Read " + lines.size() + " lines");

        Assertions.assertEquals(215, lines.size());

    }

}