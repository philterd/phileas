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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.services.filters.regex.DateFilter;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.DateFilterStrategy;
import ai.philterd.phileas.services.validators.DateSpanValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class DateFilterTest extends AbstractFilterTest {
    
    private FilterConfiguration buildFilterConfiguration() {

        return new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new DateFilterStrategy()))
                .build();

    }

    @Test
    public void filterDate1() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "May 22, 1999");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 12, FilterType.DATE));
        Assertions.assertEquals("May 22, 1999", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDate2() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "13-06-31");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate3() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "2205-02-31");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 10, FilterType.DATE));

    }

    @Test
    public void filterDate4() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "02-31-2019");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 10, FilterType.DATE));

    }

    @Test
    public void filterDate5() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "02-31-19");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate6() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context",  PIECE, "2-8-2019");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate7() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "2-15-2019");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 9, FilterType.DATE));

    }

    @Test
    public void filterDate8() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "January 2012");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 12, FilterType.DATE));

    }

    @Test
    public void filterDate9() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "December 2015");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 13, FilterType.DATE));

    }

    @Test
    public void filterDate10() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "November 1999");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 13, FilterType.DATE));

    }

    @Test
    public void filterDate11() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "april 1999");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 10, FilterType.DATE));

    }

    @Test
    public void filterDate12() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "12-05-2014");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 10, FilterType.DATE));

    }

    @Test
    public void filterDate13() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "November 22, 1999");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 17, FilterType.DATE));

    }

    @Test
    public void filterDate14() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "November 22nd, 1999");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 19, FilterType.DATE));

    }

    @Test
    public void filterDate15() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "November 22 nd, 1999");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 20, FilterType.DATE));

    }

    @Test
    public void filterDate16() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "November 22nd");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 13, FilterType.DATE));

    }

    @Test
    public void filterDate17() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "May 1 st");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate18() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "June 13th");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 9, FilterType.DATE));

    }

    @Test
    public void filterDate19() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "November 2, 1999");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 16, FilterType.DATE));

    }

    @Test
    public void filterDate20() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "May 1st");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 7, FilterType.DATE));

    }

    @Test
    public void filterDate21() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "December 4th");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 12, FilterType.DATE));

    }

    @Test
    public void filterDate22() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "02-31-19@12:00");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate23() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "02-31-19@12:00");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());

    }

    @Test
    public void filterDate24() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "02-35-19@12:00");
        Assertions.assertEquals(1, filtered.getSpans().size());

    }

    @Test
    public void filterDate25() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "02-15-19");
        Assertions.assertEquals(1, filtered.getSpans().size());

    }

    @Test
    public void filterDate26() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "The good news is everywhere we go it is that way but this may be on top of that.");
        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterDate27() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "The good news is everywhere we go it is that way but this may 15 be on top of that.");
        Assertions.assertEquals(1, filtered.getSpans().size());

    }

    @Test
    public void filterDate28() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "The good news is everywhere we go it is that way but this may 15, 2020 be on top of that.");
        Assertions.assertEquals(1, filtered.getSpans().size());

    }

    @Test
    public void filterDate29() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "The good news is everywhere we go it is that way but this may 15 2020 be on top of that.");
        Assertions.assertEquals(1, filtered.getSpans().size());

    }

    @Test
    public void filterDate30() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "The good news is everywhere we go it is that way but this may 15 19 be on top of that.");
        Assertions.assertEquals(1, filtered.getSpans().size());

    }

    @Test
    public void filterDate31() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "The good news is everywhere we go it is that way but this may 5 19 be on top of that.");
        Assertions.assertEquals(1, filtered.getSpans().size());

    }

    @Test
    public void filterDate32() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "The good news is everywhere we go it is that way but this June 21, 2020 be on top of that.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("June 21, 2020", filtered.getSpans().get(0).getText());

        showSpans(filtered.getSpans());

    }

    @Test
    public void filterDate33() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "The good news is everywhere we go it is that way but this 09-2021 be on top of that.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("09-2021", filtered.getSpans().get(0).getText());

        showSpans(filtered.getSpans());

    }

    @Test
    public void filterDate34() throws Exception {

        final DateFilterStrategy dateFilterStrategy = new DateFilterStrategy();
        dateFilterStrategy.setStrategy(AbstractFilterStrategy.RELATIVE);
        dateFilterStrategy.setShiftYears(3);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(dateFilterStrategy))
                .build();

        final DateFilter filter = new DateFilter(filterConfiguration, false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Owns and drives his own vehicle but states he has not driven his car since last October 2009.");

        LOGGER.info(filtered.getSpans().get(0).getReplacement());

        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        // This value can differ depending on when the test is run.
        Assertions.assertTrue(filtered.getSpans().get(0).getReplacement().startsWith("16 years") || filtered.getSpans().get(0).getReplacement().startsWith("14 years"));

    }

    @Test
    public void filterDate35() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Observation: 91-100% strong nuclear staining");
        Assertions.assertEquals(0, filtered.getSpans().size());

        showSpans(filtered.getSpans());

    }

    @Test
    public void filterDate36() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "That on July 3, 2012 an involuntary petition on behalf of FKAAHS, Inc. fka Aire");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(8, filtered.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(20, filtered.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("July 3, 2012", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDate37() throws Exception {

        // See PHL-204: The date is not being found when onlyValidDates=true.

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "That on July 3, 2012 an involuntary petition on behalf of FKAAHS, Inc. fka Aire");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(8, filtered.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(20, filtered.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("July 3, 2012", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDate38() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Case No. 12-12110 K");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(9, filtered.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(16, filtered.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("12-1211", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDate39() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Case No. 12-12110 K");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterDate40() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Case 1-20-01023-MJK");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterDate41() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "That on July 3, 2012 an involuntary");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(8, filtered.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(20, filtered.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("July 3, 2012", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDate42() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Entered 06/16/20 11:55:37,");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(8, filtered.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(16, filtered.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("06/16/20", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDate43() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Case 1-20-01023-MJK,    Doc 1,    Filed 06/16/20,    Entered 06/16/20 11:55:37,");

        showSpans(filtered.getSpans());

        // dropOverlappingSpans now returns non-overlapping spans in ascending start order.
        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertEquals(40, filtered.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(48, filtered.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("06/16/20", filtered.getSpans().get(0).getText());
        Assertions.assertEquals(61, filtered.getSpans().get(1).getCharacterStart());
        Assertions.assertEquals(69, filtered.getSpans().get(1).getCharacterEnd());
        Assertions.assertEquals("06/16/20", filtered.getSpans().get(1).getText());

    }

    @Test
    public void filterDate44() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "On August 22, 2012, Plaintiff, Wendy J. Christophersen, was appointed as interim");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(3, filtered.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(18, filtered.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("August 22, 2012", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDate45() throws Exception {

        // PHL-239: Support dates like Aug. 31, 2020

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "On Aug. 31, 2020, Plaintiff, Wendy J. Christophersen, was appointed as interim");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(3, filtered.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(16, filtered.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("Aug. 31, 2020", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDate46() throws Exception {

        // PHL-239: Support dates like Aug. 31, 2020

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "The date of March 4 1932 was fun");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(12, filtered.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(24, filtered.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("March 4 1932", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDate47() throws Exception {

        // day-first numeric dates (DD/MM/YYYY) are detected with only-valid-dates on.

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "25/12/1980");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("25/12/1980", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDate48() throws Exception {

        // day-first dates with the '-' and '.' delimiters are detected too.

        final DateFilter dashFilter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());
        final Filtered dashFiltered = dashFilter.filter(contextService, getPolicy(), "context", PIECE, "25-12-1980");
        showSpans(dashFiltered.getSpans());
        Assertions.assertEquals(1, dashFiltered.getSpans().size());
        Assertions.assertEquals("25-12-1980", dashFiltered.getSpans().get(0).getText());

        final DateFilter dotFilter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());
        final Filtered dotFiltered = dotFilter.filter(contextService, getPolicy(), "context", PIECE, "25.12.1980");
        showSpans(dotFiltered.getSpans());
        Assertions.assertEquals(1, dotFiltered.getSpans().size());
        Assertions.assertEquals("25.12.1980", dotFiltered.getSpans().get(0).getText());

    }

    @Test
    public void filterDate49() throws Exception {

        // a two-digit-year day-first date is detected with only-valid-dates on.

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "25/12/80");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("25/12/80", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDate50() throws Exception {

        // an ambiguous date is still redacted exactly once (month-first kept).

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "03/04/1981");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("03/04/1981", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDate51() throws Exception {

        // with only-valid-dates on, an impossible date is not redacted as a full date.

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "31/02/1980");

        showSpans(filtered.getSpans());

        for(final var span : filtered.getSpans()) {
            Assertions.assertNotEquals("31/02/1980", span.getText());
        }

    }

    @Test
    public void filterDate52() throws Exception {

        // existing month-first detection is unchanged.

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "12/25/1980");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("12/25/1980", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDate53() throws Exception {

        // A day-first date under the SHIFT strategy is still redacted. The replacement was computed
        // upstream with the month-first format, which is not a real date, so SHIFT falls back to
        // redaction (the date is removed, but not shifted).

        final DateFilterStrategy dateFilterStrategy = new DateFilterStrategy();
        dateFilterStrategy.setStrategy(AbstractFilterStrategy.SHIFT);
        dateFilterStrategy.setShiftDays(5);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(dateFilterStrategy))
                .build();

        final DateFilter filter = new DateFilter(filterConfiguration, true, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "25/12/1980");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("25/12/1980", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("{{{REDACTED-date}}}", filtered.getSpans().get(0).getReplacement());

    }

    @Test
    public void filterDate54() throws Exception {

        // The '.' delimiter must not turn a decimal number into a date. The month-and-year pattern
        // is not generated for '.', so "3.14" is not detected even with only-valid-dates off.

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "The value is 3.14 today");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("2000-01-01", "1999-12-31");

        final DateFilterStrategy dateFilterStrategy = new DateFilterStrategy();
        dateFilterStrategy.setStrategy(RANDOM_REPLACE);
        dateFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(dateFilterStrategy))
                .build();

        final DateFilter filter = new DateFilter(filterConfiguration, false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "May 22, 1999");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

}
