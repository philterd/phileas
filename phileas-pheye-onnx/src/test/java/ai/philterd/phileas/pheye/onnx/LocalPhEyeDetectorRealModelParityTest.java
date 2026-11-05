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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Real-weight parity test: runs {@link LocalPhEyeDetector} against an actual exported GLiNER model
 * and asserts it reproduces the Python {@code gliner.predict_entities} reference exactly (same
 * character offsets and labels; scores within a small tolerance).
 *
 * <p>This is skipped unless the environment variable {@code PHILEAS_GLINER_MODEL_DIR} points at a
 * model directory containing {@code model.onnx}, {@code tokenizer.json}, and {@code gliner_config.json}
 * (the ONNX export of {@code philterd/ph-eye-pii-en-small}). The normal build does not ship that
 * model, so the test does not run by default; it is the gate that confirms numerical parity on real
 * weights once a model is available, complementing {@link LocalPhEyeDetectorParityTest} (which
 * verifies the pipeline mechanics against a synthetic graph).
 *
 * <p>The expected spans below were produced by the Python reference for {@code ph-eye-pii-en-small}:
 * <pre>
 *   GLiNER.from_pretrained("ph-eye-pii-en-small").predict_entities(TEXT, ["name"], threshold=0.5)
 *   -> Maria Gonzalez[15,29] Toni Levine[33,44]   (label "name")
 * </pre>
 * If you point the test at a different model, update the expected spans to that model's reference.
 */
class LocalPhEyeDetectorRealModelParityTest {

    private static final String TEXT = "Please contact Maria Gonzalez or Toni Levine about the invoice.";

    private record Span(String text, String label, int start, int end) {}

    // Python gliner.predict_entities reference for ph-eye-pii-en-small on TEXT, label "name", threshold 0.5.
    private static final List<Span> EXPECTED = List.of(
            new Span("Maria Gonzalez", "name", 15, 29),
            new Span("Toni Levine", "name", 33, 44));

    @Test
    void matchesPythonReferenceOnRealWeights() throws Exception {

        final String dirEnv = System.getenv("PHILEAS_GLINER_MODEL_DIR");
        assumeTrue(dirEnv != null && !dirEnv.isBlank(),
                "Set PHILEAS_GLINER_MODEL_DIR to a real exported GLiNER model dir to run real-weight parity.");
        final Path dir = Path.of(dirEnv);
        assumeTrue(Files.isDirectory(dir) && Files.exists(dir.resolve("gliner_config.json")),
                "PHILEAS_GLINER_MODEL_DIR does not contain a GLiNER model; skipping.");

        try (final LocalPhEyeDetector detector = new LocalPhEyeDetector(dir)) {

            final List<PhEyeSpan> spans = detector.detect(TEXT, List.of("name"), "ctx", 0);
            spans.sort((a, b) -> Integer.compare(a.getStart(), b.getStart()));

            assertEquals(EXPECTED.size(), spans.size(),
                    "span count must match the Python reference; got " + describe(spans));

            for (int i = 0; i < EXPECTED.size(); i++) {
                final Span want = EXPECTED.get(i);
                final PhEyeSpan got = spans.get(i);
                assertEquals(want.start(), got.getStart(), "start offset of span " + i);
                assertEquals(want.end(), got.getEnd(), "end offset of span " + i);
                assertEquals(want.label(), got.getLabel(), "label of span " + i);
                assertEquals(want.text(), got.getText(), "text of span " + i);
                assertTrue(got.getScore() > 0.9, "score of span " + i + " should be high, was " + got.getScore());
            }
        }

    }

    private static String describe(final List<PhEyeSpan> spans) {
        final StringBuilder sb = new StringBuilder("[");
        for (final PhEyeSpan s : spans) {
            sb.append(s.getText()).append('[').append(s.getStart()).append(',').append(s.getEnd()).append("] ");
        }
        return sb.append(']').toString();
    }

}
