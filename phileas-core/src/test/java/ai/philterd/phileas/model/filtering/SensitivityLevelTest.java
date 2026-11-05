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
package ai.philterd.phileas.model.filtering;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SensitivityLevelTest {

    @Test
    public void everyLevelIsFoundByItsNameIgnoringCase() {
        for (final SensitivityLevel level : SensitivityLevel.values()) {
            Assertions.assertEquals(level, SensitivityLevel.fromName(level.getName()));
            Assertions.assertEquals(level, SensitivityLevel.fromName(level.getName().toUpperCase()));
        }
    }

    @Test
    public void anUnknownLevelIsRefused() {
        for (final String name : new String[]{"meduim", "", " medium", null}) {
            final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                    () -> SensitivityLevel.fromName(name));
            Assertions.assertTrue(ex.getMessage().contains("Valid values are off, high, medium, low, and auto."), ex.getMessage());
        }
    }

}
