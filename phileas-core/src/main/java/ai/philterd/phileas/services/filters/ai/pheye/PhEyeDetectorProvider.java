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
package ai.philterd.phileas.services.filters.ai.pheye;

/**
 * SPI for supplying a local (on-device) {@link PhEyeDetector}. Core phileas has no
 * ONNX or tokenizer dependency; the optional {@code ai.philterd:phileas-pheye-onnx}
 * module provides an implementation and registers it via
 * {@code META-INF/services/ai.philterd.phileas.services.filters.ai.pheye.PhEyeDetectorProvider}.
 *
 * <p>{@link PhEyeFilter} discovers the provider with {@link java.util.ServiceLoader}
 * when a {@code modelPath} is configured. If no provider is on the classpath, the
 * filter fails with a clear message pointing the user at the optional module.
 */
public interface PhEyeDetectorProvider {

    /**
     * Create a local detector for the given configuration. The configuration's
     * {@code modelPath} points at a GLiNER model directory (the ONNX model, the
     * tokenizer, and {@code gliner_config.json}).
     *
     * @param configuration the PhEye configuration, with a non-null {@code modelPath}
     * @return a ready-to-use local detector
     */
    PhEyeDetector create(PhEyeConfiguration configuration) throws Exception;

}
