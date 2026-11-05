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

public class LuhnValidatorTest {

    @Test
    public void validCanadianSinUnformatted() {
        // 046 454 286 is a published Luhn-valid Canadian SIN test value.
        Assertions.assertTrue(LuhnValidator.isValid("046454286"));
    }

    @Test
    public void validCanadianSinSpaceSeparated() {
        Assertions.assertTrue(LuhnValidator.isValid("046 454 286"));
    }

    @Test
    public void validCanadianSinHyphenated() {
        Assertions.assertTrue(LuhnValidator.isValid("046-454-286"));
    }

    @Test
    public void invalidCanadianSinFailsChecksum() {
        // Same shape as a SIN but the last digit breaks the checksum.
        Assertions.assertFalse(LuhnValidator.isValid("046454287"));
    }

    @Test
    public void looksLikeSinButFailsChecksum() {
        // A common "looks like a SIN" sequence that is not Luhn-valid.
        Assertions.assertFalse(LuhnValidator.isValid("123456789"));
    }

    @Test
    public void validCreditCardNumber() {
        // Standard Visa test number; Luhn-valid.
        Assertions.assertTrue(LuhnValidator.isValid("4111111111111111"));
    }

    @Test
    public void invalidCreditCardNumber() {
        Assertions.assertFalse(LuhnValidator.isValid("4111111111111112"));
    }

    @Test
    public void exercisesDoublingOverNine() {
        // "91": from the right, 1 (x1) = 1, 9 (x2) = 18 -> 9, sum = 10, divisible by 10.
        Assertions.assertTrue(LuhnValidator.isValid("91"));
    }

    @Test
    public void nullIsInvalid() {
        Assertions.assertFalse(LuhnValidator.isValid(null));
    }

    @Test
    public void emptyIsInvalid() {
        Assertions.assertFalse(LuhnValidator.isValid(""));
    }

    @Test
    public void noDigitsIsInvalid() {
        Assertions.assertFalse(LuhnValidator.isValid("---"));
    }

    @Test
    public void separatorsAreIgnored() {
        // Mixed and surrounding separators must not change the result.
        Assertions.assertTrue(LuhnValidator.isValid(" 046-454 286 "));
    }

}
