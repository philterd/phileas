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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

/**
 * Extracts lines of text from PDF documents.
 */
public class PdfTextExtractor extends PDFTextStripper implements TextExtractor {

    private static final Logger LOGGER = LogManager.getLogger(PdfTextExtractor.class);

    private final List<PdfLine> lines;
    private int pageNumber;

    public PdfTextExtractor() {
        this.lines = new LinkedList<>();
    }

    @Override
    public List<PdfLine> getLines(final byte[] document) throws IOException {

        final PDDocument pdDocument = Loader.loadPDF(document);

        this.setSortByPosition(true);
        this.setStartPage(0);

        for(int i = 0; i < pdDocument.getNumberOfPages(); i++) {

            pageNumber = i;

            this.setStartPage(i);
            this.setEndPage(i + 1);

            final Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
            this.writeText(pdDocument, dummy);

        }

        pdDocument.close();

        LOGGER.debug("Read {} lines from the PDF.", lines.size());

        return lines;

    }

    @Override
    public void writeString(final String text, final List<TextPosition> textPositions) {

        // The text positions is a list of positions for all characters in the string.
        lines.add(new PdfLine(text, pageNumber, textPositions));

    }

}