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

/**
 * Text in worksheet shapes and text boxes lives in the drawing part, apart from cells and charts. These
 * verify it is redacted, detected, re-redacted on modify, and that PII split across runs is caught.
 */
public class XlsxShapeRedactionTest {

    private static final String EMAIL = "george@example.com";

    private static final TextFilter FILTER = TestFilters.forPolicy("{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}}}");

    private static final List<String[]> ROWS = List.<String[]>of(new String[]{"Sheet with a text box"});

    @Test
    public void textBoxWithPiiIsRedacted() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithTextBox(ROWS, "Contact " + EMAIL + " for details");

        final byte[] output = XlsxRedactor.redact(input, FILTER, OoxmlRedactionOptions.defaults()).document();

        Assertions.assertFalse(SpreadsheetDocs.allDrawingXml(output).contains(EMAIL));
    }

    @Test
    public void textBoxPiiSplitAcrossRunsIsRedacted() throws Exception {
        // The email spans two a:t runs; the paragraph merge must catch it.
        final byte[] input = SpreadsheetDocs.createXlsxWithTextBox(ROWS, "george@", "example.com");

        final byte[] output = XlsxRedactor.redact(input, FILTER, OoxmlRedactionOptions.defaults()).document();

        final String drawing = SpreadsheetDocs.allDrawingXml(output);
        Assertions.assertFalse(drawing.contains(EMAIL));
        Assertions.assertFalse(drawing.contains("example.com"));
    }

    @Test
    public void detectFindsShapePii() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithTextBox(ROWS, "Contact " + EMAIL);

        final List<OfficeRedactionSpan> residuals = XlsxRedactor.detect(input, FILTER, OoxmlRedactionOptions.defaults());

        Assertions.assertTrue(residuals.stream().anyMatch(s -> s.getText().contains(EMAIL)));
    }

    @Test
    public void modifyApplySpansRedactsShapeText() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithTextBox(ROWS, "Contact " + EMAIL);
        final List<OfficeRedactionSpan> spans = XlsxRedactor.redact(input, FILTER, OoxmlRedactionOptions.defaults()).spans();

        final byte[] output = XlsxRedactor.applySpans(input, spans, OoxmlRedactionOptions.defaults(), FILTER);

        Assertions.assertFalse(SpreadsheetDocs.allDrawingXml(output).contains(EMAIL));
    }

    @Test
    public void textBoxNoPiiIsLeftIntact() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithTextBox(ROWS, "Quarterly summary");

        final byte[] output = XlsxRedactor.redact(input, FILTER, OoxmlRedactionOptions.defaults()).document();

        Assertions.assertTrue(SpreadsheetDocs.allDrawingXml(output).contains("Quarterly summary"));
    }

}
