package ai.philterd.phileas.policy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PolicySchemaValidationTest {

    @Test
    public void validateValidPolicy() {

        // A simple valid policy.
        // We need to make sure this matches the schema.
        // Based on Policy.java and Identifiers.java.
        final String jsonPolicy = "{\"identifiers\": {\"zipCode\": {\"enabled\": true}}}";

        final boolean valid = PolicySchema.validate(jsonPolicy);

        Assertions.assertTrue(valid);

    }

    @Test
    public void validateInvalidPolicy() {

        // zipCode should be an object, not a string.
        final String jsonPolicy = "{\"identifiers\": {\"zipCode\": \"should-be-an-object\"}}";

        final boolean valid = PolicySchema.validate(jsonPolicy);

        Assertions.assertFalse(valid);

    }

    @Test
    public void validateMalformedJson() {

        final String jsonPolicy = "{\"identifiers\": ";

        final boolean valid = PolicySchema.validate(jsonPolicy);

        Assertions.assertFalse(valid);

    }

}
