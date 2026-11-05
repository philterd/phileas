/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.data.generators;

import ai.philterd.phileas.services.filters.regex.CanadaSinFilter;
import ai.philterd.phileas.services.validators.LuhnValidator;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CanadaSINGeneratorTest {

    @Test
    public void testGenerateSIN() {
        final CanadaSINGenerator generator = new CanadaSINGenerator(new SecureRandom());
        for (int i = 0; i < 1000; i++) {
            final String sin = generator.random();
            assertNotNull(sin, "SIN should not be null");
            assertTrue(sin.matches("\\d{3}-\\d{3}-\\d{3}"), "SIN should match NNN-NNN-NNN format but was " + sin);
            assertTrue(LuhnValidator.isValid(sin), "SIN should pass the Luhn check but was " + sin);
            assertTrue(CanadaSinFilter.isValidPrefix(sin), "SIN should not begin with 0 or 8 but was " + sin);
        }
    }

    @Test
    public void testPoolSize() {
        final CanadaSINGenerator generator = new CanadaSINGenerator(new SecureRandom());
        assertEquals(8L * 10000000L, generator.poolSize());
    }

}
