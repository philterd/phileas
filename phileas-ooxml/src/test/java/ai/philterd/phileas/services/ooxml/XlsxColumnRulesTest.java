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
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.Tabular;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.OoxmlFilterService;
import ai.philterd.phileas.services.strategies.ColumnFilterStrategy;
import ai.philterd.phileas.utils.Encryption;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/** A policy's {@code tabular} column rules applied to Excel workbooks. */
public class XlsxColumnRulesTest {

    private static final String EMAIL = "jane.doe@example.com";
    private static final String OTHER_EMAIL = "john.roe@example.com";
    private static final String SSN = "123-45-6789";
    private static final String COLUMN = "{{{REDACTED-full-column}}}";
    private static final String EMAIL_REDACTED = "{{{REDACTED-email-address}}}";

    private static final String CRYPTO_KEY = "9EE7A356FDFE43F069500B0086758346E66D8583E0CE1CFCA04E50F67ECCE5D1";
    private static final String CRYPTO_IV = "B674D3B8F1C025AEFF8F6D5FA1AEAD3A";
    private static final String FPE_KEY = "EF4359D8D580AA4F7F036D6F04FC6A94";
    private static final String FPE_TWEAK = "D8E7920AFA330A73";

    // Name, SSN, Email, Notes.
    private static byte[] patients() {
        return SpreadsheetDocs.createXlsx(List.of(
                new String[]{"Name", "SSN", "Email", "Notes"},
                new String[]{"Jane", SSN, EMAIL, "Reach " + EMAIL + " or " + SSN},
                new String[]{"John", "123-45", OTHER_EMAIL, "none"}), "Patients");
    }

    private static String policy(final String sheets) {
        return "{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}},\"tabular\":{\"sheets\":" + sheets + "}}";
    }

    private static BinaryDocumentFilterResult redact(final String policyJson, final byte[] input) throws Exception {
        return redact(policyJson, input, OoxmlRedactionOptions.defaults(), new DefaultContextService());
    }

    private static BinaryDocumentFilterResult redact(final String policyJson, final byte[] input,
                                                     final OoxmlRedactionOptions options,
                                                     final ContextService contextService) throws Exception {
        final OoxmlFilterService service = new OoxmlFilterService(new PhileasConfiguration(new Properties()),
                contextService, new InMemoryVectorService(), new SecureRandom(), null, options);
        return service.filter(new Gson().fromJson(policyJson, Policy.class), "ctx", input, MimeType.APPLICATION_XLSX);
    }

    private static String cell(final BinaryDocumentFilterResult result, final String reference) {
        return SpreadsheetDocs.cellText(result.getDocument(), reference);
    }

    // --- actions ------------------------------------------------------------------------------

    @Test
    public void redactReplacesEveryDataCellWithoutDetection() throws Exception {

        // "123-45" is a partial SSN no filter matches, and the Email column holds values the email filter
        // would otherwise replace with its own classification.
        final BinaryDocumentFilterResult result = redact(policy("[{\"columns\":["
                + "{\"header\":\"SSN\",\"action\":\"redact\",\"strategies\":[{\"strategy\":\"REDACT\"}]},"
                + "{\"header\":\"Email\",\"action\":\"redact\",\"strategies\":[{\"strategy\":\"REDACT\"}]}]}]"), patients());

        Assertions.assertEquals("SSN", cell(result, "B1"));
        Assertions.assertEquals(COLUMN, cell(result, "B2"));
        Assertions.assertEquals(COLUMN, cell(result, "B3"));
        Assertions.assertEquals(COLUMN, cell(result, "C2"));
        Assertions.assertEquals(COLUMN, cell(result, "C3"));

        final List<Span> column = result.getExplanation().appliedSpans().stream()
                .filter(s -> ColumnFilterStrategy.CLASSIFICATION.equals(s.getClassification())).toList();
        Assertions.assertEquals(4, column.size());
        Assertions.assertEquals("123-45", column.get(2).getText());

    }

    @Test
    public void ignoreLeavesTheColumnUnchanged() throws Exception {

        final BinaryDocumentFilterResult result = redact(policy("[{\"columns\":["
                + "{\"header\":\"Email\",\"action\":\"ignore\"}]}]"), patients());

        Assertions.assertEquals(EMAIL, cell(result, "C2"));
        Assertions.assertEquals(OTHER_EMAIL, cell(result, "C3"));
        // Unlisted columns are still scanned.
        Assertions.assertEquals("{{{REDACTED-ssn}}}", cell(result, "B2"));

    }

