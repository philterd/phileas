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
package ai.philterd.phileas.pheye.onnx;

import ai.philterd.phileas.services.filters.ai.pheye.PhEyeSpan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for {@link LocalPhEyeDetector} behaviour that does not need a model run: the constructor's
 * config validation, and the {@code detect()} early-return guards. The full inference pipeline is
 * covered by {@link LocalPhEyeDetectorParityTest} against the synthetic fixture.
 */
class LocalPhEyeDetectorTest {

    @Test
    void constructorRejectsUnsupportedWordsSplitterType(@TempDir final Path dir) throws Exception {
        // The config is read and validated before the tokenizer/model are loaded, so a bad
        // words_splitter_type fails fast with just a config file present.
        Files.writeString(dir.resolve("gliner_config.json"), """
                { "words_splitter_type": "bpe" }
                """);

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new LocalPhEyeDetector(dir));
        assertTrue(exception.getMessage().contains("bpe"));
    }

    @Test
    void detectReturnsEmptyForEmptyLabels() throws Exception {
        final Path dir = FixtureModel.dir();
        assumeTrue(dir != null, "GLiNER fixture not on the classpath; skipping detect guard test.");

        try (final LocalPhEyeDetector detector = new LocalPhEyeDetector(dir)) {
            assertTrue(detector.detect("Alice Bob", List.of(), "ctx", 0).isEmpty());
        }
    }

    @Test
    void detectReturnsEmptyForBlankAndNullText() throws Exception {
        final Path dir = FixtureModel.dir();
        assumeTrue(dir != null, "GLiNER fixture not on the classpath; skipping detect guard test.");

        try (final LocalPhEyeDetector detector = new LocalPhEyeDetector(dir)) {
            final List<String> labels = List.of("person");
            assertTrue(detector.detect("", labels, "ctx", 0).isEmpty());
            assertTrue(detector.detect("   \t\n ", labels, "ctx", 0).isEmpty());
            assertTrue(detector.detect(null, labels, "ctx", 0).isEmpty());
        }
    }

    @Test
    void detectReturnsEmptyWhenTextHasNoWordsButLabelsPresent() throws Exception {
        final Path dir = FixtureModel.dir();
        assumeTrue(dir != null, "GLiNER fixture not on the classpath; skipping detect guard test.");

        // A blank-but-not-empty guard handles whitespace; this confirms the result is empty rather
        // than a span over nothing.
        try (final LocalPhEyeDetector detector = new LocalPhEyeDetector(dir)) {
            final List<PhEyeSpan> spans = detector.detect(" \n\t", List.of("person"), "ctx", 0);
            assertTrue(spans.isEmpty());
        }
    }

}
