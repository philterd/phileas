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

public class CityGeneratorTest {

    @Test
    public void testGenerateCity() {
        final List<String> cities = Arrays.asList("New York", "Los Angeles", "Chicago");
        final CityGenerator generator = new CityGenerator(cities, new SecureRandom());
        final String city = generator.random();
        assertNotNull(city);
        assertTrue(cities.contains(city));
    }

    @Test
    public void testPoolSize() {
        final List<String> cities = Arrays.asList("New York", "Los Angeles", "Chicago");
        final CityGenerator generator = new CityGenerator(cities, new SecureRandom());
        assertEquals(3, generator.poolSize());
    }

}
