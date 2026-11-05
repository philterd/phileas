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

public class PdfRedactionOptions {

    private int dpi = 150;
    private float compressionQuality = 1.0F;
    private float scale = 1.0F;
    private boolean preserveUnredactedPages = false;
    private int maxPages = PdfLimits.DEFAULT_MAX_PAGES;
    private long maxPixelsPerPage = PdfLimits.DEFAULT_MAX_PIXELS_PER_PAGE;
    private long maxStreamBytes = PdfLimits.DEFAULT_MAX_STREAM_BYTES;

    public PdfRedactionOptions() {

    }

    public PdfRedactionOptions(final int dpi, final float compressionQuality, final boolean preserveUnredactedPages) {
        this.dpi = dpi;
        this.compressionQuality = compressionQuality;
        this.preserveUnredactedPages = preserveUnredactedPages;
    }

    /**
     * @deprecated The scale is ignored: a redacted page keeps its input size. Use
     * {@link #PdfRedactionOptions(int, float, boolean)}.
     */
    @Deprecated
    public PdfRedactionOptions(final int dpi, final float compressionQuality, final float scale, final boolean preserveUnredactedPages) {
        this(dpi, compressionQuality, preserveUnredactedPages);
        this.scale = scale;
    }

    public int getDpi() {
        return dpi;
    }

    public void setDpi(int dpi) {
        this.dpi = dpi;
    }

    public float getCompressionQuality() {
        return compressionQuality;
    }

    public void setCompressionQuality(float compressionQuality) {
        this.compressionQuality = compressionQuality;
    }

    /** @deprecated Ignored: a redacted page keeps its input size. */
    @Deprecated
    public float getScale() {
        return scale;
    }

    /** @deprecated Ignored: a redacted page keeps its input size. */
    @Deprecated
    public void setScale(float scale) {
        this.scale = scale;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public long getMaxPixelsPerPage() {
        return maxPixelsPerPage;
    }

    public void setMaxPixelsPerPage(long maxPixelsPerPage) {
        this.maxPixelsPerPage = maxPixelsPerPage;
    }

    public long getMaxStreamBytes() {
        return maxStreamBytes;
    }

    public void setMaxStreamBytes(long maxStreamBytes) {
        this.maxStreamBytes = maxStreamBytes;
    }

    public boolean getPreserveUnredactedPages() { return preserveUnredactedPages; }

    public void setPreserveUnredactedPages(boolean preserveUnredactedPages) {
        this.preserveUnredactedPages = preserveUnredactedPages;
    }

}
