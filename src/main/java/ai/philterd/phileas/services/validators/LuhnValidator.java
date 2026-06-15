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

import ai.philterd.phileas.model.filtering.Span;

/**
 * Standard mod-10 Luhn checksum validator for the {@code identifier} filter. The check runs
 * over the digits of the matched text, ignoring any separators (spaces, hyphens) so a value
 * may be formatted or unformatted. A match is kept only if the checksum passes, which lets a
 * generic regex identifier reject format-valid but checksum-invalid values such as a Canadian
 * SIN, French SIREN, or SIRET that fails the check.
 *
 * <p>Note: La Poste SIRETs are a known exception to the standard SIRET Luhn rule (they are
 * validated by a digit-sum mod 5 rather than Luhn). They are not handled here; this validator
 * implements only the standard Luhn algorithm.</p>
 */
public class LuhnValidator implements SpanValidator {

    private static SpanValidator spanValidator;

    public static SpanValidator getInstance() {

        if (spanValidator == null) {
            spanValidator = new LuhnValidator();
        }

        return spanValidator;
    }

    private LuhnValidator() {
        // Use the static getInstance().
    }

    @Override
    public boolean validate(final Span span) {
        return span != null && isValid(span.getText());
    }

    /**
     * Runs the standard mod-10 Luhn checksum over the digits in the given text. Non-digit
     * characters are ignored, so formatted (for example {@code 046 454 286}) and unformatted
     * ({@code 046454286}) values are treated the same.
     *
     * @param text the matched text.
     * @return {@code true} if the text contains at least one digit and the digits pass Luhn.
     */
    public static boolean isValid(final String text) {

        if (text == null) {
            return false;
        }

        int sum = 0;
        boolean doubleDigit = false;
        int digitCount = 0;

        for (int i = text.length() - 1; i >= 0; i--) {

            final char c = text.charAt(i);

            // Skip separators (spaces, hyphens, and anything else that is not a digit).
            if (c < '0' || c > '9') {
                continue;
            }

            int digit = c - '0';
            digitCount++;

            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;

        }

        return digitCount > 0 && (sum % 10 == 0);

    }

}