    @Test
    public void filterScansWithOnlyTheNamedFilters() throws Exception {

        final BinaryDocumentFilterResult result = redact(policy("[{\"columns\":["
                + "{\"header\":\"Notes\",\"action\":\"filter\",\"filters\":[\"emailAddress\"]}]}]"), patients());

        Assertions.assertEquals("Reach " + EMAIL_REDACTED + " or " + SSN, cell(result, "D2"));

    }

    @Test
    public void aHeaderRuleAndAnIndexRuleThatBothFilterCombineTheirFilters() throws Exception {

        final BinaryDocumentFilterResult result = redact(policy("[{\"columns\":["
                + "{\"header\":\"Notes\",\"action\":\"filter\",\"filters\":[\"emailAddress\"]},"
                + "{\"index\":4,\"action\":\"filter\",\"filters\":[\"ssn\"]}]}]"), patients());

        Assertions.assertEquals("Reach " + EMAIL_REDACTED + " or {{{REDACTED-ssn}}}", cell(result, "D2"));

    }

    @Test
    public void filterUsesEveryFilterUnderAKey() throws Exception {

        final byte[] input = SpreadsheetDocs.createXlsx(List.of(
                new String[]{"Codes", "Other"},
                new String[]{"AB123 and CD456", "AB123 and CD456"}));
        final String json = "{\"identifiers\":{\"identifiers\":["
                + "{\"pattern\":\"AB\\\\d{3}\",\"classification\":\"first\"},"
                + "{\"pattern\":\"CD\\\\d{3}\",\"classification\":\"second\"}],\"emailAddress\":{}},"
                + "\"tabular\":{\"sheets\":[{\"unlistedColumns\":\"ignore\",\"columns\":["
                + "{\"header\":\"Codes\",\"action\":\"filter\",\"filters\":[\"identifiers\"]}]}]}}";

        final BinaryDocumentFilterResult result = redact(json, input);

        Assertions.assertFalse(cell(result, "A2").contains("AB123"), cell(result, "A2"));
        Assertions.assertFalse(cell(result, "A2").contains("CD456"), cell(result, "A2"));
        Assertions.assertEquals("AB123 and CD456", cell(result, "B2"));

    }

    @Test
    public void aFilterKeyNotInIdentifiersIsRefused() {
        final String json = "{\"identifiers\":{\"emailAddress\":{}},\"tabular\":{\"sheets\":[{\"columns\":["
                + "{\"header\":\"Notes\",\"action\":\"filter\",\"filters\":[\"ssn\"]}]}]}}";
        Assertions.assertThrows(IllegalArgumentException.class, () -> redact(json, patients()));
    }

    // --- unlisted columns -------------------------------------------------------------------------

    @Test
    public void unlistedColumnsIdentifiersScansWithEveryFilterAndKeepsTheHeaderRow() throws Exception {

        final byte[] input = SpreadsheetDocs.createXlsx(List.of(
                new String[]{EMAIL, "SSN"},
                new String[]{OTHER_EMAIL, SSN}));

        final BinaryDocumentFilterResult result = redact(policy("[{}]"), input);

        Assertions.assertEquals(EMAIL, cell(result, "A1"));
        Assertions.assertEquals(EMAIL_REDACTED, cell(result, "A2"));
        Assertions.assertEquals("{{{REDACTED-ssn}}}", cell(result, "B2"));

    }

    @Test
    public void unlistedColumnsIgnoreLeavesThemUnchanged() throws Exception {

        final BinaryDocumentFilterResult result = redact(policy("[{\"unlistedColumns\":\"ignore\",\"columns\":["
                + "{\"header\":\"SSN\",\"action\":\"redact\",\"strategies\":[{}]}]}]"), patients());

        Assertions.assertEquals(COLUMN, cell(result, "B2"));
        Assertions.assertEquals(EMAIL, cell(result, "C2"));
        Assertions.assertEquals("Reach " + EMAIL + " or " + SSN, cell(result, "D2"));

    }

    @Test
    public void unlistedColumnsRedactUsesASingleRedactByDefault() throws Exception {

        final BinaryDocumentFilterResult result = redact(policy("[{\"unlistedColumns\":\"redact\",\"columns\":["
                + "{\"header\":\"Name\",\"action\":\"ignore\"}]}]"), patients());

        Assertions.assertEquals("Jane", cell(result, "A2"));
        Assertions.assertEquals(COLUMN, cell(result, "B3"));
        Assertions.assertEquals(COLUMN, cell(result, "C2"));
        Assertions.assertEquals(COLUMN, cell(result, "D3"));
        Assertions.assertEquals("Notes", cell(result, "D1"));

    }

