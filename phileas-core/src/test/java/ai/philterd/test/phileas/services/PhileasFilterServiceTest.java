/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.test.phileas.services;

import ai.philterd.phileas.model.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.enums.MimeType;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.Ignored;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.responses.BinaryDocumentFilterResponse;
import ai.philterd.phileas.model.serializers.PlaceholderDeserializer;
import ai.philterd.phileas.model.services.CacheService;
import ai.philterd.phileas.services.PhileasFilterService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static ai.philterd.test.phileas.services.EndToEndTestsHelper.documentContainsText;
import static ai.philterd.test.phileas.services.EndToEndTestsHelper.getPdfPolicy;
import static ai.philterd.test.phileas.services.EndToEndTestsHelper.getPolicy;

public class PhileasFilterServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(PhileasFilterServiceTest.class);

    private Gson gson;
    private final CacheService cacheService = Mockito.mock(CacheService.class);

    @BeforeEach
    public void before() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(String.class, new PlaceholderDeserializer());
        gson = gsonBuilder.create();
    }

    @Test
    public void policy() throws IOException, URISyntaxException {

        final Policy policy = getPolicy("default");
        final String json = gson.toJson(policy);
        LOGGER.info(json);

        final Policy deserialized = gson.fromJson(json, Policy.class);

        Assertions.assertEquals("default", deserialized.getName());

    }

    @Test
    public void policyWithPlaceholder() throws IOException, URISyntaxException {

        final Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("john", "jeff", "${USER}"));

        final Policy policy = getPolicy("placeholder");
        policy.setIgnored(List.of(ignored));
        final String json = gson.toJson(policy);
        LOGGER.info(json);

        final Policy deserialized = gson.fromJson(json, Policy.class);

        Assertions.assertEquals("placeholder", deserialized.getName());
        Assertions.assertEquals(3, policy.getIgnored().get(0).getTerms().size());
        Assertions.assertTrue(CollectionUtils.isNotEmpty(deserialized.getIgnored().get(0).getTerms()));

    }

    @Test
    public void pdf1() throws Exception {

        final String filename = "12-12110 K.pdf";

        final InputStream is = this.getClass().getResourceAsStream("/pdfs/" + filename);
        final byte[] document = IOUtils.toByteArray(is);
        is.close();

        Assertions.assertTrue(documentContainsText(document, "Wendy"));

        Properties properties = new Properties();

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPdfPolicy("pdf");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, cacheService);
        final BinaryDocumentFilterResponse response = service.filter(policy, "context", "documentid", document, MimeType.APPLICATION_PDF, MimeType.APPLICATION_PDF);

        // Write the byte array to a file.
        final File outputFile = File.createTempFile("redact", ".pdf");
        //outputFile.deleteOnExit();
        final String output = outputFile.getAbsolutePath();
        LOGGER.info("Writing redacted PDF to {}", output);
        FileUtils.writeByteArrayToFile(new File(output), response.getDocument());

        LOGGER.info("Spans: {}", response.getExplanation().appliedSpans().size());
        showSpans(response.getExplanation().appliedSpans());

        // TODO: This is asserting that it doesn't contain anything as a text stream
        // but it's possible that they're in the images, we would need to OCR
        // the files for this assertion to be truly valuable
        Assertions.assertFalse(documentContainsText(response.getDocument(), "Wendy"));
    }

    @Test
    public void pdf2() throws Exception {

        final InputStream is = this.getClass().getResourceAsStream("/pdfs/new-lines.pdf");
        final byte[] document = IOUtils.toByteArray(is);
        is.close();

        Assertions.assertTrue(documentContainsText(document, "90210"));

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPdfPolicy("pdf");

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration, cacheService);
        final BinaryDocumentFilterResponse response = service.filter(policy, "context", "documentid", document, MimeType.APPLICATION_PDF, MimeType.APPLICATION_PDF);

        // Write the byte array to a file.
        final File outputFile = File.createTempFile("redact", ".pdf");
        outputFile.deleteOnExit();
        final String output = outputFile.getAbsolutePath();
        LOGGER.info("Writing redacted PDF to {}", output);
        FileUtils.writeByteArrayToFile(new File(output), response.getDocument());

        LOGGER.info("Spans: {}", response.getExplanation().appliedSpans().size());
        showSpans(response.getExplanation().appliedSpans());

        // output:
        // characterStart: 35;  characterEnd: 40;  filterType: zip-code;  context: context;  documentId: documentid;  confidence: 0.9;  text: 90210;  replacement: {{{REDACTED-zip-code}}};  salt: ;  ignored: false;  classification: null;

        // TODO: This is asserting that it doesn't contain anything as a text stream
        // but it's possible that they're in the images, we would need to OCR
        // the files for this assertion to be truly valuable
        Assertions.assertFalse(documentContainsText(response.getDocument(), "90210"));

    }

    private void showSpans(List<Span> spans) {

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

    }

}