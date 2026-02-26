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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZipCodeGeneratorTest {

    @Test
    public void testGenerateZipCode() {
        final ZipCodeGenerator generator = new ZipCodeGenerator(new SecureRandom());
        final String zip = generator.random();
        assertNotNull(zip);
        assertTrue(zip.matches("\\d{5}"));
    }

    @Test
    public void testGenerateMultipleZipCodes() {
        final ZipCodeGenerator generator = new ZipCodeGenerator(new SecureRandom());
        for (int i = 0; i < 100; i++) {
            final String zip = generator.random();
            assertNotNull(zip);
            assertTrue(zip.matches("\\d{5}"));
        }
    }

    @Test
    public void testGenerateMultipleValidZipCodes() {
        final ZipCodeGenerator generator = new ZipCodeGenerator(new SecureRandom(), true);
        for (int i = 0; i < 100; i++) {
            final String zip = generator.random();
            assertNotNull(zip);
            assertTrue(zip.matches("\\d{5}"));
        }
    }

    @Test
    public void testGenerateValidZipCode() throws IOException {
        final ZipCodeGenerator generator = new ZipCodeGenerator(new SecureRandom(), true);
        final String zip = generator.random();
        assertNotNull(zip);
        assertTrue(zip.matches("\\d{5}"));

        // Verify it is in the list of valid zip codes.
        final List<String> validZipCodes = new ArrayList<>();
        try (final InputStream is = getClass().getResourceAsStream("/zip-code-population.csv")) {
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("#")) {
                        final String[] parts = line.split(",");
                        validZipCodes.add(parts[0]);
                    }
                }
            }
        }

        assertTrue(validZipCodes.contains(zip));
        assertEquals(validZipCodes.size(), generator.poolSize());
    }

    @Test
    public void testPoolSize() {
        final ZipCodeGenerator generator = new ZipCodeGenerator(new SecureRandom());
        assertEquals(100000L, generator.poolSize());
    }

    @Test
    public void testVariety() {
        final ZipCodeGenerator generator = new ZipCodeGenerator(new SecureRandom());
        final List<String> zips = new ArrayList<>();

        for(int i = 0; i < 100; i++) {
            zips.add(generator.random());
        }

        // We should have some variety in the generated zip codes.
        assertTrue(zips.stream().distinct().count() > 1);
    }

}
