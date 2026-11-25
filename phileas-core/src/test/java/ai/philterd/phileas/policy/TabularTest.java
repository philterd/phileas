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
package ai.philterd.phileas.policy;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.filters.ColumnFilter;
import ai.philterd.phileas.services.FilterPolicyLoader;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.ColumnFilterStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TabularTest {

    // The workbook example from the policy schema's specification.
    private static final String WORKBOOK = """
            {
              "identifiers": {
                "emailAddress": { "emailAddressFilterStrategies": [ { "strategy": "REDACT" } ] },
                "phoneNumber": { "phoneNumberFilterStrategies": [ { "strategy": "REDACT" } ] }
              },
              "tabular": {
                "sheets": [
                  {
                    "worksheet": "Patients",
                    "unlistedColumns": "redact",
                    "unlistedStrategies": [ { "strategy": "HASH_SHA256_REPLACE" } ],
                    "columns": [
                      { "header": "Visit Date", "action": "ignore" },
                      { "header": "Department", "action": "ignore" },
                      { "header": "Comments", "action": "filter", "filters": [ "emailAddress" ] }
                    ]
                  },
                  {
                    "worksheet": "Codes",
                    "headerRows": 0,
                    "unlistedColumns": "ignore",
                    "columns": [ { "index": 2, "action": "redact", "strategies": [ { "strategy": "MASK" } ] } ]
                  },
                  { "unlistedColumns": "identifiers" }
                ]
              }
            }""";

    private static Policy load(final String json) {
        return new Gson().fromJson(json, Policy.class);
    }

    private static Policy withSheets(final String sheets) {
        return load("{\"identifiers\":{\"emailAddress\":{}},\"tabular\":{\"sheets\":" + sheets + "}}");
    }

    @Test
    public void readsTheSpecificationExample() {

        final Tabular tabular = load(WORKBOOK).getTabular();

        Assertions.assertEquals(3, tabular.getSheets().size());

        final Tabular.Sheet patients = tabular.getSheets().get(0);
        Assertions.assertEquals("Patients", patients.getWorksheet());
        Assertions.assertEquals(1, patients.getHeaderRows());
        Assertions.assertEquals(Tabular.UnlistedColumns.REDACT, patients.getUnlistedColumns());
        Assertions.assertEquals(AbstractFilterStrategy.HASH_SHA256_REPLACE, patients.getUnlistedStrategies().get(0).getStrategy());
        Assertions.assertEquals(Tabular.Action.FILTER, patients.getColumns().get(2).getAction());
        Assertions.assertEquals(List.of("emailAddress"), patients.getColumns().get(2).getFilters());

        final Tabular.Sheet codes = tabular.getSheets().get(1);
        Assertions.assertEquals(0, codes.getHeaderRows());
        Assertions.assertEquals(2, codes.getColumns().get(0).getIndex());
        Assertions.assertEquals(AbstractFilterStrategy.MASK, codes.getColumns().get(0).getStrategies().get(0).getStrategy());

    }

    @Test
    public void appliesTheDefaults() {

        final Tabular.Sheet sheet = withSheets("[{}]").getTabular().getSheets().get(0);

        Assertions.assertNull(sheet.getWorksheet());
        Assertions.assertEquals(1, sheet.getHeaderRows());
        Assertions.assertEquals(Tabular.UnlistedColumns.IDENTIFIERS, sheet.getUnlistedColumns());
        Assertions.assertNull(sheet.getUnlistedStrategies());
        Assertions.assertTrue(sheet.getColumns().isEmpty());

        // Unlisted columns that are redacted use a single REDACT by default.
        final List<ColumnFilterStrategy> unlisted = sheet.getEffectiveUnlistedStrategies();
        Assertions.assertEquals(1, unlisted.size());
        Assertions.assertEquals(AbstractFilterStrategy.REDACT, unlisted.get(0).getStrategy());

    }

    @Test
    public void aPolicyWithoutTabularHasNone() {
        Assertions.assertNull(load("{\"identifiers\":{}}").getTabular());
    }

    @Test
    public void roundTrips() {

        final Policy policy = load(WORKBOOK);
        final JsonObject json = JsonParser.parseString(new Gson().toJson(policy)).getAsJsonObject();
        final JsonObject first = json.getAsJsonObject("tabular").getAsJsonArray("sheets").get(0).getAsJsonObject();

        Assertions.assertEquals("Patients", first.get("worksheet").getAsString());
        Assertions.assertEquals(1, first.get("headerRows").getAsInt());
        Assertions.assertEquals("redact", first.get("unlistedColumns").getAsString());
        Assertions.assertEquals("filter", first.getAsJsonArray("columns").get(2).getAsJsonObject().get("action").getAsString());

        // The unnamed entry is written with its defaults and no unlistedStrategies.
        final JsonObject last = json.getAsJsonObject("tabular").getAsJsonArray("sheets").get(2).getAsJsonObject();
        Assertions.assertFalse(last.has("worksheet"));
        Assertions.assertFalse(last.has("unlistedStrategies"));
        Assertions.assertEquals("identifiers", last.get("unlistedColumns").getAsString());

        final Policy again = load(new Gson().toJson(policy));
        Assertions.assertEquals(new Gson().toJson(policy), new Gson().toJson(again));
        Assertions.assertEquals(policy.getCacheKey(), again.getCacheKey());

    }

    @ParameterizedTest
    @ValueSource(strings = {
            // The section and its sheets.
            "{\"sheets\":[]}",
            "{\"sheets\":[{}],\"other\":1}",
            "{}",
            // Sheet entries.
            "{\"sheets\":[{\"worksheet\":\"\"}]}",
            "{\"sheets\":[{\"worksheet\":\"A\"},{\"worksheet\":\"a\"}]}",
            "{\"sheets\":[{},{}]}",
            "{\"sheets\":[{\"headerRows\":-1}]}",
            "{\"sheets\":[{\"headerRows\":1.5}]}",
            "{\"sheets\":[{\"unlistedColumns\":\"scan\"}]}",
            "{\"sheets\":[{\"unlistedStrategies\":[{\"strategy\":\"REDACT\"}]}]}",
            "{\"sheets\":[{\"unlistedColumns\":\"redact\",\"unlistedStrategies\":[]}]}",
            "{\"sheets\":[{\"other\":1}]}",
            // Column rules.
            "{\"sheets\":[{\"columns\":[{\"action\":\"ignore\"}]}]}",
            "{\"sheets\":[{\"columns\":[{\"header\":\"A\",\"index\":1,\"action\":\"ignore\"}]}]}",
            "{\"sheets\":[{\"columns\":[{\"header\":\" \",\"action\":\"ignore\"}]}]}",
            "{\"sheets\":[{\"columns\":[{\"index\":0,\"action\":\"ignore\"}]}]}",
            "{\"sheets\":[{\"columns\":[{\"header\":\"A\"}]}]}",
            "{\"sheets\":[{\"columns\":[{\"header\":\"A\",\"action\":\"scan\"}]}]}",
            "{\"sheets\":[{\"columns\":[{\"header\":\"A\",\"action\":\"redact\"}]}]}",
            "{\"sheets\":[{\"columns\":[{\"header\":\"A\",\"action\":\"ignore\",\"strategies\":[{\"strategy\":\"REDACT\"}]}]}]}",
            "{\"sheets\":[{\"columns\":[{\"header\":\"A\",\"action\":\"filter\"}]}]}",
            "{\"sheets\":[{\"columns\":[{\"header\":\"A\",\"action\":\"filter\",\"filters\":[]}]}]}",
            "{\"sheets\":[{\"columns\":[{\"header\":\"A\",\"action\":\"filter\",\"filters\":[\"ssn\",\"ssn\"]}]}]}",
            "{\"sheets\":[{\"columns\":[{\"header\":\"A\",\"action\":\"ignore\",\"filters\":[\"ssn\"]}]}]}",
            "{\"sheets\":[{\"columns\":[{\"header\":\"A\",\"action\":\"ignore\",\"other\":1}]}]}",
            "{\"sheets\":[{\"columns\":[{\"header\":\"A\",\"action\":\"ignore\"},{\"header\":\" A \",\"action\":\"redact\",\"strategies\":[{}]}]}]}",
            "{\"sheets\":[{\"columns\":[{\"index\":2,\"action\":\"ignore\"},{\"index\":2,\"action\":\"ignore\"}]}]}",
            "{\"sheets\":[{\"headerRows\":0,\"columns\":[{\"header\":\"A\",\"action\":\"ignore\"}]}]}",
            // Column strategies.
            "{\"sheets\":[{\"columns\":[{\"header\":\"A\",\"action\":\"redact\",\"strategies\":[{\"strategy\":\"REDACT\",\"condition\":\"token == \\\"x\\\"\"}]}]}]}",
            "{\"sheets\":[{\"columns\":[{\"header\":\"A\",\"action\":\"redact\",\"strategies\":[{\"strategy\":\"SHIFT_DATE\"}]}]}]}",
            "{\"sheets\":[{\"columns\":[{\"header\":\"A\",\"action\":\"redact\",\"strategies\":[{\"strategy\":\"redact\"}]}]}]}",
            "{\"sheets\":[{\"columns\":[{\"header\":\"A\",\"action\":\"redact\",\"strategies\":[\"REDACT\"]}]}]}"
    })
    public void refusesAnInvalidSection(final String tabular) {
        final String json = "{\"identifiers\":{\"ssn\":{}},\"tabular\":" + tabular + "}";
        Assertions.assertThrows(JsonParseException.class, () -> load(json));
    }

    @Test
    public void refusesAFilterKeyNotDefinedInIdentifiers() {

        final Policy policy = withSheets("[{\"columns\":[{\"header\":\"Notes\",\"action\":\"filter\",\"filters\":[\"ssn\"]}]}]");
        final FilterPolicyLoader loader = new FilterPolicyLoader(new PhileasConfiguration(new Properties()), new SecureRandom(), null);

        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> loader.getFiltersForPolicy(policy, new HashMap<>()));
        Assertions.assertTrue(ex.getMessage().contains("'ssn'"), ex.getMessage());

    }

    @Test
    public void acceptsAFilterKeyDefinedInIdentifiers() throws Exception {
        final Policy policy = withSheets("[{\"columns\":[{\"header\":\"Notes\",\"action\":\"filter\",\"filters\":[\"emailAddress\"]}]}]");
        final FilterPolicyLoader loader = new FilterPolicyLoader(new PhileasConfiguration(new Properties()), new SecureRandom(), null);
        Assertions.assertFalse(loader.getFiltersForPolicy(policy, new HashMap<>()).isEmpty());
    }

    @Test
    public void selectsTheSheetEntryForAWorksheet() {

        final Tabular tabular = load(WORKBOOK).getTabular();

        // A named entry matches case-insensitively.
        Assertions.assertEquals("Patients", tabular.sheetFor("PATIENTS").getWorksheet());
        Assertions.assertEquals("Codes", tabular.sheetFor("codes").getWorksheet());

        // Other worksheets, and a CSV document, follow the entry without a worksheet.
        Assertions.assertNull(tabular.sheetFor("Billing").getWorksheet());
        Assertions.assertNull(tabular.sheetFor(null).getWorksheet());

    }

    @Test
    public void aCsvDocumentFollowsOnlyTheEntryWithoutAWorksheet() {
        final Tabular tabular = withSheets("[{\"worksheet\":\"Sheet1\"}]").getTabular();
        Assertions.assertNull(tabular.sheetFor(null));
        Assertions.assertNotNull(tabular.sheetFor("sheet1"));
    }

    @Test
    public void resolvesRulesAgainstHeaders() {

        final Tabular.Sheet sheet = withSheets("[{\"columns\":["
                + "{\"header\":\"SSN\",\"action\":\"redact\",\"strategies\":[{}]},"
                + "{\"header\":\"Missing\",\"action\":\"ignore\"},"
                + "{\"index\":4,\"action\":\"ignore\"}]}]").getTabular().getSheets().get(0);

        final Map<Integer, String> headers = Map.of(1, "Name", 2, "SSN", 3, " SSN ", 4, "Notes");
        final Tabular.ResolvedColumns resolved = sheet.resolve(headers);

        // A header rule applies to every column that shares the header, trimmed.
        Assertions.assertEquals(Tabular.Action.REDACT, resolved.ruleFor(2).getAction());
        Assertions.assertEquals(Tabular.Action.REDACT, resolved.ruleFor(3).getAction());
        Assertions.assertEquals(Tabular.Action.IGNORE, resolved.ruleFor(4).getAction());
        Assertions.assertNull(resolved.ruleFor(1));

        // An unmatched header is a warning, not an error.
        Assertions.assertEquals(1, resolved.warnings().size());
        Assertions.assertTrue(resolved.warnings().get(0).contains("'Missing'"), resolved.warnings().get(0));

    }

    @Test
    public void theMoreProtectiveRuleAppliesWhenRulesPickTheSameColumn() {

        final Tabular.Sheet sheet = withSheets("[{\"columns\":["
                + "{\"header\":\"ID\",\"action\":\"ignore\"},"
                + "{\"index\":1,\"action\":\"redact\",\"strategies\":[{}]}]}]").getTabular().getSheets().get(0);

        Assertions.assertEquals(Tabular.Action.REDACT, sheet.resolve(Map.of(1, "ID")).ruleFor(1).getAction());

    }

    @Test
    public void twoFilterRulesOnOneColumnCombineTheirFilters() {

        final Tabular.Sheet sheet = withSheets("[{\"columns\":["
                + "{\"header\":\"Notes\",\"action\":\"filter\",\"filters\":[\"emailAddress\",\"ssn\"]},"
                + "{\"index\":1,\"action\":\"filter\",\"filters\":[\"ssn\",\"phoneNumber\"]}]}]")
                .getTabular().getSheets().get(0);

        final Tabular.Column rule = sheet.resolve(Map.of(1, "Notes")).ruleFor(1);

        Assertions.assertEquals(Tabular.Action.FILTER, rule.getAction());
        Assertions.assertEquals(List.of("emailAddress", "ssn", "phoneNumber"), rule.getFilters());

    }

    @Test
    public void ofTwoRedactRulesOnOneColumnTheFirstListedApplies() {

        final Tabular.Sheet sheet = withSheets("[{\"columns\":["
                + "{\"header\":\"ID\",\"action\":\"redact\",\"strategies\":[{\"strategy\":\"MASK\"}]},"
                + "{\"index\":1,\"action\":\"redact\",\"strategies\":[{\"strategy\":\"REDACT\"}]}]}]")
                .getTabular().getSheets().get(0);

        Assertions.assertEquals(AbstractFilterStrategy.MASK,
                sheet.resolve(Map.of(1, "ID")).ruleFor(1).getStrategies().get(0).getStrategy());

    }

    @Test
    public void restrictsIdentifiersForAFilterRule() {

        final Policy policy = load("{\"identifiers\":{\"emailAddress\":{},\"ssn\":{},\"pheyes\":[{},{}]},"
                + "\"tabular\":{\"sheets\":[{}]}}");

        final Policy restricted = Tabular.restrictIdentifiers(policy, List.of("pheyes", "ssn"));

        Assertions.assertNull(restricted.getTabular());
        Assertions.assertNull(restricted.getIdentifiers().getEmailAddress());
        Assertions.assertNotNull(restricted.getIdentifiers().getSsn());
        Assertions.assertEquals(2, restricted.getIdentifiers().getPhEyes().size());

    }

    @Test
    public void aColumnFilterReplacesTheWholeValue() throws Exception {

        final Policy policy = withSheets("[{\"columns\":[{\"header\":\"SSN\",\"action\":\"redact\",\"strategies\":["
                + "{\"strategy\":\"REDACT\"}]},{\"header\":\"ID\",\"action\":\"redact\",\"strategies\":["
                + "{\"strategy\":\"STATIC_REPLACE\",\"staticReplacement\":\"[%t]\",\"redactionFormat\":\"<%t>\"}]}]}]");
        final FilterPolicyLoader loader = new FilterPolicyLoader(new PhileasConfiguration(new Properties()), new SecureRandom(), null);
        final List<Tabular.Column> columns = policy.getTabular().getSheets().get(0).getColumns();

        final ColumnFilter redact = loader.getColumnFilter(policy, columns.get(0).getStrategies());
        Assertions.assertEquals("{{{REDACTED-full-column}}}",
                redact.replace(new DefaultContextService(), policy, "ctx", "not an SSN").getReplacement());

        final ColumnFilter staticReplace = loader.getColumnFilter(policy, columns.get(1).getStrategies());
        Assertions.assertEquals("[%t]", staticReplace.replace(new DefaultContextService(), policy, "ctx", "x").getReplacement());

    }

}
