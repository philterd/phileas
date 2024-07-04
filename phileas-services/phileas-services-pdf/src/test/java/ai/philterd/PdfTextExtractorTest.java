/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd;

import ai.philterd.services.pdf.PdfTextExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class PdfTextExtractorTest {

    private static final Logger LOGGER = LogManager.getLogger(PdfTextExtractorTest.class);

    @Test
    public void test() throws IOException {

        final InputStream is = getClass().getClassLoader().getResourceAsStream("12-12110 K.pdf");
        final byte[] document = IOUtils.toByteArray(is);
        is.close();

        final PdfTextExtractor pdfTextExtractor = new PdfTextExtractor();
        final List<String> lines = pdfTextExtractor.getLines(document);

        LOGGER.info("Read " + lines.size() + " lines");

        for(final String line : lines) {
            LOGGER.info(line);
        }

        Assertions.assertEquals(215, lines.size());

    }

}