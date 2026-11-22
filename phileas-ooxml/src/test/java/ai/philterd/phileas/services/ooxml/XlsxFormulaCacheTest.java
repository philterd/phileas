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

import ai.philterd.phileas.services.ooxml.SpreadsheetDocs.CellSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * A formula cell caches its last computed value, which can duplicate PII from a redacted cell. With the
 * "redact cached formula values" option, caches holding PII are staticized and the rest cleared (with
 * recalc on load) when anything was redacted. With it off, formula cells are untouched. Detection scans
 * caches either way.
 */
public class XlsxFormulaCacheTest {

    private static final TextFilter FILTER = TestFilters.email();

    // A2 holds the email; B2 is "=A2" whose cached result duplicates it; C2 is a benign formula.
    private static byte[] makeWorkbook() {
        return SpreadsheetDocs.createTyped(List.of(
                new CellSpec[]{CellSpec.of("Name"), CellSpec.of("Copy"), CellSpec.of("Count")},
                new CellSpec[]{
                        CellSpec.of("george@example.com"),
                        CellSpec.formula("george@example.com", "A2"),
                        CellSpec.formula("2", "1+1")
                }));
    }

    @Test
    public void enabledFormulaCacheWithPiiIsRedactedAndStaticized() throws Exception {
        final byte[] output = XlsxRedactor.redact(makeWorkbook(), FILTER, OoxmlRedactionOptions.defaults()).document();

        Assertions.assertFalse(SpreadsheetDocs.allText(output).contains("george@example.com"));
        Assertions.assertFalse(SpreadsheetDocs.isFormulaCell(output, "B2"));
    }

    @Test
    public void enabledOtherFormulaCachesAreClearedAndRecalcForced() throws Exception {
        final byte[] output = XlsxRedactor.redact(makeWorkbook(), FILTER, OoxmlRedactionOptions.defaults()).document();

        Assertions.assertTrue(SpreadsheetDocs.isFormulaCell(output, "C2"));
        final String cached = SpreadsheetDocs.cachedValue(output, "C2");
        Assertions.assertTrue(cached == null || cached.isEmpty());
        Assertions.assertTrue(SpreadsheetDocs.fullCalcOnLoad(output));
    }

    @Test
    public void disabledFormulaCellsAreLeftUntouched() throws Exception {
        final byte[] output = XlsxRedactor.redact(makeWorkbook(), FILTER,
                OoxmlRedactionOptions.defaults().setRedactFormulaValues(false)).document();

        Assertions.assertFalse(SpreadsheetDocs.isFormulaCell(output, "A2"));
        Assertions.assertEquals("george@example.com", SpreadsheetDocs.cachedValue(output, "B2"));
        Assertions.assertTrue(SpreadsheetDocs.isFormulaCell(output, "B2"));
        Assertions.assertFalse(SpreadsheetDocs.fullCalcOnLoad(output));
    }

    @Test
    public void detectFindsPiiInFormulaCacheRegardlessOfOption() throws Exception {
        final List<OfficeRedactionSpan> residuals = XlsxRedactor.detect(makeWorkbook(), FILTER, OoxmlRedactionOptions.defaults());

        Assertions.assertTrue(residuals.stream().anyMatch(s -> s.getText().contains("george@example.com")));
    }

    @Test
    public void noPiiFormulaCachesAreLeftIntact() throws Exception {
        final byte[] input = SpreadsheetDocs.createTyped(List.of(
                new CellSpec[]{CellSpec.of("A"), CellSpec.of("Sum")},
                new CellSpec[]{CellSpec.of("1"), CellSpec.formula("2", "A2+1")}));

        final byte[] output = XlsxRedactor.redact(input, FILTER, OoxmlRedactionOptions.defaults()).document();

        Assertions.assertTrue(SpreadsheetDocs.isFormulaCell(output, "B2"));
        Assertions.assertEquals("2", SpreadsheetDocs.cachedValue(output, "B2"));
        Assertions.assertFalse(SpreadsheetDocs.fullCalcOnLoad(output));
    }

}
