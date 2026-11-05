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

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadCountTest {

    private static UnaryOperator<String> values(final Map<String, String> values) {
        return values::get;
    }

    private static final UnaryOperator<String> NONE = name -> null;

    @Test
    void defaultsToTheAvailableProcessors() {
        assertEquals(4, LocalPhEyeDetector.threads(NONE, NONE, 4));
    }

    @Test
    void environmentVariableOverridesTheDefault() {
        assertEquals(2, LocalPhEyeDetector.threads(NONE, values(Map.of("PHEYE_ONNX_THREADS", "2")), 4));
    }

    @Test
    void systemPropertyOverridesTheEnvironmentVariable() {
        assertEquals(1, LocalPhEyeDetector.threads(values(Map.of("pheye.onnx.threads", " 1 ")),
                values(Map.of("PHEYE_ONNX_THREADS", "2")), 4));
    }

    @Test
    void blankValuesAreIgnored() {
        assertEquals(4, LocalPhEyeDetector.threads(values(Map.of("pheye.onnx.threads", "")),
                values(Map.of("PHEYE_ONNX_THREADS", " ")), 4));
    }

    @Test
    void rejectsValuesThatAreNotPositiveWholeNumbers() {
        for (final String value : new String[] {"0", "-1", "two", "1.5"}) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> LocalPhEyeDetector.threads(NONE, values(Map.of("PHEYE_ONNX_THREADS", value)), 4));
            assertTrue(e.getMessage().contains("PHEYE_ONNX_THREADS"));
        }
    }

}
