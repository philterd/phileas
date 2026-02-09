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
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VINGeneratorTest {

    private static final String VIN_PATTERN = "^[A-HJ-NPR-Z0-9]{17}$";

    @Test
    public void testGenerateVIN() {
        final VINGenerator generator = new VINGenerator(new SecureRandom());
        final String vin = generator.random();
        assertNotNull(vin);
        assertEquals(17, vin.length());
        assertTrue(Pattern.matches(VIN_PATTERN, vin));
    }

    @Test
    public void testGenerateValidVIN() {
        final VINGenerator generator = new VINGenerator(new SecureRandom(), true);
        final String vin = generator.random();
        assertNotNull(vin);
        assertEquals(17, vin.length());
        assertTrue(Pattern.matches(VIN_PATTERN, vin));
    }

    @Test
    public void testGenerateMultipleValidVINs() throws Exception {
        final VINGenerator generator = new VINGenerator(new SecureRandom(), true);

        // We need a VinFilter to verify the VINs.
        // It requires a FilterConfiguration which we can't easily create here without more setup,
        // but we can look at how VinFilter handles validation.
        // Actually, let's just use the same logic as in VinFilter.
        for (int i = 0; i < 100; i++) {
            final String vin = generator.random();
            assertNotNull(vin);
            assertEquals(17, vin.length());
            assertTrue(Pattern.matches(VIN_PATTERN, vin));
            assertTrue(isVinValid(vin), "VIN should be valid: " + vin);
        }
    }

    private boolean isVinValid(String vin) {
        int[] values = { 1, 2, 3, 4, 5, 6, 7, 8, 0, 1, 2, 3, 4, 5, 0, 7, 0, 9, 2, 3, 4, 5, 6, 7, 8, 9 };
        int[] weights = { 8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2 };

        String s = vin.toUpperCase();
        if (s.length() != 17) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 17; i++) {
            char c = s.charAt(i);
            int value;
            int weight = weights[i];

            if (c >= 'A' && c <= 'Z') {
                value = values[c - 'A'];
                if (value == 0 && c != 'I' && c != 'O' && c != 'Q' && i != 8) {
                    // This is a bit complex because some letters have value 0 in the values array but are not illegal.
                    // However, I, O, Q are illegal and should have value 0.
                    // The 9th character (index 8) is the check digit and shouldn't be used in the sum calculation for validation?
                    // Wait, VinFilter.java line 82 uses weights[i]. weights[8] is 0.
                    // So it doesn't matter what value is at index 8.
                }
            } else if (c >= '0' && c <= '9') {
                value = c - '0';
            } else {
                return false;
            }

            sum = sum + weight * value;
        }

        sum = sum % 11;
        char check = s.charAt(8);
        if (sum == 10 && check == 'X') {
            return true;
        } else if (sum == (check >= '0' && check <= '9' ? check - '0' : transliterate(check))) {
            return true;
        } else {
            return false;
        }
    }

    private int transliterate(char check) {

        if(check == 'A' || check == 'J'){
            return 1;
        } else if(check == 'B' || check == 'K' || check == 'S'){
            return 2;
        } else if(check == 'C' || check == 'L' || check == 'T'){
            return 3;
        } else if(check == 'D' || check == 'M' || check == 'U'){
            return 4;
        } else if(check == 'E' || check == 'N' || check == 'V'){
            return 5;
        } else if(check == 'F' || check == 'W'){
            return 6;
        } else if(check == 'G' || check == 'P' || check == 'X'){
            return 7;
        } else if(check == 'H' || check == 'Y'){
            return 8;
        } else if(check == 'R' || check == 'Z'){
            return 9;
        }
        return -1;

    }

    @Test
    public void testPoolSize() {
        final VINGenerator generator = new VINGenerator(new SecureRandom());
        assertEquals(Long.MAX_VALUE, generator.poolSize());
    }

}
