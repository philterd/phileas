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

public class DePersonalausweisValidatorTest {

    @Test
    public void validNumber() {
        // Well-known German ID sample: serial T22000129, check digit 3 (7-3-1 sum = 233, mod 10 = 3).
        Assertions.assertTrue(DePersonalausweisValidator.isValid("T220001293"));
    }

    @Test
    public void validNumberSecondVector() {
        // Serial M12345678, check digit 8 (7-3-1 sum = 268, mod 10 = 8).
        Assertions.assertTrue(DePersonalausweisValidator.isValid("M123456788"));
    }

    @Test
    public void caseInsensitive() {
        Assertions.assertTrue(DePersonalausweisValidator.isValid("t220001293"));
    }

    @Test
    public void surroundingWhitespaceIgnored() {
        Assertions.assertTrue(DePersonalausweisValidator.isValid("  T220001293  "));
    }

    @Test
    public void wrongCheckDigit() {
        Assertions.assertFalse(DePersonalausweisValidator.isValid("T220001294"));
    }

    @Test
    public void alteredSerialFailsCheck() {
        // A changed serial digit no longer matches the check digit.
        Assertions.assertFalse(DePersonalausweisValidator.isValid("T220001393"));
    }

    @Test
    public void checkDigitMustBeADigit() {
        // The trailing check character must be a digit, not a letter.
        Assertions.assertFalse(DePersonalausweisValidator.isValid("T22000129X"));
    }

    @Test
    public void invalidCharacterInSerial() {
        Assertions.assertFalse(DePersonalausweisValidator.isValid("T2200012*3"));
    }

    @Test
    public void wrongLengthNine() {
        Assertions.assertFalse(DePersonalausweisValidator.isValid("T22000129"));
    }

    @Test
    public void wrongLengthEleven() {
        Assertions.assertFalse(DePersonalausweisValidator.isValid("T2200012930"));
    }

    @Test
    public void nullIsInvalid() {
        Assertions.assertFalse(DePersonalausweisValidator.isValid(null));
    }

    @Test
    public void emptyIsInvalid() {
        Assertions.assertFalse(DePersonalausweisValidator.isValid(""));
    }

}
