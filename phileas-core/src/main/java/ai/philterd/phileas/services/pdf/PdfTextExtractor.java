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

import ai.philterd.phileas.model.services.TextExtractor;
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

        final PDDocument pdDocument = Loader.loadPDF(document);

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