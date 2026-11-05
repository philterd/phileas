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
package ai.philterd.phileas.services.strategies.rules;

import java.util.Locale;

/** Reads and writes the two-digit years of dates such as {@code 3/27/65} and {@code March 27th, 65}. */
final class TwoDigitYear {

    private TwoDigitYear() {
    }

    /**
     * The most recent year ending in the two digits that is not after the current year. Most redacted
     * dates are in the past, so in 2026 {@code 65} is 1965, {@code 26} is 2026, and {@code 27} is 1927.
     * @param twoDigits The year's last two digits, 0 to 99.
     * @param currentYear The current year.
     * @return The year.
     */
    static int resolve(final int twoDigits, final int currentYear) {
        return currentYear - Math.floorMod(currentYear - twoDigits, 100);
    }

    /** A year's last two digits, zero-padded, as in {@code 00} for 2000. */
    static String format(final int year) {
        return String.format(Locale.ROOT, "%02d", Math.floorMod(year, 100));
    }

}
