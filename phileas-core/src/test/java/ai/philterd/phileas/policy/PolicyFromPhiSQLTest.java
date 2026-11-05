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
package ai.philterd.phileas.policy;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PolicyFromPhiSQLTest {

    @Test
    public void compilesPhiSQLIntoPolicy() {

        final String phisql = """
                POLICY test_policy DESCRIPTION 'demo';
                REDACT EMAIL_ADDRESS WITH REDACT;
                """;

        final Policy policy = Policy.fromPhiSQL(phisql);

        // The compiled policy enables the EMAIL_ADDRESS identifier.
        Assertions.assertNotNull(policy.getIdentifiers().getEmailAddress());
        Assertions.assertTrue(policy.getIdentifiers().getEmailAddress().isEnabled());

    }

    @Test
    public void compiledPolicyEquivalentToJson() {

        // A PhiSQL policy and a hand-written JSON policy that target the same identifier
        // deserialize to equal Policy objects, demonstrating PhiSQL is purely an authoring format.
        final Policy fromPhiSQL = Policy.fromPhiSQL("REDACT ZIP_CODE WITH REDACT;");

        final String json = "{\"identifiers\": {\"zipCode\":"
                + " {\"zipCodeFilterStrategy\": [{\"strategy\": \"REDACT\"}]}}}";
        final Policy fromJson = new Gson().fromJson(json, Policy.class);

        Assertions.assertNotNull(fromPhiSQL.getIdentifiers().getZipCode());

        // Compare structurally: Policy#equals uses reflection over nested filter objects that do not
        // override equals, so serialize both back to JSON and compare the canonical forms instead. Each
        // strategy gets a random "id" on deserialization, so normalize that field out before comparing.
        final Gson gson = new Gson();
        final String normalizedFromJson = gson.toJson(fromJson).replaceAll("\"id\":\"[^\"]*\"", "\"id\":\"\"");
        final String normalizedFromPhiSQL = gson.toJson(fromPhiSQL).replaceAll("\"id\":\"[^\"]*\"", "\"id\":\"\"");
        Assertions.assertEquals(normalizedFromJson, normalizedFromPhiSQL);

    }

    @Test
    public void throwsOnSyntaxError() {

        // "REDACTT" is not a valid keyword, so parsing fails.
        final PolicyCompilationException ex = Assertions.assertThrows(PolicyCompilationException.class,
                () -> Policy.fromPhiSQL("REDACTT EMAIL_ADDRESS WITH REDACT;"));

        Assertions.assertNotNull(ex.getCause());

    }

    @Test
    public void throwsOnSemanticError() {

        // Syntactically valid, but NOT_A_REAL_ENTITY is not a known entity type, so compilation fails.
        final PolicyCompilationException ex = Assertions.assertThrows(PolicyCompilationException.class,
                () -> Policy.fromPhiSQL("REDACT NOT_A_REAL_ENTITY WITH REDACT;"));

        Assertions.assertNotNull(ex.getCause());

    }

}
