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
 * Validator for the German tax identification number (Steuerliche Identifikationsnummer, "IdNr").
 * The number is 11 digits: a 10-digit body and a trailing check digit. Two rules are applied:
 *
 * <ul>
 *     <li><b>Structural digit-repetition rule</b> on the first ten digits: exactly one digit is
 *     repeated, appearing either twice or three times, with every other digit appearing once.
 *     This is not a checksum, but it is part of what makes a value a valid IdNr, so it is included
 *     for precision.</li>
 *     <li><b>Check digit</b>: the ISO/IEC 7064 MOD 11,10 iterated scheme over the first ten digits
 *     must produce the eleventh digit.</li>
 * </ul>
 *
 * <p>The first digit must not be zero. Whitespace and the common separators ({@code . / -}) are
 * ignored, so the value may be written grouped or ungrouped. The check is digit-only; any letter
 * makes it invalid.</p>
 */
public class DeSteuerIdValidator implements SpanValidator {

    private static SpanValidator spanValidator;

    public static SpanValidator getInstance() {

        if (spanValidator == null) {
            spanValidator = new DeSteuerIdValidator();
        }

        return spanValidator;
    }

    private DeSteuerIdValidator() {
        // Use the static getInstance().
    }

    @Override
    public boolean validate(final Span span) {
        return span != null && isValid(span.getText());
    }

    /**
     * Validates the structure and ISO/IEC 7064 MOD 11,10 check digit of a German Steuer-ID.
     *
     * @param text the matched text.
     * @return {@code true} if the text is a valid IdNr.
     */
    public static boolean isValid(final String text) {

        if (text == null) {
            return false;
        }

        final String digits = text.replaceAll("[\\s./-]", "");

        if (!digits.matches("\\d{11}")) {
            return false;
        }

        if (digits.charAt(0) == '0') {
            return false;
        }

        if (!hasValidRepetition(digits.substring(0, 10))) {
            return false;
        }

        return computeCheckDigit(digits.substring(0, 10)) == (digits.charAt(10) - '0');

    }

    /**
     * Exactly one digit in the first ten is repeated (twice or three times); every other digit
     * appears once.
     */
    private static boolean hasValidRepetition(final String firstTen) {

        final int[] counts = new int[10];
        for (int i = 0; i < firstTen.length(); i++) {
            counts[firstTen.charAt(i) - '0']++;
        }

        int twice = 0;
        int thrice = 0;

        for (final int count : counts) {
            if (count == 2) {
                twice++;
            } else if (count == 3) {
                thrice++;
            } else if (count > 3) {
                // A digit appearing four or more times is not a valid IdNr.
                return false;
            }
        }

        // One digit appears twice (and none three times), or one digit appears three times (and
        // none twice).
        return (twice == 1 && thrice == 0) || (twice == 0 && thrice == 1);

    }

    /**
     * ISO/IEC 7064 MOD 11,10 check digit over the ten body digits.
     */
    private static int computeCheckDigit(final String firstTen) {

        int product = 10;

        for (int i = 0; i < firstTen.length(); i++) {

            int sum = (firstTen.charAt(i) - '0' + product) % 10;
            if (sum == 0) {
                sum = 10;
            }
            product = (sum * 2) % 11;

        }

        return (11 - product) % 10;

    }

}
