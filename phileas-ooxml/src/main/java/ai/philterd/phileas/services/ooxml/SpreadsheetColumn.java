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
package ai.philterd.phileas.services.ooxml;

/**
 * A worksheet column for a "redact entire column" picker: its 1-based index, its letter, and the text of
 * its first-row cell.
 */
public record SpreadsheetColumn(int index, String letter, String header) {

    public String label() {
        return header == null || header.isBlank() ? letter : letter + ": " + header;
    }

    /** 1 is "A", 27 is "AA". */
    public static String indexToLetter(final int index) {
        final StringBuilder letter = new StringBuilder();
        int i = index;
        while (i > 0) {
            final int remainder = (i - 1) % 26;
            letter.insert(0, (char) ('A' + remainder));
            i = (i - 1) / 26;
        }
        return letter.toString();
    }

    /** The column of a cell reference such as "B3" (2), or 0 when there is none. */
    public static int letterToIndex(final String cellReference) {
        if (cellReference == null || cellReference.isEmpty()) {
            return 0;
        }
        int index = 0;
        for (final char c : cellReference.toCharArray()) {
            if (Character.isLetter(c)) {
                index = index * 26 + (Character.toUpperCase(c) - 'A' + 1);
            } else {
                break;
            }
        }
        return index;
    }

}
