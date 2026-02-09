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

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class BankRoutingNumberGeneratorTest {

    @Test
    public void testGenerateBankRoutingNumber() {
        final BankRoutingNumberGenerator generator = new BankRoutingNumberGenerator(new Random());
        final String routingNumber = generator.random();
        assertNotNull(routingNumber);
        assertEquals(9, routingNumber.length());
        assertTrue(routingNumber.matches("\\d{9}"));
    }

    @Test
    public void testPoolSize() {
        final BankRoutingNumberGenerator generator = new BankRoutingNumberGenerator(new Random());
        assertEquals(1000000000L, generator.poolSize());
    }
}
