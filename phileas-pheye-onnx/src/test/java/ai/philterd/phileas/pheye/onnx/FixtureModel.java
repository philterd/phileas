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

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Locates the synthetic GLiNER fixture in {@code src/test/resources/gliner-fixture/} (the real
 * {@code model.onnx}, tokenizer, and config used by the parity tests). Returns {@code null} when the
 * fixture is not on the classpath so tests can skip rather than fail.
 */
final class FixtureModel {

    private FixtureModel() {
    }

    static Path dir() {
        final URL url = FixtureModel.class.getClassLoader().getResource("gliner-fixture/model.onnx");
        if (url == null || !"file".equals(url.getProtocol())) {
            return null;
        }
        try {
            return Paths.get(url.toURI()).getParent();
        } catch (final Exception e) {
            return null;
        }
    }

}