    @Test
    public void unlistedColumnsRedactUsesUnlistedStrategies() throws Exception {

        final BinaryDocumentFilterResult result = redact(policy("[{\"unlistedColumns\":\"redact\","
                + "\"unlistedStrategies\":[{\"strategy\":\"MASK\",\"maskCharacter\":\"#\"}],\"columns\":["
                + "{\"header\":\"Name\",\"action\":\"ignore\"}]}]"), patients());

        Assertions.assertEquals("#".repeat(SSN.length()), cell(result, "B2"));
        Assertions.assertEquals("####", cell(result, "D3"));

    }

    // --- header rows ------------------------------------------------------------------------------

    @Test
    public void headerRowsZeroPicksColumnsByIndexAndRedactsTheFirstRow() throws Exception {

        final byte[] input = SpreadsheetDocs.createXlsx(List.of(
                new String[]{"A-1", SSN},
                new String[]{"A-2", "987-65-4321"}));

        final BinaryDocumentFilterResult result = redact(policy("[{\"headerRows\":0,\"unlistedColumns\":\"ignore\","
                + "\"columns\":[{\"index\":2,\"action\":\"redact\",\"strategies\":[{}]}]}]"), input);

        Assertions.assertEquals("A-1", cell(result, "A1"));
        Assertions.assertEquals(COLUMN, cell(result, "B1"));
        Assertions.assertEquals(COLUMN, cell(result, "B2"));

    }

    @Test
    public void headerRowsTwoMatchesTheLastHeaderRowAndKeepsBoth() throws Exception {

        final byte[] input = SpreadsheetDocs.createXlsx(List.of(
                new String[]{EMAIL, "Contact"},
                new String[]{"Id", "Email"},
                new String[]{"7", EMAIL}));

        final BinaryDocumentFilterResult result = redact(policy("[{\"headerRows\":2,\"columns\":["
                + "{\"header\":\"Email\",\"action\":\"redact\",\"strategies\":[{}]},"
                + "{\"header\":\"Contact\",\"action\":\"ignore\"}]}]"), input);

        Assertions.assertEquals(EMAIL, cell(result, "A1"));
        Assertions.assertEquals("Email", cell(result, "B2"));
        Assertions.assertEquals(COLUMN, cell(result, "B3"));
        Assertions.assertEquals("7", cell(result, "A3"));
        // "Contact" is in the first header row, not the last, so it matches nothing.
        Assertions.assertFalse(redactWithRules(input, "[{\"headerRows\":2,\"columns\":["
                + "{\"header\":\"Contact\",\"action\":\"ignore\"}]}]").warnings().isEmpty());

    }

    // --- header matching ----------------------------------------------------------------------------

    // Applies rules with a filter that replaces every email, and a replacement of "X".
    private static OoxmlRedactionResult redactWithRules(final byte[] input, final String sheets) throws Exception {
        final Tabular tabular = new Gson().fromJson(policy(sheets), Policy.class).getTabular();
        final TextFilter filter = TestFilters.replacing(EMAIL, "E");
        final ColumnRules rules = new ColumnRules() {
            @Override
            public Tabular tabular() {
                return tabular;
            }

            @Override
            public TextFilter filter(final List<String> identifierKeys) {
                return filter;
            }

            @Override
            public String replace(final List<ColumnFilterStrategy> strategies, final String value) {
                return "X";
            }
        };
        return XlsxRedactor.redact(input, filter, OoxmlRedactionOptions.defaults(), rules);
    }

    private static final String REDACT_SSN = "[{\"columns\":[{\"header\":\"SSN\",\"action\":\"redact\",\"strategies\":[{}]}]}]";

    @Test
    public void matchesAFormulaHeaderByItsResult() throws Exception {

        final SpreadsheetDocs.Workbook wb = new SpreadsheetDocs.Workbook(List.of(), "Sheet1");
        wb.sharedStrings = false;
        wb.sheetDataXml = "<sheetData><row r=\"1\"><c r=\"A1\" t=\"str\"><f>\"S\"&amp;\"SN\"</f><v>SSN</v></c></row>"
                + "<row r=\"2\"><c r=\"A2\" t=\"inlineStr\"><is><t>" + SSN + "</t></is></c></row></sheetData>";

        final byte[] output = redactWithRules(wb.toBytes(), REDACT_SSN).document();

        Assertions.assertEquals("X", SpreadsheetDocs.cellText(output, "A2"));

    }

