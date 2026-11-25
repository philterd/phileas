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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static ai.philterd.phileas.services.rtf.RtfDocs.doc;

public class RtfTextExtractorTest {

    @Test
    public void readsParagraphsTablesAndNotes() {
        final String rtf = doc("First {\\b bold} paragraph.\\par Second\\line line.\\par "
                + "\\trowd\\cellx1000\\cellx2000\\intbl Name\\cell Jane\\cell\\row\\pard "
                + "Note here{\\footnote\\pard Footnote text.}.\\par ");
        Assertions.assertEquals("First bold paragraph.\nSecond\nline.\nName\tJane\nNote here.\nFootnote text.",
                RtfTextExtractor.extract(rtf));
    }

    @Test
    public void decodesEscapesUnicodeAndFontCharacterSets() {
        final String rtf = "{\\rtf1\\ansi\\ansicpg1252{\\fonttbl{\\f0 Arial;}{\\f1\\fcharset204 Arial Cyr;}}"
                + "\\f0 Caf\\'e9 \\u8217?s {\\f1 \\'cf\\'f0\\'e8}\\par}";
        Assertions.assertEquals("Café ’s При", RtfTextExtractor.extract(rtf));
    }

    @Test
    public void leavesOutWhatIsNotVisibleBodyText() {
        final String rtf = "{\\rtf1\\ansi{\\fonttbl{\\f0 Arial;}}{\\info{\\author Example Author}}"
                + "{\\header\\pard Header text\\par}\\pard Visible {\\v hidden} "
                + "{\\deleted gone}{\\field{\\*\\fldinst HYPERLINK \"https://example.com\"}{\\fldrslt link}} "
                + "{\\*\\annotation comment}{\\pict\\wmetafile8 0102}end\\par}";
        Assertions.assertEquals("Visible  link end", RtfTextExtractor.extract(rtf));
    }

    @Test
    public void leavesOutTheHtmlOnlyPartsOfEncapsulatedHtml() {
        final String rtf = "{\\rtf1\\ansi\\fromhtml1{\\fonttbl{\\f0 Arial;}}{\\*\\htmltag64 <p>}\\htmlrtf {\\f0 rtf only}"
                + "\\htmlrtf0 Body text{\\*\\htmltag72 </p>}\\par}";
        Assertions.assertEquals("Body text", RtfTextExtractor.extract(rtf));
    }

    @Test
    public void refusesMalformedInput() {
        Assertions.assertThrows(RtfFormatException.class, () -> RtfTextExtractor.extract("not rtf"));
    }

}
