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

import java.util.Collections;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class FirstNameGeneratorTest {

    @Test
    public void testGenerateFirstName() {
        final FirstNameGenerator generator = new FirstNameGenerator(Collections.singletonList("John"), new Random());
        final String firstName = generator.random();
        assertNotNull(firstName, "First name should not be null");
        assertEquals("John", firstName);
    }

    @Test
    public void testPoolSize() {
        final FirstNameGenerator generator = new FirstNameGenerator(Collections.singletonList("John"), new Random());
        assertEquals(1, generator.poolSize());
    }

}
