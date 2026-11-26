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
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.ooxml.OoxmlRedactionOptions;
import ai.philterd.phileas.services.ooxml.OoxmlTestFixtures;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class OoxmlFilterServiceTest {

    private static final String EMAIL = "jane.doe@example.com";
    private static final Policy POLICY = new Gson().fromJson(
            "{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}}}", Policy.class);

    private static OoxmlFilterService service() {
        return new OoxmlFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);
    }

    @Test
    public void redactsWordDocument() throws Exception {
        final byte[] input = OoxmlTestFixtures.docx("Keep this.", "Email " + EMAIL + " now.", "SSN 123-45-6789.");

        final BinaryDocumentFilterResult result = service().filter(POLICY, "ctx", input, MimeType.APPLICATION_DOCX);

        final String document = OoxmlTestFixtures.entry(result.getDocument(), "word/document.xml");
        Assertions.assertFalse(document.contains(EMAIL));
        Assertions.assertFalse(document.contains("123-45-6789"));
        Assertions.assertTrue(document.contains("Keep this."));

        final List<Span> spans = result.getExplanation().appliedSpans();
        Assertions.assertEquals(2, spans.size());
        final Span email = spans.get(0);
        Assertions.assertEquals(1, email.getParagraphNumber());
        Assertions.assertEquals("Email ".length(), email.getCharacterStart());
        Assertions.assertEquals(EMAIL, email.getText());
        Assertions.assertEquals(2, spans.get(1).getParagraphNumber());
    }

    @Test
    public void redactsExcelWorkbook() throws Exception {
        final byte[] input = OoxmlTestFixtures.xlsx(List.of(
                new String[]{"Name", "Email"},
                new String[]{"Jane", EMAIL}));

        final BinaryDocumentFilterResult result = service().filter(POLICY, "ctx", input, null);

        Assertions.assertFalse(OoxmlTestFixtures.allXml(result.getDocument()).contains(EMAIL));
        final List<Span> spans = result.getExplanation().appliedSpans();
        Assertions.assertEquals(1, spans.size());
        Assertions.assertEquals(3, spans.get(0).getParagraphNumber()); // the fourth cell
    }

    @Test
    public void writesUnchangedPartsBackUnchanged() throws Exception {
        final byte[] input = OoxmlTestFixtures.docxWithHeader("Plain header", "Email " + EMAIL + ".");

        final byte[] output = service().filter(POLICY, "ctx", input, null).getDocument();

        final Map<String, byte[]> before = OoxmlTestFixtures.entries(input);
        final Map<String, byte[]> after = OoxmlTestFixtures.entries(output);
        Assertions.assertEquals(List.copyOf(before.keySet()), List.copyOf(after.keySet()));
        for (final String name : before.keySet()) {
            if (name.equals("word/document.xml")) {
                Assertions.assertFalse(new String(after.get(name), StandardCharsets.UTF_8).contains(EMAIL));
            } else {
                Assertions.assertArrayEquals(before.get(name), after.get(name), name);
            }
        }
    }

    @Test
    public void doesNotAddCorePropertiesOrContentTypeOverrides() throws Exception {
        // A package whose default content type for .xml is the main part's, which is the case POI's own
        // save adds a "/.xml" override for.
        final byte[] input = OoxmlTestFixtures.docxWithMainPartDefaultContentType("Email " + EMAIL + ".");

        final byte[] output = service().filter(POLICY, "ctx", input, null).getDocument();

        final Map<String, byte[]> after = OoxmlTestFixtures.entries(output);
        Assertions.assertFalse(after.containsKey("docProps/core.xml"));
        final String contentTypes = new String(after.get("[Content_Types].xml"), StandardCharsets.UTF_8);
        Assertions.assertFalse(contentTypes.contains("PartName=\"/.xml\""));
        Assertions.assertArrayEquals(OoxmlTestFixtures.entries(input).get("[Content_Types].xml"), after.get("[Content_Types].xml"));
    }

    @Test
    public void applyReappliesSpansByPosition() throws Exception {
        final OoxmlFilterService service = service();
        final byte[] input = OoxmlTestFixtures.docx("Email " + EMAIL + " now.");
        final List<Span> spans = service.filter(POLICY, "ctx", input, null).getExplanation().appliedSpans();

        final byte[] output = service.apply(POLICY, input, spans, MimeType.APPLICATION_DOCX);

        final String document = OoxmlTestFixtures.entry(output, "word/document.xml");
        Assertions.assertFalse(document.contains(EMAIL));
        Assertions.assertTrue(document.contains(spans.get(0).getReplacement()));
    }

    @Test
    public void applyRedactsHyperlinkTargetsWithThePolicy() throws Exception {
        final byte[] input = OoxmlTestFixtures.docxWithHyperlink("write to us", "mailto:" + EMAIL);

        final byte[] output = service().apply(POLICY, input, List.of(), null);

        Assertions.assertFalse(OoxmlTestFixtures.allXml(output).contains(EMAIL));
    }

    @Test
    public void rejectsAnOutputFormatDifferentFromTheInput() {
        final byte[] input = OoxmlTestFixtures.docx("Email " + EMAIL + ".");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service().filter(POLICY, "ctx", input, MimeType.APPLICATION_PDF));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service().filter(POLICY, "ctx", input, MimeType.APPLICATION_XLSX));
    }

    @Test
    public void rejectsAZipThatIsNotAWordOrExcelDocument() {
        final byte[] notOffice = OoxmlTestFixtures.zip("readme.txt", "Email " + EMAIL);
        Assertions.assertThrows(IllegalArgumentException.class, () -> service().filter(POLICY, "ctx", notOffice, null));
        final byte[] notZip = ("Email " + EMAIL).getBytes(StandardCharsets.UTF_8);
        Assertions.assertThrows(IllegalArgumentException.class, () -> service().filter(POLICY, "ctx", notZip, null));
    }

    @Test
    public void warmServiceRequiresPerCallServices() {
        final OoxmlFilterService warm = new OoxmlFilterService(new PhileasConfiguration(new Properties()), null);
        final byte[] input = OoxmlTestFixtures.docx("text");
        Assertions.assertThrows(IllegalStateException.class, () -> warm.filter(POLICY, "ctx", input, null));
        Assertions.assertDoesNotThrow(() -> warm.filter(POLICY, new DefaultContextService(), new InMemoryVectorService(),
                "ctx", input, null));
    }

    @Test
    public void reportsFullyRedactedColumnsAsSpansThatApplyReapplies() throws Exception {
        final OoxmlFilterService service = new OoxmlFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), new SecureRandom(), null,
                OoxmlRedactionOptions.defaults().setFullyRedactedColumns(Set.of(1)));
        final byte[] input = OoxmlTestFixtures.xlsx(List.of(new String[]{"Name"}, new String[]{"Zebulon Quixote"}));

        final List<Span> spans = service.filter(POLICY, "ctx", input, null).getExplanation().appliedSpans();

        Assertions.assertEquals(1, spans.size());
        Assertions.assertEquals("full-column", spans.get(0).getClassification());
        Assertions.assertEquals(1, spans.get(0).getParagraphNumber());
        final byte[] applied = service.apply(POLICY, input, spans, null);
        Assertions.assertFalse(OoxmlTestFixtures.allXml(applied).contains("Zebulon"));
    }

    @Test
    public void filterFailureInAnEmbeddedWorkbookPropagates() {
        final byte[] workbook = OoxmlTestFixtures.xlsx(List.<String[]>of(new String[]{"cell text"}));
        final byte[] input = OoxmlTestFixtures.docxWithEmbeddedXlsx("Body text.", workbook);
        final ai.philterd.phileas.services.ooxml.TextFilter failing = text -> {
            if (text.equals("cell text")) {
                throw new IllegalStateException("filter unavailable");
            }
            return ai.philterd.phileas.services.ooxml.OoxmlTestFixtures.unchanged(text);
        };

        Assertions.assertThrows(IllegalStateException.class, () -> ai.philterd.phileas.services.ooxml.WordDocumentRedactor
                .redact(input, failing, OoxmlRedactionOptions.defaults()));
    }

    @Test
    public void sameInputGivesTheSameOutput() throws Exception {
        final byte[] input = OoxmlTestFixtures.docxWithHeader("Plain header", "Email " + EMAIL + ".");
        final byte[] first = service().filter(POLICY, "ctx", input, null).getDocument();
        Thread.sleep(2100); // past the zip format's two-second time resolution
        final byte[] second = service().filter(POLICY, "ctx", input, null).getDocument();
        Assertions.assertArrayEquals(first, second);
    }

    @Test
    public void rejectsStrictOpenXml() {
        final byte[] input = OoxmlTestFixtures.strictDocx("Email " + EMAIL + ".");
        Assertions.assertThrows(IllegalArgumentException.class, () -> service().filter(POLICY, "ctx", input, null));
    }

    @Test
    public void reportsReviewSelection() throws Exception {
        final byte[] input = OoxmlTestFixtures.docx("Email " + EMAIL + " now.");
        final Policy sampled = new Gson().fromJson(
                "{\"identifiers\":{\"emailAddress\":{}},\"config\":{\"review\":{\"samplingRate\":1.0}}}", Policy.class);

        final BinaryDocumentFilterResult notSampledResult = service().filter(POLICY, "ctx", input, MimeType.APPLICATION_DOCX);
        final BinaryDocumentFilterResult sampledResult = service().filter(sampled, "ctx", input, MimeType.APPLICATION_DOCX);

        Assertions.assertFalse(notSampledResult.isReviewSampled());
        Assertions.assertTrue(sampledResult.isReviewSampled());
        Assertions.assertEquals(OoxmlTestFixtures.entry(notSampledResult.getDocument(), "word/document.xml"),
                OoxmlTestFixtures.entry(sampledResult.getDocument(), "word/document.xml"));
    }


    private static final FilterOptions LEFTOVER_CHECK = FilterOptions.defaults().withLeftoverCheck(true).withLeftoverCheckMatchedText(true);

    private static Policy leftoverCheckPolicy(final String identifiers, final String using) {
        return new Gson().fromJson("{\"identifiers\":" + identifiers + ",\"config\":{\"review\":{\"leftoverCheck\":"
                + "{\"mode\":\"onRequest\",\"using\":\"" + using + "\"}}}}", Policy.class);
    }

    @Test
    public void leftoverCheckFindsWhatIsLeftInAWordDocument() throws Exception {
        final byte[] input = OoxmlTestFixtures.docx("Keep this.", "Email " + EMAIL + " now.", "SSN 123-45-6789.");
        final Policy policy = leftoverCheckPolicy("{\"emailAddress\":{\"emailAddressFilterStrategies\":[{\"strategy\":\"RANDOM_REPLACE\"}]}}", "broad");

        final BinaryDocumentFilterResult result = service().filter(policy, new DefaultContextService(), new InMemoryVectorService(),
                "ctx", input, MimeType.APPLICATION_DOCX, LEFTOVER_CHECK);

        final LeftoverCheckResult check = result.getLeftoverCheck();
        Assertions.assertTrue(check.isPerformed());

        final LeftoverFinding ssn = check.getFindings().stream()
                .filter(finding -> finding.getFilterType() == FilterType.SSN).findFirst().orElseThrow();
        Assertions.assertEquals(2, ssn.getParagraphNumber());
        Assertions.assertEquals("SSN ".length(), ssn.getCharacterStart());
        Assertions.assertEquals("123-45-6789", ssn.getMatchedText());

        // The random stand-in for the email address is not reported.
        Assertions.assertTrue(check.getFindings().stream().noneMatch(finding -> finding.getFilterType() == FilterType.EMAIL_ADDRESS),
                check.getFindings().toString());

        // The check does not change the document.
        final BinaryDocumentFilterResult unchecked = service().filter(policy, "ctx", input, MimeType.APPLICATION_DOCX);
        Assertions.assertFalse(unchecked.getLeftoverCheck().isPerformed());
        Assertions.assertEquals(OoxmlTestFixtures.entry(unchecked.getDocument(), "word/document.xml").contains("123-45-6789"),
                OoxmlTestFixtures.entry(result.getDocument(), "word/document.xml").contains("123-45-6789"));
        Assertions.assertEquals(unchecked.getExplanation().appliedSpans().size(), result.getExplanation().appliedSpans().size());
    }

    @Test
    public void leftoverCheckFindsWhatIsLeftInAWorkbook() throws Exception {
        final byte[] input = OoxmlTestFixtures.xlsx(List.of(
                new String[]{"Name", "Email"},
                new String[]{"123-45-6789", EMAIL}));

        final BinaryDocumentFilterResult broad = service().filter(leftoverCheckPolicy("{\"emailAddress\":{}}", "broad"),
                new DefaultContextService(), new InMemoryVectorService(), "ctx", input, null, LEFTOVER_CHECK);
        final LeftoverFinding ssn = broad.getLeftoverCheck().getFindings().stream()
                .filter(finding -> finding.getFilterType() == FilterType.SSN).findFirst().orElseThrow();
        Assertions.assertEquals(2, ssn.getParagraphNumber()); // the third cell
        Assertions.assertTrue(broad.getLeftoverCheck().getFindings().stream()
                .noneMatch(finding -> finding.getFilterType() == FilterType.EMAIL_ADDRESS));

        final BinaryDocumentFilterResult samePolicy = service().filter(leftoverCheckPolicy("{\"emailAddress\":{}}", "samePolicy"),
                new DefaultContextService(), new InMemoryVectorService(), "ctx", input, null, LEFTOVER_CHECK);
        Assertions.assertTrue(samePolicy.getLeftoverCheck().isPerformed());
        Assertions.assertEquals(0, samePolicy.getLeftoverCheck().getFindingCount(), samePolicy.getLeftoverCheck().getFindings().toString());
    }

    private static final String RANDOM_EMAIL = "{\"emailAddress\":{\"emailAddressFilterStrategies\":[{\"strategy\":\"RANDOM_REPLACE\"}]}}";

    @Test
    public void leftoverCheckDoesNotReportReplacementsInAWorkbookCell() throws Exception {
        final byte[] input = OoxmlTestFixtures.xlsx(List.of(
                new String[]{"Contact"},
                new String[]{"Write to " + EMAIL + " or call 212-555-0187."}));

        final BinaryDocumentFilterResult result = service().filter(leftoverCheckPolicy(RANDOM_EMAIL, "broad"),
                new DefaultContextService(), new InMemoryVectorService(), "ctx", input, null, LEFTOVER_CHECK);

        final List<LeftoverFinding> findings = result.getLeftoverCheck().getFindings();
        Assertions.assertTrue(findings.stream().noneMatch(finding -> finding.getFilterType() == FilterType.EMAIL_ADDRESS),
                findings.toString());
        final LeftoverFinding phone = findings.stream()
                .filter(finding -> finding.getFilterType() == FilterType.PHONE_NUMBER).findFirst().orElseThrow();
        Assertions.assertEquals(1, phone.getParagraphNumber());
        Assertions.assertEquals("212-555-0187", phone.getMatchedText());
    }

    @Test
    public void leftoverCheckOfTextOutsideParagraphs() throws Exception {

        // A field instruction is not a paragraph, so its findings have no paragraph.
        final byte[] input = OoxmlTestFixtures.docxWithSimpleField("HYPERLINK \"mailto:" + EMAIL + "\"", "write to us");
        final BinaryDocumentFilterResult found = service().filter(leftoverCheckPolicy("{\"ssn\":{}}", "broad"),
                new DefaultContextService(), new InMemoryVectorService(), "ctx", input, MimeType.APPLICATION_DOCX, LEFTOVER_CHECK);
        final LeftoverFinding email = found.getLeftoverCheck().getFindings().stream()
                .filter(finding -> finding.getFilterType() == FilterType.EMAIL_ADDRESS).findFirst().orElseThrow();
        Assertions.assertEquals(-1, email.getParagraphNumber());
        Assertions.assertEquals(EMAIL, email.getMatchedText());

        // The random stand-in the redaction put in the instruction is not reported.
        final BinaryDocumentFilterResult replaced = service().filter(leftoverCheckPolicy(RANDOM_EMAIL, "samePolicy"),
                new DefaultContextService(), new InMemoryVectorService(), "ctx", input, MimeType.APPLICATION_DOCX, LEFTOVER_CHECK);
        Assertions.assertFalse(OoxmlTestFixtures.allXml(replaced.getDocument()).contains(EMAIL));
        Assertions.assertEquals(0, replaced.getLeftoverCheck().getFindingCount(), replaced.getLeftoverCheck().getFindings().toString());

        // A hyperlink target the policy flags is replaced whole with a placeholder, which is not reported, and one
        // it leaves is reported whole.
        final byte[] link = OoxmlTestFixtures.docxWithHyperlink("write to us", "mailto:" + EMAIL);
        Assertions.assertEquals(0, service().filter(leftoverCheckPolicy(RANDOM_EMAIL, "samePolicy"), new DefaultContextService(),
                new InMemoryVectorService(), "ctx", link, MimeType.APPLICATION_DOCX, LEFTOVER_CHECK).getLeftoverCheck().getFindingCount());
        final LeftoverFinding target = service().filter(leftoverCheckPolicy("{\"ssn\":{}}", "broad"), new DefaultContextService(),
                new InMemoryVectorService(), "ctx", link, MimeType.APPLICATION_DOCX, LEFTOVER_CHECK).getLeftoverCheck().getFindings()
                .stream().filter(finding -> finding.getFilterType() == FilterType.EMAIL_ADDRESS).findFirst().orElseThrow();
        Assertions.assertEquals(-1, target.getParagraphNumber());
        Assertions.assertEquals("mailto:" + EMAIL, target.getMatchedText());

    }

    @Test
    public void leftoverCheckHoldsMatchedTextOnlyWhenAsked() throws Exception {
        final byte[] input = OoxmlTestFixtures.docx("SSN 123-45-6789.");
        final BinaryDocumentFilterResult result = service().filter(leftoverCheckPolicy("{\"emailAddress\":{}}", "broad"),
                new DefaultContextService(), new InMemoryVectorService(), "ctx", input, MimeType.APPLICATION_DOCX,
                FilterOptions.defaults().withLeftoverCheck(true));
        Assertions.assertTrue(result.getLeftoverCheck().getFindingCount() > 0);
        result.getLeftoverCheck().getFindings().forEach(finding -> Assertions.assertNull(finding.getMatchedText()));
    }

}
