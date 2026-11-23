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
package ai.philterd.phileas.services.strategies.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the age in years that an age filter span expresses, so a condition such as {@code token > 89} can
 * compare it with a number.
 *
 * <p>The number is the first number written with digits in the span, such as {@code 47} in {@code Age: 47} or
 * {@code 3.5} in {@code 3.5 years old}, or else the spelled-out number, such as {@code thirty-five} or
 * {@code one hundred and two}. A span with a year unit ({@code 47 years old}, {@code 47yo}, {@code 61 y/o}) is
 * in years. A span that starts with an age keyword ({@code age 47}, {@code aged 47}, {@code age of 47}) ends at
 * the number, so the word after it in the window gives the unit: months, weeks, or days are converted to years,
 * with a month as a twelfth of a year and a week as seven of a year's 365 days. Any other word, or none, leaves
 * the number in years.</p>
 */
final class AgeInYears {

    private static final Pattern DIGITS = Pattern.compile("[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+");
    private static final Pattern KEYWORD = Pattern.compile("age(?:d|\\s+of)?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NON_LETTERS = Pattern.compile("[^a-z]+");

    // The same normalization Filter.getWindow applies to the window's tokens.
    private static final Pattern WHITESPACE = Pattern.compile("\\s");
    private static final Pattern PUNCTUATION = Pattern.compile("\\p{Punct}");

    private static final Map<String, Integer> NUMBER_WORDS = Map.ofEntries(
            Map.entry("zero", 0), Map.entry("one", 1), Map.entry("two", 2), Map.entry("three", 3),
            Map.entry("four", 4), Map.entry("five", 5), Map.entry("six", 6), Map.entry("seven", 7),
            Map.entry("eight", 8), Map.entry("nine", 9), Map.entry("ten", 10), Map.entry("eleven", 11),
            Map.entry("twelve", 12), Map.entry("thirteen", 13), Map.entry("fourteen", 14), Map.entry("fifteen", 15),
            Map.entry("sixteen", 16), Map.entry("seventeen", 17), Map.entry("eighteen", 18), Map.entry("nineteen", 19),
            Map.entry("twenty", 20), Map.entry("thirty", 30), Map.entry("forty", 40), Map.entry("fifty", 50),
            Map.entry("sixty", 60), Map.entry("seventy", 70), Map.entry("eighty", 80), Map.entry("ninety", 90));

    private static final Map<String, Double> YEARS_PER_UNIT = Map.ofEntries(
            Map.entry("month", 1 / 12.0), Map.entry("months", 1 / 12.0), Map.entry("mo", 1 / 12.0),
            Map.entry("mos", 1 / 12.0), Map.entry("mth", 1 / 12.0), Map.entry("mths", 1 / 12.0),
            Map.entry("week", 7 / 365.0), Map.entry("weeks", 7 / 365.0), Map.entry("wk", 7 / 365.0),
            Map.entry("wks", 7 / 365.0),
            Map.entry("day", 1 / 365.0), Map.entry("days", 1 / 365.0));

    private AgeInYears() {
    }

    /**
     * The age in years that an age span expresses.
     * @param token The span's text.
     * @param window The window of words around the span, including the span's own words.
     * @return The age in years, or empty when the span has no number that can be read.
     */
    static OptionalDouble of(final String token, final String[] window) {

        if (token == null) {
            return OptionalDouble.empty();
        }

        final OptionalDouble number = number(token);

        if (number.isEmpty() || !KEYWORD.matcher(token.trim()).lookingAt()) {
            return number;
        }

        final Double yearsPerUnit = YEARS_PER_UNIT.get(wordAfter(token, window));

        return yearsPerUnit == null ? number : OptionalDouble.of(number.getAsDouble() * yearsPerUnit);

    }

    private static OptionalDouble number(final String token) {

        final Matcher digits = DIGITS.matcher(token);

        if (digits.find()) {
            return OptionalDouble.of(Double.parseDouble(digits.group()));
        }

        // A spelled-out number, as the age filter matches it: up to ninety-nine, optionally after
        // "(one|a) hundred (and)". Words before the number, such as "aged", are skipped, and the first
        // word after it, such as "years", ends it.
        int hundreds = 0;
        int rest = 0;
        boolean found = false;

        for (final String word : NON_LETTERS.split(token.toLowerCase(Locale.ROOT))) {
            if (NUMBER_WORDS.containsKey(word)) {
                rest += NUMBER_WORDS.get(word);
                found = true;
            } else if (word.equals("hundred")) {
                hundreds = (rest == 0 ? 1 : rest) * 100;
                rest = 0;
                found = true;
            } else if (found && !word.equals("and")) {
                break;
            }
        }

        return found ? OptionalDouble.of(hundreds + rest) : OptionalDouble.empty();

    }

    /** The lower-cased word in the window right after the span's words, or an empty string. */
    private static String wordAfter(final String token, final String[] window) {

        if (window == null) {
            return "";
        }

        final List<String> words = new ArrayList<>();
        for (final String raw : WHITESPACE.split(token.trim())) {
            final String word = PUNCTUATION.matcher(raw).replaceAll("");
            if (!word.isEmpty()) {
                words.add(word);
            }
        }

        for (int i = 0; i + words.size() < window.length; i++) {
            boolean matches = true;
            for (int j = 0; j < words.size() && matches; j++) {
                matches = words.get(j).equalsIgnoreCase(window[i + j]);
            }
            if (matches) {
                return window[i + words.size()].toLowerCase(Locale.ROOT);
            }
        }

        return "";

    }

}
