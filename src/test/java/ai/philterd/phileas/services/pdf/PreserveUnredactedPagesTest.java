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
import ai.philterd.phileas.model.filtering.MimeType;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.mockito.Mockito.mock;

/**
 * With {@code preserveUnredactedPages}, a page covered by a bounding box is rasterized like a page with a
 * detected span, so the text under the box cannot be extracted.
 */
public class PreserveUnredactedPagesTest {

    private static final String[] PAGES = {
            "Patient SSN 123-45-6789 on file.",
            "Nothing sensitive here.",
            "Project codename Bluebird is internal."
    };

    @ParameterizedTest(name = "preserveUnredactedPages={0}")
    @ValueSource(booleans = {true, false})
    public void boundingBoxPageIsRasterized(final boolean preserveUnredactedPages) throws Exception {

        final String json = "{\"config\":{\"pdf\":{\"preserveUnredactedPages\":" + preserveUnredactedPages + "}},"
                + "\"identifiers\":{\"ssn\":{}},"
                + "\"graphical\":{\"boundingBoxes\":[{\"page\":3,\"x\":60,\"y\":690,\"w\":300,\"h\":30}]}}";

        final PdfFilterService service = new PdfFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), mock(VectorService.class), null);

        final byte[] output = service.filter(new Gson().fromJson(json, Policy.class), "ctx", document(),
                MimeType.APPLICATION_PDF).getDocument();

        final List<String> text = pageText(output);

        Assertions.assertEquals(3, text.size());
        Assertions.assertEquals("", text.get(0), "page 1 has a detected SSN");
        Assertions.assertEquals(preserveUnredactedPages ? PAGES[1] : "", text.get(1), "page 2 has nothing to redact");
        Assertions.assertEquals("", text.get(2), "page 3 is covered by a bounding box");

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
