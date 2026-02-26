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

import ai.philterd.phileas.data.DataGenerator;

import java.util.Random;

/**
 * Generates random credit card numbers.
 */
public class CreditCardNumberGenerator implements DataGenerator.Generator<String> {
    private final Random random;
    private final boolean valid;

    /**
     * Creates a new credit card number generator.
     * @param random The {@link Random} to use.
     */
    public CreditCardNumberGenerator(final Random random) {
        this(random, false);
    }

    /**
     * Creates a new credit card number generator.
     * @param random The {@link Random} to use.
     * @param valid If <code>true</code>, the generated numbers will pass Luhn validation.
     */
    public CreditCardNumberGenerator(final Random random, final boolean valid) {
        this.random = random;
        this.valid = valid;
    }

    @Override
    public String random() {
        if (valid) {
            return generateValidCreditCardNumber();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(String.format("%04d", random.nextInt(10000)));
            if (i < 3) sb.append("-");
        }
        return sb.toString();
    }

    private String generateValidCreditCardNumber() {
        final int[] digits = new int[16];
        for (int i = 0; i < 15; i++) {
            digits[i] = random.nextInt(10);
        }
        digits[15] = calculateLuhnCheckDigit(digits);

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(digits[i]);
            if ((i + 1) % 4 == 0 && i < 15) {
                sb.append("-");
            }
        }
        return sb.toString();
    }

    private int calculateLuhnCheckDigit(final int[] digits) {
        int sum = 0;
        for (int i = 0; i < 15; i++) {
            int digit = digits[i];
            if (i % 2 == 0) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }
        return (10 - (sum % 10)) % 10;
    }

    @Override
    public long poolSize() {
        if (valid) {
            return 1000000000000000L; // 10^15
        }
        return 10000L * 10000L * 10000L * 10000L; // 10^16
    }

}
