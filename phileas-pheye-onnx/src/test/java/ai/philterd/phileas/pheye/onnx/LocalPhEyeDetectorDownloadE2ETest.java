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
package ai.philterd.phileas.pheye.onnx;

import ai.philterd.phileas.services.filters.ai.pheye.PhEyeSpan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test against the published {@code philterd/ph-eye-pii-en-xsmall} model. It downloads the
 * model directly from Hugging Face (the int8 ONNX graph, the DJL/HuggingFace tokenizer, and the GLiNER
 * config), runs real in-process inference through {@link LocalPhEyeDetector}, and asserts the model
 * identifies "George Washington" in text. This exercises the full local-inference path against real
 * weights, complementing {@link LocalPhEyeDetectorParityTest} (synthetic-graph pipeline mechanics) and
 * {@link LocalPhEyeDetectorRealModelParityTest} (parity against an out-of-band model directory).
 *
 * <p>It is opt-in: it runs only when {@code PHILEAS_DOWNLOAD_MODEL=1} is set, so the default build stays
 * offline and fast. The downloaded files are cached under the temp directory, so repeated opted-in runs
 * do not re-download.
 */
@EnabledIfEnvironmentVariable(named = "PHILEAS_DOWNLOAD_MODEL", matches = "1")
class LocalPhEyeDetectorDownloadE2ETest {

    private static final String TEXT = "George Washington was the first president of the United States.";

    private static final String BASE_URL = "https://huggingface.co/philterd/ph-eye-pii-en-xsmall/resolve/main/";

    @Test
    void detectsGeorgeWashington() throws Exception {

        final Path dir = ensureDownloaded();

        try (final LocalPhEyeDetector detector = new LocalPhEyeDetector(dir)) {

            final List<PhEyeSpan> spans = detector.detect(TEXT, List.of("name"), "ctx", 0);

            assertFalse(spans.isEmpty(), "expected at least one detected span");

            // The model may return one full-name span or separate first/last spans, so assert that every
            // non-space character of "George Washington" is covered by some detected span.
            final int start = TEXT.indexOf("George Washington");
            final int end = start + "George Washington".length();
            for (int i = start; i < end; i++) {
                if (TEXT.charAt(i) == ' ') {
                    continue;
                }
                final int index = i;
                assertTrue(
                        spans.stream().anyMatch(s -> s.getStart() <= index && index < s.getEnd()),
                        "'George Washington' was not fully covered; detected: " + describe(spans));
            }
        }

    }

    /** Download the model files (cached) into a directory laid out the way LocalPhEyeDetector expects. */
    private static Path ensureDownloaded() throws Exception {

        final Path dir = Path.of(System.getProperty("java.io.tmpdir"), "phileas-ph-eye-pii-en-xsmall");
        Files.createDirectories(dir);

        final HttpClient http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofMinutes(1))
                .build();

        // (path relative to the model directory, remote path under the repo)
        final String[][] files = {
                {"gliner_config.json", "gliner_config.json"},
                {"tokenizer.json", "tokenizer.json"},
                {"onnx/model.onnx", "onnx/model.onnx"},
        };

        for (final String[] file : files) {
            final Path dest = dir.resolve(file[0]);
            if (Files.exists(dest) && Files.size(dest) > 0) {
                continue;
            }
            Files.createDirectories(dest.getParent());
            final HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + file[1]))
                    .timeout(Duration.ofMinutes(5))
                    .GET()
                    .build();
            final HttpResponse<Path> response = http.send(request, HttpResponse.BodyHandlers.ofFile(dest));
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Failed to download " + file[1] + " (HTTP " + response.statusCode() + ")");
            }
        }

        return dir;

    }

    private static String describe(final List<PhEyeSpan> spans) {
        return spans.stream()
                .map(s -> "'" + s.getText() + "'[" + s.getStart() + "," + s.getEnd() + "]")
                .collect(Collectors.joining(", "));
    }

}
