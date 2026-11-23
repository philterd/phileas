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

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Parity test for {@link LocalPhEyeDetector} against the synthetic GLiNER fixture in
 * {@code src/test/resources/gliner-fixture/} (see that directory's README).
 *
 * <p>The fixture model returns deterministic logits: it fires the span (word 0, width 1, label 0)
 * at +10 and the span it contains (word 0, width 0, label 0) at +6, with everything else at -10.
 * That lets this test exercise the full pipeline end to end (real ONNX Runtime {@code session.run}
 * and the real DJL tokenizer): tensor feed/fetch wiring, words-mask and span enumeration, the
 * sigmoid/threshold/greedy-non-overlap decode, and the word-to-character offset mapping.
 *
 * <p>The test is skipped (not failed) when the fixture model is absent, so the normal build still
 * passes without it.
 */
class LocalPhEyeDetectorParityTest {

    private static Path fixtureDir() {
        final URL url = LocalPhEyeDetectorParityTest.class.getClassLoader().getResource("gliner-fixture/model.onnx");
        if (url == null || !"file".equals(url.getProtocol())) {
            return null;
        }
        try {
            return Paths.get(url.toURI()).getParent();
        } catch (final Exception e) {
            return null;
        }
    }

    @Test
    void detectsTwoWordSpanAndResolvesOverlap() throws Exception {

        final Path dir = fixtureDir();
        assumeTrue(dir != null, "GLiNER fixture model.onnx not on the test classpath; skipping parity test.");

        try (final LocalPhEyeDetector detector = new LocalPhEyeDetector(dir)) {

            // "Alice Bob" -> two words. The fixture fires the wider span (words 0..1) at +10 and the
            // contained single-word span at +6; greedy non-overlap keeps the wider one.
            final List<PhEyeSpan> spans = detector.detect("Alice Bob", List.of("person"), "ctx", 0);

            assertEquals(1, spans.size(), "expected exactly one span after overlap resolution");
            final PhEyeSpan span = spans.get(0);
            assertEquals("Alice Bob", span.getText());
            assertEquals(0, span.getStart());
            assertEquals(9, span.getEnd());
            assertEquals("person", span.getLabel());
            assertTrue(span.getScore() > 0.99, "sigmoid(+10) ~= 0.99995 should clear the 0.5 default threshold");
        }

    }

    @Test
    void mapsFiringSpanToTheChosenLabel() throws Exception {

        final Path dir = fixtureDir();
        assumeTrue(dir != null, "GLiNER fixture model.onnx not on the test classpath; skipping parity test.");

        try (final LocalPhEyeDetector detector = new LocalPhEyeDetector(dir)) {

            // Label index 0 fires; swapping label order changes which string is reported, confirming
            // label alignment survives the ONNX run.
            final List<PhEyeSpan> asDate = detector.detect("Alice Bob", List.of("date", "person"), "ctx", 0);

            assertEquals(1, asDate.size());
            assertEquals("date", asDate.get(0).getLabel());
        }

    }

    @Test
    void singleWordInputFiresContainedSpan() throws Exception {

        final Path dir = fixtureDir();
        assumeTrue(dir != null, "GLiNER fixture model.onnx not on the test classpath; skipping parity test.");

        try (final LocalPhEyeDetector detector = new LocalPhEyeDetector(dir)) {

            // One word: the wider span (words 0..1) is out of range, so only the single-word span
            // (word 0, +6) fires.
            final List<PhEyeSpan> spans = detector.detect("Alice", List.of("person"), "ctx", 0);

            assertEquals(1, spans.size());
            assertEquals("Alice", spans.get(0).getText());
            assertEquals(0, spans.get(0).getStart());
            assertEquals(5, spans.get(0).getEnd());
        }

    }

    @Test
    void theGivenThresholdIsTheCutNotAFixedHalf() throws Exception {

        final Path dir = fixtureDir();
        assumeTrue(dir != null, "GLiNER fixture model.onnx not on the test classpath; skipping parity test.");

        try (final LocalPhEyeDetector detector = new LocalPhEyeDetector(dir)) {

            // Every span but the two the fixture fires scores sigmoid(-10), about 0.0000454. "Carl" does not
            // overlap the wider span over "Alice Bob", so it is returned only when the threshold is below its score.
            assertEquals(List.of("Alice Bob"), texts(detector.detect("Alice Bob Carl", List.of("person"), "ctx", 0)));
            assertEquals(List.of("Alice Bob"), texts(detector.detect("Alice Bob Carl", List.of("person"), 0.5, "ctx", 0)));

            final List<PhEyeSpan> low = detector.detect("Alice Bob Carl", List.of("person"), 0.00001, "ctx", 0);
            assertEquals(List.of("Alice Bob", "Carl"), texts(low));
            assertTrue(low.get(1).getScore() < 0.5, "Carl scores below the old fixed threshold of 0.5");

            // The single-word span scores sigmoid(+6), about 0.9975, so a higher threshold drops it.
            assertEquals(1, detector.detect("Alice", List.of("person"), 0.99, "ctx", 0).size());
            assertEquals(0, detector.detect("Alice", List.of("person"), 0.999, "ctx", 0).size());

        }

    }

    private static List<String> texts(final List<PhEyeSpan> spans) {
        return spans.stream().sorted(java.util.Comparator.comparingInt(PhEyeSpan::getStart)).map(PhEyeSpan::getText).toList();
    }

    @Test
    void longInputPastTheTokenLimitIsFullyTokenized() throws Exception {

        final Path dir = fixtureDir();
        assumeTrue(dir != null, "GLiNER fixture model.onnx not on the test classpath; skipping parity test.");

        try (final LocalPhEyeDetector detector = new LocalPhEyeDetector(dir)) {

            // Under max_len (384) words but well over 512 tokens: every word must still get a token.
            final String text = "Alice Bob" + " qzxvjkwq".repeat(300);
            final List<PhEyeSpan> spans = detector.detect(text, List.of("person"), "ctx", 0);

            assertEquals(1, spans.size());
            assertEquals("Alice Bob", spans.get(0).getText());
        }

    }

}
