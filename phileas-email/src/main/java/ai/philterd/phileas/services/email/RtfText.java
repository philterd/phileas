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
package ai.philterd.phileas.services.email;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

/**
 * Reads the text of an RTF document, such as the body of an Outlook message that has only an RTF body, so the body
 * can be redacted as plain text rather than dropped. Formatting is discarded. For RTF that encapsulates HTML, as
 * Outlook writes, the HTML-only parts are skipped.
 */
final class RtfText {

    // Destinations whose content is not body text.
    private static final Set<String> SKIPPED = Set.of("fonttbl", "colortbl", "stylesheet", "info", "pict", "object",
            "listtable", "listoverridetable", "rsidtbl", "generator", "xmlnstbl", "themedata", "colorschememapping",
            "latentstyles", "datastore", "filetbl", "revtbl", "header", "footer", "headerl", "headerr", "headerf",
            "footerl", "footerr", "footerf", "pgdsctbl", "mmathPr", "fldinst", "listtext", "pntext");

    private RtfText() {
    }

    /** Group state that is restored when a group ends. */
    private static final class State {
        boolean skip;
        boolean htmlOnly;
        int unicodeSkip = 1;

        State copy() {
            final State state = new State();
            state.skip = skip;
            state.htmlOnly = htmlOnly;
            state.unicodeSkip = unicodeSkip;
            return state;
        }
    }

    /**
     * The text of an RTF document.
     * @param rtf The RTF.
     * @return The text, with paragraphs on their own lines.
     */
    static String extract(final String rtf) {

        final StringBuilder text = new StringBuilder();
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final Deque<State> stack = new ArrayDeque<>();
        State state = new State();
        Charset charset = charset(1252);
        int pendingSkip = 0;

        int i = 0;
        final int length = rtf.length();

        while (i < length) {

            final char c = rtf.charAt(i);

            if (c == '{') {
                flush(bytes, charset, text);
                stack.push(state);
                state = state.copy();
                i++;
                // An ignorable destination: {\* ...}
                if (i + 1 < length && rtf.charAt(i) == '\\' && rtf.charAt(i + 1) == '*') {
                    state.skip = true;
                    i += 2;
                }
                continue;
            }

            if (c == '}') {
                flush(bytes, charset, text);
                state = stack.isEmpty() ? new State() : stack.pop();
                i++;
                continue;
            }

            if (c == '\\' && i + 1 < length) {

                final char next = rtf.charAt(i + 1);

                if (next == '\'' && i + 3 < length) {
                    if (pendingSkip > 0) {
                        pendingSkip--;
                    } else if (visible(state)) {
                        try {
                            bytes.write(Integer.parseInt(rtf.substring(i + 2, i + 4), 16));
                        } catch (final NumberFormatException e) {
                            // A malformed escape is left out.
                        }
                    }
                    i += 4;
                    continue;
                }

                if (!Character.isLetter(next)) {
                    // A control symbol: an escaped character, or a symbol such as a non-breaking space.
                    if (visible(state)) {
                        switch (next) {
                            case '\\', '{', '}' -> append(bytes, charset, text, String.valueOf(next));
                            case '~' -> append(bytes, charset, text, " ");
                            case '_' -> append(bytes, charset, text, "-");
                            case '-' -> { }
                            case '\n', '\r' -> append(bytes, charset, text, "\n");
                            default -> { }
                        }
                    }
                    i += 2;
                    continue;
                }

                // A control word with an optional numeric parameter and an optional space delimiter.
                int j = i + 1;
                while (j < length && Character.isLetter(rtf.charAt(j))) {
                    j++;
                }
                final String word = rtf.substring(i + 1, j);
                int k = j;
                if (k < length && (rtf.charAt(k) == '-' || Character.isDigit(rtf.charAt(k)))) {
                    k++;
                    while (k < length && Character.isDigit(rtf.charAt(k))) {
                        k++;
                    }
                }
                final String parameter = rtf.substring(j, k);
                if (k < length && rtf.charAt(k) == ' ') {
                    k++;
                }
                i = k;

                switch (word) {
                    case "ansicpg" -> {
                        flush(bytes, charset, text);
                        charset = charset(number(parameter, 1252));
                    }
                    case "uc" -> state.unicodeSkip = number(parameter, 1);
                    case "u" -> {
                        if (visible(state)) {
                            int code = number(parameter, '?');
                            if (code < 0) {
                                code += 65536;
                            }
                            append(bytes, charset, text, String.valueOf((char) code));
                        }
                        pendingSkip = state.unicodeSkip;
                    }
                    case "par", "line", "row", "sect", "page" -> append(bytes, charset, text, visible(state) ? "\n" : "");
                    case "tab", "cell" -> append(bytes, charset, text, visible(state) ? "\t" : "");
                    case "htmlrtf" -> state.htmlOnly = !"0".equals(parameter);
                    default -> {
                        if (SKIPPED.contains(word)) {
                            state.skip = true;
                        }
                    }
                }
                continue;

            }

            if (c == '\r' || c == '\n') {
                i++;
                continue;
            }

            if (pendingSkip > 0) {
                pendingSkip--;
            } else if (visible(state)) {
                append(bytes, charset, text, String.valueOf(c));
            }
            i++;

        }

        flush(bytes, charset, text);
        return text.toString().strip();

    }

    private static boolean visible(final State state) {
        return !state.skip && !state.htmlOnly;
    }

    private static void append(final ByteArrayOutputStream bytes, final Charset charset, final StringBuilder text,
                               final String value) {
        flush(bytes, charset, text);
        text.append(value);
    }

    // Hex escapes are bytes in the document's code page; a run of them is decoded together.
    private static void flush(final ByteArrayOutputStream bytes, final Charset charset, final StringBuilder text) {
        if (bytes.size() > 0) {
            text.append(new String(bytes.toByteArray(), charset));
            bytes.reset();
        }
    }

    private static int number(final String value, final int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            return fallback;
        }
    }

    private static Charset charset(final int codePage) {
        try {
            return Charset.forName("windows-" + codePage);
        } catch (final RuntimeException e) {
            return Charset.forName("windows-1252");
        }
    }

}
