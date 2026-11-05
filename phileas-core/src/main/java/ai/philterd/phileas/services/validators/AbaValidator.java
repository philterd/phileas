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
 * ABA routing transit number checksum validator for the {@code identifier} filter. The nine
 * digits are weighted 3, 7, 1, 3, 7, 1, 3, 7, 1 and the weighted sum must be a multiple of 10.
 * The check runs over the digits of the matched text, ignoring any separators (spaces, hyphens),
 * and requires exactly nine digits: a value with eight or ten digits is not a routing number and
 * is rejected rather than having the weights wrap around.
 */
public class AbaValidator implements SpanValidator {

    private static final int[] WEIGHTS = {3, 7, 1, 3, 7, 1, 3, 7, 1};

    private static SpanValidator spanValidator;

    public static SpanValidator getInstance() {

        if (spanValidator == null) {
            spanValidator = new AbaValidator();
        }

        return spanValidator;
    }

    private AbaValidator() {
        // Use the static getInstance().
    }

    @Override
    public boolean validate(final Span span) {
        return span != null && isValid(span.getText());
    }

    /**
     * Runs the 3-7-1 mod 10 checksum over the digits in the given text. Non-digit characters are
     * ignored, so formatted (for example {@code 0210-0002-1}) and unformatted
     * ({@code 021000021}) values are treated the same.
     *
     * @param text the matched text.
     * @return {@code true} if the text contains exactly nine digits and they pass the checksum.
     */
    public static boolean isValid(final String text) {

        if (text == null) {
            return false;
        }

        int sum = 0;
        int digitCount = 0;

        for (int i = 0; i < text.length(); i++) {

            final char c = text.charAt(i);

            // Skip separators (spaces, hyphens, and anything else that is not a digit).
            if (c < '0' || c > '9') {
                continue;
            }

            // A tenth digit means this is not a routing number.
            if (digitCount == WEIGHTS.length) {
                return false;
            }

            sum += (c - '0') * WEIGHTS[digitCount];
            digitCount++;

        }

        return digitCount == WEIGHTS.length && (sum % 10 == 0);

    }

}
