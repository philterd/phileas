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
import ai.philterd.phileas.model.filtering.LeftoverCheckResult;
import ai.philterd.phileas.model.filtering.LeftoverFinding;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextSegment;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.FilterOptions;
import ai.philterd.phileas.services.filters.filtering.OoxmlFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class OfficeTextOutputTest {

    private static final FilterOptions TEXT = FilterOptions.defaults().withOutput(FilterOptions.Output.TEXT);

    private static final String EMAIL = "test@example.com";
    private static final String PHONE = "212-555-0187";
    private static final String NAME = "Ann Example";

    private static final String EMAIL_AND_PHONE = "\"emailAddress\":{},\"phoneNumber\":{}";

    private static Policy policy(final String identifiers, final String config) {
        return new Gson().fromJson("{\"identifiers\":{" + identifiers + "}"
                + (config == null ? "" : ",\"config\":{" + config + "}") + "}", Policy.class);
    }

    private static BinaryDocumentFilterResult filter(final Policy policy, final byte[] input, final MimeType type,
                                                     final FilterOptions options) throws Exception {
        return filter(new OoxmlFilterService(new PhileasConfiguration(new Properties()), null), policy, input, type, options);
    }

    private static BinaryDocumentFilterResult filter(final OoxmlFilterService service, final Policy policy,
                                                     final byte[] input, final MimeType type,
                                                     final FilterOptions options) throws Exception {
        return service.filter(policy, new DefaultContextService(), new InMemoryVectorService(), "ctx", input, type, options);
    }

    private static TextSegment segment(final BinaryDocumentFilterResult result, final String label) {
        return result.getText().getSegments().stream().filter(s -> s.getLabel().equals(label)).findFirst()
                .orElseThrow(() -> new AssertionError(label + " in " + result.getText()));
    }

    private static List<String> labels(final BinaryDocumentFilterResult result) {
        return result.getText().getSegments().stream().map(TextSegment::getLabel).toList();
    }

    // --- Word ----------------------------------------------------------------------------------

    @Test
    public void wordBodyIsOneParagraphPerLineInPlaceOfTheDocument() throws Exception {

        final byte[] input = WordDocs.create("Call " + PHONE + " today.", "", "Mail " + EMAIL + ".");

        final BinaryDocumentFilterResult result = filter(policy(EMAIL_AND_PHONE, null), input, MimeType.APPLICATION_DOCX, TEXT);

        Assertions.assertNull(result.getDocument());
        Assertions.assertEquals(List.of(new TextSegment("body",
                "Call {{{REDACTED-phone-number}}} today.\n\nMail {{{REDACTED-email-address}}}.", TextSegment.Status.TEXT)),
                result.getText().getSegments());
        Assertions.assertEquals(2, result.getExplanation().appliedSpans().size());

    }

    @Test
    public void wordTextMatchesTheRedactedDocumentsParagraphs() throws Exception {

        final byte[] input = WordDocs.create("Call " + PHONE + " today.", "Mail " + EMAIL + " or " + EMAIL + ".", "Nothing.");
        final Policy policy = policy(EMAIL_AND_PHONE, null);

        final BinaryDocumentFilterResult text = filter(policy, input, MimeType.APPLICATION_DOCX, TEXT);
        final BinaryDocumentFilterResult document = filter(policy, input, MimeType.APPLICATION_DOCX, FilterOptions.defaults());

        Assertions.assertEquals(String.join("\n", WordDocumentRedactor.readParagraphs(document.getDocument())),
                segment(text, "body").getText());

    }

    @Test
    public void wordHeadersAndFootersAreLabeledAndLeftOutWhenKept() throws Exception {

        final byte[] input = WordDocs.createWithHeaderFooter("Header " + EMAIL, "Footer " + PHONE, "Body.");

        final BinaryDocumentFilterResult redacted = filter(policy(EMAIL_AND_PHONE, null), input, MimeType.APPLICATION_DOCX, TEXT);
        Assertions.assertEquals(List.of("body", "headers", "footers"), labels(redacted));
        Assertions.assertEquals("Header {{{REDACTED-email-address}}}", segment(redacted, "headers").getText());
        Assertions.assertEquals("Footer {{{REDACTED-phone-number}}}", segment(redacted, "footers").getText());

        final BinaryDocumentFilterResult kept = filter(policy(EMAIL_AND_PHONE, "\"word\":{\"headersFooters\":\"keep\"}"),
                input, MimeType.APPLICATION_DOCX, TEXT);
        Assertions.assertEquals(new TextSegment("headers", "", TextSegment.Status.NOT_SCANNED), segment(kept, "headers"));
        Assertions.assertEquals(new TextSegment("footers", "", TextSegment.Status.NOT_SCANNED), segment(kept, "footers"));
        Assertions.assertFalse(kept.getText().join("\n").contains(EMAIL));
        Assertions.assertFalse(kept.getText().join("\n").contains(PHONE));

    }

    @Test
    public void wordTrackedDeletionsAreRedactedWhenKeptAndGoneWhenAccepted() throws Exception {

        final String identifiers = "\"dictionaries\":[{\"terms\":[\"" + NAME + "\"]}]";
        final byte[] input = WordDocs.createWithTrackedDeletion("Signed by " + NAME + ".", "Body.");

        final BinaryDocumentFilterResult kept = filter(policy(identifiers, "\"word\":{\"trackedChanges\":\"redact\"}"),
                input, MimeType.APPLICATION_DOCX, TEXT);
        final TextSegment deleted = segment(kept, "tracked changes");
        Assertions.assertEquals(TextSegment.Status.TEXT, deleted.getStatus());
        Assertions.assertTrue(deleted.getText().startsWith("Signed by {{{REDACTED-"), deleted.getText());
        Assertions.assertFalse(kept.getText().join("\n").contains(NAME), kept.getText().join("\n"));

        final BinaryDocumentFilterResult accepted = filter(policy(identifiers, null), input, MimeType.APPLICATION_DOCX, TEXT);
        Assertions.assertEquals(List.of("body"), labels(accepted));
        Assertions.assertFalse(accepted.getText().join("\n").contains(NAME));

    }

    @Test
    public void wordCommentsAreRedactedWhenKeptAndGoneWhenRemoved() throws Exception {

        final byte[] input = WordDocs.createWithComment("Reach me at " + EMAIL, "Body.");

        final BinaryDocumentFilterResult kept = filter(policy(EMAIL_AND_PHONE, "\"word\":{\"comments\":\"redact\"}"),
                input, MimeType.APPLICATION_DOCX, TEXT);
        Assertions.assertEquals("Reach me at {{{REDACTED-email-address}}}", segment(kept, "comments").getText());

        final BinaryDocumentFilterResult removed = filter(policy(EMAIL_AND_PHONE, null), input, MimeType.APPLICATION_DOCX, TEXT);
        Assertions.assertEquals(List.of("body"), labels(removed));

    }

    @Test
    public void wordFootnotesAndEndnotesAreLabeled() throws Exception {

        final byte[] input = WordDocs.createWithNotes("Footnote " + EMAIL, "Endnote " + PHONE, "Body.");

        final BinaryDocumentFilterResult result = filter(policy(EMAIL_AND_PHONE, null), input, MimeType.APPLICATION_DOCX, TEXT);

        // The separator notes Word keeps above the notes are left out.
        Assertions.assertEquals("Footnote {{{REDACTED-email-address}}}", segment(result, "footnotes").getText());
        Assertions.assertEquals("Endnote {{{REDACTED-phone-number}}}", segment(result, "endnotes").getText());

    }

    @Test
    public void wordTextBoxFallbackIsNotRepeated() throws Exception {

        final String box = "<w:p><w:r><mc:AlternateContent xmlns:mc=\"http://schemas.openxmlformats.org/markup-compatibility/2006\">"
                + "<mc:Choice Requires=\"wps\"><w:drawing><w:txbxContent>" + WordDocs.paras("Box " + EMAIL) + "</w:txbxContent></w:drawing></mc:Choice>"
                + "<mc:Fallback><w:pict><w:txbxContent>" + WordDocs.paras("Box " + EMAIL) + "</w:txbxContent></w:pict></mc:Fallback>"
                + "</mc:AlternateContent></w:r></w:p>";
        final byte[] input = WordDocs.createRaw(WordDocs.paras("Body.") + box);

        final BinaryDocumentFilterResult result = filter(policy(EMAIL_AND_PHONE, null), input, MimeType.APPLICATION_DOCX, TEXT);

        final String body = segment(result, "body").getText();
        Assertions.assertEquals(1, TestPackage.occurrences(body, "Box {{{REDACTED-email-address}}}"), body);
        Assertions.assertFalse(body.contains(EMAIL), body);

    }

    @Test
    public void wordDetectsOnceForEachParagraphAsTheDocumentRedactionDoes() throws Exception {

        final byte[] input = WordDocs.createWithHeaderFooter("Header " + EMAIL, "Footer.", "Mail " + EMAIL + ".", "", "Done.");

        final List<String> textCalls = new ArrayList<>();
        WordDocumentRedactor.redactToText(input, TestFilters.recording(TestFilters.email(), textCalls),
                OoxmlRedactionOptions.defaults(), false);

        final List<String> documentCalls = new ArrayList<>();
        WordDocumentRedactor.redact(input, TestFilters.recording(TestFilters.email(), documentCalls),
                OoxmlRedactionOptions.defaults());

        Assertions.assertEquals(List.of("Mail " + EMAIL + ".", "Done.", "Header " + EMAIL, "Footer."), textCalls);
        Assertions.assertEquals(documentCalls, textCalls);

    }

    @Test
    public void excelDetectsOnceForEachCellAsTheDocumentRedactionDoes() throws Exception {

        final byte[] input = SpreadsheetDocs.createXlsx(List.of(new String[]{"Email", "Note"}, new String[]{EMAIL, "none"}), "Sheet1");

        final List<String> textCalls = new ArrayList<>();
        XlsxRedactor.redactToText(input, TestFilters.recording(TestFilters.email(), textCalls),
                OoxmlRedactionOptions.defaults(), null, false);

        final List<String> documentCalls = new ArrayList<>();
        XlsxRedactor.redact(input, TestFilters.recording(TestFilters.email(), documentCalls), OoxmlRedactionOptions.defaults());

        Assertions.assertEquals(4, textCalls.size(), textCalls.toString());
        Assertions.assertEquals(documentCalls, textCalls);

    }

    @Test
    public void wordTextHasEachSpansRandomReplacement() throws Exception {

        final Policy policy = policy("\"emailAddress\":{\"emailAddressFilterStrategies\":[{\"strategy\":\"RANDOM_REPLACE\"}]}", null);
        final byte[] input = WordDocs.create("Mail " + EMAIL + ".", "Or " + EMAIL + " again.");

        final BinaryDocumentFilterResult result = filter(policy, input, MimeType.APPLICATION_DOCX, TEXT);

        final String body = segment(result, "body").getText();
        Assertions.assertFalse(body.contains(EMAIL), body);
        Assertions.assertEquals(2, result.getExplanation().appliedSpans().size());
        for (final Span span : result.getExplanation().appliedSpans()) {
            Assertions.assertTrue(body.contains(span.getReplacement()), body);
        }

    }

    @Test
    public void wordMetadataFollowsTheDocumentPropertiesSetting() throws Exception {

        final TestPackage pkg = WordDocs.docx(WordDocs.paras("Body."));
        OfficePolicySettingsTest.addProperties(pkg);
        final byte[] input = pkg.toBytes();

        // Left out unless asked for.
        Assertions.assertEquals(List.of("body"), labels(filter(policy(EMAIL_AND_PHONE, null), input,
                MimeType.APPLICATION_DOCX, TEXT)));

        // The properties are removed by default, so none are included.
        Assertions.assertEquals(new TextSegment("metadata", "", TextSegment.Status.NOT_SCANNED), segment(filter(
                policy(EMAIL_AND_PHONE, null), input, MimeType.APPLICATION_DOCX, TEXT.withTextMetadata(true)), "metadata"));

        // Kept: the content properties, redacted, and never the author.
        final BinaryDocumentFilterResult kept = filter(policy("\"dictionaries\":[{\"terms\":[\"Example Client\"]}]",
                "\"word\":{\"removeDocumentProperties\":false}"), input, MimeType.APPLICATION_DOCX, TEXT.withTextMetadata(true));
        final String metadata = segment(kept, "metadata").getText();
        Assertions.assertTrue(metadata.startsWith("title: Quarterly notes\nsubject: Planning\nkeywords: budget\n"
                + "category: Finance\ndescription: Draft for review\nClient: {{{REDACTED-"), metadata);
        Assertions.assertFalse(metadata.contains("Example Client"), metadata);
        Assertions.assertFalse(metadata.contains("Example Author"), metadata);

    }

    @Test
    public void wordLeftoverCheckRunsOverTheText() throws Exception {

        final Policy policy = policy("\"emailAddress\":{\"emailAddressFilterStrategies\":[{\"strategy\":\"RANDOM_REPLACE\"}]}",
                "\"review\":{\"leftoverCheck\":{\"mode\":\"always\",\"using\":\"broad\"}}");
        final byte[] input = WordDocs.createWithHeaderFooter("Call " + PHONE, "Footer.", "Mail " + EMAIL + ".");

        final BinaryDocumentFilterResult result = filter(policy, input, MimeType.APPLICATION_DOCX,
                TEXT.withLeftoverCheckMatchedText(true));

        final LeftoverCheckResult check = result.getLeftoverCheck();
        Assertions.assertTrue(check.isPerformed());
        // The random stand-in for the address is not reported; the phone number the policy leaves is.
        Assertions.assertTrue(check.getFindings().stream().noneMatch(f -> f.getFilterType() == FilterType.EMAIL_ADDRESS),
                check.getFindings().toString());
        final LeftoverFinding phone = check.getFindings().stream().filter(f -> PHONE.equals(f.getMatchedText()))
                .findFirst().orElseThrow();
        Assertions.assertEquals("headers", phone.getSegment());
        Assertions.assertEquals(PHONE, segment(result, "headers").getText()
                .substring(phone.getCharacterStart(), phone.getCharacterEnd()));

    }

    @Test
    public void textOutputWritesNoDocument() throws Exception {

        // Writing the redacted package fails, so only a call that never writes one succeeds.
        final OoxmlFilterService service = new OoxmlFilterService(new PhileasConfiguration(new Properties()), null) {
            @Override
            protected OoxmlRedactionResult redactDocument(final OoxmlDocumentType type, final byte[] input,
                                                          final TextFilter textFilter, final OoxmlRedactionOptions options,
                                                          final ColumnRules columnRules) {
                throw new IllegalStateException("wrote the document");
            }
        };
        final Policy policy = policy(EMAIL_AND_PHONE, null);

        for (final Object[] input : new Object[][]{
                {WordDocs.create("Mail " + EMAIL + "."), MimeType.APPLICATION_DOCX},
                {SpreadsheetDocs.createXlsx(List.of(new String[]{"Email"}, new String[]{EMAIL}), "Sheet1"), MimeType.APPLICATION_XLSX}}) {
            final byte[] document = (byte[]) input[0];
            final MimeType type = (MimeType) input[1];
            final BinaryDocumentFilterResult text = filter(service, policy, document, type, TEXT);
            Assertions.assertNull(text.getDocument());
            Assertions.assertFalse(text.getText().join("\n").contains(EMAIL));
            Assertions.assertThrows(IllegalStateException.class,
                    () -> filter(service, policy, document, type, FilterOptions.defaults()));
        }

    }

    @Test
    public void theOutputTypeIsNotCheckedForText() throws Exception {
        final BinaryDocumentFilterResult result = filter(policy(EMAIL_AND_PHONE, null), WordDocs.create("Body."),
                MimeType.APPLICATION_XLSX, TEXT);
        Assertions.assertEquals("Body.", segment(result, "body").getText());
    }

    // --- Excel ---------------------------------------------------------------------------------

    @Test
    public void excelRowsAreHeaderValueLinesPerSheet() throws Exception {

        final byte[] input = SpreadsheetDocs.createXlsxWithSecondSheet(List.of(
                        new String[]{"Name", "Email", "Phone"},
                        new String[]{"Row one", EMAIL, PHONE},
                        new String[]{"Row two", "", "none"}), "People",
                List.of(new String[]{"Note"}, new String[]{"Call " + PHONE}), "Notes");

        final BinaryDocumentFilterResult result = filter(policy(EMAIL_AND_PHONE, null), input, MimeType.APPLICATION_XLSX, TEXT);

        Assertions.assertNull(result.getDocument());
        Assertions.assertEquals(List.of(
                new TextSegment("sheet: People", "Name: Row one\nEmail: {{{REDACTED-email-address}}}\n"
                        + "Phone: {{{REDACTED-phone-number}}}\n\nName: Row two\nPhone: none", TextSegment.Status.TEXT),
                new TextSegment("sheet: Notes", "Note: Call {{{REDACTED-phone-number}}}", TextSegment.Status.TEXT)),
                result.getText().getSegments());

    }

    @Test
    public void excelColumnRulesApplyAsInDocumentRedaction() throws Exception {

        final byte[] input = SpreadsheetDocs.createXlsx(List.of(
                new String[]{"ID", "Contact", "Comment"},
                new String[]{"A-1", EMAIL, "Mail " + EMAIL}), "Sheet1");
        final String tabular = "\"tabular\":{\"sheets\":[{\"columns\":["
                + "{\"header\":\"ID\",\"action\":\"ignore\"},{\"header\":\"Contact\",\"action\":\"redact\",\"strategies\":[{\"strategy\":\"REDACT\"}]}]}]}";
        final Policy policy = new Gson().fromJson("{\"identifiers\":{" + EMAIL_AND_PHONE + "}," + tabular + "}", Policy.class);

        final BinaryDocumentFilterResult text = filter(policy, input, MimeType.APPLICATION_XLSX, TEXT);
        final BinaryDocumentFilterResult document = filter(policy, input, MimeType.APPLICATION_XLSX, FilterOptions.defaults());

        // The ignored column is left unscanned, so it is left out; the redacted column has the rule's replacement.
        final String sheet = segment(text, "sheet: Sheet1").getText();
        final String contact = document.getExplanation().appliedSpans().stream()
                .filter(span -> EMAIL.equals(span.getText()) && span.getCharacterStart() == 0)
                .findFirst().orElseThrow().getReplacement();
        Assertions.assertEquals("Contact: " + contact + "\nComment: Mail {{{REDACTED-email-address}}}", sheet);

    }

    @Test
    public void excelHeaderContextIsUsedAndAnEmptyHeaderIsTheColumnLetter() throws Exception {

        final byte[] input = SpreadsheetDocs.createXlsx(List.of(
                new String[]{"Email", ""},
                new String[]{EMAIL, "free text"}), "Sheet1");

        final BinaryDocumentFilterResult result = filter(policy(EMAIL_AND_PHONE, null), input, MimeType.APPLICATION_XLSX, TEXT);

        Assertions.assertEquals("Email: {{{REDACTED-email-address}}}\nB: free text", segment(result, "sheet: Sheet1").getText());

    }

    @Test
    public void excelLeftoverCheckRunsOverTheText() throws Exception {

        final Policy policy = policy("\"emailAddress\":{}", "\"review\":{\"leftoverCheck\":{\"mode\":\"always\",\"using\":\"broad\"}}");
        final byte[] input = SpreadsheetDocs.createXlsx(List.of(
                new String[]{"Email", "Phone"},
                new String[]{EMAIL, PHONE}), "Sheet1");

        final BinaryDocumentFilterResult result = filter(policy, input, MimeType.APPLICATION_XLSX, TEXT.withLeftoverCheckMatchedText(true));

        final List<LeftoverFinding> findings = result.getLeftoverCheck().getFindings();
        Assertions.assertTrue(findings.stream().noneMatch(f -> f.getFilterType() == FilterType.EMAIL_ADDRESS), findings.toString());
        final LeftoverFinding phone = findings.stream().filter(f -> PHONE.equals(f.getMatchedText())).findFirst().orElseThrow();
        Assertions.assertEquals("sheet: Sheet1", phone.getSegment());
        Assertions.assertEquals(PHONE, segment(result, "sheet: Sheet1").getText()
                .substring(phone.getCharacterStart(), phone.getCharacterEnd()));

    }

    @Test
    public void anEmptyWorkbookHasAnEmptySegmentAndIsNotChecked() throws Exception {

        final Policy policy = policy("\"emailAddress\":{}", "\"review\":{\"leftoverCheck\":{\"mode\":\"always\"}}");
        final byte[] input = SpreadsheetDocs.createXlsx(List.of(), "Empty");

        final BinaryDocumentFilterResult result = filter(policy, input, MimeType.APPLICATION_XLSX, TEXT);

        Assertions.assertEquals(List.of(new TextSegment("sheet: Empty", "", TextSegment.Status.NO_TEXT)), result.getText().getSegments());
        Assertions.assertEquals(List.of("sheet: Empty"), result.getText().getSegmentsWithoutText());
        Assertions.assertEquals(LeftoverCheckResult.Reason.UNREADABLE_OUTPUT, result.getLeftoverCheck().getReason());

    }

}
