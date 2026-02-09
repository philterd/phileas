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

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HospitalGeneratorTest {

    @Test
    public void testGenerateHospital() {
        final List<String> hospitals = Arrays.asList("General Hospital", "St. Jude", "Mayo Clinic");
        final HospitalGenerator generator = new HospitalGenerator(hospitals, new SecureRandom());
        final String hospital = generator.random();
        assertNotNull(hospital);
        assertTrue(hospitals.contains(hospital));
    }

    @Test
    public void testHospitalGeneratorDefaultConstructor() throws IOException {
        final HospitalGenerator generator = new HospitalGenerator();
        final String hospital = generator.random();
        assertNotNull(hospital);
        assertFalse(hospital.isEmpty());
    }

    @Test
    public void testPoolSize() {
        final List<String> hospitals = Arrays.asList("General Hospital", "St. Jude", "Mayo Clinic");
        final HospitalGenerator generator = new HospitalGenerator(hospitals, new SecureRandom());
        assertEquals(3, generator.poolSize());
    }

}
