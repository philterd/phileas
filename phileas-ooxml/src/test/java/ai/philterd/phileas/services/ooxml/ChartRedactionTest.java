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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Embedded charts in .docx and .xlsx carry PII in their title text and in the cached series and category
 * values. Both are redacted when chart redaction is on and left alone when it is off. Ported from
 * phileas-dotnet's ChartRedactionTests.
 */
public class ChartRedactionTest {

    private static final TextFilter FILTER = TestFilters.forPolicy("{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}}}");

    private static final String TITLE = "Report for john@example.com";
    private static final String SERIES_NAME = "SSN 123-45-6789";   // cached series name (strCache)
    private static final String CATEGORY = "alice@example.com";    // cached category (strCache)

    private static final List<String[]> GRID = List.of(new String[]{"Name"}, new String[]{"Alice"});

    // --- .docx ---

    @Test
    public void docxChartOnRedactsTitleAndCachedValues() throws Exception {
        final byte[] input = WordDocs.createWithChartData(TITLE, SERIES_NAME, CATEGORY, "Body.");

        final byte[] output = WordDocumentRedactor.redact(input, FILTER, OoxmlRedactionOptions.defaults()
                .setHighlight(false).setRedactHeadersFooters(true).setRedactCharts(true)).document();

        assertFalse(WordDocs.anyPartContains(output, "john@example.com"));
        assertFalse(WordDocs.anyPartContains(output, "123-45-6789"));
        assertFalse(WordDocs.anyPartContains(output, "alice@example.com"));
    }

    @Test
    public void docxChartOffLeavesChartUntouched() throws Exception {
        final byte[] input = WordDocs.createWithChartData(TITLE, SERIES_NAME, CATEGORY, "Body.");

        final byte[] output = WordDocumentRedactor.redact(input, FILTER, OoxmlRedactionOptions.defaults()
                .setHighlight(false).setRedactHeadersFooters(true).setRedactCharts(false)).document();

        assertTrue(WordDocs.anyPartContains(output, "john@example.com"));
        assertTrue(WordDocs.anyPartContains(output, "123-45-6789"));
    }

    @Test
    public void docxDetectFindsChartPii() throws Exception {
        final byte[] input = WordDocs.createWithChartData(TITLE, SERIES_NAME, CATEGORY, "Body.");

        final List<OfficeRedactionSpan> residuals = WordDocumentRedactor.detect(input, FILTER, OoxmlRedactionOptions.defaults());

        assertTrue(residuals.stream().anyMatch(s -> s.getText().contains("john@example.com")));
        assertTrue(residuals.stream().anyMatch(s -> s.getText().contains("123-45-6789")));
    }

    // --- .xlsx ---

    @Test
    public void xlsxChartOnRedactsTitleAndCachedValues() throws Exception {
        final byte[] input = WordDocs.createXlsxWithChart(GRID, TITLE, SERIES_NAME, CATEGORY);

        final byte[] output = XlsxRedactor.redact(input, FILTER, OoxmlRedactionOptions.defaults()).document();

        final String chartXml = WordDocs.allChartXml(output);
        assertFalse(chartXml.contains("john@example.com"));
        assertFalse(chartXml.contains("123-45-6789"));
        assertFalse(chartXml.contains("alice@example.com"));
    }

    @Test
    public void xlsxChartOffLeavesChartUntouched() throws Exception {
        final byte[] input = WordDocs.createXlsxWithChart(GRID, TITLE, SERIES_NAME, CATEGORY);

        final byte[] output = XlsxRedactor.redact(input, FILTER, OoxmlRedactionOptions.defaults().setRedactCharts(false)).document();

        assertTrue(WordDocs.allChartXml(output).contains("john@example.com"));
    }

    @Test
    public void xlsxDetectFindsChartPii() throws Exception {
        final byte[] input = WordDocs.createXlsxWithChart(GRID, TITLE, SERIES_NAME, CATEGORY);

        final List<OfficeRedactionSpan> residuals = XlsxRedactor.detect(input, FILTER, OoxmlRedactionOptions.defaults());

        assertTrue(residuals.stream().anyMatch(s -> s.getText().contains("john@example.com")));
    }

}
