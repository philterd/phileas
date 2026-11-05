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

import ai.philterd.phileas.services.filters.ai.pheye.PhEyeConfiguration;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeDetector;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeDetectorProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Unit tests for {@link LocalPhEyeDetectorProvider}, the SPI entry point core phileas discovers via
 * {@link ServiceLoader}. Covers the modelPath validation and that the provider is actually
 * registered, since a broken {@code META-INF/services} file would silently disable local inference.
 */
class LocalPhEyeDetectorProviderTest {

    @Test
    void rejectsNullModelPath() {
        final PhEyeConfiguration configuration = new PhEyeConfiguration("http://localhost:18080");
        // modelPath is unset (null) by default.
        assertThrows(IllegalArgumentException.class,
                () -> new LocalPhEyeDetectorProvider().create(configuration));
    }

    @Test
    void rejectsBlankModelPath() {
        final PhEyeConfiguration configuration = new PhEyeConfiguration("http://localhost:18080");
        configuration.setModelPath("   ");
        assertThrows(IllegalArgumentException.class,
                () -> new LocalPhEyeDetectorProvider().create(configuration));
    }

    @Test
    void isDiscoverableViaServiceLoader() {
        // Core phileas only finds the local detector if this provider is registered for ServiceLoader.
        final boolean registered = ServiceLoader.load(PhEyeDetectorProvider.class).stream()
                .anyMatch(p -> p.type().equals(LocalPhEyeDetectorProvider.class));
        assertTrue(registered, "LocalPhEyeDetectorProvider must be registered in META-INF/services");
    }

    @Test
    void buildsADetectorForAValidModelPath() throws Exception {
        final Path dir = FixtureModel.dir();
        assumeTrue(dir != null, "GLiNER fixture not on the classpath; skipping detector-build test.");

        final PhEyeConfiguration configuration = new PhEyeConfiguration("http://localhost:18080");
        configuration.setModelPath(dir.toString());

        try (final PhEyeDetector detector = new LocalPhEyeDetectorProvider().create(configuration)) {
            assertNotNull(detector);
        }
    }

    @Test
    void policiesNamingTheSameDirectoryShareOneModel(@TempDir final Path temp) throws Exception {
        final Path dir = FixtureModel.dir();
        assumeTrue(dir != null, "GLiNER fixture not on the classpath; skipping shared-detector test.");

        // A symbolic link and a path with ".." name the same directory.
        final Path link = Files.createSymbolicLink(temp.resolve("model"), dir);
        final LocalPhEyeDetectorProvider provider = new LocalPhEyeDetectorProvider();

        final PhEyeDetector first = provider.create(configuration(dir.toString()));
        first.close();
        final PhEyeDetector second = provider.create(configuration(link.toString()));
        final PhEyeDetector third = provider.create(configuration(dir.resolve("..").resolve(dir.getFileName()).toString()));

        assertSame(shared(first), shared(second));
        assertSame(shared(first), shared(third));
        // Closing one handle leaves the shared model usable.
        assertTrue(second.detect("Alice Bob", List.of("person"), "ctx", 0).size() == 1);
    }

    @Test
    void differentDirectoriesGetDifferentModels(@TempDir final Path temp) throws Exception {
        final Path dir = FixtureModel.dir();
        assumeTrue(dir != null, "GLiNER fixture not on the classpath; skipping shared-detector test.");

        final Path copy = Files.createDirectory(temp.resolve("copy"));
        try (final var files = Files.list(dir)) {
            for (final Path file : files.toList()) {
                Files.copy(file, copy.resolve(file.getFileName()));
            }
        }
        final LocalPhEyeDetectorProvider provider = new LocalPhEyeDetectorProvider();

        assertNotSame(shared(provider.create(configuration(dir.toString()))),
                shared(provider.create(configuration(copy.toString()))));
    }

    private static PhEyeConfiguration configuration(final String modelPath) {
        final PhEyeConfiguration configuration = new PhEyeConfiguration("http://localhost:18080");
        configuration.setModelPath(modelPath);
        return configuration;
    }

    private static LocalPhEyeDetector shared(final PhEyeDetector detector) {
        return ((LocalPhEyeDetectorProvider.SharedDetector) detector).detector();
    }

    @Test
    void reportsTheModelsLimitsThroughTheSharedHandle() throws Exception {
        final Path dir = FixtureModel.dir();
        assumeTrue(dir != null, "GLiNER fixture not on the classpath; skipping limits test.");

        // The fixture's gliner_config.json sets max_len 384 and max_width 12.
        final PhEyeDetector detector = new LocalPhEyeDetectorProvider().create(configuration(dir.toString()));

        assertEquals(384, detector.maxWords());
        assertEquals(12, detector.maxWidth());
    }

}
