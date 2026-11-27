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

import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An RTF document's tokens read into text: the units of text in each paragraph, cell, note, header, comment, and
 * field instruction, each mapped back to the tokens (or the characters of a text token) it was read from, and the
 * groups and their destinations. Text is decoded with the document's code page and its fonts' character sets,
 * {@code \'hh} escapes, and Unicode control words with their fallback characters.
 */
final class RtfAnalysis {

    /** What a run of text is. */
    enum Kind { BODY, FOOTNOTE, HEADER_FOOTER, ANNOTATION, FIELD_INSTRUCTION, HTML_TAG, SHAPE_TEXT, DELETED }

    /** A place in the document: a whole token ({@code offset} -1), or one character of a text token. */
    record Ref(int token, int offset) {
    }

    /** One character of text, or a cluster of them read from the same bytes, and where it was read from. */
    static final class Unit {
        final String text;
        final List<Ref> refs = new ArrayList<>();
        final boolean hidden;
        final boolean deleted;
        final boolean htmlOnly;

        Unit(final String text, final boolean hidden, final boolean deleted, final boolean htmlOnly) {
            this.text = text;
            this.hidden = hidden;
            this.deleted = deleted;
            this.htmlOnly = htmlOnly;
        }
    }

    /** A paragraph, cell, or other run of text detected on its own. */
    static final class Target {
        final Kind kind;
        final List<Unit> units = new ArrayList<>();
        // What ended it: "cell" for a table cell, otherwise a paragraph or the end of its group.
        String ender = "par";

        Target(final Kind kind) {
            this.kind = kind;
        }

        String text() {
            final StringBuilder sb = new StringBuilder();
            for (final Unit unit : units) {
                sb.append(unit.text);
            }
            return sb.toString();
        }
    }

    // Destinations whose text is detected on its own, by kind.
    private static final Map<String, Kind> TEXT_DESTINATIONS = Map.ofEntries(
            Map.entry("footnote", Kind.FOOTNOTE),
            Map.entry("header", Kind.HEADER_FOOTER), Map.entry("headerl", Kind.HEADER_FOOTER),
            Map.entry("headerr", Kind.HEADER_FOOTER), Map.entry("headerf", Kind.HEADER_FOOTER),
            Map.entry("footer", Kind.HEADER_FOOTER), Map.entry("footerl", Kind.HEADER_FOOTER),
            Map.entry("footerr", Kind.HEADER_FOOTER), Map.entry("footerf", Kind.HEADER_FOOTER),
            Map.entry("annotation", Kind.ANNOTATION),
            Map.entry("fldinst", Kind.FIELD_INSTRUCTION),
            Map.entry("htmltag", Kind.HTML_TAG),
            Map.entry("shptxt", Kind.SHAPE_TEXT));

    // Destinations with no text to read: tables, properties, pictures, objects, and bookkeeping.
    static final Set<String> SKIPPED = Set.of("fonttbl", "colortbl", "stylesheet", "info", "pict", "object",
            "objdata", "listtable", "listoverridetable", "rsidtbl", "generator", "xmlnstbl", "themedata",
            "colorschememapping", "latentstyles", "datastore", "filetbl", "revtbl", "pgdsctbl", "mmathPr", "listtext",
            "pntext", "pntxta", "pntxtb", "bkmkstart", "bkmkend", "atnid", "atnauthor", "atrfstart", "atrfend",
            "atndate", "atnparent", "atntime", "nonshppict", "sp", "sn", "sv", "userprops", "docvar", "oldcprops",
            "oldpprops", "oldsprops", "oldtprops", "background", "wgrffmtfilter", "falt", "panose", "fname");

    // Control words that write a character.
    private static final Map<String, String> CHARACTERS = Map.ofEntries(
            Map.entry("line", "\n"), Map.entry("tab", "\t"),
            Map.entry("emdash", "—"), Map.entry("endash", "–"), Map.entry("emspace", " "),
            Map.entry("enspace", " "), Map.entry("qmspace", " "), Map.entry("bullet", "•"),
            Map.entry("lquote", "‘"), Map.entry("rquote", "’"), Map.entry("ldblquote", "“"),
            Map.entry("rdblquote", "”"));

    // The code page of a font character set.
    private static final Map<Integer, Integer> CHARSET_CODE_PAGES = Map.ofEntries(
            Map.entry(77, 10000), Map.entry(128, 932), Map.entry(129, 949), Map.entry(134, 936), Map.entry(136, 950),
            Map.entry(161, 1253), Map.entry(162, 1254), Map.entry(163, 1258), Map.entry(177, 1255),
            Map.entry(178, 1256), Map.entry(186, 1257), Map.entry(204, 1251), Map.entry(222, 874),
            Map.entry(238, 1250));

    final List<RtfToken> tokens;
    final List<Target> targets = new ArrayList<>();
    // For each group start, its end, and its destination (or null).
    final int[] groupEnd;
    final String[] destination;
    // The hidden and deleted state each token was read in.
    final boolean[] tokenHidden;
    final boolean[] tokenDeleted;

    private static final class State {
        boolean skip;
        boolean fontTable;
        boolean hidden;
        boolean deleted;
        boolean htmlOnly;
        int unicodeSkip = 1;
        Charset charset;
        Context context;

        State copy() {
            final State state = new State();
            state.skip = skip;
            state.fontTable = fontTable;
            state.hidden = hidden;
            state.deleted = deleted;
            state.htmlOnly = htmlOnly;
            state.unicodeSkip = unicodeSkip;
            state.charset = charset;
            state.context = context;
            return state;
        }
    }

    // A destination's current paragraph and current run of deleted text.
    private final class Context {
        final Kind kind;
        Target paragraph;
        Target deleted;

        Context(final Kind kind) {
            this.kind = kind;
        }

        void add(final Unit unit) {
            if (unit.deleted) {
                if (deleted == null) {
                    deleted = new Target(Kind.DELETED);
                    targets.add(deleted);
                }
                deleted.units.add(unit);
                return;
            }
            deleted = null;
            if (paragraph == null) {
                paragraph = new Target(kind);
                targets.add(paragraph);
            }
            paragraph.units.add(unit);
        }

        // The last run of text ended, so a row's end marks the cell the row's last \cell already ended.
        Target ended;

        void end(final String ender) {
            if (paragraph != null) {
                paragraph.ender = ender;
                ended = paragraph;
            } else if (ended != null && "row".equals(ender)) {
                ended.ender = ender;
            }
            paragraph = null;
            deleted = null;
        }
    }

    RtfAnalysis(final List<RtfToken> tokens) {

        this.tokens = tokens;
        final int size = tokens.size();
        this.groupEnd = new int[size];
        this.destination = new String[size];
        this.tokenHidden = new boolean[size];
        this.tokenDeleted = new boolean[size];

        final Deque<Integer> open = new ArrayDeque<>();
        for (int t = 0; t < size; t++) {
            final RtfToken token = tokens.get(t);
            if (token.is(RtfToken.Kind.GROUP_START)) {
                open.push(t);
                destination[t] = destinationOf(t);
            } else if (token.is(RtfToken.Kind.GROUP_END) && !open.isEmpty()) {
                groupEnd[open.pop()] = t;
            }
        }

        read();

    }

    // The destination word of the group starting at t: the control word right after the brace, or after \*.
    private String destinationOf(final int t) {
        int i = t + 1;
        if (i < tokens.size() && tokens.get(i).is(RtfToken.Kind.SYMBOL) && tokens.get(i).raw().equals("\\*")) {
            i++;
        }
        return i < tokens.size() && tokens.get(i).is(RtfToken.Kind.WORD) ? tokens.get(i).word() : null;
    }

    private boolean ignorable(final int t) {
        return t + 1 < tokens.size() && tokens.get(t + 1).raw().equals("\\*");
    }

    private void read() {

        final Map<Integer, Charset> fonts = new HashMap<>();
        Charset documentCharset = codePage(1252);
        int fontTableFont = -1;

        final Deque<State> stack = new ArrayDeque<>();
        State state = new State();
        state.charset = documentCharset;
        state.context = new Context(Kind.BODY);

        Unit pending = null;
        int pendingSkip = 0;

        for (int t = 0; t < tokens.size(); t++) {

            final RtfToken token = tokens.get(t);
            tokenHidden[t] = state.hidden;
            tokenDeleted[t] = state.deleted;

            switch (token.kind()) {

                case GROUP_START -> {
                    pendingSkip = 0;
                    stack.push(state);
                    state = state.copy();
                    final String dest = destination[t];
                    if (dest != null && !state.skip) {
                        if ("fonttbl".equals(dest)) {
                            state.skip = true;
                            state.fontTable = true;
                        } else if (TEXT_DESTINATIONS.containsKey(dest)) {
                            state.context = new Context(TEXT_DESTINATIONS.get(dest));
                        } else if (SKIPPED.contains(dest) || ignorable(t)) {
                            state.skip = true;
                        }
                    }
                }

                case GROUP_END -> {
                    pendingSkip = 0;
                    if (state.context != (stack.isEmpty() ? null : stack.peek().context)) {
                        state.context.end("par");
                    }
                    state = stack.isEmpty() ? state : stack.pop();
                }

                case WORD -> {
                    final String word = token.word();
                    final Integer parameter = token.parameter();
                    final boolean on = parameter == null || parameter != 0;
                    if (state.fontTable) {
                        if ("f".equals(word) && parameter != null) {
                            fontTableFont = parameter;
                        } else if ("fcharset".equals(word) && parameter != null && fontTableFont >= 0) {
                            final Integer codePage = CHARSET_CODE_PAGES.get(parameter);
                            fonts.putIfAbsent(fontTableFont, codePage != null ? codePage(codePage) : null);
                        } else if ("cpg".equals(word) && parameter != null && fontTableFont >= 0) {
                            fonts.put(fontTableFont, codePage(parameter));
                        }
                        continue;
                    }
                    switch (word) {
                        case "ansicpg" -> {
                            documentCharset = codePage(parameter == null ? 1252 : parameter);
                            state.charset = documentCharset;
                        }
                        case "f" -> {
                            final Charset font = parameter == null ? null : fonts.get(parameter);
                            state.charset = font != null ? font : documentCharset;
                        }
                        case "uc" -> state.unicodeSkip = parameter == null ? 1 : Math.max(0, parameter);
                        case "v" -> state.hidden = on;
                        case "deleted" -> state.deleted = on;
                        case "htmlrtf" -> state.htmlOnly = on;
                        case "plain" -> {
                            state.hidden = false;
                            state.deleted = false;
                        }
                        case "u" -> {
                            if (!state.skip && parameter != null) {
                                int code = parameter;
                                if (code < 0) {
                                    code += 65536;
                                }
                                pending = unit(String.valueOf((char) code), state);
                                pending.refs.add(new Ref(t, -1));
                                state.context.add(pending);
                            }
                            pendingSkip = state.unicodeSkip;
                        }
                        case "par", "sect", "page" -> {
                            if (!state.skip) {
                                state.context.end("par");
                            }
                        }
                        case "cell", "nestcell" -> {
                            if (!state.skip) {
                                state.context.end("cell");
                            }
                        }
                        case "row", "nestrow" -> {
                            if (!state.skip) {
                                state.context.end("row");
                            }
                        }
                        default -> {
                            final String character = CHARACTERS.get(word);
                            if (character != null && !state.skip) {
                                final Unit unit = unit(character, state);
                                unit.refs.add(new Ref(t, -1));
                                state.context.add(unit);
                            }
                        }
                    }
                }

                case SYMBOL -> {
                    if (state.skip) {
                        continue;
                    }
                    final char symbol = token.raw().charAt(1);
                    final String character = switch (symbol) {
                        case '\\', '{', '}' -> String.valueOf(symbol);
                        case '~' -> " ";
                        case '_' -> "-";
                        default -> null;
                    };
                    if (symbol == '\n' || symbol == '\r') {
                        state.context.end("par");
                    } else if (character != null) {
                        if (pendingSkip > 0 && pending != null) {
                            pending.refs.add(new Ref(t, -1));
                            pendingSkip--;
                        } else {
                            final Unit unit = unit(character, state);
                            unit.refs.add(new Ref(t, -1));
                            state.context.add(unit);
                        }
                    }
                }

                case HEX -> {
                    if (state.skip) {
                        continue;
                    }
                    if (pendingSkip > 0 && pending != null) {
                        pending.refs.add(new Ref(t, -1));
                        pendingSkip--;
                        continue;
                    }
                    // A run of hex escapes is decoded together, since a character can take more than one byte.
                    int end = t;
                    while (end + 1 < tokens.size() && tokens.get(end + 1).is(RtfToken.Kind.HEX)) {
                        end++;
                    }
                    final byte[] bytes = new byte[end - t + 1];
                    for (int i = t; i <= end; i++) {
                        bytes[i - t] = (byte) tokens.get(i).hexByte();
                        tokenHidden[i] = state.hidden;
                        tokenDeleted[i] = state.deleted;
                    }
                    final String decoded = new String(bytes, state.charset);
                    if (decoded.length() == bytes.length) {
                        for (int i = 0; i < bytes.length; i++) {
                            final Unit unit = unit(String.valueOf(decoded.charAt(i)), state);
                            unit.refs.add(new Ref(t + i, -1));
                            state.context.add(unit);
                        }
                    } else {
                        // Multi-byte characters: each is mapped to the whole run.
                        for (int i = 0; i < decoded.length(); i++) {
                            final Unit unit = unit(String.valueOf(decoded.charAt(i)), state);
                            for (int j = t; j <= end; j++) {
                                unit.refs.add(new Ref(j, -1));
                            }
                            state.context.add(unit);
                        }
                    }
                    t = end;
                }

                case TEXT -> {
                    if (state.skip) {
                        continue;
                    }
                    final String raw = token.raw();
                    for (int k = 0; k < raw.length(); k++) {
                        if (pendingSkip > 0 && pending != null) {
                            pending.refs.add(new Ref(t, k));
                            pendingSkip--;
                            continue;
                        }
                        final char c = raw.charAt(k);
                        // A byte above 0x7F is in the code page; text given already decoded keeps its characters.
                        final String character = c < 0x80 || c > 0xFF ? String.valueOf(c)
                                : new String(new byte[]{(byte) c}, state.charset);
                        final Unit unit = unit(character, state);
                        unit.refs.add(new Ref(t, k));
                        state.context.add(unit);
                    }
                }

                default -> {
                    // Line breaks, binary data, and the trailer have no text.
                }

            }

        }

    }

    private static Unit unit(final String text, final State state) {
        return new Unit(text, state.hidden, state.deleted, state.htmlOnly);
    }

    private static Charset codePage(final int codePage) {
        try {
            return Charset.forName(codePage == 10000 ? "x-MacRoman" : "windows-" + codePage);
        } catch (final RuntimeException e) {
            try {
                return Charset.forName("x-windows-" + codePage);
            } catch (final RuntimeException e2) {
                return Charset.forName("windows-1252");
            }
        }
    }

    /** The groups with this destination, as {start, end} pairs. */
    List<int[]> groups(final String dest) {
        final List<int[]> out = new ArrayList<>();
        for (int t = 0; t < tokens.size(); t++) {
            if (dest.equals(destination[t]) && tokens.get(t).is(RtfToken.Kind.GROUP_START)) {
                out.add(new int[]{t, groupEnd[t]});
            }
        }
        return out;
    }

    /** The index of the destination word of the group starting at t. */
    int destinationWord(final int t) {
        return ignorable(t) ? t + 2 : t + 1;
    }

}