    @Test
    public void matchesARichTextHeaderAsPlainText() throws Exception {

        final SpreadsheetDocs.Workbook wb = new SpreadsheetDocs.Workbook(List.of(), "Sheet1");
        wb.sheetDataXml = "<sheetData><row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c>"
                + "<c r=\"B1\" t=\"inlineStr\"><is><r><t>S</t></r><r><rPr><b/></rPr><t>SN</t></r></is></c></row>"
                + "<row r=\"2\"><c r=\"A2\" t=\"inlineStr\"><is><t>" + SSN + "</t></is></c>"
                + "<c r=\"B2\" t=\"inlineStr\"><is><t>" + SSN + "</t></is></c></row></sheetData>";
        wb.sharedStringsXml = "<sst xmlns=\"" + XlsxRedactor.S + "\"><si><r><t>SS</t></r><r><rPr><i/></rPr><t>N</t></r></si></sst>";

        final byte[] output = redactWithRules(wb.toBytes(), REDACT_SSN).document();

        Assertions.assertEquals("X", SpreadsheetDocs.cellText(output, "A2"));
        Assertions.assertEquals("X", SpreadsheetDocs.cellText(output, "B2"));

    }

    @Test
    public void matchesASharedStringHeaderWithoutItsPhoneticGuide() throws Exception {

        final SpreadsheetDocs.Workbook wb = new SpreadsheetDocs.Workbook(List.of(), "Sheet1");
        wb.sheetDataXml = "<sheetData><row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c></row>"
                + "<row r=\"2\"><c r=\"A2\" t=\"inlineStr\"><is><t>" + SSN + "</t></is></c></row></sheetData>";
        wb.sharedStringsXml = "<sst xmlns=\"" + XlsxRedactor.S + "\"><si><t>\u756a\u53f7</t>"
                + "<rPh sb=\"0\" eb=\"2\"><t>\u30d0\u30f3\u30b4\u30a6</t></rPh><phoneticPr fontId=\"1\"/></si></sst>";

        final byte[] output = redactWithRules(wb.toBytes(), "[{\"columns\":["
                + "{\"header\":\"\u756a\u53f7\",\"action\":\"redact\",\"strategies\":[{}]}]}]").document();

        Assertions.assertEquals("X", SpreadsheetDocs.cellText(output, "A2"));

    }

    @Test
    public void matchesABooleanHeaderAsExcelShowsIt() throws Exception {

        final SpreadsheetDocs.Workbook wb = new SpreadsheetDocs.Workbook(List.of(), "Sheet1");
        wb.sharedStrings = false;
        wb.sheetDataXml = "<sheetData><row r=\"1\"><c r=\"A1\" t=\"b\"><v>1</v></c></row>"
                + "<row r=\"2\"><c r=\"A2\" t=\"inlineStr\"><is><t>" + SSN + "</t></is></c></row></sheetData>";

        final OoxmlRedactionResult result = redactWithRules(wb.toBytes(), "[{\"columns\":["
                + "{\"header\":\"TRUE\",\"action\":\"redact\",\"strategies\":[{}]}]}]");

        Assertions.assertEquals("X", SpreadsheetDocs.cellText(result.document(), "A2"));
        Assertions.assertTrue(result.warnings().isEmpty(), result.warnings().toString());

    }

