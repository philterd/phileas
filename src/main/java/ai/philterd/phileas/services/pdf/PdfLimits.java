/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

/**
 * Bounds on the PDFs Phileas will process. Each redacted page is rendered to an image held in memory, so a
 * small file with a huge page size, a high DPI, or very many pages could otherwise exhaust the heap.
 */
public final class PdfLimits {

    public static final int DEFAULT_MAX_PAGES = 2000;

    /** About 200 MB for one RGB page image. A Letter page at 600 DPI is about 34 million pixels. */
    public static final long DEFAULT_MAX_PIXELS_PER_PAGE = 50_000_000L;

    private PdfLimits() {
    }

    /** Refuses a document with more than {@code maxPages} pages. Zero or less sets no limit. */
    public static void checkPages(final PDDocument document, final int maxPages) {
        if (maxPages > 0 && document.getNumberOfPages() > maxPages) {
            throw new PdfLimitExceededException("The PDF has " + document.getNumberOfPages()
                    + " pages, more than the limit of " + maxPages + ".");
        }
    }

    /**
     * Refuses a document with a page that would render to more than {@code maxPixelsPerPage} pixels at
     * {@code dpi}, the same size PDFBox renders the page's crop box to. Zero or less sets no limit.
     */
    public static void checkPixels(final PDDocument document, final int dpi, final long maxPixelsPerPage) {

        if (dpi <= 0) {
            throw new PdfLimitExceededException("The policy's PDF dpi must be greater than zero.");
        }

        if (maxPixelsPerPage <= 0) {
            return;
        }

        final double scale = dpi / 72.0;
        for (int page = 0; page < document.getNumberOfPages(); page++) {
            final PDRectangle box = document.getPage(page).getCropBox();
            final double pixels = Math.ceil(box.getWidth() * scale) * Math.ceil(box.getHeight() * scale);
            if (pixels > maxPixelsPerPage) {
                throw new PdfLimitExceededException("Page " + (page + 1) + " of the PDF would render to "
                        + (long) pixels + " pixels at " + dpi + " DPI, more than the limit of " + maxPixelsPerPage
                        + ". Lower the policy's dpi or reduce the page size.");
            }
        }

    }

}
