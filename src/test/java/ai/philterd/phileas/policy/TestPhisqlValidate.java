package ai.philterd.phileas.policy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestPhisqlValidate {

    @Test
    public void test() {
        // This returns true because {} is apparently a valid (empty) policy.
        Assertions.assertTrue(PolicySchema.validate("{}"));
    }

}
