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
import java.util.Set;

/**
 * Redaction can target one worksheet: the column picker reads that sheet's headers, and redaction (per
 * cell and whole column) applies only to that sheet.
 */
public class XlsxWorksheetTest {

    private static final TextFilter FILTER = TestFilters.email();

    // A "People" sheet and a "Vendors" sheet, each with a header row and PII.
    private static byte[] twoSheetWorkbook() {
        return SpreadsheetDocs.createXlsxWithSecondSheet(
                List.of(new String[]{"Name", "Email"}, new String[]{"John", "john@example.com"}), "People",
                List.of(new String[]{"Company", "Contact"}, new String[]{"Acme", "jane@example.com"}), "Vendors");
    }

    @Test
    public void readSheetNamesReturnsSheetsInOrder() throws Exception {
        Assertions.assertEquals(List.of("People", "Vendors"), XlsxRedactor.readSheetNames(twoSheetWorkbook()));
    }

    @Test
    public void readColumnsReadsTheChosenSheetsHeaders() throws Exception {
        final byte[] book = twoSheetWorkbook();

        final List<SpreadsheetColumn> vendors = XlsxRedactor.readColumns(book, "Vendors");
        Assertions.assertTrue(vendors.stream().anyMatch(c -> "Company".equals(c.header())));
        Assertions.assertTrue(vendors.stream().anyMatch(c -> "Contact".equals(c.header())));
        Assertions.assertFalse(vendors.stream().anyMatch(c -> "Name".equals(c.header())));

        // A null sheet name defaults to the first sheet.
        Assertions.assertTrue(XlsxRedactor.readColumns(book, null).stream().anyMatch(c -> "Name".equals(c.header())));
    }

    @Test
    public void redactOnlyTheChosenWorksheetLeavesOtherSheetsUntouched() throws Exception {
        final byte[] output = XlsxRedactor.redact(twoSheetWorkbook(), FILTER,
                OoxmlRedactionOptions.defaults().setWorksheet("Vendors")).document();

        final String all = SpreadsheetDocs.allText(output);
        Assertions.assertFalse(all.contains("jane@example.com"));
        Assertions.assertTrue(all.contains("john@example.com"));
    }

    @Test
    public void redactFullColumnAppliesOnlyToTheChosenWorksheet() throws Exception {
        final byte[] output = XlsxRedactor.redact(twoSheetWorkbook(), FILTER,
                OoxmlRedactionOptions.defaults().setFullyRedactedColumns(Set.of(1)).setWorksheet("People")).document();

        final String all = SpreadsheetDocs.allText(output);
        Assertions.assertFalse(all.contains("John"));
        Assertions.assertTrue(all.contains("Name"));
        Assertions.assertTrue(all.contains("Acme"));
        Assertions.assertTrue(all.contains("Company"));
    }

}
