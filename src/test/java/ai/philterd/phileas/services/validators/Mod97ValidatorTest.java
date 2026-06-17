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

public class Mod97ValidatorTest {

    private static final Map<String, String> NIR_SUBS = Mod97Validator.DEFAULT_NIR_SUBSTITUTIONS;

    @Test
    public void validNir() {
        Assertions.assertTrue(Mod97Validator.isValidNir("255081416802538", NIR_SUBS));
    }

    @Test
    public void validNirCorsica2A() {
        // Department 2A is substituted to 19 before the key is computed.
        Assertions.assertTrue(Mod97Validator.isValidNir("220032A00801642", NIR_SUBS));
    }

    @Test
    public void invalidNirKey() {
        Assertions.assertFalse(Mod97Validator.isValidNir("255081416802539", NIR_SUBS));
    }

    @Test
    public void nirWrongLength() {
        Assertions.assertFalse(Mod97Validator.isValidNir("25508141680253", NIR_SUBS));
    }

    @Test
    public void nirNonNumericBodyWithoutSubstitution() {
        // A 'Q' in the body has no substitution and is not a digit, so it is invalid.
        Assertions.assertFalse(Mod97Validator.isValidNir("2200Q2A00801642", NIR_SUBS));
    }

    @Test
    public void validIbanGb() {
        Assertions.assertTrue(Mod97Validator.isValidIban("GB82WEST12345698765432"));
    }

    @Test
    public void validIbanDeWithSpaces() {
        Assertions.assertTrue(Mod97Validator.isValidIban("DE89 3704 0044 0532 0130 00"));
    }

    @Test
    public void invalidIban() {
        Assertions.assertFalse(Mod97Validator.isValidIban("GB82WEST12345698765431"));
    }

    @Test
    public void ibanWrongStructure() {
        Assertions.assertFalse(Mod97Validator.isValidIban("1234"));
    }

    @Test
    public void nullIsInvalid() {
        Assertions.assertFalse(Mod97Validator.isValidNir(null, NIR_SUBS));
        Assertions.assertFalse(Mod97Validator.isValidIban(null));
    }

    @Test
    public void fromParamsRequiresVariant() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Mod97Validator.fromParams(Map.of()));
    }

    @Test
    public void fromParamsUnknownVariant() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Mod97Validator.fromParams(Map.of("variant", "rib")));
    }

    @Test
    public void fromParamsVariantsResolve() {
        Assertions.assertNotNull(Mod97Validator.fromParams(Map.of("variant", "nir")));
        Assertions.assertNotNull(Mod97Validator.fromParams(Map.of("variant", "iban")));
    }

}
