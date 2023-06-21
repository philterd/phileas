/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
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
