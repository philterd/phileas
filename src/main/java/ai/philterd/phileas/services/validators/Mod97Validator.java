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

import java.math.BigInteger;
import java.util.Locale;
import java.util.Map;

/**
 * Control-key validator based on a value mod 97. Two variants:
 *
 * <ul>
 *     <li>{@code nir}: the French social-security number (INSEE/NIR). 13-character body plus a
 *     two-digit key, where key = 97 - (body mod 97). Corsica department codes 2A and 2B are
 *     substituted (2A to 19, 2B to 18) before the body is read as a number; the substitution map
 *     can be overridden via the {@code substitutions} parameter.</li>
 *     <li>{@code iban}: an IBAN, validated by the ISO 13616 / ISO 7064 MOD-97-10 rule (move the
 *     first four characters to the end, map letters A-Z to 10-35, and require the value mod 97 to
 *     equal 1).</li>
 * </ul>
 */
public class Mod97Validator implements SpanValidator {

    public enum Variant {
        NIR,
        IBAN
    }

    static final Map<String, String> DEFAULT_NIR_SUBSTITUTIONS = Map.of("2A", "19", "2B", "18");

    private final Variant variant;
    private final Map<String, String> substitutions;

    private Mod97Validator(final Variant variant, final Map<String, String> substitutions) {
        this.variant = variant;
        this.substitutions = substitutions;
    }

    /**
     * @param params the validator parameters; must include a {@code variant} of {@code nir} or
     *               {@code iban}, and may include {@code substitutions} for the nir variant.
     */
    public static SpanValidator fromParams(final Map<String, Object> params) {

        final String variant = ValidatorParams.string(params, "variant");

        if (variant == null) {
            throw new IllegalArgumentException("The mod97 validator requires a 'variant' parameter (nir or iban).");
        }

        switch (variant.toLowerCase(Locale.ROOT)) {

            case "nir":
                return new Mod97Validator(Variant.NIR, ValidatorParams.stringMap(params, "substitutions", DEFAULT_NIR_SUBSTITUTIONS));

            case "iban":
                return new Mod97Validator(Variant.IBAN, Map.of());

            default:
                throw new IllegalArgumentException("Unsupported mod97 variant '" + variant + "'. Supported: nir, iban.");

        }

    }

    @Override
    public boolean validate(final Span span) {

        if (span == null) {
            return false;
        }

        return variant == Variant.NIR ? isValidNir(span.getText(), substitutions) : isValidIban(span.getText());

    }

    /**
     * Validates a French NIR using the given Corsica letter substitutions.
     */
    public static boolean isValidNir(final String text, final Map<String, String> substitutions) {

        if (text == null) {
            return false;
        }

        final String s = text.replaceAll("\\s", "").toUpperCase(Locale.ROOT);

        if (s.length() != 15) {
            return false;
        }

        String body = s.substring(0, 13);
        final String key = s.substring(13);

        if (!key.matches("\\d{2}")) {
            return false;
        }

        for (final Map.Entry<String, String> entry : substitutions.entrySet()) {
            body = body.replace(entry.getKey(), entry.getValue());
        }

        if (!body.matches("\\d{13}")) {
            return false;
        }

        final long n = Long.parseLong(body);
        final int expectedKey = 97 - (int) (n % 97);

        return expectedKey == Integer.parseInt(key);

    }

    /**
     * Validates an IBAN by the MOD-97-10 rule (value mod 97 equals 1).
     */
    public static boolean isValidIban(final String text) {

        if (text == null) {
            return false;
        }

        final String s = text.replaceAll("\\s", "").toUpperCase(Locale.ROOT);

        if (!s.matches("[A-Z]{2}\\d{2}[A-Z0-9]+") || s.length() < 5 || s.length() > 34) {
            return false;
        }

        final String rearranged = s.substring(4) + s.substring(0, 4);

        final StringBuilder numeric = new StringBuilder();
        for (int i = 0; i < rearranged.length(); i++) {
            final char c = rearranged.charAt(i);
            if (c >= '0' && c <= '9') {
                numeric.append(c);
            } else {
                // A-Z map to 10-35.
                numeric.append(10 + (c - 'A'));
            }
        }

        return new BigInteger(numeric.toString()).mod(BigInteger.valueOf(97)).intValue() == 1;

    }

}
