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
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Structural validator for SWIFT/BIC codes (ISO 9362). SWIFT/BIC has no checksum, so this checks
 * the structure instead: 4 letters (institution), 2 letters (country), 2 alphanumeric (location),
 * and an optional 3 alphanumeric (branch), giving a length of 8 or 11. The country segment must be
 * a valid ISO 3166-1 alpha-2 code. The check is case-insensitive and ignores surrounding
 * whitespace; it is structural only, not a checksum.
 */
public class BicStructuralValidator implements SpanValidator {

    // 4 letters institution, 2 letters country, 2 alphanumeric location, optional 3 alphanumeric branch.
    private static final Pattern BIC_PATTERN = Pattern.compile("[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?");

    // ISO 3166-1 alpha-2 country codes, from the JDK rather than a hand-maintained list.
    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

    private static SpanValidator spanValidator;

    public static SpanValidator getInstance() {

        if (spanValidator == null) {
            spanValidator = new BicStructuralValidator();
        }

        return spanValidator;
    }

    private BicStructuralValidator() {
        // Use the static getInstance().
    }

    @Override
    public boolean validate(final Span span) {
        return span != null && isValid(span.getText());
    }

    /**
     * Checks that the given text is a structurally valid BIC with a recognized country code.
     *
     * @param text the matched text.
     * @return {@code true} if the text is a structurally valid BIC.
     */
    public static boolean isValid(final String text) {

        if (text == null) {
            return false;
        }

        final String bic = text.trim().toUpperCase(Locale.ROOT);

        if (bic.length() != 8 && bic.length() != 11) {
            return false;
        }

        if (!BIC_PATTERN.matcher(bic).matches()) {
            return false;
        }

        // Characters 5 and 6 (1-based) are the ISO 3166-1 alpha-2 country code.
        final String country = bic.substring(4, 6);

        return ISO_COUNTRIES.contains(country);

    }

}
