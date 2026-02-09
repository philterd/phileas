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

import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.*;

public class SurnameGeneratorTest {

    @Test
    public void testGenerateSurname() {
        final SurnameGenerator generator = new SurnameGenerator(Collections.singletonList("Doe"), new Random());
        final String surname = generator.random();
        assertNotNull("Surname should not be null", surname);
        assertEquals("Doe", surname);
    }

    @Test
    public void testPoolSize() {
        final SurnameGenerator generator = new SurnameGenerator(Collections.singletonList("Doe"), new Random());
        assertEquals(1, generator.poolSize());
    }

}
