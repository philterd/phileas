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

public class BicStructuralValidatorTest {

    @Test
    public void valid8CharBic() {
        // Deutsche Bank, Frankfurt (country DE).
        Assertions.assertTrue(BicStructuralValidator.isValid("DEUTDEFF"));
    }

    @Test
    public void valid11CharBicWithBranch() {
        Assertions.assertTrue(BicStructuralValidator.isValid("DEUTDEFF500"));
    }

    @Test
    public void validBicUnitedStates() {
        // Bank of America (country US); location code contains a digit.
        Assertions.assertTrue(BicStructuralValidator.isValid("BOFAUS3N"));
    }

    @Test
    public void caseInsensitive() {
        Assertions.assertTrue(BicStructuralValidator.isValid("deutdeff"));
    }

    @Test
    public void surroundingWhitespaceIgnored() {
        Assertions.assertTrue(BicStructuralValidator.isValid("  DEUTDEFF  "));
    }

    @Test
    public void validBicAnotherCountry() {
        // A different country (ZA, South Africa) confirms the country check is not hardcoded.
        Assertions.assertTrue(BicStructuralValidator.isValid("NEDSZAJJ"));
    }

    @Test
    public void invalidCountryCode() {
        // Structurally a BIC, but ZZ is not an assigned ISO 3166-1 country code.
        Assertions.assertFalse(BicStructuralValidator.isValid("DEUTZZFF"));
    }

    @Test
    public void countrySegmentMustBeLetters() {
        // The country segment must be two letters; digits there are not a valid BIC.
        Assertions.assertFalse(BicStructuralValidator.isValid("DEUT12FF"));
    }

    @Test
    public void institutionCodeMustBeLetters() {
        // A digit in the 4-letter institution segment is not a valid BIC.
        Assertions.assertFalse(BicStructuralValidator.isValid("DEU1DEFF"));
    }

    @Test
    public void wrongLengthSeven() {
        Assertions.assertFalse(BicStructuralValidator.isValid("DEUTDEF"));
    }

    @Test
    public void wrongLengthNine() {
        Assertions.assertFalse(BicStructuralValidator.isValid("DEUTDEFF5"));
    }

    @Test
    public void wrongLengthTen() {
        Assertions.assertFalse(BicStructuralValidator.isValid("DEUTDEFF50"));
    }

    @Test
    public void nullIsInvalid() {
        Assertions.assertFalse(BicStructuralValidator.isValid(null));
    }

    @Test
    public void emptyIsInvalid() {
        Assertions.assertFalse(BicStructuralValidator.isValid(""));
    }

}
