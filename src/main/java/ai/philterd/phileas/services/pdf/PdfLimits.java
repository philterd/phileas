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

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Bounds on the PDFs Phileas will process. Each redacted page is rendered to an image held in memory, so a
 * small file with a huge page size, a high DPI, or very many pages could otherwise exhaust the heap.
 */
public final class PdfLimits {

    public static final int DEFAULT_MAX_PAGES = 2000;

    /** About 200 MB for one RGB page image. A Letter page at 600 DPI is about 34 million pixels. */
    public static final long DEFAULT_MAX_PIXELS_PER_PAGE = 50_000_000L;

    /**
     * About 256 MB. PDFBox decompresses a stream into memory before reading it, so a few kilobytes of compressed
     * data could otherwise expand to fill the heap. An RGB image at the default pixel limit is about 150 MB.
     */
    public static final long DEFAULT_MAX_STREAM_BYTES = 256L * 1024 * 1024;

    private PdfLimits() {
    }

    /**
     * Refuses a document whose pages use an image larger than {@code maxPixelsPerPage}, judged by the width and
     * height it declares, or a Flate-compressed stream that decompresses to more than {@code maxStreamBytes}. Both are
     * checked without decoding into memory, before the document is read or rendered, so a small file cannot make
     * PDFBox allocate far more than its size. Zero or less sets no limit. Covers every object each page reaches,
     * including content streams, images and forms nested in forms, fonts, and annotation appearances. Not covered:
     * images inline in a content stream, streams with more than one filter, and a JPEG whose image data is larger
     * than the size its dictionary declares.
     */
    public static void checkContent(final PDDocument document, final long maxPixelsPerPage, final long maxStreamBytes) {

        if (maxPixelsPerPage <= 0 && maxStreamBytes <= 0) {
            return;
        }

        final Set<COSBase> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        int pageNumber = 0;
        for (final PDPage page : document.getPages()) {
            pageNumber++;
            final Deque<COSBase> pending = new ArrayDeque<>();
            pending.push(page.getCOSObject());
            // Resources can be inherited from the page tree, which the walk does not follow through /Parent.
            if (page.getResources() != null) {
                pending.push(page.getResources().getCOSObject());
            }
            while (!pending.isEmpty()) {
                COSBase base = pending.pop();
                if (base instanceof COSObject object) {
                    base = object.getObject();
                }
                if (base == null || !visited.add(base)) {
                    continue;
                }
                if (base instanceof COSStream stream) {
                    checkStream(stream, pageNumber, maxPixelsPerPage, maxStreamBytes);
                }
                if (base instanceof COSDictionary dictionary) {
                    for (final COSName key : dictionary.keySet()) {
                        // Back to the page tree, or from an annotation to its page: already covered page by page.
                        if (!COSName.PARENT.equals(key) && !COSName.P.equals(key)) {
                            pending.push(dictionary.getItem(key));
                        }
                    }
                } else if (base instanceof COSArray array) {
                    for (int i = 0; i < array.size(); i++) {
                        pending.push(array.get(i));
                    }
                }
            }
        }

    }

    private static void checkStream(final COSStream stream, final int pageNumber, final long maxPixelsPerPage,
                                    final long maxStreamBytes) {

        if (maxPixelsPerPage > 0 && COSName.IMAGE.equals(stream.getCOSName(COSName.SUBTYPE))) {
            final long pixels = (long) Math.max(0, stream.getInt(COSName.WIDTH, COSName.W, 0))
                    * Math.max(0, stream.getInt(COSName.HEIGHT, COSName.H, 0));
            if (pixels > maxPixelsPerPage) {
                throw new PdfLimitExceededException("Page " + pageNumber + " of the PDF uses an image of " + pixels
                        + " pixels, more than the limit of " + maxPixelsPerPage + ".");
            }
        }

        if (maxStreamBytes > 0 && onlyFlate(stream.getFilters())) {
            final long decoded = inflatedLength(stream, maxStreamBytes);
            if (decoded > maxStreamBytes) {
                throw new PdfLimitExceededException("Page " + pageNumber + " of the PDF uses a compressed stream that "
                        + "decompresses to more than the limit of " + maxStreamBytes + " bytes.");
            }
        }

    }

    private static boolean onlyFlate(final COSBase filters) {
        if (filters instanceof COSArray array) {
            return array.size() == 1 && onlyFlate(array.getObject(0));
        }
        return COSName.FLATE_DECODE.equals(filters) || COSName.FLATE_DECODE_ABBREVIATION.equals(filters);
    }

    /**
     * How many bytes the stream inflates to, counted without keeping them, stopping once past {@code limit}. Inflated
     * as PDFBox's {@code FlateFilterDecoderStream} does: the two-byte zlib header skipped and the rest read as raw
     * deflate data, ignoring the checksum, so data PDFBox would decompress cannot pass by having a bad header. Stops,
     * as PDFBox does, at data that is not valid.
     */
    private static long inflatedLength(final COSStream stream, final long limit) {
        final Inflater inflater = new Inflater(true);
        long total = 0;
        try (InputStream raw = stream.createRawInputStream()) {
            raw.read();
            raw.read();
            final byte[] input = new byte[8 * 1024];
            final byte[] output = new byte[64 * 1024];
            while (!inflater.finished() && total <= limit) {
                if (inflater.needsInput()) {
                    final int read = raw.read(input);
                    if (read < 0) {
                        break;
                    }
                    inflater.setInput(input, 0, read);
                }
                final int inflated = inflater.inflate(output);
                // Zero only when more input, or a preset dictionary PDFBox does not supply either, is needed.
                if (inflated == 0 && inflater.needsDictionary()) {
                    break;
                }
                total += inflated;
            }
        } catch (final IOException | DataFormatException e) {
            // Truncated or not valid: counted as far as it went, as PDFBox reads it.
        } finally {
            inflater.end();
        }
        return total;
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
