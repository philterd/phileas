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

import java.util.Random;

import static org.junit.Assert.*;

public class CustomIdGeneratorTest {

    @Test
    public void testGenerateCustomId() {
        final String pattern = "123-ABC-abc";
        final CustomIdGenerator generator = new CustomIdGenerator(new Random(), pattern);
        final String id = generator.random();
        
        assertNotNull(id);
        assertEquals(pattern.length(), id.length());
        
        // Check pattern: xxx-XXX-xxx
        assertTrue(Character.isDigit(id.charAt(0)));
        assertTrue(Character.isDigit(id.charAt(1)));
        assertTrue(Character.isDigit(id.charAt(2)));
        assertEquals('-', id.charAt(3));
        assertTrue(Character.isUpperCase(id.charAt(4)));
        assertTrue(Character.isUpperCase(id.charAt(5)));
        assertTrue(Character.isUpperCase(id.charAt(6)));
        assertEquals('-', id.charAt(7));
        assertTrue(Character.isLowerCase(id.charAt(8)));
        assertTrue(Character.isLowerCase(id.charAt(9)));
        assertTrue(Character.isLowerCase(id.charAt(10)));
    }

    @Test
    public void testPoolSize() {
        final CustomIdGenerator generator1 = new CustomIdGenerator(new Random(), "123");
        assertEquals(1000L, generator1.poolSize());

        final CustomIdGenerator generator2 = new CustomIdGenerator(new Random(), "ABC");
        assertEquals(26L * 26L * 26L, generator2.poolSize());

        final CustomIdGenerator generator3 = new CustomIdGenerator(new Random(), "1A-");
        assertEquals(10L * 26L, generator3.poolSize());
    }

    @Test
    public void testNullPattern() {
        final CustomIdGenerator generator = new CustomIdGenerator(new Random(), null);
        assertNull(generator.random());
        assertEquals(0, generator.poolSize());
    }

    @Test
    public void testCustomIdGeneratorSingleArgConstructor() {
        final CustomIdGenerator generator = new CustomIdGenerator("123");
        assertNotNull(generator.random());
        assertEquals(1000L, generator.poolSize());
    }

}