    @Test
    public void matchesAMergedHeaderByItsTopLeftValue() throws Exception {

        // A1:B1 is one merged "SSN" header over two columns; C1:C2 is merged down over two header rows.
        final SpreadsheetDocs.Workbook wb = new SpreadsheetDocs.Workbook(List.of(), "Sheet1");
        wb.sharedStrings = false;
        wb.sheetDataXml = "<sheetData>"
                + "<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>SSN</t></is></c><c r=\"B1\"/>"
                + "<c r=\"C1\" t=\"inlineStr\"><is><t>SSN</t></is></c></row>"
                + "<row r=\"2\"><c r=\"A2\" t=\"inlineStr\"><is><t>" + SSN + "</t></is></c>"
                + "<c r=\"B2\" t=\"inlineStr\"><is><t>" + SSN + "</t></is></c>"
                + "<c r=\"C2\" t=\"inlineStr\"><is><t>" + SSN + "</t></is></c></row>"
                + "</sheetData>";
        wb.afterSheetData.append("<mergeCells count=\"1\"><mergeCell ref=\"A1:B1\"/></mergeCells>");

        final byte[] output = redactWithRules(wb.toBytes(), REDACT_SSN).document();
        Assertions.assertEquals("X", SpreadsheetDocs.cellText(output, "A2"));
        Assertions.assertEquals("X", SpreadsheetDocs.cellText(output, "B2"));

        final SpreadsheetDocs.Workbook down = new SpreadsheetDocs.Workbook(List.of(), "Sheet1");
        down.sharedStrings = false;
        down.sheetDataXml = "<sheetData>"
                + "<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>SSN</t></is></c></row>"
                + "<row r=\"2\"><c r=\"A2\"/></row>"
                + "<row r=\"3\"><c r=\"A3\" t=\"inlineStr\"><is><t>" + SSN + "</t></is></c></row>"
                + "</sheetData>";
        down.afterSheetData.append("<mergeCells count=\"1\"><mergeCell ref=\"A1:A2\"/></mergeCells>");

        final byte[] downOutput = redactWithRules(down.toBytes(), "[{\"headerRows\":2,\"columns\":["
                + "{\"header\":\"SSN\",\"action\":\"redact\",\"strategies\":[{}]}]}]").document();
        Assertions.assertEquals("X", SpreadsheetDocs.cellText(downOutput, "A3"));

    }

    @Test
    public void trimsHeadersAndAppliesARuleToEveryColumnSharingIt() throws Exception {

        final byte[] input = SpreadsheetDocs.createXlsx(List.of(
                new String[]{"  SSN ", "Name", "SSN"},
                new String[]{SSN, "Jane", "987-65-4321"}));

        final OoxmlRedactionResult result = redactWithRules(input, REDACT_SSN);

        Assertions.assertEquals("X", SpreadsheetDocs.cellText(result.document(), "A2"));
        Assertions.assertEquals("Jane", SpreadsheetDocs.cellText(result.document(), "B2"));
        Assertions.assertEquals("X", SpreadsheetDocs.cellText(result.document(), "C2"));
        Assertions.assertTrue(result.warnings().isEmpty(), result.warnings().toString());

    }

    @Test
    public void reportsAHeaderThatMatchesNoColumnAsAWarning() throws Exception {

        final OoxmlRedactionResult result = redactWithRules(patients(), "[{\"columns\":["
                + "{\"header\":\"Social Security\",\"action\":\"redact\",\"strategies\":[{}]}]}]");

        Assertions.assertEquals(List.of(RedactionWarning.unmatchedHeader("Social Security", null)), result.warnings());
        // The rest of the sheet is still redacted.
        Assertions.assertEquals("E", SpreadsheetDocs.cellText(result.document(), "C2"));

    }

    // --- sheet selection ----------------------------------------------------------------------------

    private static byte[] twoSheets() {
        return SpreadsheetDocs.createXlsxWithSecondSheet(
                List.of(new String[]{"Name", "Email"}, new String[]{"Jane", EMAIL}), "Patients",
                List.of(new String[]{"Company", "Email"}, new String[]{"Acme", EMAIL}), "Vendors");
    }

    private static String sheetCell(final byte[] xlsx, final String entry, final String reference) {
        final List<String> shared = new ArrayList<>();
        if (TestPackage.entries(xlsx).containsKey("xl/sharedStrings.xml")) {
            for (final Element si : OoxmlPackage.children(TestPackage.dom(xlsx, "xl/sharedStrings.xml").getDocumentElement(),
                    XlsxRedactor.S, "si")) {
                shared.add(si.getTextContent());
            }
        }
        for (final Element cell : TestPackage.elements(xlsx, entry, XlsxRedactor.S, "c")) {
            if (reference.equals(cell.getAttribute("r"))) {
                return SpreadsheetDocs.readCell(cell, shared);
            }
        }
        return null;
    }

    @Test
    public void aNamedEntryMatchesCaseInsensitivelyAndOtherSheetsFollowTheUnnamedEntry() throws Exception {

        final BinaryDocumentFilterResult result = redact(policy("[{\"worksheet\":\"PATIENTS\",\"columns\":["
                + "{\"header\":\"Email\",\"action\":\"ignore\"}]},{\"unlistedColumns\":\"redact\"}]"), twoSheets());

        final byte[] output = result.getDocument();
        Assertions.assertEquals(EMAIL, sheetCell(output, "xl/worksheets/sheet1.xml", "B2"));
        Assertions.assertEquals(COLUMN, sheetCell(output, "xl/worksheets/sheet2.xml", "A2"));
        Assertions.assertEquals(COLUMN, sheetCell(output, "xl/worksheets/sheet2.xml", "B2"));
        Assertions.assertEquals("Company", sheetCell(output, "xl/worksheets/sheet2.xml", "A1"));

    }

