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

public class AgeGeneratorTest {

    @Test
    public void testGenerateAge() {
        final AgeGenerator generator = new AgeGenerator(new Random());
        final int age = generator.random();
        assertTrue("Age should be between 0 and 100", age >= 0 && age <= 100);
    }

    @Test
    public void testPoolSize() {
        final AgeGenerator generator = new AgeGenerator(new Random());
        assertEquals(101L, generator.poolSize());
    }

}
