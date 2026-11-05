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
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link GlinerConfig}. A wrong default or a misread field changes span enumeration
 * (max_width), truncation (max_len), or the prompt tokens, so each is asserted explicitly.
 */
class GlinerConfigTest {

    @Test
    void readsAllFieldsFromConfig(@TempDir final Path dir) throws IOException {
        write(dir, """
                {
                  "max_width": 8,
                  "max_len": 256,
                  "ent_token": "<<E>>",
                  "sep_token": "<<S>>",
                  "words_splitter_type": "whitespace"
                }
                """);

        final GlinerConfig config = GlinerConfig.load(dir);

        assertEquals(8, config.maxWidth);
        assertEquals(256, config.maxLen);
        assertEquals("<<E>>", config.entToken);
        assertEquals("<<S>>", config.sepToken);
        assertEquals("whitespace", config.wordsSplitterType);
    }

    @Test
    void fallsBackToGlinerDefaultsForMissingFields(@TempDir final Path dir) throws IOException {
        write(dir, "{}");

        final GlinerConfig config = GlinerConfig.load(dir);

        // The GLiNER GLiNERConfig defaults the load() method mirrors.
        assertEquals(12, config.maxWidth);
        assertEquals(512, config.maxLen);
        assertEquals("<<ENT>>", config.entToken);
        assertEquals("<<SEP>>", config.sepToken);
        assertEquals("whitespace", config.wordsSplitterType);
    }

    @Test
    void fallsBackToDefaultsForExplicitNullValues(@TempDir final Path dir) throws IOException {
        write(dir, """
                {
                  "max_width": null,
                  "ent_token": null
                }
                """);

        final GlinerConfig config = GlinerConfig.load(dir);

        assertEquals(12, config.maxWidth);
        assertEquals("<<ENT>>", config.entToken);
    }

    @Test
    void throwsWhenConfigFileIsMissing(@TempDir final Path dir) {
        assertThrows(IOException.class, () -> GlinerConfig.load(dir));
    }

    @Test
    void loadsTheCommittedTestFixtureConfig() throws Exception {
        final Path fixture = FixtureModel.dir();
        if (fixture == null) {
            return; // fixture not on the classpath; covered by the temp-dir cases above.
        }

        final GlinerConfig config = GlinerConfig.load(fixture);

        assertEquals(12, config.maxWidth);
        assertEquals(384, config.maxLen);
        assertEquals("whitespace", config.wordsSplitterType);
    }

    private static void write(final Path dir, final String json) throws IOException {
        Files.writeString(dir.resolve("gliner_config.json"), json);
    }

}
