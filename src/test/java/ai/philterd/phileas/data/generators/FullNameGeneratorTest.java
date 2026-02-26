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

import ai.philterd.phileas.data.DataGenerator;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FullNameGeneratorTest {

    @Test
    public void testGenerateFullName() {
        final List<String> firstNamePool = Arrays.asList("John", "Jane", "Mary");
        final List<String> surnamePool = Arrays.asList("Doe", "Smith", "Jones");
        final DataGenerator.Generator<String> firstNameGenerator = new FirstNameGenerator(firstNamePool, new SecureRandom());
        final DataGenerator.Generator<String> surnameGenerator = new SurnameGenerator(surnamePool, new SecureRandom());
        final FullNameGenerator generator = new FullNameGenerator(firstNameGenerator, surnameGenerator);
        
        final String fullName = generator.random();
        assertNotNull(fullName, "Full name should not be null");
        assertTrue(fullName.contains(" "), "Full name should contain a space");
        final String[] parts = fullName.split(" ");
        assertEquals(2, parts.length, "Full name should have two parts");
        assertTrue(firstNamePool.contains(parts[0]));
        assertTrue(surnamePool.contains(parts[1]));
    }

    @Test
    public void testPoolSize() {
        final List<String> firstNamePool = Arrays.asList("John", "Jane", "Mary");
        final List<String> surnamePool = Arrays.asList("Doe", "Smith", "Jones");
        final DataGenerator.Generator<String> firstNameGenerator = new FirstNameGenerator(firstNamePool, new SecureRandom());
        final DataGenerator.Generator<String> surnameGenerator = new SurnameGenerator(surnamePool, new SecureRandom());
        final FullNameGenerator generator = new FullNameGenerator(firstNameGenerator, surnameGenerator);
        assertEquals(3 * 3, generator.poolSize());
    }

}
