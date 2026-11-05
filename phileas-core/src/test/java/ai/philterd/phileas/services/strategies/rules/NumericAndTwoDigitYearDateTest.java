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

import ai.philterd.phileas.filters.AbstractFilterTest;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.services.filters.regex.DateFilter;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.validators.DateSpanValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;

/**
 * Two-digit years (issue #469) and day-first numeric dates (issue #467) under TRUNCATE_TO_YEAR, SHIFT, and
 * RELATIVE. The strategy reads the current date from a fixed clock, so no result depends on today's date.
 */
public class NumericAndTwoDigitYearDateTest extends AbstractFilterTest {

    private static final String REDACTED = "{{{REDACTED-date}}}";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC);

    /** The date's span after the date filter with one strategy, configured by the given consumer. */
    private Span span(final Consumer<DateFilterStrategy> configure, final String date, final boolean onlyValidDates) throws Exception {

        final DateFilterStrategy strategy = new DateFilterStrategy();
        strategy.setClock(CLOCK);
        configure.accept(strategy);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(strategy))
                .withWindowSize(windowSize)
                .build();

        final DateFilter filter = new DateFilter(filterConfiguration, onlyValidDates, DateSpanValidator.getInstance());
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "It was on " + date + " at the office.");

        Assertions.assertEquals(1, filtered.getSpans().size(), date);
        // Trimmed: the filter's span for "March 27" includes the space after it.
        Assertions.assertEquals(date, filtered.getSpans().get(0).getText().trim(), date);
        return filtered.getSpans().get(0);

    }

    private String truncate(final String date, final boolean onlyValidDates) throws Exception {
        return span(s -> s.setStrategy(AbstractFilterStrategy.TRUNCATE_TO_YEAR), date, onlyValidDates).getReplacement();
    }

    private String shift(final String date, final int days, final int years, final boolean onlyValidDates) throws Exception {
        return span(s -> {
            s.setStrategy(AbstractFilterStrategy.SHIFT);
            s.setShiftDays(days);
            s.setShiftYears(years);
        }, date, onlyValidDates).getReplacement();
    }

    private String relative(final String date, final boolean onlyValidDates) throws Exception {
        return span(s -> s.setStrategy(AbstractFilterStrategy.RELATIVE), date, onlyValidDates).getReplacement();
    }

    // Reading a two-digit year, with the current year supplied.

    @ParameterizedTest(name = "{0} in {1}")
    @CsvSource({
            "25,2026,2025", "26,2026,2026", "27,2026,1927",
            "65,2026,1965", "10,2026,2010", "0,2026,2000", "99,2026,1999",
            "0,2000,2000", "1,2000,1901", "99,2000,1999"
    })
    public void twoDigitYearIsTheMostRecentNotAfterTheCurrentYear(final int twoDigits, final int currentYear, final int expected) {
        Assertions.assertEquals(expected, TwoDigitYear.resolve(twoDigits, currentYear));
    }

    @Test
    public void readersResolveAroundTheCurrentYear() {
        // A year one before, equal to, and one after the year written.
        Assertions.assertEquals(1926, NumericDate.parse("3/27/26", 2025, false).orElseThrow().date().getYear());
        Assertions.assertEquals(2026, NumericDate.parse("3/27/26", 2026, false).orElseThrow().date().getYear());
        Assertions.assertEquals(2026, NumericDate.parse("3/27/26", 2027, false).orElseThrow().date().getYear());
        Assertions.assertEquals(1926, MonthNameDate.parse("March 27th, 26", 2025).orElseThrow().date().getYear());
        Assertions.assertEquals(2026, MonthNameDate.parse("March 27th, 26", 2026).orElseThrow().date().getYear());
        Assertions.assertEquals(2026, MonthNameDate.parse("March 27th, 26", 2027).orElseThrow().date().getYear());
    }

    @Test
    public void readersDoNotTakeAMonthNameAndOneNumberAsADate() {
        // "March 27" could be a day or a two-digit year, and "March 2765" is a month and a year.
        Assertions.assertTrue(MonthNameDate.parse("March 27", 2026).isEmpty());
        Assertions.assertTrue(MonthNameDate.parse("Mar 65", 2026).isEmpty());
        Assertions.assertFalse(MonthNameDate.parse("March 2765", 2026).orElseThrow().hasDay());
    }

    // Issue #469: two-digit years.

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "3/27/65|65", "3-27-65|65", "3.27.65|65", "2/29/00|00", "3/65|65",
            "March 27th, 65|65", "Mar. 5, 10|10", "March 27-65|65",
            "3/27/1965|1965", "03/27/1965|1965", "1965-03-27|1965"
    })
    public void truncateToYearWritesTheYearAsWritten(final String date, final String expected) throws Exception {
        Assertions.assertEquals(expected, truncate(date, false));
    }

    @ParameterizedTest(name = "{0} +{1}d +{2}y")
    @CsvSource(delimiter = '|', value = {
            "12/25/99|10|0|1/4/00",
            "3/27/65|10|0|4/6/65",
            "3-27-65|10|0|4-6-65",
            "2/29/00|10|0|3/10/00",
            "2/29/00|0|1|2/28/01",
            "March 27th, 65|10|0|April 6th, 65",
            "Mar. 5, 10|10|0|Mar. 15, 10",
            "March 27-65|10|0|April 6-65",
            "03/27/1965|10|1|04/06/1966",
            "3/27/1965|10|1|4/6/1966",
            "1965-03-27|10|1|1966-04-06"
    })
    public void shiftWritesTheYearWithItsWidth(final String date, final int days, final int years, final String expected) throws Exception {
        Assertions.assertEquals(expected, shift(date, days, years, false));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "3/27/65|3/27/1965", "3-27-65|3/27/1965", "March 27th, 65|3/27/1965", "12/25/99|12/25/1999",
            "Mar. 5, 10|3/5/2010", "3/65|3/1/1965"
    })
    public void relativeDescribesTheResolvedDate(final String date, final String fourDigitDate) throws Exception {
        final String expected = relative(fourDigitDate, false);
        Assertions.assertTrue(expected.endsWith(" ago"), expected);
        Assertions.assertEquals(expected, relative(date, false));
    }

    @Test
    public void relativeOnASixtyFiveDateIsAboutSixtyOneYearsAgo() throws Exception {
        Assertions.assertEquals("61 years 7 months ago", relative("3/27/65", false));
    }

    @Test
    public void shiftRedactsAMonthAndTwoDigitYear() throws Exception {
        Assertions.assertEquals(REDACTED, shift("3/65", 10, 0, false));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"2/29/01", "March 27"})
    public void datesThatCannotBeReadAreRedacted(final String date) throws Exception {
        // 2001 was not a leap year, and "March 27" could be a day or a two-digit year.
        Assertions.assertEquals(REDACTED, truncate(date, false));
        Assertions.assertEquals(REDACTED, shift(date, 10, 0, false));
        Assertions.assertEquals(REDACTED, relative(date, false));
    }

    // Issue #467: day-first numeric dates.

    @ParameterizedTest(name = "{0} onlyValidDates={1}")
    @CsvSource(delimiter = '|', value = {
            "25/12/1980|false|1980|04/01/1981|12/25/1980|dd/MM/uuuu",
            "25/12/1980|true|1980|04/01/1981|12/25/1980|dd/MM/uuuu",
            "25-12-1980|false|1980|04-01-1981|12/25/1980|dd-MM-uuuu",
            "25-12-1980|true|1980|04-01-1981|12/25/1980|dd-MM-uuuu",
            "25.12.1980|false|1980|04.01.1981|12/25/1980|dd.MM.uuuu",
            "25.12.1980|true|1980|04.01.1981|12/25/1980|dd.MM.uuuu",
            "13/01/2020|false|2020|23/01/2020|01/13/2020|dd/MM/uuuu",
            "13/01/2020|true|2020|23/01/2020|01/13/2020|dd/MM/uuuu",
            "25/12/80|false|80|4/1/81|12/25/1980|d/M/u",
            "25/12/80|true|80|4/1/81|12/25/1980|d/M/u"
    })
    public void dayFirstDatesAreTransformed(final String date, final boolean onlyValidDates, final String year,
                                            final String shifted, final String monthFirst, final String format) throws Exception {

        Assertions.assertEquals(year, truncate(date, onlyValidDates));
        Assertions.assertEquals(shifted, shift(date, 10, 0, onlyValidDates));
        Assertions.assertEquals(relative(monthFirst, false), relative(date, onlyValidDates));

        // The span records the order its replacement was computed in.
        Assertions.assertEquals(format, span(s -> s.setStrategy(AbstractFilterStrategy.SHIFT), date, onlyValidDates).getPattern());

    }

    @ParameterizedTest(name = "onlyValidDates={0}")
    @ValueSource(booleans = {false, true})
    public void aDateValidInBothOrdersIsReadMonthFirst(final boolean onlyValidDates) throws Exception {

        // May 12, not December 5.
        Assertions.assertEquals("05/22/1980", shift("05/12/1980", 10, 0, onlyValidDates));
        Assertions.assertEquals(relative("5/12/1980", false), relative("05/12/1980", onlyValidDates));
        Assertions.assertEquals("MM/dd/uuuu", span(s -> s.setStrategy(AbstractFilterStrategy.SHIFT), "05/12/1980", onlyValidDates).getPattern());

    }

    @Test
    public void aDateValidInNeitherOrderIsRedacted() throws Exception {
        Assertions.assertEquals(REDACTED, shift("31/02/1980", 10, 0, false));
        Assertions.assertTrue(NumericDate.parse("31/02/1980", 2026, true).isEmpty());
        Assertions.assertEquals(LocalDate.of(1980, 12, 25), NumericDate.parse("25/12/1980", 2026, true).orElseThrow().date());
    }

}
