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

import ai.philterd.phileas.policy.config.Word;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/** A redacted Word paragraph keeps everything but the replaced text. */
public class WordInPlaceRedactionTest {

    private static final String EMAIL = "jane.doe@example.com";
    private static final String REDACTED = "{{{REDACTED-email-address}}}";
    private static final String W = TestPackage.W;

    private static byte[] redact(final byte[] input) throws Exception {
        return WordDocumentRedactor.redact(input, TestFilters.email(), OoxmlTestFixtures.keepingCommentsAndRevisions()).document();
    }

    private static List<Element> runs(final byte[] docx) {
        return TestPackage.elements(docx, "word/document.xml", W, "r");
    }

    private static String text(final Element run) {
        final StringBuilder sb = new StringBuilder();
        for (final Element t : OoxmlPackage.children(run, W, "t")) {
            sb.append(t.getTextContent());
        }
        return sb.toString();
    }

    private static boolean has(final Element run, final String property) {
        final Element rPr = OoxmlPackage.firstChild(run, W, "rPr");
        return rPr != null && OoxmlPackage.firstChild(rPr, W, property) != null;
    }

    @Test
    public void keepsTheFormattingOfTheRunsAroundAndUnderTheReplacement() throws Exception {

        final byte[] input = WordDocs.createRaw("<w:p><w:r><w:rPr><w:b/></w:rPr><w:t xml:space=\"preserve\">Write to "
                + EMAIL + " today</w:t></w:r><w:r><w:rPr><w:i/></w:rPr><w:t>Italic.</w:t></w:r></w:p>");

        final byte[] output = redact(input);

        final List<Element> runs = runs(output);
        Assertions.assertEquals(2, runs.size());
        Assertions.assertEquals("Write to " + REDACTED + " today", text(runs.get(0)));
        Assertions.assertTrue(has(runs.get(0), "b"));
        Assertions.assertEquals("Italic.", text(runs.get(1)));
        Assertions.assertTrue(has(runs.get(1), "i"));

    }

    @Test
    public void aValueAcrossRunsIsReplacedInItsFirstRunAndRemovedFromTheOthers() throws Exception {

        final byte[] input = WordDocs.createRaw("<w:p><w:r><w:t xml:space=\"preserve\">Mail jane.doe@</w:t></w:r>"
                + "<w:r><w:rPr><w:b/></w:rPr><w:t xml:space=\"preserve\">example.com now</w:t></w:r></w:p>");

        final byte[] output = redact(input);

        final List<Element> runs = runs(output);
        Assertions.assertEquals("Mail " + REDACTED, text(runs.get(0)));
        Assertions.assertEquals(" now", text(runs.get(1)));
        Assertions.assertTrue(has(runs.get(1), "b"));
        Assertions.assertEquals(List.of("Mail " + REDACTED + " now"), WordDocumentRedactor.readParagraphs(output));

    }

    @Test
    public void keepsAHyperlinkAndACommentAnchor() throws Exception {

        final byte[] input = WordDocs.createRaw("<w:p><w:commentRangeStart w:id=\"1\"/>"
                + "<w:hyperlink w:anchor=\"top\"><w:r><w:t xml:space=\"preserve\">See " + EMAIL + "</w:t></w:r></w:hyperlink>"
                + "<w:commentRangeEnd w:id=\"1\"/><w:r><w:commentReference w:id=\"1\"/></w:r></w:p>");

        final byte[] output = redact(input);

        final List<Element> links = TestPackage.elements(output, "word/document.xml", W, "hyperlink");
        Assertions.assertEquals(1, links.size());
        Assertions.assertEquals("See " + REDACTED, links.get(0).getTextContent());
        Assertions.assertEquals(1, TestPackage.elements(output, "word/document.xml", W, "commentRangeStart").size());
        Assertions.assertEquals(1, TestPackage.elements(output, "word/document.xml", W, "commentReference").size());

    }

    @Test
    public void keepsAnInsertionAroundARedactedValue() throws Exception {

        final byte[] input = WordDocs.createRaw("<w:p><w:ins w:id=\"1\" w:author=\"A\">"
                + WordDocs.run("Added " + EMAIL) + "</w:ins></w:p>");

        final byte[] output = redact(input);

        final List<Element> insertions = TestPackage.elements(output, "word/document.xml", W, "ins");
        Assertions.assertEquals(1, insertions.size());
        Assertions.assertEquals("Added " + REDACTED, insertions.get(0).getTextContent());

    }

    @Test
    public void highlightsTheReplacementInItsOwnRunInSchemaOrder() throws Exception {

        final byte[] input = WordDocs.createRaw("<w:p><w:r><w:rPr><w:b/><w:u w:val=\"single\"/></w:rPr>"
                + "<w:t xml:space=\"preserve\">To " + EMAIL + " now</w:t></w:r></w:p>");

        final byte[] output = WordDocumentRedactor.redact(input, TestFilters.email(),
                OoxmlRedactionOptions.defaults().setWord(new Word().setHighlight(true))).document();

        final List<String> texts = new ArrayList<>();
        for (final Element run : runs(output)) {
            texts.add(text(run));
            Assertions.assertTrue(has(run, "b") && has(run, "u"), "every part keeps the run's formatting");
        }
        Assertions.assertEquals(List.of("To ", REDACTED, " now"), texts);

        final Element highlighted = runs(output).get(1);
        final List<String> order = new ArrayList<>();
        for (final Element child : OoxmlPackage.childElements(OoxmlPackage.firstChild(highlighted, W, "rPr"))) {
            order.add(child.getLocalName());
        }
        Assertions.assertEquals(List.of("b", "highlight", "u"), order);
        Assertions.assertFalse(has(runs(output).get(0), "highlight"));
        Assertions.assertFalse(has(runs(output).get(2), "highlight"));

    }

    @Test
    public void aCopiedRunLeavesOutTheFormattingChangeRecord() throws Exception {

        final byte[] input = WordDocs.createRaw("<w:p><w:r><w:rPr><w:b/><w:rPrChange w:id=\"7\" w:author=\"A\"><w:rPr/>"
                + "</w:rPrChange></w:rPr><w:t xml:space=\"preserve\">To " + EMAIL + " now</w:t></w:r></w:p>");

        final byte[] output = WordDocumentRedactor.redact(input, TestFilters.email(),
                OoxmlTestFixtures.keepingCommentsAndRevisions().setWord(
                        OoxmlTestFixtures.keepingCommentsAndRevisions().getWord().setHighlight(true))).document();

        Assertions.assertEquals(1, TestPackage.elements(output, "word/document.xml", W, "rPrChange").size());

    }

}
