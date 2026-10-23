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

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Properties;

/**
 * SHIFT, TRUNCATE_TO_YEAR, and RELATIVE on dates written with a month name, through the date filter so
 * each span carries the format its pattern gives it. See issue #461.
 */
public class MonthNameDateStrategyTest {

    private static final String REDACTED = "{{{REDACTED-date}}}";

    private final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
            new DefaultContextService(), new InMemoryVectorService(), null);

    private String apply(final String strategy, final String date) throws Exception {
        final String json = "{\"identifiers\":{\"date\":{\"dateFilterStrategies\":[" + strategy + "]}}}";
        final String output = service.filter(new Gson().fromJson(json, Policy.class), "context",
                "It was on " + date + " at the office.").getFilteredText();
        Assertions.assertTrue(output.startsWith("It was on ") && output.endsWith(" at the office."), output);
        return output.substring("It was on ".length(), output.length() - " at the office.".length());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "March 27, 1965|1965",
            "March 27 1965|1965",
            "March 5, 2020|2020",
            "Mar 27, 1965|1965",
            "March 27th, 1965|1965",
            "Mar. 27, 1965|1965",
            "27 March 1965|1965",
            "27 March, 1965|1965",
            "27-Mar-1965|1965",
            "27th March 1965|1965",
            "March 27 - 1965|1965",
            "March 27-1965|1965",
            "MARCH 27, 1965|1965",
            "march 5 2020|2020",
            "March 1865|1865",
            "March 1965|1965",
            "Mar. 27, 2150|2150"
    })
    public void truncateToYear(final String date, final String expected) throws Exception {
        Assertions.assertEquals(expected, apply("{\"strategy\":\"TRUNCATE_TO_YEAR\"}", date));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "March 27, 1965|April 6, 1966",
            "March 27 1965|April 6 1966",
            "March 5, 2020|March 15, 2021",
            "Mar 27, 1965|Apr 6, 1966",
            "March 27th, 1965|April 6th, 1966",
            "Mar. 27, 1965|Apr. 6, 1966",
            "27 March 1965|6 April 1966",
            "27 March, 1965|6 April, 1966",
            "27-Mar-1965|6-Apr-1966",
            "27th March 1965|6th April 1966",
            "March 27 - 1965|April 6 - 1966",
            "March 27-1965|April 6-1966",
            "MARCH 27, 1965|APRIL 6, 1966",
            "march 5 2020|march 15 2021",
            "March 1st, 2020|March 11th, 2021",
            "March 12th, 2020|March 22nd, 2021",
            "March 13th, 2020|March 23rd, 2021",
            "March 1ST, 2020|March 11TH, 2021",
            "Jun 25, 2020|Jul 5, 2021",
            "May 3, 2020|May 13, 2021",
            "December 27, 1899|January 6, 1901"
    })
    public void shiftKeepsTheForm(final String date, final String expected) throws Exception {
        Assertions.assertEquals(expected, apply("{\"strategy\":\"SHIFT\",\"shiftDays\":10,\"shiftYears\":1}", date));
    }

    @Test
    public void shiftKeepsAZeroPaddedDay() throws Exception {
        Assertions.assertEquals("March 06, 2020", apply("{\"strategy\":\"SHIFT\",\"shiftDays\":1}", "March 05, 2020"));
        Assertions.assertEquals("06 March 2020", apply("{\"strategy\":\"SHIFT\",\"shiftDays\":1}", "05 March 2020"));
    }

    @Test
    public void shiftRandomChangesTheDateAndKeepsTheForm() throws Exception {
        for (int run = 0; run < 20; run++) {
            final String shifted = apply("{\"strategy\":\"SHIFT\",\"shiftRandom\":true}", "Mar. 27th, 1965");
            Assertions.assertTrue(shifted.matches("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\. \\d{1,2}(st|nd|rd|th), \\d{4}"), shifted);
            Assertions.assertNotEquals("Mar. 27th, 1965", shifted);
        }
    }

    @Test
    public void shiftRedactsAMonthAndYearAlone() throws Exception {
        // Shifting by days alone would leave the month and year unchanged.
        Assertions.assertEquals(REDACTED, apply("{\"strategy\":\"SHIFT\",\"shiftDays\":10}", "March 1965"));
        Assertions.assertEquals(REDACTED, apply("{\"strategy\":\"SHIFT\",\"shiftYears\":1}", "March 1965"));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"March 27, 1965", "March 27 1965", "Mar 27, 1965", "March 27th, 1965", "Mar. 27, 1965",
            "27 March 1965", "27 March, 1965", "27-Mar-1965", "27th March 1965", "March 27 - 1965", "March 27-1965",
            "MARCH 27, 1965", "march 27 1965"})
    public void relativeMatchesTheNumericDate(final String date) throws Exception {
        // The same date written numerically gives the expected relative text.
        final String expected = apply("{\"strategy\":\"RELATIVE\"}", "03/27/1965");
        Assertions.assertTrue(expected.matches("\\d+ years \\d+ months ago"), expected);
        Assertions.assertEquals(expected, apply("{\"strategy\":\"RELATIVE\"}", date));
    }

    @Test
    public void relativeTakesAMonthAndYearAsTheFirst() throws Exception {
        Assertions.assertEquals(apply("{\"strategy\":\"RELATIVE\"}", "03/01/1965"), apply("{\"strategy\":\"RELATIVE\"}", "March 1965"));
        Assertions.assertEquals(apply("{\"strategy\":\"RELATIVE\"}", "03/05/2020"), apply("{\"strategy\":\"RELATIVE\"}", "March 5, 2020"));
    }

    @Test
    public void relativeRedactsAFutureDateUnlessFutureDatesIsSet() throws Exception {
        final String date = "March 27, " + (LocalDate.now().getYear() + 2);
        Assertions.assertEquals(REDACTED, apply("{\"strategy\":\"RELATIVE\"}", date));
        Assertions.assertTrue(apply("{\"strategy\":\"RELATIVE\",\"futureDates\":true}", date).startsWith("in "));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"March 27", "March 27th, 65", "February 30, 2020", "Feb 29, 2021"})
    public void datesThatCannotBeReadAreRedacted(final String date) throws Exception {
        // A two-digit year is ambiguous, and a day the month does not have is not a date.
        for (final String strategy : new String[]{"{\"strategy\":\"TRUNCATE_TO_YEAR\"}", "{\"strategy\":\"SHIFT\",\"shiftDays\":10}", "{\"strategy\":\"RELATIVE\"}"}) {
            Assertions.assertEquals(REDACTED, apply(strategy, date), strategy);
        }
    }

    @Test
    public void numericDatesAreUnchanged() throws Exception {
        Assertions.assertEquals("1965", apply("{\"strategy\":\"TRUNCATE_TO_YEAR\"}", "03/27/1965"));
        Assertions.assertEquals("04/06/1966", apply("{\"strategy\":\"SHIFT\",\"shiftDays\":10,\"shiftYears\":1}", "03/27/1965"));
        Assertions.assertEquals("1966-04-06", apply("{\"strategy\":\"SHIFT\",\"shiftDays\":10,\"shiftYears\":1}", "1965-03-27"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "Sept 3, 2020|2020",
            "Sept. 3, 2020|2020",
            "Sept 3rd, 2020|2020",
            "SEPT 3, 2020|2020",
            "3 Sept 2020|2020",
            "3 Sept, 2020|2020",
            "3-Sept-2020|2020",
            "Sept 2020|2020",
            "Sept 3 2020|2020"
    })
    public void truncateToYearOnSept(final String date, final String expected) throws Exception {
        // https://github.com/philterd/phileas/issues/466
        Assertions.assertEquals(expected, apply("{\"strategy\":\"TRUNCATE_TO_YEAR\"}", date));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            // A month that does not change is written as it was.
            "Sept 3, 2020|Sept 13, 2021",
            "Sept. 3rd, 2020|Sept. 13th, 2021",
            "3 Sept 2020|13 Sept 2021",
            "3-Sept-2020|13-Sept-2021",
            "Sept 3 2020|Sept 13 2021",
            // A month that changes is abbreviated to three letters.
            "Sept 25, 2020|Oct 5, 2021",
            "SEPT 25, 2020|OCT 5, 2021",
            "Aug 25, 2020|Sep 4, 2021",
            "Aug. 25th, 2020|Sep. 4th, 2021",
            "August 25, 2020|September 4, 2021",
            "Sep 3, 2020|Sep 13, 2021"
    })
    public void shiftOnSept(final String date, final String expected) throws Exception {
        Assertions.assertEquals(expected, apply("{\"strategy\":\"SHIFT\",\"shiftDays\":10,\"shiftYears\":1}", date));
    }

    @Test
    public void shiftRedactsSeptAndYearAlone() throws Exception {
        Assertions.assertEquals(REDACTED, apply("{\"strategy\":\"SHIFT\",\"shiftYears\":1}", "Sept 2020"));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"Sept 3, 2020", "Sept. 3, 2020", "Sept 3rd, 2020", "SEPT 3, 2020", "3 Sept 2020", "3-Sept-2020", "Sept 3 2020"})
    public void relativeOnSeptMatchesTheNumericDate(final String date) throws Exception {
        Assertions.assertEquals(apply("{\"strategy\":\"RELATIVE\"}", "09/03/2020"), apply("{\"strategy\":\"RELATIVE\"}", date));
    }

    @Test
    public void parseReadsEachOrder() {
        Assertions.assertEquals(LocalDate.of(1965, 3, 27), MonthNameDate.parse("March 27, 1965").orElseThrow().date());
        Assertions.assertEquals(LocalDate.of(1965, 3, 27), MonthNameDate.parse("27th Mar. 1965").orElseThrow().date());
        Assertions.assertFalse(MonthNameDate.parse("March 1965").orElseThrow().hasDay());
        Assertions.assertTrue(MonthNameDate.parse("03/27/1965").isEmpty());
        Assertions.assertTrue(MonthNameDate.parse("Marching 27, 1965").isEmpty());
        Assertions.assertTrue(MonthNameDate.parse(null).isEmpty());
    }

}
