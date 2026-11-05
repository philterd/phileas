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
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A numeric date, such as {@code 03/27/1965}, {@code 25.12.1980}, {@code 3/27/65}, or {@code 1965-03-27},
 * read from its text rather than with its pattern's format, so that its day order and two-digit year are
 * decided in one place.
 *
 * <p>Two numbers before the year are read month first when that is a valid date, and day first otherwise,
 * so {@code 05/12/1980} is May 12 and {@code 25/12/1980} is December 25. This is the same rule the date
 * filter uses to record a span's format. A two-digit year is read with {@link TwoDigitYear#resolve}.</p>
 */
final class NumericDate implements DateText {

    private static final String DELIMITER = "(?<delimiter>[-/ .])";

    private static final Pattern YEAR_FIRST = Pattern.compile("(?<year>\\d{4})" + DELIMITER + "(?<first>\\d{2})\\k<delimiter>(?<second>\\d{2})");
    private static final Pattern YEAR_LAST = Pattern.compile("(?<first>\\d{1,2})" + DELIMITER + "(?<second>\\d{1,2})\\k<delimiter>(?<year>\\d{4}|\\d{2})");
    private static final Pattern MONTH_AND_YEAR = Pattern.compile("(?<first>\\d{1,2})" + DELIMITER + "(?<year>\\d{4}|\\d{2})");

    private final String text;
    private final Matcher matcher;
    private final String monthGroup;
    private final String dayGroup;
    private final LocalDate date;
    private final boolean padded;

    private NumericDate(final String text, final Matcher matcher, final String monthGroup, final String dayGroup,
                        final LocalDate date, final boolean padded) {
        this.text = text;
        this.padded = padded;
        this.matcher = matcher;
        this.monthGroup = monthGroup;
        this.dayGroup = dayGroup;
        this.date = date;
    }

    /**
     * Reads a numeric date.
     * @param text The date's text, such as a span's text.
     * @param currentYear The current year, for reading a two-digit year.
     * @param padded Whether another date is written with a two-digit month and day, as the pattern that found
     * this one requires (its format has {@code MM}, as in {@code MM/dd/uuuu}), or with as few digits as needed.
     * @return The date, or empty if the text is not a numeric date with a two- or four-digit year, or is not
     * a real date in either order.
     */
    static Optional<NumericDate> parse(final String text, final int currentYear, final boolean padded) {

        if(text == null) {
            return Optional.empty();
        }

        final String trimmed = text.trim();

        Matcher matcher = YEAR_FIRST.matcher(trimmed);
        if(matcher.matches()) {
            return date(trimmed, matcher, "first", "second", currentYear, padded);
        }

        matcher = YEAR_LAST.matcher(trimmed);
        if(matcher.matches()) {
            final Optional<NumericDate> monthFirst = date(trimmed, matcher, "first", "second", currentYear, padded);
            return monthFirst.isPresent() ? monthFirst : date(trimmed, matcher, "second", "first", currentYear, padded);
        }

        matcher = MONTH_AND_YEAR.matcher(trimmed);
        if(matcher.matches()) {
            return date(trimmed, matcher, "first", null, currentYear, padded);
        }

        return Optional.empty();

    }

    private static Optional<NumericDate> date(final String text, final Matcher matcher, final String monthGroup,
                                              final String dayGroup, final int currentYear, final boolean padded) {

        final String yearText = matcher.group("year");
        final int year = yearText.length() == 2
                ? TwoDigitYear.resolve(Integer.parseInt(yearText), currentYear)
                : Integer.parseInt(yearText);
        final int day = dayGroup == null ? 1 : Integer.parseInt(matcher.group(dayGroup));

        try {
            return Optional.of(new NumericDate(text, matcher, monthGroup, dayGroup,
                    LocalDate.of(year, Integer.parseInt(matcher.group(monthGroup)), day), padded));
        } catch (final DateTimeException e) {
            return Optional.empty();
        }

    }

    @Override
    public boolean hasDay() {
        return dayGroup != null;
    }

    @Override
    public LocalDate date() {
        return date;
    }

    /** The year as written: two digits for a two-digit year, so {@code 2/29/00} gives {@code 00}. */
    @Override
    public String yearText() {
        final String yearText = matcher.group("year");
        return yearText.length() == 2 ? yearText : String.valueOf(date.getYear());
    }

    /** Writes another date in this date's form: the same order, delimiter, padding, and year width. */
    @Override
    public String format(final LocalDate other) {

        final Map<Integer, Integer> ends = new TreeMap<>(Comparator.reverseOrder());
        final Map<Integer, String> values = new TreeMap<>();

        ends.put(matcher.start(monthGroup), matcher.end(monthGroup));
        values.put(matcher.start(monthGroup), number(other.getMonthValue(), padded));

        if(dayGroup != null) {
            ends.put(matcher.start(dayGroup), matcher.end(dayGroup));
            values.put(matcher.start(dayGroup), number(other.getDayOfMonth(), padded));
        }

        ends.put(matcher.start("year"), matcher.end("year"));
        values.put(matcher.start("year"), matcher.group("year").length() == 2
                ? TwoDigitYear.format(other.getYear())
                : String.format(Locale.ROOT, "%04d", other.getYear()));

        // Replace from the end so earlier positions still hold.
        final StringBuilder result = new StringBuilder(text);
        for(final Map.Entry<Integer, Integer> position : ends.entrySet()) {
            result.replace(position.getKey(), position.getValue(), values.get(position.getKey()));
        }

        return result.toString();

    }

    private static String number(final int value, final boolean padded) {
        return padded ? String.format(Locale.ROOT, "%02d", value) : String.valueOf(value);
    }

}
