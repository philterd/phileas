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

public class AbaValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"021000021", "011000015", "111000025"})
    public void validRoutingNumbers(final String value) {
        // 3*0 + 7*2 + 1*1 + 3*0 + 7*0 + 1*0 + 3*0 + 7*2 + 1*1 = 30 for 021000021.
        Assertions.assertTrue(AbaValidator.isValid(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"021000022", "021000020", "123456789", "120000021"})
    public void invalidRoutingNumbersFailChecksum(final String value) {
        Assertions.assertFalse(AbaValidator.isValid(value));
    }

    @Test
    public void separatorsAreIgnored() {
        Assertions.assertTrue(AbaValidator.isValid("0210-0002-1"));
        Assertions.assertTrue(AbaValidator.isValid("021 000 021"));
        Assertions.assertFalse(AbaValidator.isValid("021-000-022"));
    }

    @Test
    public void eightDigitsIsInvalid() {
        // 02100002 has a weighted sum of 20, a multiple of 10, but is one digit short.
        Assertions.assertFalse(AbaValidator.isValid("02100002"));
    }

    @Test
    public void tenDigitsIsInvalid() {
        // The first nine digits are a valid routing number; the tenth must not be accepted, and the
        // weights must not wrap around to cover it.
        Assertions.assertFalse(AbaValidator.isValid("0210000210"));
        Assertions.assertFalse(AbaValidator.isValid("021000021-0"));
    }

    @Test
    public void everyBodyHasExactlyOneCheckDigit() {

        for (int body = 0; body < 100_000_000; body += 9_973) {

            final String prefix = String.format("%08d", body);
            int valid = 0;

            for (int check = 0; check <= 9; check++) {
                if (AbaValidator.isValid(prefix + check)) {
                    valid++;
                }
            }

            Assertions.assertEquals(1, valid, prefix);

        }

    }

    @Test
    public void nullIsInvalid() {
        Assertions.assertFalse(AbaValidator.isValid(null));
    }

    @Test
    public void emptyIsInvalid() {
        Assertions.assertFalse(AbaValidator.isValid(""));
    }

    @Test
    public void noDigitsIsInvalid() {
        Assertions.assertFalse(AbaValidator.isValid("---"));
    }

    @Test
    public void validateNullSpanIsInvalid() {
        Assertions.assertFalse(AbaValidator.getInstance().validate(null));
    }

}
