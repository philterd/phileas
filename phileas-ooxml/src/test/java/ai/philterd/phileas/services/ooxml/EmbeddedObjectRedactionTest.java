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

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * An embedded object carries its own content. An embedded Word or Excel document is redacted in place by
 * recursing into it; an opaque object is removed (default) or kept with a verification caveat. Covers
 * both the .docx and .xlsx containers.
 */
public class EmbeddedObjectRedactionTest {

    private static final String XLSX = TestPackage.CT_XLSX;
    private static final String DOCX = TestPackage.CT_DOCX;
    private static final String EMAIL = "george@example.com";

    private static final TextFilter FILTER = TestFilters.email();

    private static byte[] xlsxWithEmail() {
        return SpreadsheetDocs.createXlsx(List.of(new String[]{"Name", "Email"}, new String[]{"Bob", EMAIL}));
    }

    private static byte[] docxWithEmail() {
        return WordFixturesForEmbeddedTests.create("Reach " + EMAIL + " for details.");
    }

    // --- docx container -----------------------------------------------------------

    @Test
    public void docxEmbeddedXlsxIsRedactedInPlace() throws Exception {
        final byte[] input = WordFixturesForEmbeddedTests.createWithEmbeddedPackage(xlsxWithEmail(), XLSX, "See attached.");

        final byte[] output = WordDocumentRedactor.redact(input, FILTER, OoxmlRedactionOptions.defaults()).document();

        Assertions.assertTrue(WordFixturesForEmbeddedTests.hasAnyEmbeddedObject(output),
                "a readable Office document is redacted in place, not removed");
        Assertions.assertFalse(WordFixturesForEmbeddedTests.embeddedPackageAllTextAsXlsx(output).contains(EMAIL));
    }

    @Test
    public void docxEmbeddedDocxIsRedactedInPlace() throws Exception {
        final byte[] input = WordFixturesForEmbeddedTests.createWithEmbeddedPackage(docxWithEmail(), DOCX, "See attached.");

        final byte[] output = WordDocumentRedactor.redact(input, FILTER, OoxmlRedactionOptions.defaults()).document();

        Assertions.assertFalse(WordFixturesForEmbeddedTests.embeddedPackageAllTextAsDocx(output).contains(EMAIL));
    }

    @Test
    public void docxOpaqueOleObjectRemovedByDefault() throws Exception {
        final byte[] garbage = ("legacy OLE blob with " + EMAIL).getBytes(StandardCharsets.UTF_8);
        final byte[] input = WordFixturesForEmbeddedTests.createWithEmbeddedOleObject(garbage, "Object below.");

        final byte[] output = WordDocumentRedactor.redact(input, FILTER, OoxmlRedactionOptions.defaults()).document();

        Assertions.assertFalse(WordFixturesForEmbeddedTests.hasAnyEmbeddedObject(output), "an un-inspectable object must be removed");
        Assertions.assertFalse(WordFixturesForEmbeddedTests.anyPartContains(output, EMAIL));
    }

    @Test
    public void docxOpaqueOleObjectKeptWhenDisabledAndCaveatSurfaces() throws Exception {
        final byte[] garbage = "legacy OLE blob".getBytes(StandardCharsets.UTF_8);
        final byte[] input = WordFixturesForEmbeddedTests.createWithEmbeddedOleObject(garbage, "Object below.");

        final byte[] output = WordDocumentRedactor.redact(input, FILTER,
                OoxmlRedactionOptions.defaults().setWord(new Word().setUninspectableObjects(Word.RemoveOrKeep.KEEP))).document();

        Assertions.assertTrue(WordFixturesForEmbeddedTests.hasAnyEmbeddedObject(output), "the object is kept when the option is off");
        Assertions.assertTrue(EmbeddedObjectRedactor.docxHasUninspectable(output), "verification should flag the kept object");
    }

    @Test
    public void detectFindsPiiInEmbeddedXlsx() throws Exception {
        final byte[] input = WordFixturesForEmbeddedTests.createWithEmbeddedPackage(xlsxWithEmail(), XLSX, "See attached.");

        final List<OfficeRedactionSpan> spans = WordDocumentRedactor.detect(input, FILTER, OoxmlRedactionOptions.defaults());

        Assertions.assertTrue(spans.stream().anyMatch(s -> s.getText().contains(EMAIL)));
    }

    // --- xlsx container -----------------------------------------------------------

    @Test
    public void xlsxEmbeddedXlsxIsRedactedInPlace() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithEmbeddedPackage(List.<String[]>of(new String[]{"A"}), xlsxWithEmail(), XLSX);

        final byte[] output = XlsxRedactor.redact(input, FILTER, OoxmlRedactionOptions.defaults()).document();

        Assertions.assertTrue(SpreadsheetDocs.xlsxHasAnyEmbeddedObject(output));
        Assertions.assertFalse(SpreadsheetDocs.xlsxEmbeddedPackageAllText(output).contains(EMAIL));
    }

    @Test
    public void xlsxOpaqueOleObjectRemovedByDefault() throws Exception {
        final byte[] garbage = ("legacy OLE blob with " + EMAIL).getBytes(StandardCharsets.UTF_8);
        final byte[] input = SpreadsheetDocs.createXlsxWithEmbeddedOleObject(List.<String[]>of(new String[]{"A"}), garbage);

        final byte[] output = XlsxRedactor.redact(input, FILTER, OoxmlRedactionOptions.defaults()).document();

        Assertions.assertFalse(SpreadsheetDocs.xlsxHasAnyEmbeddedObject(output));
    }

}
