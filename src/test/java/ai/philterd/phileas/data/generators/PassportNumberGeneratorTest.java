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

public class PassportNumberGeneratorTest {

    @Test
    public void testGeneratePassportNumber() {
        final PassportNumberGenerator generator = new PassportNumberGenerator(new SecureRandom());
        final String passport = generator.random();
        assertNotNull(passport);
        assertTrue(passport.matches("[A-Z]\\d{8}"));
    }

    @Test
    public void testPoolSize() {
        final PassportNumberGenerator generator = new PassportNumberGenerator(new SecureRandom());
        assertEquals(2600000000L, generator.poolSize());
    }
}
