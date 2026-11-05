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

import ai.philterd.phileas.data.DataGenerator;

import java.security.SecureRandom;

/**
 * Generates random Canadian Social Insurance Numbers (SIN) in the form {@code NNN-NNN-NNN} that pass
 * the Luhn check and begin with a digit issued to individuals (1 to 7, or 9).
 */
public class CanadaSINGenerator implements DataGenerator.Generator<String> {

    /** The first digits issued to individuals. 0 is not issued and 8 is used for business numbers. */
    private static final int[] PREFIXES = {1, 2, 3, 4, 5, 6, 7, 9};

    private final SecureRandom random;

    /**
     * Creates a new Canadian SIN generator.
     * @param random The {@link SecureRandom} to use.
     */
    public CanadaSINGenerator(final SecureRandom random) {
        this.random = random;
    }

    @Override
    public String random() {

        final int[] digits = new int[9];
        digits[0] = PREFIXES[random.nextInt(PREFIXES.length)];

        for (int i = 1; i < 8; i++) {
            digits[i] = random.nextInt(10);
        }

        digits[8] = checkDigit(digits);

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            if (i == 3 || i == 6) {
                sb.append('-');
            }
            sb.append(digits[i]);
        }

        return sb.toString();

    }

    @Override
    public long poolSize() {
        // The check digit is determined by the other eight.
        return (long) PREFIXES.length * 10000000L;
    }

    /** The Luhn check digit for the first eight digits. */
    private static int checkDigit(final int[] digits) {

        int sum = 0;

        for (int i = 0; i < 8; i++) {
            int digit = digits[i];
            // Counting from the check digit, every second digit is doubled: positions 1, 3, 5 and 7.
            if (i % 2 == 1) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }

        return (10 - (sum % 10)) % 10;

    }

}
