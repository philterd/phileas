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

import ai.philterd.phileas.policy.filters.Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IdentifierValidatorsTest {

    @Test
    public void nullValidatorResolvesToNull() {
        // No validator declared means: keep every match, no post-match validation.
        Assertions.assertNull(IdentifierValidators.fromPolicy(null));
    }

    @Test
    public void luhnResolvesToLuhnValidator() {
        final SpanValidator validator = IdentifierValidators.fromPolicy(new Validator("luhn"));
        Assertions.assertNotNull(validator);
        Assertions.assertTrue(validator instanceof LuhnValidator);
    }

    @Test
    public void unknownNameIsAPolicyError() {
        // A name that is not implemented must fail loudly, never be silently ignored.
        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> IdentifierValidators.fromPolicy(new Validator("does-not-exist")));
        Assertions.assertTrue(ex.getMessage().contains("does-not-exist"));
    }

    @Test
    public void schemaNameNotYetImplementedIsAPolicyError() {
        // mod11 is in the schema's validatorName enum but not implemented in this build; it must
        // still be a policy error rather than passing matches through unvalidated.
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> IdentifierValidators.fromPolicy(new Validator("mod11")));
    }

    @Test
    public void blankNameIsAPolicyError() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> IdentifierValidators.fromPolicy(new Validator("   ")));
    }

    @Test
    public void nullNameIsAPolicyError() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> IdentifierValidators.fromPolicy(new Validator((String) null)));
    }

}
