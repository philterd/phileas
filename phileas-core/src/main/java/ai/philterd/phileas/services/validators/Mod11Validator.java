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
 * Weighted-sum mod-11 check-digit validator. The mod-11 family covers several national identifiers
 * that differ only in their weights, length, and number of check digits; the {@code variant}
 * parameter selects the scheme. This build implements the Brazilian {@code cpf} (11 digits, two
 * check digits) and {@code cnpj} (14 digits, two check digits) variants.
 */
public class Mod11Validator implements SpanValidator {

    public enum Variant {
        CPF,
        CNPJ
    }

    private final Variant variant;

    private Mod11Validator(final Variant variant) {
        this.variant = variant;
    }

    /**
     * @param params the validator parameters; must include a {@code variant} of {@code cpf} or
     *               {@code cnpj}.
     */
    public static SpanValidator fromParams(final Map<String, Object> params) {

        final String variant = ValidatorParams.string(params, "variant");

        if (variant == null) {
            throw new IllegalArgumentException("The mod11 validator requires a 'variant' parameter (cpf or cnpj).");
        }

        switch (variant.toLowerCase(Locale.ROOT)) {

            case "cpf":
                return new Mod11Validator(Variant.CPF);

            case "cnpj":
                return new Mod11Validator(Variant.CNPJ);

            default:
                throw new IllegalArgumentException("Unsupported mod11 variant '" + variant + "'. Supported: cpf, cnpj.");

        }

    }

    @Override
    public boolean validate(final Span span) {

        if (span == null) {
            return false;
        }

        return variant == Variant.CPF ? isValidCpf(span.getText()) : isValidCnpj(span.getText());

    }

    /**
     * Validates a Brazilian CPF (11 digits, two mod-11 check digits).
     */
    public static boolean isValidCpf(final String text) {

        final String d = digitsOnly(text);

        if (d.length() != 11 || allSameDigit(d)) {
            return false;
        }

        // First check digit over digits 0-8 with weights 10..2; second over digits 0-9 with 11..2.
        final int c1 = checkDigit(d, 9, 10);
        final int c2 = checkDigit(d, 10, 11);

        return c1 == (d.charAt(9) - '0') && c2 == (d.charAt(10) - '0');

    }

    /**
     * Validates a Brazilian CNPJ (14 digits, two mod-11 check digits).
     */
    public static boolean isValidCnpj(final String text) {

        final String d = digitsOnly(text);

        if (d.length() != 14 || allSameDigit(d)) {
            return false;
        }

        final int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        final int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        final int c1 = checkDigit(d, weights1, 12);
        final int c2 = checkDigit(d, weights2, 13);

        return c1 == (d.charAt(12) - '0') && c2 == (d.charAt(13) - '0');

    }

    // CPF-style: weights run startWeight, startWeight-1, ... over the first len digits.
    private static int checkDigit(final String d, final int len, final int startWeight) {

        int sum = 0;
        for (int i = 0; i < len; i++) {
            sum += (d.charAt(i) - '0') * (startWeight - i);
        }

        final int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;

    }

    // CNPJ-style: explicit weight array over the first len digits.
    private static int checkDigit(final String d, final int[] weights, final int len) {

        int sum = 0;
        for (int i = 0; i < len; i++) {
            sum += (d.charAt(i) - '0') * weights[i];
        }

        final int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;

    }

    private static String digitsOnly(final String text) {
        return text == null ? "" : text.replaceAll("\\D", "");
    }

    private static boolean allSameDigit(final String d) {
        for (int i = 1; i < d.length(); i++) {
            if (d.charAt(i) != d.charAt(0)) {
                return false;
            }
        }
        return true;
    }

}
