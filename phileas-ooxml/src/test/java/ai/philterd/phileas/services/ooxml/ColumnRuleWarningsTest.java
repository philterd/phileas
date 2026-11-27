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
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.RedactionWarning;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.FilterOptions;
import ai.philterd.phileas.services.filters.filtering.OoxmlFilterService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

public class ColumnRuleWarningsTest {

    private static final String EMAIL = "pat@example.com";

    // The sheet's name and a cell's text, which must never appear in a warning.
    private static final String SHEET = "Confidential Roster";
    private static final String CELL = "Roster cell text";

    private static byte[] workbook() {
        return SpreadsheetDocs.createXlsx(List.of(
                new String[]{"Name", "Email"},
                new String[]{CELL, EMAIL}), SHEET);
    }

    private static Policy policy(final String sheets) {
        return new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{}},\"tabular\":{\"sheets\":" + sheets + "}}",
                Policy.class);
    }

    private static BinaryDocumentFilterResult filter(final Policy policy, final byte[] input, final MimeType type,
                                                     final FilterOptions options) throws Exception {
        return new OoxmlFilterService(new PhileasConfiguration(new Properties()), null).filter(policy,
                new DefaultContextService(), new InMemoryVectorService(), "ctx", input, type, options);
    }

    private static BinaryDocumentFilterResult filter(final Policy policy, final byte[] input, final MimeType type)
            throws Exception {
        return filter(policy, input, type, FilterOptions.defaults());
    }

    @Test
    public void aHeaderThatMatchesNoColumnIsAWarningOnTheResult() throws Exception {

        final BinaryDocumentFilterResult result = filter(policy("[{\"unlistedColumns\":\"ignore\",\"columns\":["
                + "{\"header\":\"Social Security\",\"action\":\"redact\",\"strategies\":[{}]}]}]"),
                workbook(), MimeType.APPLICATION_XLSX);

        Assertions.assertEquals(List.of(RedactionWarning.unmatchedHeader("Social Security", null)), result.getWarnings());
        final RedactionWarning warning = result.getWarnings().get(0);
        Assertions.assertEquals(RedactionWarning.Code.UNMATCHED_HEADER, warning.getCode());
        Assertions.assertFalse(warning.isEmbedded());

    }

    @Test
    public void aWarningHasTheEntrysWorksheetAndNoDocumentContent() throws Exception {

        final BinaryDocumentFilterResult result = filter(policy("[{\"worksheet\":\"" + SHEET + "\",\"columns\":["
                + "{\"header\":\"Missing\",\"action\":\"ignore\"}]}]"), workbook(), MimeType.APPLICATION_XLSX);

        // The worksheet named by the policy's entry is the policy's value, so it is kept.
        Assertions.assertEquals(List.of(RedactionWarning.unmatchedHeader("Missing", SHEET)), result.getWarnings());

        // With an entry that names no worksheet, nothing of the document is in the warning.
        final BinaryDocumentFilterResult unnamed = filter(policy("[{\"columns\":["
                + "{\"header\":\"Missing\",\"action\":\"ignore\"}]}]"), workbook(), MimeType.APPLICATION_XLSX);
        final String json = new Gson().toJson(unnamed.getWarnings()) + unnamed.getWarnings();
        Assertions.assertFalse(json.contains(SHEET), json);
        Assertions.assertFalse(json.contains(CELL), json);
        Assertions.assertFalse(json.contains("Name"), json);

    }

    @Test
    public void aMatchedHeaderAndAnIndexRuleGiveNoWarning() throws Exception {

        final BinaryDocumentFilterResult result = filter(policy("[{\"columns\":["
                + "{\"header\":\"Email\",\"action\":\"redact\",\"strategies\":[{}]},"
                + "{\"index\":7,\"action\":\"ignore\"}]}]"), workbook(), MimeType.APPLICATION_XLSX);

        Assertions.assertTrue(result.getWarnings().isEmpty(), result.getWarnings().toString());

    }

    @Test
    public void aWarningRepeatedAcrossWorksheetsIsReportedOnce() throws Exception {

        final byte[] twoSheets = SpreadsheetDocs.createXlsxWithSecondSheet(
                List.of(new String[]{"Name"}, new String[]{"A"}), "First",
                List.of(new String[]{"Name"}, new String[]{"B"}), "Second");

        final BinaryDocumentFilterResult result = filter(policy("[{\"columns\":["
                + "{\"header\":\"Missing\",\"action\":\"ignore\"}]}]"), twoSheets, MimeType.APPLICATION_XLSX);

        Assertions.assertEquals(List.of(RedactionWarning.unmatchedHeader("Missing", null)), result.getWarnings());

    }

    @Test
    public void anEmbeddedWorkbooksWarningIsMarkedAsEmbedded() throws Exception {

        final Policy policy = policy("[{\"columns\":[{\"header\":\"Missing\",\"action\":\"ignore\"}]}]");
        final RedactionWarning embedded = RedactionWarning.unmatchedHeader("Missing", null).asEmbedded();

        // In a workbook, which has the column neither: its own warning, then the embedded one.
        final byte[] outer = SpreadsheetDocs.createXlsxWithEmbeddedPackage(
                List.of(new String[]{"Name"}, new String[]{"A"}), workbook(), TestPackage.CT_XLSX);
        Assertions.assertEquals(List.of(RedactionWarning.unmatchedHeader("Missing", null), embedded),
                filter(policy, outer, MimeType.APPLICATION_XLSX).getWarnings());

        // In a Word document, which has no worksheets of its own.
        final byte[] word = OoxmlTestFixtures.docxWithEmbeddedXlsx("Body.", workbook());
        Assertions.assertEquals(List.of(embedded), filter(policy, word, MimeType.APPLICATION_DOCX).getWarnings());

    }

    @Test
    public void warningsAreSerializedOnlyWhenThereAreAny() throws Exception {

        final JsonObject none = new Gson().toJsonTree(filter(policy("[{\"columns\":["
                + "{\"header\":\"Email\",\"action\":\"ignore\"}]}]"), workbook(), MimeType.APPLICATION_XLSX)).getAsJsonObject();
        Assertions.assertFalse(none.has("warnings"), none.toString());

        final JsonObject some = new Gson().toJsonTree(filter(policy("[{\"worksheet\":\"" + SHEET + "\",\"columns\":["
                + "{\"header\":\"Missing\",\"action\":\"ignore\"}]}]"), workbook(), MimeType.APPLICATION_XLSX)).getAsJsonObject();
        final JsonObject warning = some.getAsJsonArray("warnings").get(0).getAsJsonObject();
        Assertions.assertEquals("unmatched-header", warning.get("code").getAsString());
        Assertions.assertEquals("Missing", warning.get("header").getAsString());
        Assertions.assertEquals(SHEET, warning.get("worksheet").getAsString());
        Assertions.assertFalse(warning.get("embedded").getAsBoolean());

    }

    @Test
    public void textOutputHasTheWarningsToo() throws Exception {

        final BinaryDocumentFilterResult result = filter(policy("[{\"columns\":["
                + "{\"header\":\"Missing\",\"action\":\"ignore\"}]}]"), workbook(), MimeType.APPLICATION_XLSX,
                FilterOptions.defaults().withOutput(FilterOptions.Output.TEXT));

        Assertions.assertEquals(List.of(RedactionWarning.unmatchedHeader("Missing", null)), result.getWarnings());

    }

}
