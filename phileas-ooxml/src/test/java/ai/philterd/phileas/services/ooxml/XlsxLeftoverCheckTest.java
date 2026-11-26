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

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.LeftoverFinding;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.FilterOptions;
import ai.philterd.phileas.services.filters.filtering.OoxmlFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

/** The leftover check over workbook text that is not in a cell. */
public class XlsxLeftoverCheckTest {

    private static final String EMAIL = "jane.doe@example.com";
    private static final List<String[]> ROWS = List.<String[]>of(new String[]{"Name"}, new String[]{"Jane"});

    // Email addresses get random stand-ins, which look like real addresses.
    private static final Policy POLICY = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":"
            + "{\"emailAddressFilterStrategies\":[{\"strategy\":\"RANDOM_REPLACE\"}]}},"
            + "\"config\":{\"review\":{\"leftoverCheck\":{\"mode\":\"always\",\"using\":\"broad\"}}}}", Policy.class);

    private static List<LeftoverFinding> findings(final byte[] input) throws Exception {
        final BinaryDocumentFilterResult result = new OoxmlFilterService(new PhileasConfiguration(new Properties()), null)
                .filter(POLICY, new DefaultContextService(), new InMemoryVectorService(), "ctx", input,
                        MimeType.APPLICATION_XLSX, FilterOptions.defaults().withLeftoverCheckMatchedText(true));
        Assertions.assertTrue(result.getLeftoverCheck().isPerformed());
        return result.getLeftoverCheck().getFindings();
    }

    private static void assertFoundOutsideCells(final List<LeftoverFinding> findings, final String value) {
        // The stand-in for the email address is not reported.
        Assertions.assertTrue(findings.stream().noneMatch(f -> f.getFilterType() == FilterType.EMAIL_ADDRESS), findings.toString());
        final LeftoverFinding phone = findings.stream().filter(f -> value.equals(f.getMatchedText())).findFirst()
                .orElseThrow(() -> new AssertionError(value + " not in " + findings));
        Assertions.assertEquals(-1, phone.getParagraphNumber());
    }

    @Test
    public void findsWhatIsLeftInAComment() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithLegacyComment(ROWS, "A2",
                "Mail " + EMAIL + " or call 212-555-0187.");
        assertFoundOutsideCells(findings(input), "212-555-0187");
    }

    @Test
    public void findsWhatIsLeftInAChartTitle() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithChart(ROWS, "Mail " + EMAIL + " or call 212-555-0143",
                "Series", "Category");
        assertFoundOutsideCells(findings(input), "212-555-0143");
    }

    @Test
    public void numbersCellsAsTheRedactionDidAcrossWorksheets() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithSecondSheet(
                List.<String[]>of(new String[]{"Contact", WordLeftoverCheckTest.OTHER_EMAIL}), "First",
                List.<String[]>of(new String[]{"Note", WordLeftoverCheckTest.TARGET}), "Second");
        WordLeftoverCheckTest.assertLinedUp("two worksheets",
                WordLeftoverCheckTest.filter(null, input, MimeType.APPLICATION_XLSX));
    }

}
