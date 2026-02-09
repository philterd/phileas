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

public class SSNGeneratorTest {

    @Test
    public void testGenerateSSN() {
        final SSNGenerator generator = new SSNGenerator(new Random());
        final String ssn = generator.random();
        assertNotNull("SSN should not be null", ssn);
        assertTrue("SSN should match XXX-XX-XXXX format", ssn.matches("\\d{3}-\\d{2}-\\d{4}"));
    }

    @Test
    public void testPoolSize() {
        final SSNGenerator generator = new SSNGenerator(new Random());
        assertEquals(900000000L, generator.poolSize());
    }

}
