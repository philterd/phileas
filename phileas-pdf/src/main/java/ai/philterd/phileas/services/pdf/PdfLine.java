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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.pdfbox.text.TextPosition;

import java.util.List;

/**
 * Uniquely represents a line of text in a PDF document.
 */
public class PdfLine {

    final String text;
    final int pageNumber;
    final List<TextPosition> textPositions;

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

    public String getLineHash() {
        return lineHash(textPositions, pageNumber);
    }

}
