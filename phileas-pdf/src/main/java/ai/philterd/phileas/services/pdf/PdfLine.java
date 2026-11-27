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

import ai.philterd.phileas.policy.graphical.BoundingBox;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;

import java.util.Arrays;
import java.util.List;

/**
 * Uniquely represents a line of text in a PDF document.
 */
public class PdfLine {

    final String text;
    final int pageNumber;
    final List<TextPosition> textPositions;

    // What follows this piece of text on the page: a space when the next piece is on the same line.
    private String separatorAfter = "\n";

    public PdfLine(final String text, final int pageNumber, final List<TextPosition> textPositions) {
        this.text = text;
        this.pageNumber = pageNumber;
        this.textPositions = textPositions;
    }

    // The MS5 hash is not used for cryptographic purposes. It is used to generate a unique identifier for a line.
    @SuppressWarnings("squid:S4790")
    public static String lineHash(final List<TextPosition> textPositions, final int pageNumber) {

        final StringBuilder sb = new StringBuilder();

        for (final TextPosition textPosition : textPositions) {
            sb.append(textPosition.getUnicode());
        }

        sb.append(pageNumber);

        return DigestUtils.md5Hex(sb.toString());

    }

    /**
     * Which characters of the line a bounding box covers, by each glyph's box in the page's user space, the space
     * the bounding boxes are drawn in. A glyph that touches a box counts as covered. When the line's characters
     * cannot be matched to its glyphs one for one, the whole line counts as covered if any glyph is.
     * @param boxes The bounding boxes on the line's page.
     * @return One flag per character of {@link #getText()}, or {@code null} when no character is covered.
     */
    public boolean[] coveredBy(final List<BoundingBox> boxes) {

        if (boxes.isEmpty() || textPositions == null || textPositions.isEmpty()) {
            return null;
        }

        final boolean[] glyphs = new boolean[textPositions.size()];
        boolean any = false;
        for (int i = 0; i < glyphs.length; i++) {
            glyphs[i] = isCovered(textPositions.get(i), boxes);
            any |= glyphs[i];
        }

        if (!any) {
            return null;
        }

        final boolean[] covered = new boolean[text.length()];
        if (glyphs.length == text.length()) {
            System.arraycopy(glyphs, 0, covered, 0, glyphs.length);
        } else {
            Arrays.fill(covered, true);
        }
        return covered;

    }

    private static boolean isCovered(final TextPosition position, final List<BoundingBox> boxes) {

        // The text rendering matrix puts the glyph's baseline origin in user space.
        final Matrix matrix = position.getTextMatrix();
        final float x = matrix.getTranslateX();
        final float y = matrix.getTranslateY();
        final float width = Math.max(position.getWidthDirAdj(), 0.01f);
        final float height = Math.max(position.getHeightDir(), 0.01f);

        // Below the baseline too, for descenders.
        final float bottom = y - height / 4;
        final float top = y + height;

        for (final BoundingBox box : boxes) {
            final float left = Math.min(box.getX(), box.getX() + box.getW());
            final float right = Math.max(box.getX(), box.getX() + box.getW());
            final float lower = Math.min(box.getY(), box.getY() + box.getH());
            final float upper = Math.max(box.getY(), box.getY() + box.getH());
            if (x <= right && x + width >= left && bottom <= upper && top >= lower) {
                return true;
            }
        }
        return false;

    }

    @Override
    public String toString() {
        return text + " (" + pageNumber + ")";
    }

    public String getText() {
        return text;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    /**
     * What follows this text in the page's text: a space when the extractor read the next text as another word
     * of the same line, as it does for words placed separately on the page, and otherwise a line break.
     */
    public String getSeparatorAfter() {
        return separatorAfter;
    }

    void setSeparatorAfter(final String separatorAfter) {
        this.separatorAfter = separatorAfter;
    }

    public String getLineHash() {
        return lineHash(textPositions, pageNumber);
    }

}
