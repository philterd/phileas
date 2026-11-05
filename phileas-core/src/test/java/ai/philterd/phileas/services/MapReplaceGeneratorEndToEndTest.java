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
package ai.philterd.phileas.services;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end tests for the MAP_REPLACE generator path wired through {@link PlainTextFilterService},
 * exercising the real PII re-scan of generated values against the policy's filter set.
 */
public class MapReplaceGeneratorEndToEndTest {

    private final ContextService contextService = new DefaultContextService();
    private final VectorService vectorService = new InMemoryVectorService();

    // An SSN filter using MAP_REPLACE (generator + REDACT fallback) plus an email filter, so the
    // re-scan can detect an email address reintroduced by the generator.
    private Policy policy() {
        final String json = """
                {
                  "name": "map-replace-generator",
                  "generators": {
                    "g": {
                      "type": "ollama",
                      "endpoint": "http://localhost:11434",
                      "model": "m",
                      "prompt": "Replace {{token}}.",
                      "timeoutMs": 1000
                    }
                  },
                  "identifiers": {
                    "ssn": {
                      "ssnFilterStrategies": [
                        {
                          "strategy": "MAP_REPLACE",
                          "generator": "g",
                          "fallbackStrategy": "REDACT"
                        }
                      ]
                    },
                    "emailAddress": {
                      "emailAddressFilterStrategies": [
                        { "strategy": "REDACT" }
                      ]
                    }
                  }
                }
                """;
        return new Gson().fromJson(json, Policy.class);
    }

    private HttpClient generatorReturning(final String value) throws Exception {
        final HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            when(response.getCode()).thenReturn(200);
            when(response.getEntity()).thenReturn(new StringEntity("{\"response\": \"" + value + "\"}"));
            return handler.handleResponse(response);
        });
        return httpClient;
    }

    @Test
    public void generatedValueWithReintroducedPiiIsRejected() throws Exception {

        // The generator returns text containing an email address; the re-scan must reject it and the
        // strategy must fall back to REDACT.
        final HttpClient httpClient = generatorReturning("reach me at bob@example.com");

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), contextService, vectorService, httpClient);

        final TextFilterResult result = service.filter(policy(), "context", "My SSN is 123-45-6789 today.");

        Assertions.assertEquals("My SSN is {{{REDACTED-ssn}}} today.", result.getFilteredText());
        Assertions.assertFalse(result.getFilteredText().contains("bob@example.com"));

    }

    @Test
    public void mappingLoadedFromTsvFileIsApplied() throws Exception {

        // A TSV mapping file mapping a detected SSN to a fixed replacement.
        final File tsv = File.createTempFile("map-replace", ".tsv");
        tsv.deleteOnExit();
        Files.writeString(tsv.toPath(), "123-45-6789\tMAPPED-SSN\n", StandardCharsets.UTF_8);

        final String json = """
                {
                  "name": "map-replace-file",
                  "identifiers": {
                    "ssn": {
                      "ssnFilterStrategies": [
                        {
                          "strategy": "MAP_REPLACE",
                          "mappingFiles": [ "%s" ],
                          "fallbackStrategy": "REDACT"
                        }
                      ]
                    }
                  }
                }
                """.formatted(tsv.getAbsolutePath().replace("\\", "\\\\"));

        final Policy policy = new Gson().fromJson(json, Policy.class);

        // No generator, so no HttpClient is needed.
        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), contextService, vectorService, null);

        final TextFilterResult result = service.filter(policy, "context", "My SSN is 123-45-6789 today.");

        Assertions.assertEquals("My SSN is MAPPED-SSN today.", result.getFilteredText());

    }

    @Test
    public void duplicateKeyAcrossMappingFilesUsesLastFile() throws Exception {

        // Two files map the same key to different values; the later file in the list wins.
        final File first = File.createTempFile("map-replace-a", ".tsv");
        first.deleteOnExit();
        Files.writeString(first.toPath(), "123-45-6789\tFROM-FIRST\n", StandardCharsets.UTF_8);

        final File second = File.createTempFile("map-replace-b", ".tsv");
        second.deleteOnExit();
        Files.writeString(second.toPath(), "123-45-6789\tFROM-SECOND\n", StandardCharsets.UTF_8);

        final String json = """
                {
                  "identifiers": {
                    "ssn": {
                      "ssnFilterStrategies": [
                        {
                          "strategy": "MAP_REPLACE",
                          "mappingFiles": [ "%s", "%s" ],
                          "fallbackStrategy": "REDACT"
                        }
                      ]
                    }
                  }
                }
                """.formatted(
                        first.getAbsolutePath().replace("\\", "\\\\"),
                        second.getAbsolutePath().replace("\\", "\\\\"));

        final Policy policy = new Gson().fromJson(json, Policy.class);

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), contextService, vectorService, null);

        final TextFilterResult result = service.filter(policy, "context", "My SSN is 123-45-6789 today.");

        Assertions.assertEquals("My SSN is FROM-SECOND today.", result.getFilteredText());

    }

    @Test
    public void generatedValueWithoutPiiIsUsed() throws Exception {

        // A clean generated value passes the re-scan and is used as the replacement.
        final HttpClient httpClient = generatorReturning("Redacted Vendor");

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), contextService, vectorService, httpClient);

        final TextFilterResult result = service.filter(policy(), "context", "My SSN is 123-45-6789 today.");

        Assertions.assertEquals("My SSN is Redacted Vendor today.", result.getFilteredText());

    }

}
