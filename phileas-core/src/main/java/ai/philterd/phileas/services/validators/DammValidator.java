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
 * Damm check digit validator for the {@code identifier} filter. The scheme uses a totally
 * anti-symmetric quasigroup of order 10 and detects every single-digit error and every
 * transposition of two adjacent digits. The last digit of the value is the check digit. The check
 * runs over the digits of the matched text, ignoring any separators (spaces, hyphens), so a value
 * may be formatted or unformatted.
 */
public class DammValidator implements SpanValidator {

    /** The quasigroup operation table. */
    private static final int[][] QUASIGROUP = {
            {0, 3, 1, 7, 5, 9, 8, 6, 4, 2},
            {7, 0, 9, 2, 1, 5, 4, 8, 6, 3},
            {4, 2, 0, 6, 8, 7, 1, 3, 5, 9},
            {1, 7, 5, 0, 9, 8, 3, 4, 2, 6},
            {6, 1, 2, 3, 0, 4, 5, 9, 7, 8},
            {3, 6, 7, 4, 2, 0, 9, 5, 8, 1},
            {5, 8, 6, 9, 7, 2, 0, 1, 3, 4},
            {8, 9, 4, 5, 3, 6, 2, 0, 1, 7},
            {9, 4, 3, 8, 6, 1, 7, 2, 0, 5},
            {2, 5, 8, 1, 4, 3, 6, 7, 9, 0}
    };

    private static SpanValidator spanValidator;

    public static SpanValidator getInstance() {

        if (spanValidator == null) {
            spanValidator = new DammValidator();
        }

        return spanValidator;
    }

    private DammValidator() {
        // Use the static getInstance().
    }

    @Override
    public boolean validate(final Span span) {
        return span != null && isValid(span.getText());
    }

    /**
     * Runs the Damm check over the digits in the given text, the last digit being the check
     * digit. Non-digit characters are ignored.
     *
     * @param text the matched text.
     * @return {@code true} if the text contains at least one digit and the digits pass Damm.
     */
    public static boolean isValid(final String text) {

        if (text == null) {
            return false;
        }

        int interim = 0;
        int digitCount = 0;

        for (int i = 0; i < text.length(); i++) {

            final char c = text.charAt(i);

            // Skip separators (spaces, hyphens, and anything else that is not a digit).
            if (c < '0' || c > '9') {
                continue;
            }

            interim = QUASIGROUP[interim][c - '0'];
            digitCount++;

        }

        return digitCount > 0 && interim == 0;

    }

}
