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
package ai.philterd.phileas.services.pdf;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.filtering.PdfFilterService;
import com.google.gson.Gson;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.mockito.Mockito.mock;

public class PdfPagePoliciesFilterTest {

    private static final String SSN = "123-45-6789";
    private static final String EMAIL = "jane.doe@example.com";

    private static final String[] PAGES = {
            "Patient SSN " + SSN + " on file.",
            "Email " + EMAIL + " today.",
            "Exhibit SSN " + SSN + " and " + EMAIL + " here.",
            "Contact " + EMAIL + " again."
    };

    private static final String SSN_ONLY = "{\"identifiers\":{\"ssn\":{}}}";
    private static final String EMAIL_ONLY = "{\"identifiers\":{\"emailAddress\":{}}}";

    private static PdfFilterService service() {
        return new PdfFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), mock(VectorService.class), null);
    }

    private static Policy policy(final String json) {
        return new Gson().fromJson(json, Policy.class);
    }

    @Test
    public void eachPageIsFilteredWithItsPolicyAndUnassignedPagesWithTheDefault() throws Exception {

        final PdfPagePolicies pagePolicies = new PdfPagePolicies().assign("2-3", policy(EMAIL_ONLY), "exhibits");

        final BinaryDocumentFilterResult result = service().filter(policy(SSN_ONLY), "ctx", document(),
                MimeType.APPLICATION_PDF, pagePolicies);

        final List<String> found = new ArrayList<>();
        for (final Span span : result.getExplanation().appliedSpans()) {
            found.add(span.getPageNumber() + " " + span.getText() + " " + span.getPageAssignment());
        }

        // Page numbers are zero-based indexes into the original document, as without page policies.
        Assertions.assertEquals(List.of(
                "0 " + SSN + " default",
                "1 " + EMAIL + " exhibits",
                "2 " + EMAIL + " exhibits"), found);

    }

    @Test
    public void oneContextSpansPoliciesForConsistentReplacement() throws Exception {

        final String ssn = "\"ssn\":{\"ssnFilterStrategies\":[{\"strategy\":\"RANDOM_REPLACE\",\"replacementScope\":\"CONTEXT\"}]}";
        final Policy defaultPolicy = policy("{\"identifiers\":{" + ssn + "}}");
        final Policy exhibitPolicy = policy("{\"identifiers\":{" + ssn + ",\"emailAddress\":{}}}");

        final BinaryDocumentFilterResult result = service().filter(defaultPolicy, "ctx", document(),
                MimeType.APPLICATION_PDF, new PdfPagePolicies().assign("3", exhibitPolicy, "exhibit"));

        final List<Span> ssnSpans = result.getExplanation().appliedSpans().stream()
                .filter(s -> s.getText().equals(SSN)).toList();

        Assertions.assertEquals(2, ssnSpans.size());
        Assertions.assertEquals("default", ssnSpans.get(0).getPageAssignment());
        Assertions.assertEquals("exhibit", ssnSpans.get(1).getPageAssignment());
        Assertions.assertNotEquals(SSN, ssnSpans.get(0).getReplacement());
        Assertions.assertEquals(ssnSpans.get(0).getReplacement(), ssnSpans.get(1).getReplacement());

    }

    @Test
    public void boundingBoxesApplyOnlyToTheirPolicysPagesAndRenderingComesFromTheDefault() throws Exception {

        final String box = "{\"page\":%d,\"x\":60,\"y\":690,\"w\":400,\"h\":30}";
        final Policy defaultPolicy = policy("{\"config\":{\"pdf\":{\"preserveUnredactedPages\":true}},"
                + "\"graphical\":{\"boundingBoxes\":[" + box.formatted(1) + "," + box.formatted(3) + "]}}");
        final Policy boxPolicy = policy("{\"graphical\":{\"boundingBoxes\":[" + box.formatted(2) + "," + box.formatted(4) + "]}}");
        final Policy notPreserving = policy("{\"config\":{\"pdf\":{\"preserveUnredactedPages\":false}}}");

        final PdfPagePolicies pagePolicies = new PdfPagePolicies()
                .assign("2", boxPolicy, "boxes")
                .assign("3", notPreserving, "plain");

        final byte[] output = service().filter(defaultPolicy, "ctx", document(), MimeType.APPLICATION_PDF, pagePolicies)
                .getDocument();

        // Page 1: the default's box. Page 2: the assigned policy's box. Page 3: the default's box is not drawn on a
        // page assigned to another policy, and the page is kept because rendering settings come from the default.
        // Page 4: the assigned policy's box for page 4 is outside its pages.
        Assertions.assertEquals(List.of("", "", PAGES[2], PAGES[3]), pageText(output));

    }

    @Test
    public void pagesBeyondTheDocumentFailBeforeRedaction() {

        final PdfPagePolicies pagePolicies = new PdfPagePolicies().assign("4-6", policy(EMAIL_ONLY), "exhibits");

        final IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> service().filter(policy(SSN_ONLY), "ctx", document(), MimeType.APPLICATION_PDF, pagePolicies));
        Assertions.assertEquals("The document has 4 pages, but pages beyond it are assigned a policy: 5-6 ('exhibits').",
                e.getMessage());

    }

    @Test
    public void withoutPagePoliciesSpansHaveNoAssignment() throws Exception {

        final BinaryDocumentFilterResult result = service().filter(policy(SSN_ONLY), "ctx", document(), MimeType.APPLICATION_PDF);

        Assertions.assertEquals(2, result.getExplanation().appliedSpans().size());
        for (final Span span : result.getExplanation().appliedSpans()) {
            Assertions.assertNull(span.getPageAssignment());
        }

    }

    @Test
    public void applyDrawsBoundingBoxesOnlyOnTheirPolicysPages() throws Exception {

        final String box = "{\"page\":%d,\"x\":60,\"y\":690,\"w\":400,\"h\":30}";
        final Policy defaultPolicy = policy("{\"config\":{\"pdf\":{\"preserveUnredactedPages\":true}},"
                + "\"graphical\":{\"boundingBoxes\":[" + box.formatted(3) + "]}}");
        final Policy boxPolicy = policy("{\"graphical\":{\"boundingBoxes\":[" + box.formatted(1) + "," + box.formatted(3) + "]}}");

        final byte[] output = service().apply(defaultPolicy, document(), List.of(), MimeType.APPLICATION_PDF,
                new PdfPagePolicies().assign("3-", boxPolicy, "boxes"));

        Assertions.assertEquals(List.of(PAGES[0], PAGES[1], "", PAGES[3]), pageText(output));

    }

    private static byte[] document() throws Exception {

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (final String line : PAGES) {
                final PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(72, 700);
                    content.showText(line);
                    content.endText();
                }
            }

            document.save(out);
            return out.toByteArray();

        }

    }

    private static List<String> pageText(final byte[] pdf) throws Exception {

        final List<String> text = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdf)) {
            final PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                text.add(stripper.getText(document).trim());
            }
        }

        return text;

    }

}
