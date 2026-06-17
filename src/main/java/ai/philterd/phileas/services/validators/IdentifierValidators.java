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

/**
 * Resolves the {@code validator} named on an {@code identifier} filter to its built-in
 * {@link SpanValidator} implementation. An unknown or not-yet-implemented name is a loud
 * policy error, never silently ignored, so a policy can never quietly skip the check it asked
 * for.
 *
 * <p>The redaction policy schema's {@code validatorName} enum lists the full validator
 * vocabulary; this build implements the subset below. References to a name that is in the
 * schema but not yet implemented here fail rather than pass through unvalidated.</p>
 */
public final class IdentifierValidators {

    private IdentifierValidators() {
        // Static utility.
    }

    /**
     * @param validator the {@link Validator} from the policy, or {@code null} if the identifier
     *                  declares no validator.
     * @return the matching {@link SpanValidator}, or {@code null} if {@code validator} is null
     *         (meaning: keep every match, no post-match validation).
     * @throws IllegalArgumentException if the validator name is empty, unknown, or recognized by
     *                                  the schema but not implemented in this build.
     */
    public static SpanValidator fromPolicy(final Validator validator) {

        if (validator == null) {
            return null;
        }

        final String name = validator.getName();

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("An identifier validator must have a non-empty name.");
        }

        switch (name) {

            case "luhn":
                return LuhnValidator.getInstance();

            case "bic-structural":
                return BicStructuralValidator.getInstance();

            case "de-personalausweis":
                return DePersonalausweisValidator.getInstance();

            case "de-steuerid":
                return DeSteuerIdValidator.getInstance();

            case "mod11":
                return Mod11Validator.fromParams(validator.getParams());

            case "mod97":
                return Mod97Validator.fromParams(validator.getParams());

            case "mod23-letter":
                return Mod23LetterValidator.fromParams(validator.getParams());

            case "es-cif":
                return EsCifValidator.getInstance();

            default:
                throw new IllegalArgumentException("Unsupported identifier validator '" + name
                        + "'. This build implements: luhn, bic-structural, de-personalausweis, de-steuerid, "
                        + "mod11, mod97, mod23-letter, es-cif.");

        }

    }

}
