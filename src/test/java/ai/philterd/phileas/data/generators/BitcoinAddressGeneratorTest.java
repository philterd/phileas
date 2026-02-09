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

public class BitcoinAddressGeneratorTest {

    @Test
    public void testGenerateBitcoinAddress() {
        final BitcoinAddressGenerator generator = new BitcoinAddressGenerator(new SecureRandom());
        final String bitcoin = generator.random();
        assertNotNull(bitcoin);
        assertTrue(bitcoin.startsWith("1"));
        assertTrue(bitcoin.length() >= 26 && bitcoin.length() <= 35);
    }

    @Test
    public void testPoolSize() {
        final BitcoinAddressGenerator generator = new BitcoinAddressGenerator(new SecureRandom());
        assertEquals(Long.MAX_VALUE, generator.poolSize());
    }

}
