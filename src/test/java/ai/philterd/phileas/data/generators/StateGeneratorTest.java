/*
 * Copyright 2026 Philterd, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.data.generators;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StateGeneratorTest {

    @Test
    public void testGenerateState() {
        final StateGenerator generator = new StateGenerator(new SecureRandom());
        final String state = generator.random();
        assertNotNull(state);
        assertFalse(state.isEmpty());
    }

    @Test
    public void testCustomPools() {
        final List<String> customStates = Arrays.asList("Confusion", "Disbelief");
        final StateGenerator stateGenerator = new StateGenerator(new SecureRandom(), customStates);
        assertTrue(customStates.contains(stateGenerator.random()));
        assertEquals(2, stateGenerator.poolSize());
    }

    @Test
    public void testPoolSize() {
        final StateGenerator generator = new StateGenerator(new SecureRandom());
        assertEquals(50L, generator.poolSize());
    }
}
