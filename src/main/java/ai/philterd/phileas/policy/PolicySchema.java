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
package ai.philterd.phileas.policy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Provides the Phileas redaction policy JSON schema that is embedded in the
 * Phileas jar, along with the single schema version this build of Phileas
 * supports.
 *
 * <p>There is a one-to-one relationship between a Phileas release and the policy
 * schema it understands: a given build supports exactly one schema version, the
 * version of the schema bundled with this artifact.</p>
 */
public final class PolicySchema {

    /**
     * Classpath location of the embedded schema. Copied into the jar from
     * {@code policy-schema/redaction-policy-schema.json} at build time.
     */
    private static final String SCHEMA_RESOURCE = "redaction-policy-schema.json";

    private static final String SCHEMA = loadSchema();
    private static final String VERSION = readVersion(SCHEMA);

    private PolicySchema() {
        // Utility class; not instantiable.
    }

    /**
     * Returns the redaction policy schema version that this build of Phileas
     * supports (for example, {@code "1.0.0"}).
     *
     * @return the supported schema version
     */
    public static String getSupportedSchemaVersion() {
        return VERSION;
    }

    /**
     * Returns the full redaction policy JSON schema embedded in the Phileas jar.
     *
     * @return the schema document as a JSON string
     */
    public static String getSchema() {
        return SCHEMA;
    }

    private static String loadSchema() {
        try (InputStream inputStream =
                     PolicySchema.class.getClassLoader().getResourceAsStream(SCHEMA_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException(
                        "Redaction policy schema not found on the classpath: " + SCHEMA_RESOURCE);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException("Unable to read the embedded redaction policy schema.", e);
        }
    }

    private static String readVersion(final String schemaJson) {
        final JsonObject root = new Gson().fromJson(schemaJson, JsonObject.class);
        if (root == null || !root.has("version") || root.get("version").isJsonNull()) {
            throw new IllegalStateException(
                    "The embedded redaction policy schema does not declare a \"version\".");
        }
        return root.get("version").getAsString();
    }

}
