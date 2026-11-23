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

import ai.philterd.phileas.model.filtering.Span;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ai.philterd.phileas.services.ooxml.TestPackage.R;
import static ai.philterd.phileas.services.ooxml.TestPackage.REL;
import static ai.philterd.phileas.services.ooxml.TestPackage.S;
import static ai.philterd.phileas.services.ooxml.TestPackage.escape;

/**
 * The header-context option prepends a column's header to a data cell's text as detection context, and
 * maps the detected spans back onto the cell so only the cell is rewritten.
 */
public class XlsxHeaderContextTest {

    // Fires only when its input contains both the "SSN" header and the value "999".
    private static final TextFilter CONTEXT_DEPENDENT_FILTER = input -> {
        final int index = input.indexOf("999");
        if (index < 0 || !input.contains("SSN")) {
            return TestFilters.result(input, List.of());
        }
        final Span span = TestFilters.span(input, index, index + 3, "***");
        return TestFilters.result(input.substring(0, index) + "***" + input.substring(index + 3), List.of(span));
    };

    @Test
    public void withoutHeaderContextContextDependentDetectorDoesNotFire() throws Exception {
        final byte[] xlsx = buildSingleColumnXlsx("SSN", "999");

        final OoxmlRedactionResult result = XlsxRedactor.redact(xlsx, CONTEXT_DEPENDENT_FILTER, OoxmlRedactionOptions.defaults());

        Assertions.assertTrue(result.spans().isEmpty());
        // The C# test reads the input here; the redacted output is what the test means to check.
        Assertions.assertEquals("999", SpreadsheetDocs.cellText(result.document(), "A2"));
    }

    @Test
    public void withHeaderContextDetectsUsingHeaderAndRemapsSpanOntoCell() throws Exception {
        final byte[] xlsx = buildSingleColumnXlsx("SSN", "999");

        final OoxmlRedactionResult result = XlsxRedactor.redact(xlsx, CONTEXT_DEPENDENT_FILTER,
                OoxmlRedactionOptions.defaults().setUseHeaderContext(true));

        final List<OfficeRedactionSpan> spans = result.spans();
        Assertions.assertEquals(1, spans.size());
        Assertions.assertEquals(0, spans.get(0).getCharacterStart());
        Assertions.assertEquals(3, spans.get(0).getCharacterEnd());
        Assertions.assertEquals("999", spans.get(0).getText());

        Assertions.assertEquals("***", SpreadsheetDocs.cellText(result.document(), "A2"));
        Assertions.assertEquals("SSN", SpreadsheetDocs.cellText(result.document(), "A1"));
    }

    // Column A holds the values as inline strings, one per row; no shared-string table.
    private static byte[] buildSingleColumnXlsx(final String... columnAValues) {
        final StringBuilder sheetData = new StringBuilder("<sheetData>");
        for (int r = 0; r < columnAValues.length; r++) {
            sheetData.append("<row r=\"").append(r + 1).append("\"><c r=\"A").append(r + 1)
                    .append("\" t=\"inlineStr\"><is><t>").append(escape(columnAValues[r])).append("</t></is></c></row>");
        }
        sheetData.append("</sheetData>");
        final TestPackage pkg = new TestPackage();
        pkg.rel("/", TestPackage.REL_OFFICE_DOCUMENT, "/xl/workbook.xml");
        final String sheetId = pkg.rel("/xl/workbook.xml", REL + "worksheet", "/xl/worksheets/sheet1.xml");
        pkg.part("/xl/workbook.xml", TestPackage.CT_XLSX_MAIN, "<workbook xmlns=\"" + S + "\" xmlns:r=\"" + R
                + "\"><sheets><sheet name=\"Sheet1\" sheetId=\"1\" r:id=\"" + sheetId + "\"/></sheets></workbook>");
        pkg.part("/xl/worksheets/sheet1.xml", SpreadsheetDocs.CT_WORKSHEET,
                "<worksheet xmlns=\"" + S + "\">" + sheetData + "</worksheet>");
        return pkg.toBytes();
    }

}
