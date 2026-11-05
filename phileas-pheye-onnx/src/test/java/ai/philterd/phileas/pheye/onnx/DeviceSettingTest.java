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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DeviceSettingTest {

    private static final UnaryOperator<String> NONE = name -> null;

    private static UnaryOperator<String> values(final Map<String, String> values) {
        return values::get;
    }

    private static int env(final String value) {
        return LocalPhEyeDetector.cudaDevice(NONE, values(Map.of("PHEYE_ONNX_DEVICE", value)));
    }

    @AfterEach
    void clearProperty() {
        System.clearProperty(LocalPhEyeDetector.DEVICE_PROPERTY);
    }

    @Test
    void defaultsToTheCpu() {
        assertEquals(-1, LocalPhEyeDetector.cudaDevice(NONE, NONE));
        assertEquals(-1, env(" "));
        assertEquals(-1, env("cpu"));
        assertEquals(-1, env("CPU"));
    }

    @Test
    void cudaIsDeviceZeroAndCudaNIsDeviceN() {
        assertEquals(0, env("cuda"));
        assertEquals(0, env("CUDA"));
        assertEquals(1, env("cuda:1"));
        assertEquals(2, env(" cuda:2 "));
    }

    @Test
    void systemPropertyOverridesTheEnvironmentVariable() {
        assertEquals(3, LocalPhEyeDetector.cudaDevice(values(Map.of("pheye.onnx.device", "cuda:3")),
                values(Map.of("PHEYE_ONNX_DEVICE", "cpu"))));
        assertEquals(-1, LocalPhEyeDetector.cudaDevice(values(Map.of("pheye.onnx.device", "cpu")),
                values(Map.of("PHEYE_ONNX_DEVICE", "cuda"))));
    }

    @Test
    void rejectsAnythingElse() {
        for (final String value : new String[] {"gpu", "cuda:", "cuda:-1", "cuda:+1", "cuda:one", "cuda1", "tpu", "cuda:1234567890"}) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> env(value));
            assertTrue(e.getMessage().contains("PHEYE_ONNX_DEVICE"), value);
        }
    }

    @Test
    void askingForCudaWithTheCpuPackageRunsOnTheCpu() throws Exception {
        final Path dir = FixtureModel.dir();
        assumeTrue(dir != null, "GLiNER fixture not on the classpath; skipping device fallback test.");

        // The build depends on the CPU-only onnxruntime package, which has no CUDA provider.
        System.setProperty(LocalPhEyeDetector.DEVICE_PROPERTY, "cuda");
        try (LocalPhEyeDetector detector = new LocalPhEyeDetector(dir)) {
            assertEquals("cpu", detector.device());
            final List<PhEyeSpan> spans = detector.detect("Alice Bob", List.of("person"), "ctx", 0);
            assertEquals(1, spans.size());
        }
    }

    @Test
    void aBadValueFailsTheLoad() {
        final Path dir = FixtureModel.dir();
        assumeTrue(dir != null, "GLiNER fixture not on the classpath; skipping device validation test.");

        System.setProperty(LocalPhEyeDetector.DEVICE_PROPERTY, "gpu");
        assertThrows(IllegalArgumentException.class, () -> new LocalPhEyeDetector(dir));
    }

    @Test
    void theCpuTokenizerLibraryIsUsedUnlessRustFlavorIsSet() {
        final Map<String, String> set = new java.util.HashMap<>();
        LocalPhEyeDetector.useCpuTokenizerLibrary(NONE, NONE, set::put);
        assertEquals(Map.of("RUST_FLAVOR", "cpu"), set);

        set.clear();
        LocalPhEyeDetector.useCpuTokenizerLibrary(values(Map.of("RUST_FLAVOR", "cu124")), NONE, set::put);
        LocalPhEyeDetector.useCpuTokenizerLibrary(NONE, values(Map.of("RUST_FLAVOR", "cu124")), set::put);
        assertTrue(set.isEmpty());
    }

}
