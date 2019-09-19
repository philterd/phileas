package com.mtnfog.test.phileas.model.enums;

import com.mtnfog.phileas.model.enums.SensitivityLevel;
import org.junit.Assert;
import org.junit.Test;

public class SensitivityLevelTest {

    @Test
    public void test1() {

        SensitivityLevel sensitivityLevel = SensitivityLevel.fromName("low");
        Assert.assertEquals(SensitivityLevel.LOW, sensitivityLevel);

    }

    @Test
    public void test2() {

        SensitivityLevel sensitivityLevel = SensitivityLevel.fromName("medium");
        Assert.assertEquals(SensitivityLevel.MEDIUM, sensitivityLevel);

    }

    @Test
    public void test3() {

        SensitivityLevel sensitivityLevel = SensitivityLevel.fromName("high");
        Assert.assertEquals(SensitivityLevel.HIGH, sensitivityLevel);

    }

}
