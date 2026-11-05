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

import java.util.Locale;

/**
 * Check-digit validator for the German Personalausweis (national ID card) number. The number is
 * 10 characters: a 9-character document number followed by a single check digit. The check digit
 * is the ICAO 9303 scheme: each of the first 9 characters is converted to a value (digits 0-9 map
 * to 0-9, letters A-Z map to 10-35), multiplied by the repeating weights 7, 3, 1, summed, and
 * reduced mod 10. The result must equal the trailing check digit.
 *
 * <p>The check is case-insensitive and ignores surrounding whitespace. It validates the check
 * digit only; the surrounding format (a leading letter followed by digits) is the job of the
 * identifier pattern.</p>
 */
public class DePersonalausweisValidator implements SpanValidator {

    private static final int[] WEIGHTS = {7, 3, 1};

    private static SpanValidator spanValidator;

    public static SpanValidator getInstance() {

        if (spanValidator == null) {
            spanValidator = new DePersonalausweisValidator();
        }

        return spanValidator;
    }

    private DePersonalausweisValidator() {
        // Use the static getInstance().
    }

    @Override
    public boolean validate(final Span span) {
        return span != null && isValid(span.getText());
    }

    /**
     * Validates the ICAO 9303 7-3-1 check digit of a 10-character German ID card number.
     *
     * @param text the matched text.
     * @return {@code true} if the text is a 10-character value whose trailing check digit is correct.
     */
    public static boolean isValid(final String text) {

        if (text == null) {
            return false;
        }

        final String id = text.trim().toUpperCase(Locale.ROOT);

        if (id.length() != 10) {
            return false;
        }

        // The 10th character is the check digit and must be a digit.
        final char checkChar = id.charAt(9);
        if (checkChar < '0' || checkChar > '9') {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 9; i++) {
            final int value = charValue(id.charAt(i));
            if (value < 0) {
                // A character outside 0-9 / A-Z is not a valid document-number character.
                return false;
            }
            sum += value * WEIGHTS[i % WEIGHTS.length];
        }

        return (sum % 10) == (checkChar - '0');

    }

    private static int charValue(final char c) {

        if (c >= '0' && c <= '9') {
            return c - '0';
        }

        if (c >= 'A' && c <= 'Z') {
            return 10 + (c - 'A');
        }

        return -1;

    }

}
