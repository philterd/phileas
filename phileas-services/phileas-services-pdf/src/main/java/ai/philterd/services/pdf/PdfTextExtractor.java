package ai.philterd.services.pdf;

import ai.philterd.phileas.model.services.TextExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

/**
 * Extracts lines of text from PDF documes.
 */
public class PdfTextExtractor extends PDFTextStripper implements TextExtractor {

    private static final Logger LOGGER = LogManager.getLogger(PdfTextExtractor.class);

    private List<String> lines;

    public PdfTextExtractor() throws IOException {
        this.lines = new LinkedList<>();
    }

    @Override
    public List<String> getLines(byte[] document) throws IOException {

        final PDDocument pdDocument = PDDocument.load(document);

        this.setSortByPosition(true);
        this.setStartPage(0);
        this.setEndPage(pdDocument.getNumberOfPages());

        final Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
        this.writeText(pdDocument, dummy);

        pdDocument.close();

        LOGGER.debug("Read {} lines from the PDF.", lines.size());

        return this.lines;

    }

    @Override
    public void writeString(String str, List<TextPosition> textPositions) {
        lines.add(str);
    }

}