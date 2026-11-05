/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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

public class DeSteuerIdValidatorTest {

    @Test
    public void validRepeatedTwice() {
        // Documented sample; in the first ten digits the 7 appears twice. Check digit 9.
        Assertions.assertTrue(DeSteuerIdValidator.isValid("86095742719"));
    }

    @Test
    public void validRepeatedThreeTimes() {
        // In the first ten digits the 9 appears three times. Check digit 9.
        Assertions.assertTrue(DeSteuerIdValidator.isValid("65929970489"));
    }

    @Test
    public void validDifferentCheckDigit() {
        // The 8 appears twice; check digit 6 (confirms the checksum is not constant).
        Assertions.assertTrue(DeSteuerIdValidator.isValid("47036892816"));
    }

    @Test
    public void validWithSeparators() {
        Assertions.assertTrue(DeSteuerIdValidator.isValid("86 095 742 719"));
    }

    @Test
    public void wrongCheckDigit() {
        Assertions.assertFalse(DeSteuerIdValidator.isValid("86095742718"));
    }

    @Test
    public void noRepeatedDigitFailsStructure() {
        // All ten body digits are distinct, so the repetition rule is not satisfied.
        Assertions.assertFalse(DeSteuerIdValidator.isValid("12345678905"));
    }

    @Test
    public void twoDifferentDigitsRepeatedFailsStructure() {
        // Two distinct digits each appear twice; only one repeated digit is allowed.
        Assertions.assertFalse(DeSteuerIdValidator.isValid("11223456780"));
    }

    @Test
    public void digitRepeatedFourTimesFailsStructure() {
        Assertions.assertFalse(DeSteuerIdValidator.isValid("11110234567"));
    }

    @Test
    public void leadingZeroIsInvalid() {
        Assertions.assertFalse(DeSteuerIdValidator.isValid("01234567890"));
    }

    @Test
    public void wrongLengthTen() {
        Assertions.assertFalse(DeSteuerIdValidator.isValid("8609574271"));
    }

    @Test
    public void wrongLengthTwelve() {
        Assertions.assertFalse(DeSteuerIdValidator.isValid("860957427190"));
    }

    @Test
    public void lettersAreInvalid() {
        Assertions.assertFalse(DeSteuerIdValidator.isValid("8609574271A"));
    }

    @Test
    public void nullIsInvalid() {
        Assertions.assertFalse(DeSteuerIdValidator.isValid(null));
    }

    @Test
    public void emptyIsInvalid() {
        Assertions.assertFalse(DeSteuerIdValidator.isValid(""));
    }

}
