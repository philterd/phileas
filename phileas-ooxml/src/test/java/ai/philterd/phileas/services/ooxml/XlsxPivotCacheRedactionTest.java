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

import ai.philterd.phileas.policy.config.Excel;
import ai.philterd.phileas.policy.config.Word;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * A pivot table keeps a denormalized copy of its source data in the pivot cache. These verify the cached
 * values are redacted, the cache refreshes on open, indices survive, detection finds a residual, and the
 * option can turn it off.
 */
public class XlsxPivotCacheRedactionTest {

    private static final String EMAIL = "george@example.com";
    private static final String EMAIL2 = "mary@example.com";

    private static final TextFilter FILTER = TestFilters.forPolicy("{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}}}");

    private static final List<String[]> ROWS = List.<String[]>of(new String[]{"Name"});

    // A shared item email and an inline-string record email.
    private static byte[] makeWorkbook() {
        return SpreadsheetDocs.createXlsxWithPivotCache(ROWS, new String[]{EMAIL}, EMAIL2);
    }

    @Test
    public void enabledRedactsSharedItemAndInlineRecord() throws Exception {
        final byte[] output = XlsxRedactor.redact(makeWorkbook(), FILTER, OoxmlRedactionOptions.defaults()).document();

        final String pivot = SpreadsheetDocs.allPivotXml(output);
        Assertions.assertFalse(pivot.contains(EMAIL));
        Assertions.assertFalse(pivot.contains(EMAIL2));
    }

    @Test
    public void enabledSetsRefreshOnLoadAndKeepsIndices() throws Exception {
        final byte[] output = XlsxRedactor.redact(makeWorkbook(), FILTER, OoxmlRedactionOptions.defaults()).document();

        Assertions.assertTrue(SpreadsheetDocs.anyPivotRefreshOnLoad(output));
        Assertions.assertTrue(SpreadsheetDocs.allPivotXml(output).contains("v=\"0\""));
    }

    @Test
    public void disabledLeavesPivotCacheIntact() throws Exception {
        final byte[] output = XlsxRedactor.redact(makeWorkbook(), FILTER,
                OoxmlRedactionOptions.defaults().setExcel(new Excel().setPivotCaches(Word.RedactOrKeep.KEEP))).document();

        final String pivot = SpreadsheetDocs.allPivotXml(output);
        Assertions.assertTrue(pivot.contains(EMAIL));
        Assertions.assertTrue(pivot.contains(EMAIL2));
    }

    @Test
    public void detectFindsPivotCachePiiRegardlessOfOption() throws Exception {
        final List<OfficeRedactionSpan> residuals = XlsxRedactor.detect(makeWorkbook(), FILTER,
                OoxmlRedactionOptions.defaults().setExcel(new Excel().setPivotCaches(Word.RedactOrKeep.KEEP)));

        Assertions.assertTrue(residuals.stream().anyMatch(s -> s.getText().contains(EMAIL)));
        Assertions.assertTrue(residuals.stream().anyMatch(s -> s.getText().contains(EMAIL2)));
    }

    @Test
    public void noPiiPivotCacheLeftIntact() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithPivotCache(ROWS, new String[]{"North", "South"}, null);

        final byte[] output = XlsxRedactor.redact(input, FILTER, OoxmlRedactionOptions.defaults()).document();

        final String pivot = SpreadsheetDocs.allPivotXml(output);
        Assertions.assertTrue(pivot.contains("North"));
        Assertions.assertTrue(pivot.contains("South"));
        Assertions.assertFalse(SpreadsheetDocs.anyPivotRefreshOnLoad(output));
    }

}