    @Test
    public void aSheetWithoutAnEntryIsRedactedAsWithoutRules() throws Exception {

        final BinaryDocumentFilterResult result = redact(policy("[{\"worksheet\":\"Patients\",\"columns\":["
                + "{\"header\":\"Email\",\"action\":\"ignore\"}]}]"), twoSheets());

        final byte[] output = result.getDocument();
        Assertions.assertEquals(EMAIL, sheetCell(output, "xl/worksheets/sheet1.xml", "B2"));
        Assertions.assertEquals(EMAIL_REDACTED, sheetCell(output, "xl/worksheets/sheet2.xml", "B2"));
        Assertions.assertEquals("Acme", sheetCell(output, "xl/worksheets/sheet2.xml", "A2"));

    }

    // --- strategies ---------------------------------------------------------------------------------

    @Test
    public void replacementScopeContextGivesTheSameValueTheSameReplacement() throws Exception {

        final byte[] input = SpreadsheetDocs.createXlsx(List.of(
                new String[]{"SSN", "Other SSN"},
                new String[]{SSN, SSN},
                new String[]{"987-65-4321", SSN}));
        final String json = policy("[{\"unlistedColumns\":\"ignore\",\"columns\":["
                + "{\"header\":\"SSN\",\"action\":\"redact\",\"strategies\":[{\"strategy\":\"RANDOM_REPLACE\",\"replacementScope\":\"CONTEXT\"}]},"
                + "{\"header\":\"Other SSN\",\"action\":\"redact\",\"strategies\":[{\"strategy\":\"RANDOM_REPLACE\",\"replacementScope\":\"CONTEXT\"}]}]}]");
        final ContextService context = new DefaultContextService();

        final BinaryDocumentFilterResult result = redact(json, input, OoxmlRedactionOptions.defaults(), context);

        final String a2 = cell(result, "A2");
        Assertions.assertNotEquals(SSN, a2);
        Assertions.assertEquals(a2, cell(result, "B2"));
        Assertions.assertEquals(a2, cell(result, "B3"));
        Assertions.assertNotEquals(a2, cell(result, "A3"));

        // And across documents in the same context.
        Assertions.assertEquals(a2, cell(redact(json, input, OoxmlRedactionOptions.defaults(), context), "A2"));

    }

    @Test
    public void cryptoReplaceIsReversible() throws Exception {

        final String json = "{\"crypto\":{\"key\":\"" + CRYPTO_KEY + "\",\"iv\":\"" + CRYPTO_IV + "\"},"
                + "\"identifiers\":{},\"tabular\":{\"sheets\":[{\"columns\":["
                + "{\"header\":\"SSN\",\"action\":\"redact\",\"strategies\":[{\"strategy\":\"CRYPTO_REPLACE\"}]}]}]}}";

        final String replaced = cell(redact(json, patients()), "B2");

        Assertions.assertTrue(replaced.startsWith("{{") && replaced.endsWith("}}"), replaced);
        Assertions.assertEquals(SSN, Encryption.decrypt(replaced.substring(2, replaced.length() - 2),
                new Crypto(CRYPTO_KEY, CRYPTO_IV)));

    }

    @Test
    public void oneServiceKeepsTheColumnFiltersOfDifferentPoliciesApart() throws Exception {

        final String otherKey = "1EE7A356FDFE43F069500B0086758346E66D8583E0CE1CFCA04E50F67ECCE5D1";
        final String template = "{\"crypto\":{\"key\":\"%s\",\"iv\":\"" + CRYPTO_IV + "\"},"
                + "\"identifiers\":{},\"tabular\":{\"sheets\":[{\"columns\":["
                + "{\"header\":\"SSN\",\"action\":\"redact\",\"strategies\":[{\"strategy\":\"CRYPTO_REPLACE\"}]}]}]}}";
        final OoxmlFilterService service = new OoxmlFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);

