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

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A native chart in a .docx keeps its source workbook as an embedded package, which can hold more than
 * the plotted cache. The redactor recurses into it, and strips an unparseable one. Ported from
 * phileas-dotnet's WordChartEmbeddedWorkbookTests.
 */
public class WordChartEmbeddedWorkbookTest {

    private static final TextFilter FILTER = TestFilters.email();
    private static final String EMAIL = "george@example.com";

    private static OoxmlRedactionOptions defaults() {
        return OoxmlRedactionOptions.defaults();
    }

    // Column C (Contact) holds PII the chart does not plot.
    private static byte[] embeddedWorkbookWithUnplottedPii() {
        return SpreadsheetDocs.createXlsx(List.of(
                new String[]{"Region", "Sales", "Contact"},
                new String[]{"North", "42", EMAIL}));
    }

    @Test
    public void redactEmbeddedWorkbookUnplottedPiiColumnIsRedacted() throws Exception {
        final byte[] input = WordDocs.createWithChartEmbeddedWorkbook(embeddedWorkbookWithUnplottedPii(), "Sales", "North");
        assertTrue(WordDocs.embeddedChartWorkbookAllText(input).contains(EMAIL));

        final byte[] output = WordDocumentRedactor.redact(input, FILTER, defaults()).document();

        assertTrue(WordDocs.hasChartEmbeddedWorkbook(output), "a parseable workbook is redacted in place, not removed");
        assertFalse(WordDocs.embeddedChartWorkbookAllText(output).contains(EMAIL));
    }

    @Test
    public void detectFindsPiiInEmbeddedWorkbook() throws Exception {
        final byte[] input = WordDocs.createWithChartEmbeddedWorkbook(embeddedWorkbookWithUnplottedPii(), "Sales", "North");

        final List<OfficeRedactionSpan> spans = WordDocumentRedactor.detect(input, FILTER, defaults());

        assertTrue(spans.stream().anyMatch(s -> s.getText().contains(EMAIL)));
    }

    @Test
    public void applySpansWithDrawingFilterRedactsEmbeddedWorkbook() throws Exception {
        final byte[] input = WordDocs.createWithChartEmbeddedWorkbook(embeddedWorkbookWithUnplottedPii(), "Sales", "North");

        final List<OfficeRedactionSpan> spans = WordDocumentRedactor.detect(input, FILTER, defaults());
        final byte[] output = WordDocumentRedactor.applySpans(input, spans, defaults(), FILTER);

        assertFalse(WordDocs.embeddedChartWorkbookAllText(output).contains(EMAIL));
    }

    // An embedded package that is not a valid workbook cannot be redacted, so it is removed.
    @Test
    public void redactUnparseableEmbeddedPackageIsStripped() throws Exception {
        final byte[] garbage = ("not a real xlsx " + EMAIL).getBytes(StandardCharsets.UTF_8);
        final byte[] input = WordDocs.createWithChartEmbeddedWorkbook(garbage, "Sales", "North");

        final byte[] output = WordDocumentRedactor.redact(input, FILTER, defaults()).document();

        assertFalse(WordDocs.hasChartEmbeddedWorkbook(output), "an unredactable embedded package must be removed");
        assertFalse(WordDocs.anyPartContains(output, EMAIL));
    }

}
