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
 * Generates random Vehicle Identification Numbers (VIN).
 */
public class VINGenerator implements DataGenerator.Generator<String> {
    private final Random random;
    private final boolean onlyValid;
    private final String chars = "0123456789ABCDEFGHJKLMNPRSTUVWXYZ";

    /**
     * Creates a new VIN generator.
     * @param random The {@link Random} to use.
     */
    public VINGenerator(final Random random) {
        this(random, false);
    }

    /**
     * Creates a new VIN generator.
     * @param random The {@link Random} to use.
     * @param onlyValid If <code>true</code>, only valid VINs will be generated.
     */
    public VINGenerator(final Random random, final boolean onlyValid) {
        this.random = random;
        this.onlyValid = onlyValid;
    }

    @Override
    public String random() {
        if (onlyValid) {
            return generateValidVin();
        } else {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 17; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            return sb.toString();
        }
    }

    /**
     * This generates a VIN that matches the general VIN pattern. These may or may not be actually valid VINs.
     * @return A VIN that matches the general pattern.
     */
    private String generateValidVin() {

        final int[] values = { 1, 2, 3, 4, 5, 6, 7, 8, 0, 1, 2, 3, 4, 5, 0, 7, 0, 9, 2, 3, 4, 5, 6, 7, 8, 9 };
        final int[] weights = { 8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2 };

        String vin = "";
        boolean valid = false;

        while (!valid) {
            final StringBuilder sb = new StringBuilder();
            int sum = 0;

            for (int i = 0; i < 17; i++) {
                char c;
                if (i == 8) {
                    // Check digit placeholder
                    c = '0';
                } else {
                    c = chars.charAt(random.nextInt(chars.length()));
                }

                sb.append(c);

                int value;
                if (c >= 'A' && c <= 'Z') {
                    value = values[c - 'A'];
                } else {
                    value = c - '0';
                }

                sum += value * weights[i];
            }

            final int checkDigitValue = sum % 11;
            char checkDigit;
            if (checkDigitValue == 10) {
                checkDigit = 'X';
            } else {
                checkDigit = (char) (checkDigitValue + '0');
            }

            sb.setCharAt(8, checkDigit);
            vin = sb.toString();
            valid = true;
        }

        return vin;

    }

    @Override
    public long poolSize() {
        return Long.MAX_VALUE;
    }

}
