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

public class Mod11ValidatorTest {

    @Test
    public void validCpf() {
        Assertions.assertTrue(Mod11Validator.isValidCpf("52998224725"));
    }

    @Test
    public void validCpfFormatted() {
        Assertions.assertTrue(Mod11Validator.isValidCpf("529.982.247-25"));
    }

    @Test
    public void invalidCpfCheckDigit() {
        Assertions.assertFalse(Mod11Validator.isValidCpf("52998224724"));
    }

    @Test
    public void cpfAllSameDigitsRejected() {
        // Repeated-digit sequences satisfy the math but are not valid CPFs.
        Assertions.assertFalse(Mod11Validator.isValidCpf("11111111111"));
    }

    @Test
    public void cpfWrongLength() {
        Assertions.assertFalse(Mod11Validator.isValidCpf("5299822472"));
    }

    @Test
    public void validCnpj() {
        Assertions.assertTrue(Mod11Validator.isValidCnpj("11222333000181"));
    }

    @Test
    public void validCnpjFormatted() {
        Assertions.assertTrue(Mod11Validator.isValidCnpj("11.222.333/0001-81"));
    }

    @Test
    public void invalidCnpjCheckDigit() {
        Assertions.assertFalse(Mod11Validator.isValidCnpj("11222333000182"));
    }

    @Test
    public void cnpjAllSameDigitsRejected() {
        Assertions.assertFalse(Mod11Validator.isValidCnpj("00000000000000"));
    }

    @Test
    public void fromParamsRequiresVariant() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Mod11Validator.fromParams(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Mod11Validator.fromParams(Map.of()));
    }

    @Test
    public void fromParamsUnknownVariant() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Mod11Validator.fromParams(Map.of("variant", "rut")));
    }

    @Test
    public void fromParamsCpfResolves() {
        Assertions.assertNotNull(Mod11Validator.fromParams(Map.of("variant", "cpf")));
    }

}
