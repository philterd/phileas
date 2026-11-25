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

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Changes to an RTF document's tokens: tokens and characters of text tokens removed, and text inserted before a
 * token or a character. Writing applies them; every other byte is written as it was read.
 */
final class RtfEdits {

    private final List<RtfToken> tokens;
    private final boolean[] removed;
    private final Map<Integer, BitSet> removedCharacters = new HashMap<>();
    private final Map<RtfAnalysis.Ref, StringBuilder> inserted = new HashMap<>();

    RtfEdits(final List<RtfToken> tokens) {
        this.tokens = tokens;
        this.removed = new boolean[tokens.size()];
    }

    void remove(final RtfAnalysis.Ref ref) {
        if (ref.offset() < 0) {
            removed[ref.token()] = true;
        } else {
            removedCharacters.computeIfAbsent(ref.token(), k -> new BitSet()).set(ref.offset());
        }
    }

    void removeToken(final int token) {
        removed[token] = true;
    }

    void removeTokens(final int from, final int to) {
        for (int t = from; t <= to; t++) {
            removed[t] = true;
        }
    }

    void remove(final RtfAnalysis.Unit unit) {
        for (final RtfAnalysis.Ref ref : unit.refs) {
            remove(ref);
        }
    }

    /** Inserts RTF before the place: a whole token, or a character of a text token. */
    void insertRtf(final RtfAnalysis.Ref ref, final String rtf) {
        inserted.computeIfAbsent(ref, k -> new StringBuilder()).append(rtf);
    }

    /** Inserts text, escaped for RTF, before the place. */
    void insertText(final RtfAnalysis.Ref ref, final String text) {
        insertRtf(ref, escape(text));
    }

    String write() {
        final StringBuilder out = new StringBuilder();
        // Whether the last thing written is a control word without a delimiter, so a letter, digit, hyphen, or
        // space written next would join it; and whether something was removed or inserted since, so what follows
        // is newly next to it. Text that was already next to the control word is written as it was.
        boolean open = false;
        boolean gap = false;
        for (int t = 0; t < tokens.size(); t++) {
            final RtfToken token = tokens.get(t);
            final StringBuilder before = inserted.get(new RtfAnalysis.Ref(t, -1));
            if (before != null) {
                open = append(out, before, open, true);
                gap = true;
            }
            if (removed[t]) {
                gap = true;
                continue;
            }
            if (token.is(RtfToken.Kind.TEXT)) {
                final BitSet gone = removedCharacters.get(t);
                final String raw = token.raw();
                for (int k = 0; k < raw.length(); k++) {
                    final StringBuilder at = inserted.get(new RtfAnalysis.Ref(t, k));
                    if (at != null) {
                        open = append(out, at, open, true);
                        gap = true;
                    }
                    if (gone != null && gone.get(k)) {
                        gap = true;
                    } else {
                        open = append(out, String.valueOf(raw.charAt(k)), open, gap);
                        gap = false;
                    }
                }
            } else {
                open = append(out, token.raw(), open, gap);
                gap = false;
                if (token.is(RtfToken.Kind.WORD)) {
                    open = token.needsDelimiter();
                }
            }
        }
        return out.toString();
    }

    private static boolean append(final StringBuilder out, final CharSequence text, final boolean open, final boolean fresh) {
        if (text == null || text.length() == 0) {
            return open;
        }
        final char first = text.charAt(0);
        if (open && fresh && (Character.isLetterOrDigit(first) || first == '-' || first == ' ')) {
            out.append(' ');
        }
        out.append(text);
        return false;
    }

    /**
     * Text written as RTF: a backslash and braces escaped, a line break as {@code \line}, a tab as {@code \tab},
     * and any character outside printable ASCII as a Unicode control word in a group of its own, with {@code ?} for readers
     * without Unicode.
     */
    static String escape(final String text) {
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c == '\\' || c == '{' || c == '}') {
                out.append('\\').append(c);
            } else if (c == '\n') {
                out.append("\\line ");
            } else if (c == '\t') {
                out.append("\\tab ");
            } else if (c >= 0x20 && c < 0x7F) {
                out.append(c);
            } else {
                out.append("{\\uc1\\u").append((int) (short) c).append("?}");
            }
        }
        return out.toString();
    }

}
