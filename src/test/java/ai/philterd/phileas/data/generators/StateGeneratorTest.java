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

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class StateGeneratorTest {

    @Test
    public void testGenerateState() {
        final StateGenerator generator = new StateGenerator(new Random());
        final String state = generator.random();
        assertNotNull(state);
        assertFalse(state.isEmpty());
    }

    @Test
    public void testCustomPools() {
        final List<String> customStates = Arrays.asList("Confusion", "Disbelief");
        final StateGenerator stateGenerator = new StateGenerator(new Random(), customStates);
        assertTrue(customStates.contains(stateGenerator.random()));
        assertEquals(2, stateGenerator.poolSize());
    }

    @Test
    public void testPoolSize() {
        final StateGenerator generator = new StateGenerator(new Random());
        assertEquals(50L, generator.poolSize());
    }
}
