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
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PdfFilterService;
import com.google.gson.Gson;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Properties;

/**
 * A span trimmed because it overlaps a span of another filter still gets a box on the page.
 */
class OverlappingSpansPdfTest {

    @Test
    void aTrimmedSpanIsPlacedBesideTheSpanItOverlapped() throws Exception {

        final ByteArrayOutputStream pdf = new ByteArrayOutputStream();

        try (final PDDocument document = new PDDocument()) {

            final PDPage page = new PDPage();
            document.addPage(page);

            try (final PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(72, 700);
                stream.showText("DOB 1/2/20 CAD stable.");
                stream.endText();
            }

            document.save(pdf);

        }

        final Policy policy = new Gson().fromJson("{\"identifiers\": {\"date\": {}, \"currency\": {}}}", Policy.class);
        final PdfFilterService service = new PdfFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);

        final BinaryDocumentFilterResult result = service.filter(policy, "context", pdf.toByteArray(), MimeType.APPLICATION_PDF);

        final List<Span> spans = result.getExplanation().appliedSpans();

        final Span date = spans.stream().filter(span -> span.getFilterType() == FilterType.DATE).findFirst().orElseThrow();
        final Span currency = spans.stream().filter(span -> span.getFilterType() == FilterType.CURRENCY).findFirst().orElseThrow();

        Assertions.assertEquals("1/2/20", date.getText());
        Assertions.assertEquals("CAD", currency.getText());
        Assertions.assertTrue(date.getUpperRightX() > date.getLowerLeftX());
        Assertions.assertTrue(currency.getUpperRightX() > currency.getLowerLeftX());
        Assertions.assertTrue(currency.getLowerLeftX() >= date.getUpperRightX() - 0.01f);

    }

}
