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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EINGeneratorTest {

    @Test
    public void testGenerateEIN() {
        final EINGenerator generator = new EINGenerator(new SecureRandom());
        for (int i = 0; i < 1000; i++) {
            final String ein = generator.random();
            assertNotNull(ein, "EIN should not be null");
            assertTrue(ein.matches("\\d{2}-\\d{7}"), "EIN should match NN-NNNNNNN format but was " + ein);
        }
    }

    @Test
    public void testPoolSize() {
        final EINGenerator generator = new EINGenerator(new SecureRandom());
        assertEquals(83L * 10000000L, generator.poolSize());
    }

}
