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
package ai.philterd.phileas.utils;

/**
 * The Luhn (mod 10) check used to weed out numbers that cannot be valid payment card numbers,
 * kept in-tree so Phileas does not depend on commons-validator (which pulled in four further
 * transitive jars).
 *
 * <p>Behaviour matches commons-validator's {@code LuhnCheckDigit.isValid}: the supplied string
 * must be non-null, non-empty, and contain only ASCII digits, and the Luhn-weighted sum of those
 * digits must be a non-zero multiple of ten. Any other input (null, empty, containing a non-digit,
 * or all zeros) is not valid. The all-zeros case is rejected to match commons-validator, which
 * treats a zero weighted sum as an invalid code rather than a passing checksum.</p>
 *
 * <p>This is the strict variant used for already-normalised, digits-only payment card numbers. It
 * is deliberately distinct from {@code ai.philterd.phileas.services.validators.LuhnValidator}, the
 * lenient variant used by the {@code identifier} filter, which skips separators in formatted input
 * and accepts an all-zero value. Keep the two separate: the card path needs the strict contract.</p>
 */
public final class LuhnCheck {

    private LuhnCheck() {
        // Utility class; not instantiable.
    }

    /**
     * Returns {@code true} when the digit string passes the Luhn checksum.
     *
     * @param code the candidate number as a string of ASCII digits, may be {@code null}
     * @return {@code true} if the value is non-empty, all digits, and Luhn-valid
     */
    public static boolean isValid(final String code) {

        if (code == null || code.isEmpty()) {
            return false;
        }

        int sum = 0;

        // Walk right to left. The right-most digit (the check digit) has weight one; every second
        // digit moving left is doubled, and a doubled value above nine has nine subtracted (the
        // same as summing its two decimal digits).
        boolean doubling = false;

        for (int i = code.length() - 1; i >= 0; i--) {

            final char c = code.charAt(i);

            if (c < '0' || c > '9') {
                return false;
            }

            int digit = c - '0';

            if (doubling) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubling = !doubling;

        }

        // commons-validator treats an all-zero code (zero weighted sum) as invalid, not as a
        // passing checksum, so reject it here to keep identical behaviour.
        return sum != 0 && sum % 10 == 0;

    }

}
