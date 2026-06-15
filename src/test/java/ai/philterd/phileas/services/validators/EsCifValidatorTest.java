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

public class EsCifValidatorTest {

    @Test
    public void validCifDigitControl() {
        Assertions.assertTrue(EsCifValidator.isValid("A58818501"));
    }

    @Test
    public void validCifLetterControl() {
        Assertions.assertTrue(EsCifValidator.isValid("P1234567D"));
    }

    @Test
    public void caseInsensitive() {
        Assertions.assertTrue(EsCifValidator.isValid("p1234567d"));
    }

    @Test
    public void invalidDigitControl() {
        Assertions.assertFalse(EsCifValidator.isValid("A58818502"));
    }

    @Test
    public void invalidLetterControl() {
        Assertions.assertFalse(EsCifValidator.isValid("P1234567E"));
    }

    @Test
    public void invalidOrganizationTypeLetter() {
        // I is not a valid leading organization-type letter.
        Assertions.assertFalse(EsCifValidator.isValid("I58818501"));
    }

    @Test
    public void middleMustBeDigits() {
        Assertions.assertFalse(EsCifValidator.isValid("A5881X501"));
    }

    @Test
    public void wrongLength() {
        Assertions.assertFalse(EsCifValidator.isValid("A5881850"));
    }

    @Test
    public void nullIsInvalid() {
        Assertions.assertFalse(EsCifValidator.isValid(null));
    }

    @Test
    public void emptyIsInvalid() {
        Assertions.assertFalse(EsCifValidator.isValid(""));
    }

}
