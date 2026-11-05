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

import ai.philterd.phileas.services.filters.ai.pheye.PhEyeConfiguration;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeDetector;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeDetectorProvider;

import ai.philterd.phileas.services.filters.ai.pheye.PhEyeSpan;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link PhEyeDetectorProvider} that builds a local ONNX Runtime GLiNER detector. Registered
 * via {@code META-INF/services} so core phileas discovers it with {@link java.util.ServiceLoader}
 * when a {@code modelPath} is configured.
 */
public class LocalPhEyeDetectorProvider implements PhEyeDetectorProvider {

    // One detector per model directory for the life of the process: every policy that names a directory
    // shares its loaded model, rather than each filter set loading its own copy.
    private static final ConcurrentMap<Path, LocalPhEyeDetector> DETECTORS = new ConcurrentHashMap<>();

    @Override
    public PhEyeDetector create(final PhEyeConfiguration configuration) throws Exception {

        final String modelPath = configuration.getModelPath();

        if (modelPath == null || modelPath.isBlank()) {
            throw new IllegalArgumentException("PhEyeConfiguration.modelPath must be set to build a local detector.");
        }

        final Path modelDir = Path.of(modelPath).toRealPath();

        try {
            return new SharedDetector(DETECTORS.computeIfAbsent(modelDir, dir -> {
                try {
                    return new LocalPhEyeDetector(dir);
                } catch (final Exception e) {
                    throw new CompletionException(e);
                }
            }));
        } catch (final CompletionException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw e;
        }

    }

    /** A handle on a shared detector. Closing it does nothing, since other policies may be using the model. */
    record SharedDetector(LocalPhEyeDetector detector) implements PhEyeDetector {

        @Override
        public List<PhEyeSpan> detect(final String text, final Collection<String> labels,
                                      final String context, final int piece) throws Exception {
            return detector.detect(text, labels, context, piece);
        }

        @Override
        public int maxWords() {
            return detector.maxWords();
        }

        @Override
        public int maxWidth() {
            return detector.maxWidth();
        }

    }

}
