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

import java.util.Map;

public class Mod23LetterValidatorTest {

    private static final Map<String, String> SUBS = Mod23LetterValidator.DEFAULT_PREFIX_SUBSTITUTIONS;

    @Test
    public void validDni() {
        Assertions.assertTrue(Mod23LetterValidator.isValid("12345678Z", SUBS));
    }

    @Test
    public void invalidDniLetter() {
        Assertions.assertFalse(Mod23LetterValidator.isValid("12345678A", SUBS));
    }

    @Test
    public void validNieX() {
        Assertions.assertTrue(Mod23LetterValidator.isValid("X1234567L", SUBS));
    }

    @Test
    public void validNieY() {
        Assertions.assertTrue(Mod23LetterValidator.isValid("Y1234567X", SUBS));
    }

    @Test
    public void invalidNieLetter() {
        Assertions.assertFalse(Mod23LetterValidator.isValid("X1234567A", SUBS));
    }

    @Test
    public void caseInsensitive() {
        Assertions.assertTrue(Mod23LetterValidator.isValid("12345678z", SUBS));
    }

    @Test
    public void wrongLength() {
        Assertions.assertFalse(Mod23LetterValidator.isValid("1234567Z", SUBS));
    }

    @Test
    public void controlMustBeLetter() {
        Assertions.assertFalse(Mod23LetterValidator.isValid("123456781", SUBS));
    }

    @Test
    public void dniWithNonDigitsIsInvalid() {
        Assertions.assertFalse(Mod23LetterValidator.isValid("1234A678Z", SUBS));
    }

    @Test
    public void nullIsInvalid() {
        Assertions.assertFalse(Mod23LetterValidator.isValid(null, SUBS));
    }

    @Test
    public void fromParamsResolvesWithDefault() {
        Assertions.assertNotNull(Mod23LetterValidator.fromParams(null));
    }

}
