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
package ai.philterd.phileas.services.rtf;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits an RTF document into tokens. The document is read one character per byte (ISO-8859-1), so joining the
 * tokens' raw text gives back the same bytes. RTF from outside is untrusted: a document that is not RTF, has
 * not close its groups, nests groups deeper than the limit, has an over-long control word or parameter, or declares
 * more binary data than it has, is refused with an {@link RtfFormatException}. The work is linear in the input.
 */
final class RtfTokenizer {

    /** The deepest group nesting read. Word writes far less. */
    static final int MAX_DEPTH = 1000;

    // The spec limits a control word's name to 32 letters and its parameter to a signed 16-bit number; some
    // writers use larger numbers, so up to 10 digits are read.
    private static final int MAX_WORD = 32;
    private static final int MAX_DIGITS = 10;

    private RtfTokenizer() {
    }

    static List<RtfToken> tokenize(final String rtf) {

        if (!rtf.startsWith("{\\rtf")) {
            throw new RtfFormatException("The document is not RTF: it does not start with {\\rtf.");
        }

        final List<RtfToken> tokens = new ArrayList<>();
        final int length = rtf.length();
        int depth = 0;
        int i = 0;

        while (i < length) {

            final char c = rtf.charAt(i);

            if (c == '{') {
                if (++depth > MAX_DEPTH) {
                    throw new RtfFormatException("The RTF document nests groups more than " + MAX_DEPTH + " deep.");
                }
                tokens.add(RtfToken.of(RtfToken.Kind.GROUP_START, "{"));
                i++;
            } else if (c == '}') {
                depth--;
                tokens.add(RtfToken.of(RtfToken.Kind.GROUP_END, "}"));
                i++;
                if (depth == 0) {
                    // The document ends at its closing brace; anything after it is kept, not read.
                    if (i < length) {
                        tokens.add(RtfToken.of(RtfToken.Kind.TRAILER, rtf.substring(i)));
                    }
                    return tokens;
                }
            } else if (c == '\\') {
                i = control(rtf, i, tokens);
            } else if (c == '\r' || c == '\n') {
                int j = i;
                while (j < length && (rtf.charAt(j) == '\r' || rtf.charAt(j) == '\n')) {
                    j++;
                }
                tokens.add(RtfToken.of(RtfToken.Kind.NEWLINE, rtf.substring(i, j)));
                i = j;
            } else {
                int j = i;
                while (j < length) {
                    final char d = rtf.charAt(j);
                    if (d == '{' || d == '}' || d == '\\' || d == '\r' || d == '\n') {
                        break;
                    }
                    j++;
                }
                tokens.add(RtfToken.of(RtfToken.Kind.TEXT, rtf.substring(i, j)));
                i = j;
            }

        }

        throw new RtfFormatException("The RTF document ends before its groups are closed.");

    }

    // A control word, control symbol, or hex escape starting at the backslash at i. Returns where it ends.
    private static int control(final String rtf, final int i, final List<RtfToken> tokens) {

        final int length = rtf.length();
        if (i + 1 >= length) {
            throw new RtfFormatException("The RTF document ends with a backslash.");
        }

        final char next = rtf.charAt(i + 1);

        if (next == '\'') {
            if (i + 3 >= length || hex(rtf.charAt(i + 2)) < 0 || hex(rtf.charAt(i + 3)) < 0) {
                throw new RtfFormatException("The RTF document has a malformed hex escape.");
            }
            tokens.add(RtfToken.of(RtfToken.Kind.HEX, rtf.substring(i, i + 4)));
            return i + 4;
        }

        if (!isLetter(next)) {
            tokens.add(RtfToken.of(RtfToken.Kind.SYMBOL, rtf.substring(i, i + 2)));
            return i + 2;
        }

        int j = i + 1;
        while (j < length && isLetter(rtf.charAt(j))) {
            if (j - i > MAX_WORD) {
                throw new RtfFormatException("The RTF document has a control word longer than " + MAX_WORD + " letters.");
            }
            j++;
        }
        final String word = rtf.substring(i + 1, j);

        Integer parameter = null;
        int k = j;
        if (k < length && (rtf.charAt(k) == '-' || isDigit(rtf.charAt(k)))) {
            final int start = k;
            k++;
            while (k < length && isDigit(rtf.charAt(k))) {
                if (k - start >= MAX_DIGITS) {
                    throw new RtfFormatException("The RTF document has a control word parameter longer than "
                            + MAX_DIGITS + " digits.");
                }
                k++;
            }
            final String digits = rtf.substring(start, k);
            if (digits.equals("-")) {
                k = start;
            } else {
                final long value = Long.parseLong(digits);
                if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                    throw new RtfFormatException("The RTF document has a control word parameter out of range.");
                }
                parameter = (int) value;
            }
        }
        if (k < length && rtf.charAt(k) == ' ') {
            k++;
        }
        tokens.add(new RtfToken(RtfToken.Kind.WORD, rtf.substring(i, k), word, parameter));

        if ("bin".equals(word) && parameter != null && parameter > 0) {
            if (parameter > length - k) {
                throw new RtfFormatException("The RTF document declares more binary data than it has.");
            }
            tokens.add(RtfToken.of(RtfToken.Kind.BINARY, rtf.substring(k, k + parameter)));
            k += parameter;
        }

        return k;

    }

    private static boolean isLetter(final char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isDigit(final char c) {
        return c >= '0' && c <= '9';
    }

    private static int hex(final char c) {
        return Character.digit(c, 16);
    }

}
