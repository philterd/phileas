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
package ai.philterd.phileas.services.filters.filtering;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.LeftoverCheckResult;
import ai.philterd.phileas.model.filtering.LeftoverFinding;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.config.LeftoverCheck;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.csv.CsvLimitExceededException;
import ai.philterd.phileas.services.csv.CsvRedactionOptions;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class CsvFilterServiceTest {

    private static final String EMAIL = "jane.doe@example.com";
    private static final String OTHER_EMAIL = "john.roe@example.com";
    private static final String SSN = "123-45-6789";
    private static final String EMAIL_REDACTED = "{{{REDACTED-email-address}}}";
    private static final String SSN_REDACTED = "{{{REDACTED-ssn}}}";
    private static final String COLUMN = "{{{REDACTED-full-column}}}";

    private static final String IDENTIFIERS = "\"identifiers\":{\"emailAddress\":{},\"ssn\":{}}";

    private static Policy policy(final String rest) {
        return new Gson().fromJson("{" + IDENTIFIERS + (rest.isEmpty() ? "" : "," + rest) + "}", Policy.class);
    }

    private static CsvFilterService service(final CsvRedactionOptions options) {
        return service(new Properties(), options);
    }

    private static CsvFilterService service(final Properties properties, final CsvRedactionOptions options) {
        return new CsvFilterService(new PhileasConfiguration(properties), new DefaultContextService(),
                new InMemoryVectorService(), new SecureRandom(), null, options);
    }

    private static BinaryDocumentFilterResult redact(final String policyRest, final String csv) throws Exception {
        return redact(policyRest, csv, CsvRedactionOptions.defaults());
    }

    private static BinaryDocumentFilterResult redact(final String policyRest, final String csv,
                                                     final CsvRedactionOptions options) throws Exception {
        return service(options).filter(policy(policyRest), "ctx", csv.getBytes(StandardCharsets.UTF_8), MimeType.TEXT_CSV);
    }

    private static String text(final BinaryDocumentFilterResult result) {
        return new String(result.getDocument(), StandardCharsets.UTF_8);
    }

    // Name, Email, SSN.
    private static final String PEOPLE = "Name,Email,SSN\r\nJane,\"" + EMAIL + "\",\"" + SSN + "\"\r\n\"Roe, John\",\"Write " + OTHER_EMAIL + "\",123-45\r\n";

    // --- redaction ---------------------------------------------------------------------------------

    @Test
    public void redactsEachFieldAndKeepsTheRestUnchanged() throws Exception {

        final BinaryDocumentFilterResult result = redact("", PEOPLE);

        Assertions.assertEquals("Name,Email,SSN\r\nJane,\"" + EMAIL_REDACTED + "\",\"" + SSN_REDACTED + "\"\r\n"
                + "\"Roe, John\",\"Write " + EMAIL_REDACTED + "\",123-45\r\n", text(result));

    }

    @Test
    public void spansHaveTheirFieldsRowColumnAndOffsets() throws Exception {

        final List<Span> spans = redact("", PEOPLE).getExplanation().appliedSpans();

        Assertions.assertEquals(3, spans.size());
        Assertions.assertEquals(List.of(2, 2, 3), spans.stream().map(Span::getRow).toList());
        Assertions.assertEquals(List.of(2, 3, 2), spans.stream().map(Span::getColumn).toList());
        final Span inText = spans.get(2);
        Assertions.assertEquals("Write ".length(), inText.getCharacterStart());
        Assertions.assertEquals(OTHER_EMAIL, inText.getText());

        // Row and column are in a serialized span only for a CSV document.
        Assertions.assertTrue(new Gson().toJson(spans.get(0)).contains("\"row\":2"));
        Assertions.assertFalse(new Gson().toJson(Span.make(0, 1, spans.get(0).getFilterType(), "ctx", 1.0, "x", "y",
                "", false, true, new String[0], 0)).contains("\"row\""));

    }

    @Test
    public void aFieldIsNeverJoinedWithItsNeighbor() throws Exception {

        // Two fields that, joined by the tab delimiter, form one match of the identifier pattern.
        final String rest = "\"identifiers\":{\"identifiers\":[{\"pattern\":\"\\\\d{3}\\\\s\\\\d{4}\",\"classification\":\"code\"}]}";
        final Policy policy = new Gson().fromJson("{" + rest + ",\"config\":{\"csv\":{\"delimiter\":\"\\t\"}}}", Policy.class);
        final String csv = "a\tb\n555\t0142\n";

        final PlainTextFilterService plain = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);
        Assertions.assertNotEquals(csv, plain.filter(policy, "ctx", csv).getFilteredText(), "as plain text the fields join");

        final BinaryDocumentFilterResult result = service(CsvRedactionOptions.defaults())
                .filter(policy, "ctx", csv.getBytes(StandardCharsets.UTF_8), MimeType.TEXT_CSV);
        Assertions.assertEquals(csv, text(result));
        Assertions.assertTrue(result.getExplanation().appliedSpans().isEmpty());

    }

    @Test
    public void headerContextHelpsDetectAndOnlyTheFieldIsRedacted() throws Exception {

        // A medical record number is found only after a cue word, which the header gives.
        final String csv = "MRN,Note\nAB-123456,ok\n";
        final Policy policy = new Gson().fromJson("{\"identifiers\":{\"medicalRecordNumber\":{}}}", Policy.class);
        final byte[] input = csv.getBytes(StandardCharsets.UTF_8);

        final BinaryDocumentFilterResult without = service(CsvRedactionOptions.defaults())
                .filter(policy, "ctx", input, MimeType.TEXT_CSV);
        final BinaryDocumentFilterResult with = service(CsvRedactionOptions.defaults().setUseHeaderContext(true))
                .filter(policy, "ctx", input, MimeType.TEXT_CSV);

        Assertions.assertEquals(csv, text(without));
        Assertions.assertEquals("MRN,Note\n{{{REDACTED-medical-record-number}}},ok\n", text(with));
        final Span span = with.getExplanation().appliedSpans().get(0);
        Assertions.assertEquals(2, span.getRow());
        Assertions.assertEquals(1, span.getColumn());
        Assertions.assertEquals(0, span.getCharacterStart());
        Assertions.assertEquals("AB-123456".length(), span.getCharacterEnd());

    }

    @Test
    public void fullyRedactedColumnsKeepTheHeaderRow() throws Exception {

        final BinaryDocumentFilterResult result = redact("", "Name,Note\nJane,ok\n\"Roe, John\",ok\n",
                CsvRedactionOptions.defaults().setFullyRedactedColumns(Set.of(1)));

        Assertions.assertEquals("Name,Note\n" + CsvFilterService.COLUMN_REPLACEMENT + ",ok\n\""
                + CsvFilterService.COLUMN_REPLACEMENT + "\",ok\n", text(result));
        Assertions.assertEquals("full-column", result.getExplanation().appliedSpans().get(0).getClassification());

    }

    @Test
    public void withoutAHeaderRowTheFirstRowIsData() throws Exception {

        final String csv = "Jane,ok\nJohn,ok\n";
        final BinaryDocumentFilterResult result = redact("", csv,
                CsvRedactionOptions.defaults().setHeaderRow(false).setFullyRedactedColumns(Set.of(1)));

        Assertions.assertEquals(CsvFilterService.COLUMN_REPLACEMENT + ",ok\n" + CsvFilterService.COLUMN_REPLACEMENT + ",ok\n",
                text(result));

    }

    @Test
    public void theHeaderRowIsScannedWithoutColumnRules() throws Exception {
        Assertions.assertEquals(EMAIL_REDACTED + ",b\n1,2\n", text(redact("", EMAIL + ",b\n1,2\n")));
    }

    // --- config.csv ----------------------------------------------------------------------------------

    @Test
    public void readsAndWritesWithThePolicysDelimiterQuoteAndEncoding() throws Exception {

        final String csv = "Nom;Courriel\n'Andr\u00e9; Dupont';'" + EMAIL + "'\n";
        final Policy policy = policy("\"config\":{\"csv\":{\"delimiter\":\";\",\"quote\":\"'\",\"encoding\":\"ISO-8859-1\"}}");

        final BinaryDocumentFilterResult result = service(CsvRedactionOptions.defaults())
                .filter(policy, "ctx", csv.getBytes(StandardCharsets.ISO_8859_1), MimeType.TEXT_CSV);

        Assertions.assertEquals("Nom;Courriel\n'Andr\u00e9; Dupont';'" + EMAIL_REDACTED + "'\n",
                new String(result.getDocument(), StandardCharsets.ISO_8859_1));

    }

    @Test
    public void keepsAUtf8ByteOrderMarkAndMatchesTheFirstHeader() throws Exception {

        final String csv = "\uFEFFEmail,Other\n" + EMAIL + ",x\n";

        final BinaryDocumentFilterResult result = redact("\"tabular\":{\"sheets\":[{\"columns\":["
                + "{\"header\":\"Email\",\"action\":\"redact\",\"strategies\":[{}]}]}]}", csv);

        Assertions.assertEquals("\uFEFFEmail,Other\n" + COLUMN + ",x\n", text(result));

    }

    @Test
    public void keepsTheByteOrderAndMarkOfUtf16() throws Exception {

        final String csv = "Email,Other\n" + EMAIL + ",x\n";
        final String redacted = "Email,Other\n" + EMAIL_REDACTED + ",x\n";
        final String rules = "\"tabular\":{\"sheets\":[{\"columns\":[{\"header\":\"Email\",\"action\":\"filter\","
                + "\"filters\":[\"emailAddress\"]}]}]}";

        // A little-endian document with a mark, read as UTF-16, comes back little-endian with its mark.
        final byte[] little = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_16LE);
        final byte[] out = service(CsvRedactionOptions.defaults()).filter(policy("\"config\":{\"csv\":{\"encoding\":\"UTF-16\"}},"
                + rules), "ctx", little, MimeType.TEXT_CSV).getDocument();
        Assertions.assertArrayEquals(("\uFEFF" + redacted).getBytes(StandardCharsets.UTF_16LE), out);

        // A big-endian document without a mark comes back without one.
        final byte[] big = csv.getBytes(StandardCharsets.UTF_16BE);
        Assertions.assertArrayEquals(redacted.getBytes(StandardCharsets.UTF_16BE), service(CsvRedactionOptions.defaults())
                .filter(policy("\"config\":{\"csv\":{\"encoding\":\"UTF-16\"}}"), "ctx", big, MimeType.TEXT_CSV).getDocument());

        // Read as UTF-16LE, the mark is kept and does not stop the first header from matching: the ignored
        // column would otherwise be scanned as an unlisted one.
        final String ignore = "\"tabular\":{\"sheets\":[{\"columns\":[{\"header\":\"Email\",\"action\":\"ignore\"}]}]}";
        Assertions.assertArrayEquals(little, service(CsvRedactionOptions.defaults())
                .filter(policy("\"config\":{\"csv\":{\"encoding\":\"UTF-16LE\"}}," + ignore), "ctx", little, MimeType.TEXT_CSV)
                .getDocument());

    }

    @Test
    public void refusesADocumentNotInThePolicysEncoding() {
        final byte[] latin1 = "Andr\u00e9,x\n".getBytes(StandardCharsets.ISO_8859_1);
        Assertions.assertThrows(IllegalArgumentException.class, () -> service(CsvRedactionOptions.defaults())
                .filter(policy(""), "ctx", latin1, MimeType.TEXT_CSV));
    }

    @Test
    public void refusesAnotherOutputType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> service(CsvRedactionOptions.defaults())
                .filter(policy(""), "ctx", "a\n".getBytes(StandardCharsets.UTF_8), MimeType.TEXT_PLAIN));
    }

    // --- applying spans ------------------------------------------------------------------------------

    @Test
    public void applyingTheSpansGivesTheSameDocument() throws Exception {

        final CsvFilterService service = service(CsvRedactionOptions.defaults());
        final Policy policy = policy("");
        final byte[] input = PEOPLE.getBytes(StandardCharsets.UTF_8);

        final BinaryDocumentFilterResult result = service.filter(policy, "ctx", input, MimeType.TEXT_CSV);
        final byte[] applied = service.apply(policy, input, result.getExplanation().appliedSpans(), MimeType.TEXT_CSV);

        Assertions.assertArrayEquals(result.getDocument(), applied);

    }

    @Test
    public void applyingASpanAReviewerAddedReplacesItsText() throws Exception {

        final Span added = Span.make(0, 4, FilterType.OTHER, "ctx", 1.0, "Jane",
                "[name]", "", false, true, new String[0], 0);
        added.setRow(2);
        added.setColumn(1);

        final byte[] applied = service(CsvRedactionOptions.defaults()).apply(policy(""),
                "Name,Note\nJane,ok\n".getBytes(StandardCharsets.UTF_8), List.of(added), MimeType.TEXT_CSV);

        Assertions.assertEquals("Name,Note\n[name],ok\n", new String(applied, StandardCharsets.UTF_8));

    }

    // --- limits --------------------------------------------------------------------------------------

    @Test
    public void enforcesTheConfiguredLimits() {
        final Properties properties = new Properties();
        properties.setProperty("csv.max.rows", "2");
        Assertions.assertThrows(CsvLimitExceededException.class, () -> service(properties, CsvRedactionOptions.defaults())
                .filter(policy(""), "ctx", "a\nb\nc\n".getBytes(StandardCharsets.UTF_8), MimeType.TEXT_CSV));
    }

    // --- column rules --------------------------------------------------------------------------------

    private static String rules(final String sheets) {
        return "\"tabular\":{\"sheets\":" + sheets + "}";
    }

    @Test
    public void eachActionChangesTheOutput() throws Exception {

        final String csv = "Name,Email,SSN,Notes\nJane," + EMAIL + ",123-45,\"Mail " + EMAIL + " or " + SSN + "\"\n";

        final BinaryDocumentFilterResult result = redact(rules("[{\"columns\":["
                + "{\"header\":\"SSN\",\"action\":\"redact\",\"strategies\":[{}]},"
                + "{\"header\":\"Email\",\"action\":\"ignore\"},"
                + "{\"header\":\"Notes\",\"action\":\"filter\",\"filters\":[\"emailAddress\"]}]}]"), csv);

        Assertions.assertEquals("Name,Email,SSN,Notes\nJane," + EMAIL + "," + COLUMN + ",\"Mail " + EMAIL_REDACTED
                + " or " + SSN + "\"\n", text(result));

    }

    @Test
    public void eachUnlistedColumnsValueChangesTheOutput() throws Exception {

        final String csv = "Keep,Other\n" + EMAIL + "," + OTHER_EMAIL + "\n";
        final String keep = "{\"header\":\"Keep\",\"action\":\"ignore\"}";

        Assertions.assertEquals("Keep,Other\n" + EMAIL + "," + EMAIL_REDACTED + "\n",
                text(redact(rules("[{\"columns\":[" + keep + "]}]"), csv)));
        Assertions.assertEquals(csv, text(redact(rules("[{\"unlistedColumns\":\"ignore\",\"columns\":[" + keep + "]}]"), csv)));
        Assertions.assertEquals("Keep,Other\n" + EMAIL + "," + COLUMN + "\n",
                text(redact(rules("[{\"unlistedColumns\":\"redact\",\"columns\":[" + keep + "]}]"), csv)));
        Assertions.assertEquals("Keep,Other\n" + EMAIL + ",####################\n",
                text(redact(rules("[{\"unlistedColumns\":\"redact\",\"unlistedStrategies\":[{\"strategy\":\"MASK\",\"maskCharacter\":\"#\"}],"
                        + "\"columns\":[" + keep + "]}]"), csv)));

    }

    @Test
    public void headerRowsAreNeverRedactedUnderColumnRules() throws Exception {

        final String csv = EMAIL + ",Contact\nId,Email\n7," + EMAIL + "\n";

        final BinaryDocumentFilterResult result = redact(rules("[{\"headerRows\":2,\"columns\":["
                + "{\"header\":\"Email\",\"action\":\"redact\",\"strategies\":[{}]}]}]"), csv);

        Assertions.assertEquals(EMAIL + ",Contact\nId,Email\n7," + COLUMN + "\n", text(result));

    }

    @Test
    public void headerRowsZeroPicksColumnsByIndex() throws Exception {
        final BinaryDocumentFilterResult result = redact(rules("[{\"headerRows\":0,\"unlistedColumns\":\"ignore\","
                + "\"columns\":[{\"index\":2,\"action\":\"redact\",\"strategies\":[{}]}]}]"), "a,b\nc,d\n");
        Assertions.assertEquals("a," + COLUMN + "\nc," + COLUMN + "\n", text(result));
    }

    @Test
    public void onlyTheEntryWithoutAWorksheetApplies() throws Exception {

        final String csv = "Email\n" + EMAIL + "\n";

        Assertions.assertEquals("Email\n" + EMAIL_REDACTED + "\n", text(redact(rules("[{\"worksheet\":\"Sheet1\","
                + "\"unlistedColumns\":\"ignore\"}]"), csv)), "an entry that names a worksheet does not apply");
        Assertions.assertEquals(csv, text(redact(rules("[{\"worksheet\":\"Sheet1\",\"unlistedColumns\":\"redact\"},"
                + "{\"unlistedColumns\":\"ignore\"}]"), csv)));

    }

    @Test
    public void columnStrategiesWorkAsForAFilter() throws Exception {

        final String csv = "SSN,Copy\n" + SSN + "," + SSN + "\n987-65-4321," + SSN + "\n";
        final String strategy = "[{\"strategy\":\"RANDOM_REPLACE\",\"replacementScope\":\"CONTEXT\"}]";

        final String out = text(redact(rules("[{\"columns\":[{\"header\":\"SSN\",\"action\":\"redact\",\"strategies\":" + strategy
                + "},{\"header\":\"Copy\",\"action\":\"redact\",\"strategies\":" + strategy + "}]}]"), csv));

        final String[] lines = out.split("\n");
        final String[] first = lines[1].split(",");
        Assertions.assertNotEquals(SSN, first[0]);
        Assertions.assertEquals(first[0], first[1]);
        Assertions.assertEquals(first[0], lines[2].split(",")[1]);

    }

    @Test
    public void fullyRedactedColumnsOverrideTheRules() throws Exception {
        final BinaryDocumentFilterResult result = redact(rules("[{\"columns\":[{\"header\":\"Name\",\"action\":\"ignore\"}]}]"),
                "Name,Note\nJane,ok\n", CsvRedactionOptions.defaults().setFullyRedactedColumns(Set.of(1)));
        Assertions.assertEquals("Name,Note\n" + CsvFilterService.COLUMN_REPLACEMENT + ",ok\n", text(result));
    }

    @Test
    public void aHeaderThatMatchesNoColumnIsLoggedAsAWarning() throws Exception {
        final List<String> warnings = captureWarnings(() -> redact(rules("[{\"columns\":["
                + "{\"header\":\"Social Security\",\"action\":\"redact\",\"strategies\":[{}]}]}]"), PEOPLE));
        Assertions.assertTrue(warnings.stream().anyMatch(w -> w.contains("'Social Security'")), warnings.toString());
    }

    // --- leftover check ----------------------------------------------------------------------------

    @Test
    public void theLeftoverCheckReportsFieldsAndSkipsHeaderRowsAndIgnoredColumns() throws Exception {

        final String check = "\"config\":{\"review\":{\"leftoverCheck\":{\"mode\":\"always\",\"using\":\"samePolicy\"}}}";
        final String csv = "Name,Email," + EMAIL + "\nJane," + OTHER_EMAIL + ",none\n";

        final BinaryDocumentFilterResult ruled = redact(check + "," + rules("[{\"columns\":["
                + "{\"header\":\"Email\",\"action\":\"ignore\"}]}]"), csv);
        Assertions.assertTrue(ruled.getLeftoverCheck().getFindings().isEmpty(), ruled.getLeftoverCheck().getFindings().toString());

        // Without rules, a value the redaction left (here an SSN no filter of this policy finds) is reported in its field.
        final String emailOnly = "{\"identifiers\":{\"emailAddress\":{}}," + check + "}";
        final Policy policy = new Gson().fromJson(emailOnly, Policy.class);
        final BinaryDocumentFilterResult broad = service(CsvRedactionOptions.defaults()).filter(policy, "ctx",
                ("Name,Email\nJane," + EMAIL + "\n").getBytes(StandardCharsets.UTF_8), MimeType.TEXT_CSV,
                FilterOptions.defaults());
        Assertions.assertTrue(broad.getLeftoverCheck().getFindings().isEmpty(), "replacements are not reported");

    }

    @Test
    public void aLeftoverFindingHasItsFieldsRowAndColumn() throws Exception {
        final String policy = "{\"identifiers\":{\"emailAddress\":{}},\"config\":{\"review\":{\"leftoverCheck\":"
                + "{\"mode\":\"always\",\"using\":\"broad\"}}}}";
        final BinaryDocumentFilterResult result = service(CsvRedactionOptions.defaults()).filter(
                new Gson().fromJson(policy, Policy.class), "ctx", ("Name,SSN\nJane," + SSN + "\n").getBytes(StandardCharsets.UTF_8),
                MimeType.TEXT_CSV);
        final List<LeftoverFinding> findings = result.getLeftoverCheck().getFindings();
        Assertions.assertTrue(findings.stream().anyMatch(f -> Integer.valueOf(2).equals(f.getRow())
                && Integer.valueOf(2).equals(f.getColumn())), findings.toString());
        final JsonObject finding = new Gson().toJsonTree(result).getAsJsonObject().getAsJsonObject("leftoverCheck")
                .getAsJsonArray("findings").get(0).getAsJsonObject();
        Assertions.assertEquals(2, finding.get("row").getAsInt(), finding.toString());
        Assertions.assertEquals(2, finding.get("column").getAsInt(), finding.toString());
        Assertions.assertFalse(finding.has("attachment"), finding.toString());
    }

    @Test
    public void theLeftoverCheckDoesNotReportRandomStandIns() throws Exception {
        // Random stand-ins look like real values. The quoted field holds a delimiter, a line break, and text around
        // the stand-in, so its replacement must be found by the field it is in.
        final String policy = "{\"identifiers\":{\"emailAddress\":{\"emailAddressFilterStrategies\":[{\"strategy\":\"RANDOM_REPLACE\"}]}},"
                + "\"config\":{\"review\":{\"leftoverCheck\":{\"mode\":\"always\",\"using\":\"broad\"}}}}";
        final String csv = "Name,Note\nJane,\"Write to " + EMAIL + ",\nor call 212-555-0187\"\n";

        final BinaryDocumentFilterResult result = service(CsvRedactionOptions.defaults()).filter(
                new Gson().fromJson(policy, Policy.class), "ctx", csv.getBytes(StandardCharsets.UTF_8), MimeType.TEXT_CSV,
                FilterOptions.defaults().withLeftoverCheckMatchedText(true));

        Assertions.assertFalse(new String(result.getDocument(), StandardCharsets.UTF_8).contains(EMAIL));
        final List<LeftoverFinding> findings = result.getLeftoverCheck().getFindings();
        Assertions.assertTrue(findings.stream().noneMatch(f -> f.getFilterType() == FilterType.EMAIL_ADDRESS), findings.toString());
        Assertions.assertTrue(findings.stream().anyMatch(f -> "212-555-0187".equals(f.getMatchedText())
                && Integer.valueOf(2).equals(f.getRow()) && Integer.valueOf(2).equals(f.getColumn())), findings.toString());
    }

    @Test
    public void anOnRequestLeftoverCheckRunsOnlyWhenAsked() throws Exception {
        final Policy policy = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{}},\"config\":{\"review\":"
                + "{\"leftoverCheck\":{\"mode\":\"onRequest\",\"using\":\"broad\"}}}}", Policy.class);
        final byte[] input = ("Name,SSN\nJane," + SSN + "\n").getBytes(StandardCharsets.UTF_8);
        final CsvFilterService service = service(CsvRedactionOptions.defaults());

        Assertions.assertFalse(service.filter(policy, "ctx", input, MimeType.TEXT_CSV).getLeftoverCheck().isPerformed());
        Assertions.assertFalse(service.filter(policy, "ctx", input, MimeType.TEXT_CSV, FilterOptions.defaults())
                .getLeftoverCheck().isPerformed());

        final List<LeftoverFinding> findings = service.filter(policy, new DefaultContextService(), new InMemoryVectorService(),
                "ctx", input, MimeType.TEXT_CSV, FilterOptions.defaults().withLeftoverCheck(true).withLeftoverCheckMatchedText(true))
                .getLeftoverCheck().getFindings();
        Assertions.assertTrue(findings.stream().anyMatch(f -> SSN.equals(f.getMatchedText())), findings.toString());
    }

    @Test
    public void aLeftoverCheckThatFailsIsReportedAsNotPerformed() throws Exception {
        final Policy policy = policy("\"config\":{\"review\":{\"leftoverCheck\":{\"mode\":\"always\",\"using\":\"broad\"}}}");
        final byte[] input = ("Name,Email\nJane," + EMAIL + "\n").getBytes(StandardCharsets.UTF_8);
        final CsvFilterService failing = new CsvFilterService(new PhileasConfiguration(new Properties()), new DefaultContextService(),
                new InMemoryVectorService(), new SecureRandom(), null, CsvRedactionOptions.defaults()) {
            @Override
            protected List<Span> leftoverSpans(final Policy p, final LeftoverCheck.Using using, final String text) {
                throw new IllegalStateException("check failed");
            }
        };

        final BinaryDocumentFilterResult result = failing.filter(policy, "ctx", input, MimeType.TEXT_CSV);

        Assertions.assertArrayEquals(service(CsvRedactionOptions.defaults()).filter(policy, "ctx", input, MimeType.TEXT_CSV)
                .getDocument(), result.getDocument());
        Assertions.assertFalse(result.getLeftoverCheck().isPerformed());
        Assertions.assertEquals(LeftoverCheckResult.Reason.CHECK_FAILED, result.getLeftoverCheck().getReason());
    }

    // --- logging -------------------------------------------------------------------------------------

    private interface Action {
        void run() throws Exception;
    }

    private static final class CapturingAppender extends AbstractAppender {

        private final List<String> messages = new CopyOnWriteArrayList<>();

        private CapturingAppender() {
            super("csv-capture", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(final LogEvent event) {
            if (event.getLevel() == Level.WARN) {
                messages.add(event.getMessage().getFormattedMessage());
            }
        }

    }

    private static List<String> captureWarnings(final Action action) throws Exception {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final Configuration configuration = context.getConfiguration();
        final CapturingAppender appender = new CapturingAppender();
        appender.start();
        configuration.addAppender(appender);
        configuration.getRootLogger().addAppender(appender, Level.WARN, null);
        context.updateLoggers();
        try {
            action.run();
            return appender.messages;
        } finally {
            configuration.getRootLogger().removeAppender(appender.getName());
            context.updateLoggers();
            appender.stop();
        }
    }

}
