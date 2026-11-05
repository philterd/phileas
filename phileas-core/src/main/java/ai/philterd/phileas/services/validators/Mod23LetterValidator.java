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
import java.util.Map;

/**
 * Control-letter validator: the control letter is taken from a 23-entry table indexed by the
 * number mod 23. This validates the Spanish DNI (8 digits plus a control letter) and NIE (a leading
 * X, Y, or Z, mapped to 0, 1, or 2, then 7 digits and a control letter). The leading-letter
 * substitution map defaults to the NIE mapping and can be overridden via the {@code substitutions}
 * parameter.
 */
public class Mod23LetterValidator implements SpanValidator {

    private static final String CONTROL_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";

    static final Map<String, String> DEFAULT_PREFIX_SUBSTITUTIONS = Map.of("X", "0", "Y", "1", "Z", "2");

    private final Map<String, String> prefixSubstitutions;

    private Mod23LetterValidator(final Map<String, String> prefixSubstitutions) {
        this.prefixSubstitutions = prefixSubstitutions;
    }

    /**
     * @param params the validator parameters; may include {@code substitutions} for the leading
     *               letter, defaulting to the NIE mapping (X to 0, Y to 1, Z to 2).
     */
    public static SpanValidator fromParams(final Map<String, Object> params) {
        return new Mod23LetterValidator(ValidatorParams.stringMap(params, "substitutions", DEFAULT_PREFIX_SUBSTITUTIONS));
    }

    @Override
    public boolean validate(final Span span) {
        return span != null && isValid(span.getText(), prefixSubstitutions);
    }

    /**
     * Validates a Spanish DNI or NIE control letter.
     *
     * @param text                the matched text.
     * @param prefixSubstitutions the leading-letter substitutions (for the NIE prefix).
     */
    public static boolean isValid(final String text, final Map<String, String> prefixSubstitutions) {

        if (text == null) {
            return false;
        }

        final String s = text.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");

        if (s.length() != 9) {
            return false;
        }

        final char control = s.charAt(8);
        if (control < 'A' || control > 'Z') {
            return false;
        }

        final String numberPart;
        final String prefix = String.valueOf(s.charAt(0));

        if (prefixSubstitutions.containsKey(prefix)) {
            // NIE: substitute the leading letter, then seven digits.
            final String rest = s.substring(1, 8);
            if (!rest.matches("\\d{7}")) {
                return false;
            }
            numberPart = prefixSubstitutions.get(prefix) + rest;
        } else {
            // DNI: eight digits.
            numberPart = s.substring(0, 8);
            if (!numberPart.matches("\\d{8}")) {
                return false;
            }
        }

        final long n = Long.parseLong(numberPart);
        return CONTROL_LETTERS.charAt((int) (n % 23)) == control;

    }

}
