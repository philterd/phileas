package ai.philterd.test.phileas.model.enums;

import ai.philterd.phileas.model.enums.SensitivityLevel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SensitivityLevelTest {

    @Test
    public void test1() {

        SensitivityLevel sensitivityLevel = SensitivityLevel.fromName("low");
        Assertions.assertEquals(SensitivityLevel.LOW, sensitivityLevel);

    }

    @Test
    public void test2() {

        SensitivityLevel sensitivityLevel = SensitivityLevel.fromName("medium");
        Assertions.assertEquals(SensitivityLevel.MEDIUM, sensitivityLevel);

    }

    @Test
    public void test3() {

        SensitivityLevel sensitivityLevel = SensitivityLevel.fromName("high");
        Assertions.assertEquals(SensitivityLevel.HIGH, sensitivityLevel);

    }

}
