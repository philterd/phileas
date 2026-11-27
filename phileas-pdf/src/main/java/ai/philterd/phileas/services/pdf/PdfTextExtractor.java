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
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Extracts lines of text from PDF documents.
 */
public class PdfTextExtractor extends PDFTextStripper implements TextExtractor {

    private static final Logger LOGGER = LogManager.getLogger(PdfTextExtractor.class);

    private final LinkedList<PdfLine> lines;
    private final int maxPages;
    private final long maxPixelsPerPage;
    private final long maxStreamBytes;
    private int pageNumber;
    private int pageCount;
    private final Map<String, String> documentInformation = new LinkedHashMap<>();

    public PdfTextExtractor() {
        this(0);
    }

    /** @param maxPages the most pages a document may have; zero or less sets no limit. */
    public PdfTextExtractor(final int maxPages) {
        this(maxPages, 0, 0);
    }

    /**
     * @param maxPages the most pages a document may have; zero or less sets no limit.
     * @param maxPixelsPerPage the largest image a page may use, in pixels; zero or less sets no limit.
     * @param maxStreamBytes the most bytes a compressed stream may decompress to; zero or less sets no limit.
     */
    public PdfTextExtractor(final int maxPages, final long maxPixelsPerPage, final long maxStreamBytes) {
        this.lines = new LinkedList<>();
        this.maxPages = maxPages;
        this.maxPixelsPerPage = maxPixelsPerPage;
        this.maxStreamBytes = maxStreamBytes;
    }

    @Override
    public List<PdfLine> getLines(final byte[] document) throws IOException {

        final PDDocument pdDocument = Loader.loadPDF(document);

        try {
            PdfLimits.checkPages(pdDocument, maxPages);
            PdfLimits.checkContent(pdDocument, maxPixelsPerPage, maxStreamBytes);
        } catch (final PdfLimitExceededException e) {
            pdDocument.close();
            throw e;
        }

        pageCount = pdDocument.getNumberOfPages();
        readDocumentInformation(pdDocument);

        this.setSortByPosition(true);
        this.setStartPage(0);

        for(int i = 0; i < pdDocument.getNumberOfPages(); i++) {

            pageNumber = i;

            // PDFTextStripper numbers pages from 1.
            this.setStartPage(i + 1);
            this.setEndPage(i + 1);

            final Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
            this.writeText(pdDocument, dummy);

        }

        pdDocument.close();

        LOGGER.debug("Read {} lines from the PDF.", lines.size());

        return lines;

    }

    /**
     * The text entries of the document information dictionary (such as {@code Author} and {@code Title}) of the
     * document last read by {@link #getLines(byte[])}, in the dictionary's order. XMP metadata is not read.
     */
    public Map<String, String> getDocumentInformation() {
        return documentInformation;
    }

    private void readDocumentInformation(final PDDocument pdDocument) {
        documentInformation.clear();
        final COSDictionary info = pdDocument.getDocumentInformation().getCOSObject();
        for (final Map.Entry<COSName, COSBase> entry : info.entrySet()) {
            final COSBase value = entry.getValue() == null ? null : entry.getValue().getCOSObject();
            if (value instanceof COSString string && !string.getString().isBlank()) {
                documentInformation.put(entry.getKey().getName(), string.getString());
            }
        }
    }

    /** The number of pages in the document last read by {@link #getLines(byte[])}. */
    public int getPageCount() {
        return pageCount;
    }

    @Override
    protected void writeWordSeparator() throws IOException {
        // The next text is another word of the same line.
        if (!lines.isEmpty()) {
            lines.getLast().setSeparatorAfter(" ");
        }
        super.writeWordSeparator();
    }

    @Override
    public void writeString(final String text, final List<TextPosition> textPositions) {

        // The text positions is a list of positions for all characters in the string.
        lines.add(new PdfLine(text, pageNumber, textPositions));

    }

}