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

import java.io.InputStream;
import java.util.Map;

/**
 * A document saved by Word (from Philter Desktop's test documents), with the markup the generated
 * fixtures leave out: styles, theme, settings, rsid attributes, and a hyperlink Word created for an email.
 */
public class WordSavedDocumentTest {

    static final String RESOURCE = "/office/word-saved.docx";
    private static final String EMAIL = "george@example.com";
    private static final String SSN = "123-45-6789";

    static byte[] wordSaved() throws Exception {
        try (InputStream in = WordSavedDocumentTest.class.getResourceAsStream(RESOURCE)) {
            return in.readAllBytes();
        }
    }

    @Test
    public void redactsTheBodyAndHyperlinkAndKeepsWordsOtherParts() throws Exception {
        final byte[] input = wordSaved();

        final OoxmlRedactionResult result = WordDocumentRedactor.redact(input,
                TestFilters.forPolicy("{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}}}"), OoxmlRedactionOptions.defaults());
        final byte[] output = result.document();

        final String xml = TestPackage.allXml(output);
        Assertions.assertFalse(xml.contains(EMAIL), xml);
        Assertions.assertFalse(xml.contains(SSN), xml);
        Assertions.assertTrue(TestPackage.xml(output, "word/_rels/document.xml.rels")
                .contains(WordDocumentRedactor.REDACTED_HYPERLINK_TARGET));
        Assertions.assertEquals(2, result.spans().stream().filter(s -> s.getParagraphIndex() == 0).count());

        final Map<String, byte[]> before = TestPackage.entries(input);
        final Map<String, byte[]> after = TestPackage.entries(output);
        Assertions.assertEquals(before.keySet(), after.keySet());
        for (final String name : new String[]{"word/styles.xml", "word/theme/theme1.xml", "word/settings.xml",
                "word/fontTable.xml", "word/webSettings.xml", "docProps/app.xml"}) {
            Assertions.assertArrayEquals(before.get(name), after.get(name), name);
        }

        // Word saved the author as creator and last editor; app.xml's Company is already empty, so it is unchanged.
        final String core = TestPackage.xml(output, "docProps/core.xml");
        Assertions.assertFalse(core.contains("Test Author"), core);
        Assertions.assertTrue(core.contains("<cp:revision>2</cp:revision>"), core);
    }

}
