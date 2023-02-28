package io.philterd.test.phileas.services.analyzers;

import io.philterd.phileas.model.objects.DocumentAnalysis;
import io.philterd.phileas.model.objects.DocumentType;
import io.philterd.phileas.services.analyzers.DocumentAnalyzer;
import io.philterd.services.pdf.PdfTextExtractor;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class DocumentAnalyzerTest {

    @Test
    public void identify_B2540_blank() throws IOException {

        final byte[] document = readPDF("form_b2540_0.pdf");

        // Get the lines of text from the PDF file.
        final PdfTextExtractor pdfTextExtractor = new PdfTextExtractor();
        final List<String> lines = pdfTextExtractor.getLines(document);

        final DocumentAnalyzer documentAnalyzer = new DocumentAnalyzer();
        final DocumentAnalysis documentAnalysis = documentAnalyzer.analyze(lines);

        Assertions.assertEquals(DocumentType.SUBPOENA, documentAnalysis.getDocumentType());
        Assertions.assertEquals("Form 2540", documentAnalysis.getDocument());

    }

    @Test
    public void identify_B2540_1() throws IOException {

        final byte[] document = readPDF("Form_254_Case_11-34325.pdf");

        // Get the lines of text from the PDF file.
        final PdfTextExtractor pdfTextExtractor = new PdfTextExtractor();
        final List<String> lines = pdfTextExtractor.getLines(document);

        final DocumentAnalyzer documentAnalyzer = new DocumentAnalyzer();
        final DocumentAnalysis documentAnalysis = documentAnalyzer.analyze(lines);

        Assertions.assertEquals(DocumentType.SUBPOENA, documentAnalysis.getDocumentType());
        Assertions.assertEquals("Form 2540", documentAnalysis.getDocument());

    }

    @Test
    public void identify_B2540_2() throws IOException {

        final byte[] document = readPDF("Form_254_Case_11-42072.pdf");

        // Get the lines of text from the PDF file.
        final PdfTextExtractor pdfTextExtractor = new PdfTextExtractor();
        final List<String> lines = pdfTextExtractor.getLines(document);

        final DocumentAnalyzer documentAnalyzer = new DocumentAnalyzer();
        final DocumentAnalysis documentAnalysis = documentAnalyzer.analyze(lines);

        Assertions.assertEquals(DocumentType.SUBPOENA, documentAnalysis.getDocumentType());
        Assertions.assertEquals("Form 2540", documentAnalysis.getDocument());

    }

    private byte[] readPDF(String fileName) throws IOException {

        final InputStream is = this.getClass().getResourceAsStream("/pdfs/" + fileName);
        final byte[] document = IOUtils.toByteArray(is);
        is.close();

        return document;

    }

}
