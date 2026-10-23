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

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A date written with a month name, such as {@code March 27, 1965}, {@code Mar. 5th 2020}, or
 * {@code 27-Mar-1965}, read from its text rather than with a {@code DateTimeFormatter}. One date pattern
 * matches full and abbreviated months, one- and two-digit days, ordinals, and several separators, which no
 * single formatter pattern can both parse and write back. A changed date replaces the month, day, and year
 * in place, so it keeps the form of the original.
 *
 * <p>Only a four-digit year is read. A two-digit year is ambiguous, so such a date is not parsed.</p>
 */
final class MonthNameDate {

    private static final String[] MONTHS = {"January", "February", "March", "April", "May", "June", "July",
            "August", "September", "October", "November", "December"};

    private static final String MONTH = "(?<month>jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|june?|july?"
            + "|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\.?";
    private static final String DAY = "(?<day>\\d{1,2})(?<ordinal>st|nd|rd|th)?";
    private static final String YEAR = "(?<year>\\d{4})";
    private static final String SEPARATOR = "[\\s,./\\-\\u2013\\u2014]*";

    private static final Pattern MONTH_FIRST = Pattern.compile(MONTH + SEPARATOR + DAY + SEPARATOR + YEAR, Pattern.CASE_INSENSITIVE);
    private static final Pattern DAY_FIRST = Pattern.compile(DAY + SEPARATOR + MONTH + SEPARATOR + YEAR, Pattern.CASE_INSENSITIVE);
    private static final Pattern MONTH_AND_YEAR = Pattern.compile(MONTH + SEPARATOR + YEAR, Pattern.CASE_INSENSITIVE);

    private final String text;
    private final Matcher matcher;
    private final boolean hasDay;
    private final LocalDate date;

    private MonthNameDate(final String text, final Matcher matcher, final boolean hasDay, final LocalDate date) {
        this.text = text;
        this.matcher = matcher;
        this.hasDay = hasDay;
        this.date = date;
    }

    /**
     * Reads a month-name date.
     * @param text The date's text, such as a span's text.
     * @return The date, or empty if the text is not a month-name date with a four-digit year, or names a day
     * the month does not have.
     */
    static Optional<MonthNameDate> parse(final String text) {

        if(text == null) {
            return Optional.empty();
        }

        for(final Pattern form : List.of(MONTH_FIRST, DAY_FIRST, MONTH_AND_YEAR)) {

            final Matcher matcher = form.matcher(text.trim());

            if(matcher.matches()) {

                final boolean hasDay = form != MONTH_AND_YEAR;
                final int year = Integer.parseInt(matcher.group("year"));
                final int month = monthNumber(matcher.group("month"));
                final int day = hasDay ? Integer.parseInt(matcher.group("day")) : 1;

                try {
                    return Optional.of(new MonthNameDate(text.trim(), matcher, hasDay, LocalDate.of(year, month, day)));
                } catch (final DateTimeException e) {
                    return Optional.empty();
                }

            }

        }

        return Optional.empty();

    }

    /** Whether the date has a day. A month and year alone, such as {@code March 1965}, does not. */
    boolean hasDay() {
        return hasDay;
    }

    /** The date. A month and year alone is the first of the month. */
    LocalDate date() {
        return date;
    }

    /**
     * Writes another date in this date's form: the same month spelling and case, separators, ordinal, and
     * day padding.
     * @param other The date to write.
     * @return The date's text.
     */
    String format(final LocalDate other) {

        final List<int[]> positions = new ArrayList<>();
        final List<String> values = new ArrayList<>();

        positions.add(new int[]{matcher.start("month"), matcher.end("month")});
        values.add(monthText(matcher.group("month"), other.getMonthValue()));

        positions.add(new int[]{matcher.start("year"), matcher.end("year")});
        values.add(String.format(Locale.ROOT, "%04d", other.getYear()));

        if(hasDay) {
            final int end = matcher.group("ordinal") == null ? matcher.end("day") : matcher.end("ordinal");
            positions.add(new int[]{matcher.start("day"), end});
            values.add(dayText(matcher.group("day"), matcher.group("ordinal"), other.getDayOfMonth()));
        }

        // Replace from the end so earlier positions still hold.
        final List<Integer> order = new ArrayList<>();
        for(int i = 0; i < positions.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingInt((Integer i) -> positions.get(i)[0]).reversed());

        final StringBuilder result = new StringBuilder(text);
        for(final int i : order) {
            result.replace(positions.get(i)[0], positions.get(i)[1], values.get(i));
        }

        return result.toString();

    }

    private static int monthNumber(final String month) {

        final String prefix = month.substring(0, 3).toLowerCase(Locale.ROOT);

        for(int i = 0; i < MONTHS.length; i++) {
            if(MONTHS[i].toLowerCase(Locale.ROOT).startsWith(prefix)) {
                return i + 1;
            }
        }

        throw new IllegalArgumentException("Not a month name.");

    }

    /** The month's name, abbreviated if the original was (May counts as full), in the original's case. */
    private static String monthText(final String original, final int month) {

        final boolean abbreviated = original.length() == 3 && !original.equalsIgnoreCase("may");
        final String name = abbreviated ? MONTHS[month - 1].substring(0, 3) : MONTHS[month - 1];

        return inCaseOf(original, name);

    }

    /** The day, zero-padded if the original was, with an ordinal if the original had one. */
    private static String dayText(final String originalDay, final String originalOrdinal, final int day) {

        final String digits = originalDay.length() == 2 && originalDay.startsWith("0")
                ? String.format(Locale.ROOT, "%02d", day)
                : String.valueOf(day);

        if(originalOrdinal == null) {
            return digits;
        }

        final String suffix;
        if(day % 100 >= 11 && day % 100 <= 13) {
            suffix = "th";
        } else if(day % 10 == 1) {
            suffix = "st";
        } else if(day % 10 == 2) {
            suffix = "nd";
        } else if(day % 10 == 3) {
            suffix = "rd";
        } else {
            suffix = "th";
        }

        return digits + (originalOrdinal.equals(originalOrdinal.toUpperCase(Locale.ROOT)) ? suffix.toUpperCase(Locale.ROOT) : suffix);

    }

    private static String inCaseOf(final String original, final String value) {

        if(original.equals(original.toUpperCase(Locale.ROOT))) {
            return value.toUpperCase(Locale.ROOT);
        }

        if(original.equals(original.toLowerCase(Locale.ROOT))) {
            return value.toLowerCase(Locale.ROOT);
        }

        return value;

    }

}
