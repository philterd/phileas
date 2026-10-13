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

import ai.philterd.phileas.services.filters.regex.ItinFilter;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ITINGeneratorTest {

    @Test
    public void testGenerateITIN() {
        final ITINGenerator generator = new ITINGenerator(new SecureRandom());
        for (int i = 0; i < 1000; i++) {
            final String itin = generator.random();
            assertNotNull(itin, "ITIN should not be null");
            assertTrue(itin.matches("9\\d{2}-\\d{2}-\\d{4}"), "ITIN should match 9XX-XX-XXXX format but was " + itin);
            assertTrue(ItinFilter.isValidRange(itin), "ITIN should be in an IRS range but was " + itin);
        }
    }

    @Test
    public void testPoolSize() {
        final ITINGenerator generator = new ITINGenerator(new SecureRandom());
        // 16 + 19 + 3 + 6 issued values for the fourth and fifth digits.
        assertEquals(100L * 44L * 10000L, generator.poolSize());
    }

}
