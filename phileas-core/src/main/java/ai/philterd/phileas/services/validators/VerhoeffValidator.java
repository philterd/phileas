/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
 * Verhoeff check digit validator for the {@code identifier} filter. The scheme works in the
 * dihedral group D5 and detects every single-digit error and every transposition of two adjacent
 * digits. The last digit of the value is the check digit. The check runs over the digits of the
 * matched text, ignoring any separators (spaces, hyphens), so a value may be formatted or
 * unformatted. Verhoeff is used by, for example, India's Aadhaar number.
 */
public class VerhoeffValidator implements SpanValidator {

    /** The multiplication table of the dihedral group D5. */
    private static final int[][] MULTIPLICATION = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            {1, 2, 3, 4, 0, 6, 7, 8, 9, 5},
            {2, 3, 4, 0, 1, 7, 8, 9, 5, 6},
            {3, 4, 0, 1, 2, 8, 9, 5, 6, 7},
            {4, 0, 1, 2, 3, 9, 5, 6, 7, 8},
            {5, 9, 8, 7, 6, 0, 4, 3, 2, 1},
            {6, 5, 9, 8, 7, 1, 0, 4, 3, 2},
            {7, 6, 5, 9, 8, 2, 1, 0, 4, 3},
            {8, 7, 6, 5, 9, 3, 2, 1, 0, 4},
            {9, 8, 7, 6, 5, 4, 3, 2, 1, 0}
    };

    /** The permutation table, applied by digit position (counted from the right) modulo 8. */
    private static final int[][] PERMUTATION = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            {1, 5, 7, 6, 2, 8, 3, 0, 9, 4},
            {5, 8, 0, 3, 7, 9, 6, 1, 4, 2},
            {8, 9, 1, 6, 0, 4, 3, 5, 2, 7},
            {9, 4, 5, 3, 1, 2, 6, 8, 7, 0},
            {4, 2, 8, 6, 5, 7, 3, 9, 0, 1},
            {2, 7, 9, 3, 8, 0, 6, 4, 1, 5},
            {7, 0, 4, 6, 9, 1, 3, 2, 5, 8}
    };

    private static SpanValidator spanValidator;

    public static SpanValidator getInstance() {

        if (spanValidator == null) {
            spanValidator = new VerhoeffValidator();
        }

        return spanValidator;
    }

    private VerhoeffValidator() {
        // Use the static getInstance().
    }

    @Override
    public boolean validate(final Span span) {
        return span != null && isValid(span.getText());
    }

    /**
     * Runs the Verhoeff check over the digits in the given text, the last digit being the check
     * digit. Non-digit characters are ignored.
     *
     * @param text the matched text.
     * @return {@code true} if the text contains at least one digit and the digits pass Verhoeff.
     */
    public static boolean isValid(final String text) {

        if (text == null) {
            return false;
        }

        int check = 0;
        int position = 0;

        // Digits are consumed right to left, starting with the check digit.
        for (int i = text.length() - 1; i >= 0; i--) {

            final char c = text.charAt(i);

            // Skip separators (spaces, hyphens, and anything else that is not a digit).
            if (c < '0' || c > '9') {
                continue;
            }

            check = MULTIPLICATION[check][PERMUTATION[position % 8][c - '0']];
            position++;

        }

        return position > 0 && check == 0;

    }

}
