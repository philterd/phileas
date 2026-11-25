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

import java.util.Map;

/**
 * The visible text of HTML, with where each visible character came from in the markup, so a value found in the
 * text can be replaced in the markup. Tags contribute no text; a character reference maps its decoded characters
 * to the whole reference. A value split by tags or written with character references, which a scan of the markup
 * misses, is found this way.
 */
final class HtmlText {

    private static final Map<String, String> NAMED = Map.ofEntries(
            Map.entry("amp", "&"), Map.entry("lt", "<"), Map.entry("gt", ">"), Map.entry("quot", "\""),
            Map.entry("apos", "'"), Map.entry("nbsp", "\u00a0"), Map.entry("commat", "@"),
            Map.entry("period", "."), Map.entry("hyphen", "-"), Map.entry("dash", "-"), Map.entry("lpar", "("),
            Map.entry("rpar", ")"), Map.entry("plus", "+"), Map.entry("colon", ":"), Map.entry("sol", "/"),
            Map.entry("num", "#"), Map.entry("lowbar", "_"), Map.entry("ndash", "\u2013"),
            Map.entry("mdash", "\u2014"), Map.entry("shy", "\u00ad"));

    private final String text;
    private final int[] rawStart;
    private final int[] rawEnd;

    private HtmlText(final String text, final int[] rawStart, final int[] rawEnd) {
        this.text = text;
        this.rawStart = rawStart;
        this.rawEnd = rawEnd;
    }

    /**
     * Reads the visible text of HTML.
     * @param html The markup.
     * @return The {@link HtmlText}.
     */
    static HtmlText of(final String html) {

        final StringBuilder visible = new StringBuilder(html.length());
        final int[] starts = new int[html.length()];
        final int[] ends = new int[html.length()];
        int count = 0;

        int i = 0;
        while (i < html.length()) {

            final char c = html.charAt(i);

            if (c == '<') {
                final int close = html.indexOf('>', i + 1);
                i = close < 0 ? html.length() : close + 1;
                continue;
            }

            if (c == '&') {
                final int semicolon = html.indexOf(';', i + 1);
                if (semicolon > i && semicolon - i <= 12) {
                    final String decoded = decode(html.substring(i + 1, semicolon));
                    if (decoded != null) {
                        for (int k = 0; k < decoded.length(); k++) {
                            visible.append(decoded.charAt(k));
                            starts[count] = i;
                            ends[count] = semicolon + 1;
                            count++;
                        }
                        i = semicolon + 1;
                        continue;
                    }
                }
            }

            visible.append(c);
            starts[count] = i;
            ends[count] = i + 1;
            count++;
            i++;

        }

        return new HtmlText(visible.toString(), java.util.Arrays.copyOf(starts, count), java.util.Arrays.copyOf(ends, count));

    }

    // The characters a reference stands for, or null when it is not one.
    private static String decode(final String reference) {
        try {
            if (reference.startsWith("#x") || reference.startsWith("#X")) {
                return Character.toString(Integer.parseInt(reference.substring(2), 16));
            }
            if (reference.startsWith("#")) {
                return Character.toString(Integer.parseInt(reference.substring(1)));
            }
        } catch (final IllegalArgumentException e) {
            return null;
        }
        return NAMED.get(reference);
    }

    String text() {
        return text;
    }

    /**
     * The start in the markup of a range of the visible text.
     * @param start The start of the range in the visible text.
     * @return The start in the markup.
     */
    int rawStart(final int start) {
        return rawStart[start];
    }

    /**
     * The end in the markup of a range of the visible text.
     * @param end The end of the range in the visible text, exclusive.
     * @return The end in the markup, exclusive.
     */
    int rawEnd(final int end) {
        return rawEnd[end - 1];
    }

}
