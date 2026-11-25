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

import java.nio.charset.StandardCharsets;

/**
 * Reads the visible text of an RTF document's body: its paragraphs, tables, footnotes, and endnotes, without
 * headers and footers, comments, field instructions, hidden text, tracked deletions, or pictures and objects.
 * For RTF that encapsulates HTML, as Outlook writes, the HTML-only parts are left out. Formatting is discarded.
 */
public final class RtfTextExtractor {

    private RtfTextExtractor() {
    }

    /**
     * The visible text of an RTF document.
     * @param rtf The RTF document.
     * @return The text, with a line for each paragraph and a tab between the cells of a table row.
     * @throws RtfFormatException when the document is malformed or exceeds a limit.
     */
    public static String extract(final byte[] rtf) {
        return extract(new String(rtf, StandardCharsets.ISO_8859_1));
    }

    /**
     * The visible text of an RTF document given as text, one character per byte.
     * @param rtf The RTF document.
     * @return The text, with a line for each paragraph and a tab between the cells of a table row.
     * @throws RtfFormatException when the document is malformed or exceeds a limit.
     */
    public static String extract(final String rtf) {
        final RtfAnalysis analysis = new RtfAnalysis(RtfTokenizer.tokenize(rtf));
        final StringBuilder text = new StringBuilder();
        for (final RtfAnalysis.Target target : analysis.targets) {
            if (target.kind != RtfAnalysis.Kind.BODY && target.kind != RtfAnalysis.Kind.FOOTNOTE) {
                continue;
            }
            final StringBuilder paragraph = new StringBuilder();
            for (final RtfAnalysis.Unit unit : target.units) {
                if (!unit.hidden && !unit.htmlOnly) {
                    paragraph.append(unit.text);
                }
            }
            if (paragraph.isEmpty()) {
                continue;
            }
            text.append(paragraph).append("cell".equals(target.ender) ? "\t" : "\n");
        }
        return text.toString().strip();
    }

}
