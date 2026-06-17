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
 * Validator for the Spanish CIF (organization tax identifier). The CIF is a leading organization
 * type letter, seven digits, and a control character. The control is a Luhn-like weighted sum of
 * the seven digits: odd positions (first, third, fifth, seventh) are doubled and their digits
 * summed, even positions are taken as is. The control is then either a digit or a letter from the
 * table {@code JABCDEFGHI}, depending on the organization type; both forms are accepted whenever the
 * computed value matches. This is a bespoke algorithm, not a member of a generic family.
 */
public class EsCifValidator implements SpanValidator {

    private static final String VALID_FIRST = "ABCDEFGHJNPQRSUVW";
    private static final String CONTROL_LETTERS = "JABCDEFGHI";

    private static SpanValidator spanValidator;

    public static SpanValidator getInstance() {

        if (spanValidator == null) {
            spanValidator = new EsCifValidator();
        }

        return spanValidator;
    }

    private EsCifValidator() {
        // Use the static getInstance().
    }

    @Override
    public boolean validate(final Span span) {
        return span != null && isValid(span.getText());
    }

    /**
     * Validates a Spanish CIF control character.
     *
     * @param text the matched text.
     * @return {@code true} if the CIF is structurally valid and its control character is correct.
     */
    public static boolean isValid(final String text) {

        if (text == null) {
            return false;
        }

        final String s = text.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");

        if (s.length() != 9) {
            return false;
        }

        if (VALID_FIRST.indexOf(s.charAt(0)) < 0) {
            return false;
        }

        final String middle = s.substring(1, 8);
        if (!middle.matches("\\d{7}")) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 7; i++) {

            final int digit = middle.charAt(i) - '0';

            if (i % 2 == 0) {
                // Odd position (1-based): double and sum the resulting digits.
                final int doubled = digit * 2;
                sum += (doubled / 10) + (doubled % 10);
            } else {
                sum += digit;
            }

        }

        final int check = (10 - (sum % 10)) % 10;
        final char control = s.charAt(8);

        if (control >= '0' && control <= '9') {
            return (control - '0') == check;
        }

        return control == CONTROL_LETTERS.charAt(check);

    }

}
