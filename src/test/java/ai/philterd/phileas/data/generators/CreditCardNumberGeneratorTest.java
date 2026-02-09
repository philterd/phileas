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

public class CreditCardNumberGeneratorTest {

    @Test
    public void testGenerateCreditCardNumber() {
        final CreditCardNumberGenerator generator = new CreditCardNumberGenerator(new Random());
        final String cc = generator.random();
        assertNotNull(cc);
        assertTrue(cc.matches("\\d{4}-\\d{4}-\\d{4}-\\d{4}"));
    }

    @Test
    public void testGenerateValidCreditCardNumber() {
        final CreditCardNumberGenerator validGenerator = new CreditCardNumberGenerator(new Random(), true);
        
        for (int i = 0; i < 100; i++) {
            final String cc = validGenerator.random();
            assertTrue(isValidLuhn(cc.replace("-", "")), "CC " + cc + " should be valid");
        }
    }

    @Test
    public void testPoolSize() {
        final CreditCardNumberGenerator generator = new CreditCardNumberGenerator(new Random());
        assertEquals(10000L * 10000L * 10000L * 10000L, generator.poolSize());

        final CreditCardNumberGenerator validGenerator = new CreditCardNumberGenerator(new Random(), true);
        assertEquals(1000000000000000L, validGenerator.poolSize());
    }

    private boolean isValidLuhn(final String cc) {
        int sum = 0;
        boolean alternate = false;
        for (int i = cc.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cc.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}
