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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Passes phileas-dotnet's Office tests do not exercise: notes, Word comments, the threaded-comment copy,
 * field instructions, and Excel print headers and footers.
 */
public class OoxmlCoverageTest {

    private static final String EMAIL = "jane.doe@example.com";
    private static final String OTHER_EMAIL = "john.roe@example.org";
    private static final String REDACTED = "{{{REDACTED-email-address}}}";

    private static OoxmlRedactionResult redactDocx(final byte[] input) throws Exception {
        return WordDocumentRedactor.redact(input, TestFilters.email(), OoxmlRedactionOptions.defaults());
    }

    @Test
    public void footnotesAndEndnotesAreRedacted() throws Exception {
        final byte[] input = WordDocs.createWithNotes("Footnote " + EMAIL, "Endnote " + OTHER_EMAIL, "Body.");

        final OoxmlRedactionResult result = redactDocx(input);

        Assertions.assertEquals(List.of("Footnote " + REDACTED), nonEmpty(WordDocs.footnoteTexts(result.document())));
        Assertions.assertEquals(List.of("Endnote " + REDACTED), nonEmpty(WordDocs.endnoteTexts(result.document())));
        Assertions.assertFalse(WordDocs.anyPartContains(result.document(), EMAIL));
        Assertions.assertFalse(WordDocs.anyPartContains(result.document(), OTHER_EMAIL));
        // Notes follow the body in paragraph order, so their spans carry paragraph indexes after it.
        Assertions.assertEquals(2, result.spans().size());
        Assertions.assertTrue(result.spans().stream().allMatch(s -> s.getParagraphIndex() > 0));
    }

    @Test
    public void wordCommentTextIsRedacted() throws Exception {
        final byte[] input = WordDocs.createWithComment("Reviewer note for " + EMAIL, "Body.");

        final byte[] output = redactDocx(input).document();

        Assertions.assertEquals(List.of("Reviewer note for " + REDACTED), WordDocs.commentTexts(output));
        Assertions.assertFalse(WordDocs.anyPartContains(output, EMAIL));
    }

    @Test
    public void threadedCommentCopyAndItsCompanionsAreRemoved() throws Exception {
        final byte[] input = WordDocs.createWithThreadedThread(new String[]{"First " + EMAIL, "Reply " + OTHER_EMAIL}, "Body.");
        Assertions.assertTrue(WordDocs.anyPartContains(input, EMAIL));

        final byte[] output = redactDocx(input).document();

        Assertions.assertFalse(WordDocs.anyPartContains(output, EMAIL));
        Assertions.assertFalse(WordDocs.anyPartContains(output, OTHER_EMAIL));
        final Map<String, byte[]> entries = TestPackage.entries(output);
        Assertions.assertTrue(entries.keySet().stream().noneMatch(n -> n.toLowerCase().contains("commentsextended")), entries.keySet().toString());
        final String rels = TestPackage.xml(output, "word/_rels/document.xml.rels");
        Assertions.assertFalse(rels.contains("threadedComment"), rels);
        Assertions.assertFalse(rels.contains("commentsExtended"), rels);
        final String contentTypes = TestPackage.xml(output, "[Content_Types].xml");
        Assertions.assertFalse(contentTypes.toLowerCase().contains("threadedcomments"), contentTypes);
        Assertions.assertFalse(contentTypes.contains("commentsExtended"), contentTypes);
        // The classic comments, redacted, remain.
        Assertions.assertEquals(List.of("First " + REDACTED, "Reply " + REDACTED), WordDocs.commentTexts(output));
    }

    @Test
    public void simpleFieldInstructionIsRedacted() throws Exception {
        final byte[] input = WordDocs.createWithSimpleField("HYPERLINK \"mailto:" + EMAIL + "\"", "Contact us", "Body.");

        final OoxmlRedactionResult result = redactDocx(input);

        Assertions.assertFalse(WordDocs.anyPartContains(result.document(), EMAIL));
        Assertions.assertTrue(result.spans().stream().anyMatch(s -> s.getParagraphIndex() == -1 && EMAIL.equals(s.getText())));
    }

    @Test
    public void complexFieldInstructionSplitAcrossRunsIsRedacted() throws Exception {
        final byte[] input = WordDocs.createWithComplexField(new String[]{"HYPERLINK \"mailto:jane.doe@", "example.com\""}, "Contact", "Body.");

        final OoxmlRedactionResult result = redactDocx(input);

        Assertions.assertFalse(WordDocs.anyPartContains(result.document(), "jane.doe@"));
        Assertions.assertTrue(result.spans().stream().anyMatch(s -> EMAIL.equals(s.getText())));
    }

    @Test
    public void wordHeadersAndFootersAreKeptWhenTheOptionIsOff() throws Exception {
        final byte[] input = WordDocs.createWithHeaderFooter("Header " + EMAIL, "Footer", "Body " + OTHER_EMAIL);

        final byte[] output = WordDocumentRedactor.redact(input, TestFilters.email(),
                OoxmlRedactionOptions.defaults().setRedactHeadersFooters(false)).document();

        Assertions.assertTrue(WordDocs.anyPartContains(output, EMAIL));
        Assertions.assertFalse(WordDocs.anyPartContains(output, OTHER_EMAIL));
    }

    @Test
    public void excelPrintHeadersAndFootersAreRedacted() throws Exception {
        final List<String[]> rows = List.<String[]>of(new String[]{"Name"}, new String[]{"Jane"});
        final byte[] input = SpreadsheetDocs.createXlsxWithHeaderFooter(rows, "&LHeader " + EMAIL, "&CFooter " + OTHER_EMAIL);

        final OoxmlRedactionResult result = XlsxRedactor.redact(input, TestFilters.email(), OoxmlRedactionOptions.defaults());

        final String text = SpreadsheetDocs.headerFooterText(result.document());
        Assertions.assertFalse(text.contains(EMAIL), text);
        Assertions.assertFalse(text.contains(OTHER_EMAIL), text);
        Assertions.assertTrue(text.contains("&LHeader " + REDACTED), text);
        Assertions.assertEquals(2, result.spans().stream().filter(s -> s.getParagraphIndex() == -1).count());
    }

    @Test
    public void excelPrintHeadersAndFootersAreKeptWhenTheOptionIsOff() throws Exception {
        final List<String[]> rows = List.<String[]>of(new String[]{"Name"}, new String[]{"Jane"});
        final byte[] input = SpreadsheetDocs.createXlsxWithHeaderFooter(rows, "&LHeader " + EMAIL, "&CFooter");

        final byte[] output = XlsxRedactor.redact(input, TestFilters.email(),
                OoxmlRedactionOptions.defaults().setRedactHeadersFooters(false)).document();

        Assertions.assertTrue(SpreadsheetDocs.headerFooterText(output).contains(EMAIL));
    }

    private static List<String> nonEmpty(final List<String> texts) {
        return texts.stream().filter(t -> !t.isEmpty()).toList();
    }

}
