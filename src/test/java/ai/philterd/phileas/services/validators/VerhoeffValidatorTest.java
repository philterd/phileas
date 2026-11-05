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
package ai.philterd.phileas.services.validators;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class VerhoeffValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"2363", "123451", "758722"})
    public void validValues(final String value) {
        // Published Verhoeff examples: 236 has check digit 3, 12345 has 1, 75872 has 2.
        Assertions.assertTrue(VerhoeffValidator.isValid(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "2364", "123452", "758723"})
    public void invalidValuesFailCheck(final String value) {
        Assertions.assertFalse(VerhoeffValidator.isValid(value));
    }

    @Test
    public void separatorsAreIgnored() {
        Assertions.assertTrue(VerhoeffValidator.isValid("75 87 22"));
    }

    @Test
    public void everyBodyHasExactlyOneCheckDigit() {

        for (int body = 0; body < 100_000; body++) {

            final String prefix = String.valueOf(body);
            int valid = 0;

            for (int check = 0; check <= 9; check++) {
                if (VerhoeffValidator.isValid(prefix + check)) {
                    valid++;
                }
            }

            Assertions.assertEquals(1, valid, prefix);

        }

    }

    @Test
    public void adjacentTranspositionsAreRejected() {

        for (int body = 0; body < 100_000; body++) {

            final String value = withCheckDigit(String.format("%05d", body));
            Assertions.assertTrue(VerhoeffValidator.isValid(value), value);

            for (int i = 0; i < value.length() - 1; i++) {

                if (value.charAt(i) == value.charAt(i + 1)) {
                    continue;
                }

                final char[] swapped = value.toCharArray();
                swapped[i] = value.charAt(i + 1);
                swapped[i + 1] = value.charAt(i);

                Assertions.assertFalse(VerhoeffValidator.isValid(new String(swapped)), value + " -> " + new String(swapped));

            }

        }

    }

    @Test
    public void singleDigitErrorsAreRejected() {

        for (int body = 0; body < 10_000; body++) {

            final String value = withCheckDigit(String.format("%04d", body));

            for (int i = 0; i < value.length(); i++) {
                for (char c = '0'; c <= '9'; c++) {

                    if (c == value.charAt(i)) {
                        continue;
                    }

                    final char[] changed = value.toCharArray();
                    changed[i] = c;

                    Assertions.assertFalse(VerhoeffValidator.isValid(new String(changed)), value + " -> " + new String(changed));

                }
            }

        }

    }

    @Test
    public void nullIsInvalid() {
        Assertions.assertFalse(VerhoeffValidator.isValid(null));
    }

    @Test
    public void emptyIsInvalid() {
        Assertions.assertFalse(VerhoeffValidator.isValid(""));
    }

    @Test
    public void noDigitsIsInvalid() {
        Assertions.assertFalse(VerhoeffValidator.isValid("---"));
    }

    @Test
    public void validateNullSpanIsInvalid() {
        Assertions.assertFalse(VerhoeffValidator.getInstance().validate(null));
    }

    private static String withCheckDigit(final String body) {

        for (int check = 0; check <= 9; check++) {
            if (VerhoeffValidator.isValid(body + check)) {
                return body + check;
            }
        }

        throw new AssertionError("No check digit validates " + body);

    }

}
