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

    @Test
    public void validateEinPolicy() {

        final String jsonPolicy = "{\"identifiers\": {\"ein\": {\"onlyValidPrefixes\": true, \"einFilterStrategies\": [{\"strategy\": \"REDACT\"}]}}}";

        final boolean valid = PolicySchema.validate(jsonPolicy);

        Assertions.assertTrue(valid);

    }

    @Test
    public void validateItinPolicy() {

        final String jsonPolicy = "{\"identifiers\": {\"itin\": {\"onlyValidRanges\": true, \"itinFilterStrategies\": [{\"strategy\": \"LAST_4\"}]}}}";

        final boolean valid = PolicySchema.validate(jsonPolicy);

        Assertions.assertTrue(valid);

    }

    @Test
    public void validateCanadaSinPolicy() {

        final String jsonPolicy = "{\"identifiers\": {\"canadaSin\": {\"onlyValidPrefixes\": true, \"canadaSinFilterStrategies\": [{\"strategy\": \"LAST_4\"}]}}}";

        final boolean valid = PolicySchema.validate(jsonPolicy);

        Assertions.assertTrue(valid);

    }

    @Test
    public void validatePhoneNumberRegionString() {

        final String jsonPolicy = "{\"identifiers\": {\"phoneNumber\": {\"region\": \"GB\"}}}";

        final boolean valid = PolicySchema.validate(jsonPolicy);

        Assertions.assertTrue(valid);

    }

    @Test
    public void validatePhoneNumberRegionArray() {

        final String jsonPolicy = "{\"identifiers\": {\"phoneNumber\": {\"region\": [\"US\", \"GB\", \"FR\"]}}}";

        final boolean valid = PolicySchema.validate(jsonPolicy);

        Assertions.assertTrue(valid);

    }


    @Test
    public void validateHealthcareIdentifierPolicies() {

        Assertions.assertTrue(PolicySchema.validate("{\"identifiers\": {\"npi\": {\"npiFilterStrategies\": [{\"strategy\": \"REDACT\"}]}}}"));
        Assertions.assertTrue(PolicySchema.validate("{\"identifiers\": {\"deaNumber\": {\"deaNumberFilterStrategies\": [{\"strategy\": \"LAST_4\"}]}}}"));
        Assertions.assertTrue(PolicySchema.validate("{\"identifiers\": {\"mbi\": {\"mbiFilterStrategies\": [{\"strategy\": \"RANDOM_REPLACE\"}]}}}"));

    }

    @Test
    public void validateCueWordFilterPolicies() {

        Assertions.assertTrue(PolicySchema.validate("{\"identifiers\": {\"healthPlanMemberId\": {\"patterns\": [\"[A-Z]{3}[0-9]{9}\"], \"cueWords\": [\"Plan ID\"], \"defaultCueWords\": false, \"healthPlanMemberIdFilterStrategies\": [{\"strategy\": \"REDACT\"}]}}}"));
        Assertions.assertTrue(PolicySchema.validate("{\"identifiers\": {\"deviceIdentifier\": {\"windowSize\": 8, \"deviceIdentifierFilterStrategies\": [{\"strategy\": \"MASK\"}]}}}"));

        // defaultCueWords false is only valid with at least one cue word.
        Assertions.assertFalse(PolicySchema.validate("{\"identifiers\": {\"deviceIdentifier\": {\"defaultCueWords\": false}}}"));
        Assertions.assertFalse(PolicySchema.validate("{\"identifiers\": {\"deviceIdentifier\": {\"defaultCueWords\": false, \"cueWords\": []}}}"));

    }

}
