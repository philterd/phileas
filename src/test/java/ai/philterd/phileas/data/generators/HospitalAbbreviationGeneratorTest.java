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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class HospitalAbbreviationGeneratorTest {

    @Test
    public void testGenerateHospitalAbbreviation() throws IOException {
        final HospitalAbbreviationGenerator generator = new HospitalAbbreviationGenerator(new Random());
        final String abbreviation = generator.random();
        assertNotNull(abbreviation);
        assertFalse(abbreviation.isEmpty());
        // Initials should be uppercase
        assertEquals(abbreviation.toUpperCase(), abbreviation);
    }

    @Test
    public void testCustomPool() {
        final List<String> customHospitals = Arrays.asList("St. Jude Children's Research Hospital", "Mayo Clinic");
        final HospitalAbbreviationGenerator generator = new HospitalAbbreviationGenerator(customHospitals, new Random());
        final String abbreviation = generator.random();
        assertTrue(Arrays.asList("SJCRH", "MC").contains(abbreviation));
        assertEquals(2, generator.poolSize());
    }

    @Test
    public void testPoolSize() throws IOException {
        final HospitalAbbreviationGenerator generator = new HospitalAbbreviationGenerator(new Random());
        assertTrue(generator.poolSize() > 0);
    }

    @Test
    public void testHospitalAbbreviationGeneratorDefaultConstructor() throws IOException {
        final HospitalAbbreviationGenerator generator = new HospitalAbbreviationGenerator();
        final String abbreviation = generator.random();
        assertNotNull(abbreviation);
        assertFalse(abbreviation.isEmpty());
    }

}
