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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FirstNameGeneratorTest {

    @Test
    public void testGenerateFirstName() {
        final List<String> firstNames = Arrays.asList("John", "Jane", "Mary");
        final FirstNameGenerator generator = new FirstNameGenerator(firstNames, new SecureRandom());
        final String firstName = generator.random();
        assertNotNull(firstName, "First name should not be null");
        assertTrue(firstNames.contains(firstName));
    }

    @Test
    public void testPoolSize() {
        final List<String> firstNames = Arrays.asList("John", "Jane", "Mary");
        final FirstNameGenerator generator = new FirstNameGenerator(firstNames, new SecureRandom());
        assertEquals(3, generator.poolSize());
    }

}
