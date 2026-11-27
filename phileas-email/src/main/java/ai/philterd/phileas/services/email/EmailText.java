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
package ai.philterd.phileas.services.email;

import ai.philterd.phileas.model.filtering.ReplacedRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A redacted email body as plain text for text output, with where the redaction's replacements are in it.
 */
public final class EmailText {

    // Elements that start or end a line of text.
    private static final Set<String> BLOCKS = Set.of("br", "p", "div", "li", "tr", "h1", "h2", "h3", "h4", "h5", "h6",
            "table", "ul", "ol", "blockquote", "hr", "pre", "section", "article", "header", "footer", "title");

    // Elements whose content is not text.
    private static final Set<String> HIDDEN = Set.of("script", "style", "head");

    /**
     * Text and where its replacements are.
     * @param text The text.
     * @param replacedRanges Where the replacements are.
     */
    public record Rendered(String text, List<ReplacedRange> replacedRanges) {
    }

    private EmailText() {
    }

    /**
     * A plain body with its line endings as line breaks and its ends trimmed.
     * @param text The redacted body.
     * @param replacedRanges Where its replacements are.
     * @return The text.
     */
    public static Rendered plain(final String text, final List<ReplacedRange> replacedRanges) {
        final Builder out = new Builder();
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c == '\r') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    continue;
                }
                out.append('\n', i);
            } else {
                out.append(c, i);
            }
        }
        return out.build(replacedRanges);
    }

    /**
     * The visible text of a redacted HTML body: tags and the content of scripts, styles, and the head left out,
     * character references decoded, runs of white space as one space, and a line break at each block element.
     * @param html The redacted markup.
     * @param replacedRanges Where its replacements are in the markup.
     * @return The text.
     */
    public static Rendered html(final String html, final List<ReplacedRange> replacedRanges) {
        final Builder out = new Builder();
        int i = 0;
        while (i < html.length()) {
            final char c = html.charAt(i);
            if (c == '<') {
                final int close = html.indexOf('>', i + 1);
                final int end = close < 0 ? html.length() : close + 1;
                final String name = tagName(html.substring(i + 1, end));
                if (HIDDEN.contains(name) && i + 1 < html.length() && html.charAt(i + 1) != '/') {
                    final int closing = html.toLowerCase(Locale.ROOT).indexOf("</" + name, end);
                    i = closing < 0 ? html.length() : closing;
                    continue;
                }
                if (BLOCKS.contains(name)) {
                    out.lineBreak();
                } else if ("td".equals(name) || "th".equals(name)) {
                    out.space();
                }
                i = end;
                continue;
            }
            if (c == '&') {
                final int semicolon = html.indexOf(';', i + 1);
                if (semicolon > i && semicolon - i <= 12) {
                    final String decoded = HtmlText.decodeReference(html.substring(i + 1, semicolon));
                    if (decoded != null) {
                        for (int k = 0; k < decoded.length(); k++) {
                            out.visible(decoded.charAt(k), i);
                        }
                        i = semicolon + 1;
                        continue;
                    }
                }
            }
            out.visible(c, i);
            i++;
        }
        return out.build(replacedRanges);
    }

    // The lower-case name of a tag, from the text after its "<".
    private static String tagName(final String tag) {
        int start = tag.startsWith("/") ? 1 : 0;
        int end = start;
        while (end < tag.length() && Character.isLetterOrDigit(tag.charAt(end))) {
            end++;
        }
        return tag.substring(start, end).toLowerCase(Locale.ROOT);
    }

    // Text built a character at a time, each with the place in the source it came from, or -1 for none.
    private static final class Builder {

        private final StringBuilder text = new StringBuilder();
        private final List<Integer> sources = new ArrayList<>();

        void append(final char c, final int source) {
            text.append(c);
            sources.add(source);
        }

        // HTML text: white space collapses to one space, and none starts a line.
        void visible(final char c, final int source) {
            if (Character.isWhitespace(c) || c == '\u00a0') {
                space();
                return;
            }
            append(c, source);
        }

        void space() {
            if (!text.isEmpty() && text.charAt(text.length() - 1) != ' ' && text.charAt(text.length() - 1) != '\n') {
                append(' ', -1);
            }
        }

        void lineBreak() {
            while (!text.isEmpty() && text.charAt(text.length() - 1) == ' ') {
                text.setLength(text.length() - 1);
                sources.remove(sources.size() - 1);
            }
            if (!text.isEmpty() && text.charAt(text.length() - 1) != '\n') {
                append('\n', -1);
            }
        }

        Rendered build(final List<ReplacedRange> replacedRanges) {
            // Trimmed at both ends.
            int from = 0;
            int to = text.length();
            while (from < to && Character.isWhitespace(text.charAt(from))) {
                from++;
            }
            while (to > from && Character.isWhitespace(text.charAt(to - 1))) {
                to--;
            }
            final List<ReplacedRange> ranges = new ArrayList<>();
            for (final ReplacedRange range : replacedRanges) {
                int start = -1;
                int end = -1;
                for (int k = from; k < to; k++) {
                    final int source = sources.get(k);
                    if (source >= range.start() && source < range.end()) {
                        if (start < 0) {
                            start = k;
                        }
                        end = k + 1;
                    }
                }
                if (start >= 0) {
                    ranges.add(new ReplacedRange(start - from, end - from));
                }
            }
            return new Rendered(text.substring(from, to), ranges);
        }

    }

}
