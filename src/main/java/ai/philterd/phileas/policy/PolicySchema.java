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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.util.Set;

/**
 * Describes the redaction policy JSON schema that this release of Phileas supports.
 * Each Phileas release supports exactly one schema version. The canonical schema
 * version is owned and versioned by the {@code phisql} dependency.
 */
public final class PolicySchema {

    private PolicySchema() {
        // Access the members of this class through its static methods.
    }

    /**
     * Returns the redaction policy schema.
     * @return The redaction policy schema.
     */
    public static String getSchema() {
        return ai.philterd.phisql.PolicySchema.getSchema();
    }

    /**
     * Returns the redaction policy schema version supported by this release of Phileas.
     * @return The supported schema version.
     */
    public static String getSupportedSchemaVersion() {
        return ai.philterd.phisql.PolicySchema.getSupportedSchemaVersion();
    }

    /**
     * Validates a JSON policy against the schema.
     * @param jsonPolicy The JSON policy to validate.
     * @return <code>true</code> if the policy is valid; otherwise <code>false</code>.
     */
    public static boolean validate(final String jsonPolicy) {

        try {

            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode node = mapper.readTree(jsonPolicy);

            final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            final JsonSchema schema = factory.getSchema(getSchema());

            final Set<ValidationMessage> errors = schema.validate(node);

            return errors.isEmpty();

        } catch (Exception ex) {
            return false;
        }

    }

}
