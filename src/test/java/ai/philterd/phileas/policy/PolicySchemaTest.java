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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PolicySchemaTest {

    @Test
    public void supportedSchemaVersionIsExposed() {
        final String version = PolicySchema.getSupportedSchemaVersion();
        assertNotNull(version);
        assertFalse(version.isBlank());
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+"),
                "expected a semantic version, got: " + version);
    }

    @Test
    public void schemaIsEmbeddedAndParseable() {
        final String schema = PolicySchema.getSchema();
        assertNotNull(schema);
        final JsonObject root = new Gson().fromJson(schema, JsonObject.class);
        assertEquals("Phileas Redaction Policy", root.get("title").getAsString());
        assertTrue(root.has("$schema"));
    }

    @Test
    public void schemaVersionMatchesSchemaIdAndBody() {
        final String supported = PolicySchema.getSupportedSchemaVersion();
        final JsonObject root = new Gson().fromJson(PolicySchema.getSchema(), JsonObject.class);

        // The version returned by the API must match the schema body's version field.
        assertEquals(root.get("version").getAsString(), supported);

        // ...and the version embedded in the $id URL must agree, so the two never drift.
        assertTrue(root.get("$id").getAsString().contains("/" + supported + "/"),
                "schema $id should contain the supported version: " + supported);
    }

}
