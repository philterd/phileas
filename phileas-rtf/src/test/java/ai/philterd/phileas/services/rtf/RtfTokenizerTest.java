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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;

public class RtfTokenizerTest {

    private static String join(final List<RtfToken> tokens) {
        final StringBuilder sb = new StringBuilder();
        tokens.forEach(t -> sb.append(t.raw()));
        return sb.toString();
    }

    @Test
    public void joiningTheTokensGivesBackTheDocument() {
        final String rtf = "{\\rtf1\\ansi\\ansicpg1252{\\fonttbl{\\f0 Arial;}}\r\n\\f0\\fs20 Caf\\'e9 {\\b bold}\\u8217?s "
                + "\\~\\-\\{x\\}\\\\ {\\pict\\bin3 \u0001{}}\\par}\r\n\u0000";
        final List<RtfToken> tokens = RtfTokenizer.tokenize(rtf);
        Assertions.assertEquals(rtf, join(tokens));
        Assertions.assertTrue(tokens.stream().anyMatch(t -> t.is(RtfToken.Kind.BINARY) && t.raw().equals("\u0001{}")));
        Assertions.assertTrue(tokens.get(tokens.size() - 1).is(RtfToken.Kind.TRAILER));
    }

    @Test
    public void readsControlWordsWithParametersAndDelimiters() {
        final List<RtfToken> tokens = RtfTokenizer.tokenize("{\\rtf1\\fs-20 x\\b\\'41}");
        Assertions.assertEquals("fs", tokens.get(2).word());
        Assertions.assertEquals(-20, tokens.get(2).parameter());
        Assertions.assertEquals("\\fs-20 ", tokens.get(2).raw());
        Assertions.assertTrue(tokens.get(4).needsDelimiter());
        Assertions.assertEquals(0x41, tokens.get(5).hexByte());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "plain text",
            "{\\rtf1 unclosed",
            "{\\rtf1 {x}",
            "{\\rtf1 \\'4}",
            "{\\rtf1 \\'zz}",
            "{\\rtf1 x\\",
            "{\\rtf1 \\abcdefghijklmnopqrstuvwxyzabcdefghij x}",
            "{\\rtf1 \\fs12345678901 x}",
            "{\\rtf1 \\fs99999999999 x}",
            "{\\rtf1 \\bin100 x}"
    })
    public void refusesMalformedInput(final String rtf) {
        Assertions.assertThrows(RtfFormatException.class, () -> RtfTokenizer.tokenize(rtf));
    }

    @Test
    public void refusesDeepNestingQuickly() {
        final String deep = "{\\rtf1 " + "{".repeat(5_000_000);
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> Assertions.assertThrows(RtfFormatException.class, () -> RtfTokenizer.tokenize(deep)));
        final String atLimit = "{\\rtf1 " + "{".repeat(RtfTokenizer.MAX_DEPTH - 1) + "}".repeat(RtfTokenizer.MAX_DEPTH);
        Assertions.assertFalse(RtfTokenizer.tokenize(atLimit).isEmpty());
    }

    @Test
    public void escapesTextForRtf() {
        Assertions.assertEquals("a\\{b\\}\\\\c\\line \\tab {\\uc1\\u233?}{\\uc1\\u-2?}",
                RtfEdits.escape("a{b}\\c\n\té￾"));
    }

}
