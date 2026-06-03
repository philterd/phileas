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

import ai.philterd.phisql.PolicySchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that the redaction policy JSON schema is reachable from Phileas's classpath. The schema
 * is owned, versioned, and bundled by the {@code phisql} dependency (Phileas no longer downloads or
 * embeds it), so this guards against the dependency being dropped or the schema resource going
 * missing.
 */
public class PolicySchemaTest {

    @Test
    public void schemaIsAvailableFromPhisql() {

        final String schema = PolicySchema.getSchema();

        assertNotNull(schema);
        assertFalse(schema.isBlank());

    }

    @Test
    public void supportedSchemaVersionIsAvailableFromPhisql() {

        assertEquals("1.0.0", ai.philterd.phisql.PolicySchema.getSupportedSchemaVersion());

    }

    @Test
    public void supportedSchemaVersionIsAvailable() {

        assertEquals("1.0.0", ai.philterd.phileas.policy.PolicySchema.getSupportedSchemaVersion());

    }

    @Test
    public void schemaIsAvailable() {

        final String schema = ai.philterd.phileas.policy.PolicySchema.getSchema();

        assertNotNull(schema);
        assertFalse(schema.isBlank());

    }

}
