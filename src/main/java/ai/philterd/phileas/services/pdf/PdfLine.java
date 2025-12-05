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

import org.apache.pdfbox.text.TextPosition;

import java.util.List;

public class PdfLine {

    final String text;
    final int pageNumber;
    final List<TextPosition> textPositions;

    public PdfLine(final String text, final int pageNumber, final List<TextPosition> textPositions) {
        this.text = text;
        this.pageNumber = pageNumber;
        this.textPositions = textPositions;
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

    public List<TextPosition> getTextPositions() {
        return textPositions;
    }

    public String getLineHash() {
        return PdfRedacter.lineHash(text, textPositions, pageNumber);
    }

}