        for (final String key : new String[]{CRYPTO_KEY, otherKey, CRYPTO_KEY}) {
            final Policy policy = new Gson().fromJson(String.format(template, key), Policy.class);
            final String replaced = SpreadsheetDocs.cellText(
                    service.filter(policy, "ctx", patients(), MimeType.APPLICATION_XLSX).getDocument(), "B2");
            Assertions.assertEquals(SSN, Encryption.decrypt(replaced.substring(2, replaced.length() - 2),
                    new Crypto(key, CRYPTO_IV)));
        }

    }

    @Test
    public void fpeEncryptReplaceIsReversible() throws Exception {

        final byte[] input = SpreadsheetDocs.createXlsx(List.of(
                new String[]{"Account"},
                new String[]{"4111111111111111"}));
        final String json = "{\"fpe\":{\"key\":\"" + FPE_KEY + "\",\"tweak\":\"" + FPE_TWEAK + "\"},"
                + "\"identifiers\":{},\"tabular\":{\"sheets\":[{\"columns\":["
                + "{\"header\":\"Account\",\"action\":\"redact\",\"strategies\":[{\"strategy\":\"FPE_ENCRYPT_REPLACE\"}]}]}]}}";

        final String replaced = cell(redact(json, input), "A2");

        Assertions.assertNotEquals("4111111111111111", replaced);
        Assertions.assertEquals("4111111111111111",
                Encryption.formatPreservingDecrypt(new FPE(FPE_KEY, FPE_TWEAK), replaced));

    }

    @Test
    public void redactionFormatExpandsTypeToFullColumn() throws Exception {

        final BinaryDocumentFilterResult result = redact(policy("[{\"columns\":["
                + "{\"header\":\"SSN\",\"action\":\"redact\",\"strategies\":[{\"strategy\":\"REDACT\",\"redactionFormat\":\"[%t]\"}]}]}]"),
                patients());

        Assertions.assertEquals("[full-column]", cell(result, "B2"));

    }

    // --- per-call options -----------------------------------------------------------------------------

    @Test
    public void fullyRedactedColumnsOverrideTheRules() throws Exception {

        final BinaryDocumentFilterResult result = redact(policy("[{\"columns\":["
                        + "{\"header\":\"Name\",\"action\":\"ignore\"}]}]"), patients(),
                OoxmlRedactionOptions.defaults().setFullyRedactedColumns(Set.of(1)), new DefaultContextService());

        Assertions.assertEquals("Name", cell(result, "A1"));
        Assertions.assertEquals(XlsxRedactor.COLUMN_REPLACEMENT, cell(result, "A2"));

    }

    @Test
    public void theWorksheetOptionRestrictsRedactionToOneSheet() throws Exception {

        final BinaryDocumentFilterResult result = redact(policy("[{\"unlistedColumns\":\"redact\"}]"), twoSheets(),
                OoxmlRedactionOptions.defaults().setWorksheet("Vendors"), new DefaultContextService());

        final byte[] output = result.getDocument();
        Assertions.assertEquals("Jane", sheetCell(output, "xl/worksheets/sheet1.xml", "A2"));
        Assertions.assertEquals(EMAIL, sheetCell(output, "xl/worksheets/sheet1.xml", "B2"));
        Assertions.assertEquals(COLUMN, sheetCell(output, "xl/worksheets/sheet2.xml", "A2"));

    }

    // --- embedded workbooks ---------------------------------------------------------------------------

    // A partial SSN no filter detects, in a column the policy redacts whole, and an email in a column it ignores.
    private static byte[] innerWorkbook() {
        return SpreadsheetDocs.createXlsx(List.of(
                new String[]{"SSN", "Email"},
                new String[]{"123-45", EMAIL}));
    }

    private static final String EMBEDDED_POLICY = policy("[{\"columns\":["
            + "{\"header\":\"SSN\",\"action\":\"redact\",\"strategies\":[{}]},"
            + "{\"header\":\"Email\",\"action\":\"ignore\"}]}]");

    private static void assertRulesApplied(final byte[] inner) {
        Assertions.assertEquals("SSN", SpreadsheetDocs.cellText(inner, "A1"));
        Assertions.assertEquals(COLUMN, SpreadsheetDocs.cellText(inner, "A2"));
        Assertions.assertEquals(EMAIL, SpreadsheetDocs.cellText(inner, "B2"));
    }

    private static byte[] entry(final byte[] pkg, final String name) {
        return TestPackage.entries(pkg).get(name);
    }

    @Test
    public void aWorkbookEmbeddedInAnExcelChartFollowsTheRules() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithChartEmbeddedWorkbook(
                List.of(new String[]{"SSN"}, new String[]{SSN}), innerWorkbook());

        final byte[] output = redact(EMBEDDED_POLICY, input).getDocument();

        assertRulesApplied(entry(output, "xl/embeddings/Microsoft_Excel_Worksheet1.xlsx"));
    }

    @Test
    public void aWorkbookEmbeddedInAnExcelWorkbookFollowsTheRules() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithEmbeddedPackage(
                List.of(new String[]{"SSN"}, new String[]{SSN}), innerWorkbook(), TestPackage.CT_XLSX);

        final byte[] output = redact(EMBEDDED_POLICY, input).getDocument();

        assertRulesApplied(entry(output, "xl/embeddings/package1.xlsx"));
    }

    private static BinaryDocumentFilterResult redactWord(final String policyJson, final byte[] input) throws Exception {
        final OoxmlFilterService service = new OoxmlFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);
        return service.filter(new Gson().fromJson(policyJson, Policy.class), "ctx", input, MimeType.APPLICATION_DOCX);
    }

    @Test
    public void aWorkbookEmbeddedInAWordChartFollowsTheRules() throws Exception {
        final byte[] input = WordDocs.createWithChartEmbeddedWorkbook(innerWorkbook(), "Series", "Category");

        final byte[] output = redactWord(EMBEDDED_POLICY, input).getDocument();

        assertRulesApplied(entry(output, "word/embeddings/Microsoft_Excel_Worksheet1.xlsx"));
    }

    @Test
    public void aWorkbookEmbeddedInAWordDocumentFollowsTheRules() throws Exception {
        final byte[] input = OoxmlTestFixtures.docxWithEmbeddedXlsx("Body.", innerWorkbook());

        final byte[] output = redactWord(EMBEDDED_POLICY, input).getDocument();

        assertRulesApplied(entry(output, "word/embeddings/Microsoft_Excel_Worksheet.xlsx"));
    }

    @Test
    public void applyingSpansRedactsEmbeddedWorkbooksWithTheRules() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithChartEmbeddedWorkbook(
                List.of(new String[]{"Name"}, new String[]{"Jane"}), innerWorkbook());
        final OoxmlFilterService service = new OoxmlFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);

        final byte[] output = service.apply(new Gson().fromJson(EMBEDDED_POLICY, Policy.class), input, List.of(),
                MimeType.APPLICATION_XLSX);

        assertRulesApplied(entry(output, "xl/embeddings/Microsoft_Excel_Worksheet1.xlsx"));
    }

    @Test
    public void theLeftoverCheckSkipsIgnoredColumnsOfAnEmbeddedWorkbook() throws Exception {
        final String json = "{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}},"
                + "\"config\":{\"review\":{\"leftoverCheck\":{\"mode\":\"always\",\"using\":\"samePolicy\"}}},"
                + "\"tabular\":{\"sheets\":[{\"columns\":[{\"header\":\"Email\",\"action\":\"ignore\"}]}]}}";
        final byte[] input = WordDocs.createWithChartEmbeddedWorkbook(innerWorkbook(), "Series", "Category");

        final BinaryDocumentFilterResult result = redactWord(json, input);

        Assertions.assertEquals(EMAIL, SpreadsheetDocs.cellText(
                entry(result.getDocument(), "word/embeddings/Microsoft_Excel_Worksheet1.xlsx"), "B2"));
        Assertions.assertTrue(result.getLeftoverCheck().getFindings().isEmpty(),
                result.getLeftoverCheck().getFindings().toString());
    }

    // --- leftover check -------------------------------------------------------------------------------

    @Test
    public void theLeftoverCheckSkipsHeaderRowsAndIgnoredColumns() throws Exception {

        final String json = "{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}},"
                + "\"config\":{\"review\":{\"leftoverCheck\":{\"mode\":\"always\",\"using\":\"samePolicy\"}}},"
                + "\"tabular\":{\"sheets\":[{\"columns\":[{\"header\":\"Email\",\"action\":\"ignore\"}]}]}}";
        final byte[] input = SpreadsheetDocs.createXlsx(List.of(
                new String[]{"Name", "Email", EMAIL},
                new String[]{"Jane", OTHER_EMAIL, "none"}));

        final BinaryDocumentFilterResult result = redact(json, input);

        Assertions.assertEquals(EMAIL, cell(result, "C1"));
        Assertions.assertEquals(OTHER_EMAIL, cell(result, "B2"));
        Assertions.assertNotNull(result.getLeftoverCheck());
        Assertions.assertTrue(result.getLeftoverCheck().getFindings().isEmpty(),
                result.getLeftoverCheck().getFindings().toString());

    }

}
