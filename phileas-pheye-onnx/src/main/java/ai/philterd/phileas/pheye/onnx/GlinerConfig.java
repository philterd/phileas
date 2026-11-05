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

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The subset of {@code gliner_config.json} the local detector needs. Values mirror the
 * GLiNER {@code GLiNERConfig} defaults so a missing field falls back to the same value the
 * Python library would use.
 */
public final class GlinerConfig {

    /** Maximum span width in words (the model scores spans [i, i+width) for width in 0..maxWidth-1). */
    public final int maxWidth;

    /** Maximum number of text words; longer inputs are truncated (the model's context limit). */
    public final int maxLen;

    /** Entity-marker prompt token prepended before each entity type. */
    public final String entToken;

    /** Separator prompt token placed between the entity prompt and the text. */
    public final String sepToken;

    /** Word-splitter strategy. Only "whitespace" is supported here (the model's configured type). */
    public final String wordsSplitterType;

    private GlinerConfig(final int maxWidth, final int maxLen, final String entToken,
                         final String sepToken, final String wordsSplitterType) {
        this.maxWidth = maxWidth;
        this.maxLen = maxLen;
        this.entToken = entToken;
        this.sepToken = sepToken;
        this.wordsSplitterType = wordsSplitterType;
    }

    /** Load {@code gliner_config.json} from the model directory. */
    public static GlinerConfig load(final Path modelDir) throws IOException {

        final Path file = modelDir.resolve("gliner_config.json");

        try (final Reader reader = Files.newBufferedReader(file)) {

            final JsonObject json = new Gson().fromJson(reader, JsonObject.class);

            return new GlinerConfig(
                    getInt(json, "max_width", 12),
                    getInt(json, "max_len", 512),
                    getString(json, "ent_token", "<<ENT>>"),
                    getString(json, "sep_token", "<<SEP>>"),
                    getString(json, "words_splitter_type", "whitespace"));

        }

    }

    private static int getInt(final JsonObject json, final String key, final int fallback) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsInt() : fallback;
    }

    private static String getString(final JsonObject json, final String key, final String fallback) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : fallback;
    }

}
