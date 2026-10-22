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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.services.filters.regex.DateFilter;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.DateFilterStrategy;
import ai.philterd.phileas.services.validators.DateSpanValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

// The many near-identical test methods predate this fix; converting them to @ParameterizedTest
// groups would be a separate, unrelated refactor of the whole file.
@SuppressWarnings("java:S5976")
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
        // The replacement is relative to today, so compute the expected years rather than fixing them.
        final int years = Period.between(LocalDate.of(2009, 10, 1), LocalDate.now()).getYears();
        final String replacement = filtered.getSpans().get(0).getReplacement();
        Assertions.assertTrue(replacement.startsWith(years + " years ") && replacement.endsWith(" months ago"), replacement);

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

        // https://github.com/philterd/phileas/issues/341
        // "12-12110" is 2 digits, a delimiter, and 5 digits: not a M-u shaped date. The pattern
        // used to accept a truncated match ("12-1211") because it had no trailing boundary, so
        // nothing stopped it from ignoring the digit run's last character.
        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Case No. 12-12110 K");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterDate38b() throws Exception {

        // https://github.com/philterd/phileas/issues/341
        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Case 1-20-01023-MJK");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterDate38c() throws Exception {

        // https://github.com/philterd/phileas/pull/347#pullrequestreview
        // Space is also a word separator, so "date + space + digit" must not be treated the same
        // as "date + delimiter + digit" for the '-'/'/' delimiters.
        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "12 25 2020 4 days");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("12 25 2020", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDate38d() throws Exception {

        // https://github.com/philterd/phileas/pull/347#pullrequestreview
        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "01 15 2026 42 units");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("01 15 2026", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDate38e() throws Exception {

        // https://github.com/philterd/phileas/issues/353
        // A range without spaces: the #341 boundary dropped every date but the last.

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "seen 2024-06-01-2024-06-30");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertEquals("2024-06-01", filtered.getSpans().get(0).getText());
        Assertions.assertEquals(5, filtered.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(15, filtered.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("2024-06-30", filtered.getSpans().get(1).getText());
        Assertions.assertEquals(16, filtered.getSpans().get(1).getCharacterStart());
        Assertions.assertEquals(26, filtered.getSpans().get(1).getCharacterEnd());

    }

    @Test
    public void filterDate38f() throws Exception {

        // https://github.com/philterd/phileas/issues/353

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "log 2024-06-01-2024-06-30-2024-07-01");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(3, filtered.getSpans().size());
        Assertions.assertEquals("2024-06-01", filtered.getSpans().get(0).getText());
        Assertions.assertEquals(4, filtered.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(14, filtered.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("2024-06-30", filtered.getSpans().get(1).getText());
        Assertions.assertEquals(15, filtered.getSpans().get(1).getCharacterStart());
        Assertions.assertEquals(25, filtered.getSpans().get(1).getCharacterEnd());
        Assertions.assertEquals("2024-07-01", filtered.getSpans().get(2).getText());
        Assertions.assertEquals(26, filtered.getSpans().get(2).getCharacterStart());
        Assertions.assertEquals(36, filtered.getSpans().get(2).getCharacterEnd());

    }

    @Test
    public void filterDate38g() throws Exception {

        // https://github.com/philterd/phileas/issues/353
        // Month-and-year dates chained the same way.

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "ref 12-2020-12-2021");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertEquals("12-2020", filtered.getSpans().get(0).getText());
        Assertions.assertEquals(4, filtered.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(11, filtered.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("12-2021", filtered.getSpans().get(1).getText());
        Assertions.assertEquals(12, filtered.getSpans().get(1).getCharacterStart());
        Assertions.assertEquals(19, filtered.getSpans().get(1).getCharacterEnd());

    }

    @Test
    public void filterDate38h() throws Exception {

        // https://github.com/philterd/phileas/issues/353
        // A phone number is still not a date.

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "phone 555-123-4567");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterDate38i() throws Exception {

        // https://github.com/philterd/phileas/issues/353
        // The tail starts date-shaped ("1-20") but does not decompose into dates.

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "acct 12-2020-1-20-01023");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterDate38j() throws Exception {

        // https://github.com/philterd/phileas/issues/353
        // ".3.14" is not a date for the "." delimiter, so it does not make this one either.

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "x 25.12.1980.3.14");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(0, filtered.getSpans().size());

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

    private void assertWholeDate(final DateFilter filter, final String date) throws Exception {

        final String input = "It was on " + date + " at the office.";
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);

        Assertions.assertEquals(1, filtered.getSpans().size(), date);
        Assertions.assertEquals(date, filtered.getSpans().get(0).getText(), date);
        Assertions.assertEquals(10, filtered.getSpans().get(0).getCharacterStart(), date);

    }

    @Test
    public void filterDateMonthNameYearBefore1970() throws Exception {

        // https://github.com/philterd/phileas/issues/459
        for (final boolean onlyValidDates : List.of(false, true)) {

            final DateFilter filter = new DateFilter(buildFilterConfiguration(), onlyValidDates, DateSpanValidator.getInstance());

            for (final String date : List.of("March 27, 1965", "Sep 10, 1950", "January 14 1957", "Sep 27, 1966",
                    "March 3, 1965", "Sep 9, 1950", "January 4 1957", "Feb 10, 1945", "Dec 31 1969",
                    "March 27th, 1965", "Mar. 27, 1965", "March 27, 1975")) {
                assertWholeDate(filter, date);
            }

        }

    }

    @Test
    public void filterDateMonthNameEveryDay() throws Exception {

        // Every day of the month, with years on either side of 1970.
        // https://github.com/philterd/phileas/issues/459
        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        for (final int year : List.of(1900, 1950, 1969, 1970, 2025, 2099)) {
            for (int day = 1; day <= 31; day++) {
                assertWholeDate(filter, "March " + day + ", " + year);
                assertWholeDate(filter, "Mar " + day + ", " + year);
                assertWholeDate(filter, "March " + day + " " + year);
            }
        }

    }

    @Test
    public void filterDateMonthNameEveryYear() throws Exception {

        // https://github.com/philterd/phileas/issues/459
        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        for (int year = 1900; year <= 2099; year++) {
            assertWholeDate(filter, "September 27, " + year);
            assertWholeDate(filter, "Sep 27, " + year);
            assertWholeDate(filter, "September 27 " + year);
        }

    }

    @Test
    public void filterDateDayNotReadAsTwoDigitYear() throws Exception {

        // "MMMM yy" must not take the day as a two-digit year when a four-digit year follows it.
        // https://github.com/philterd/phileas/issues/459
        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        for (final String date : List.of("March 27, 1965", "March 27 1965", "March 27, 2150")) {
            final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "It was on " + date + " at the office.");
            Assertions.assertEquals(1, filtered.getSpans().size(), date);
            Assertions.assertNotEquals("MMMM yy", filtered.getSpans().get(0).getPattern(), date);
            Assertions.assertEquals(date, filtered.getSpans().get(0).getText(), date);
        }

        // Without a four-digit year after it, a two-digit number after the month is still matched.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "It was in March 65 at the office.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(filtered.getSpans().get(0).getText().startsWith("March 65"));

    }

    @Test
    public void filterDateDayFirstWithComma() throws Exception {

        // https://github.com/philterd/phileas/issues/462
        for (final boolean onlyValidDates : List.of(false, true)) {

            final DateFilter filter = new DateFilter(buildFilterConfiguration(), onlyValidDates, DateSpanValidator.getInstance());

            for (final String date : List.of("27 March, 1965", "27 Mar, 1965", "27th March, 1965", "27 March, 1975",
                    "3 March, 2020", "3 Mar, 2020", "1st May, 2020", "2nd June, 1950", "3rd Sep, 1969", "22nd Dec, 2099",
                    "9 January, 1900", "09 January, 1968", "31 Oct, 2025", "27 Mar., 1965")) {
                assertWholeDate(filter, date);
            }

        }

    }

    @Test
    public void filterDateDayFirstEveryDay() throws Exception {

        // Every day of the month, with years on either side of 1970.
        // https://github.com/philterd/phileas/issues/462
        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        for (final int year : List.of(1900, 1950, 1969, 1970, 2025, 2099)) {
            for (int day = 1; day <= 31; day++) {
                assertWholeDate(filter, day + " March, " + year);
                assertWholeDate(filter, day + " Mar, " + year);
                assertWholeDate(filter, day + " March " + year);
                assertWholeDate(filter, day + " Mar " + year);
                assertWholeDate(filter, day + "-Mar-" + year);
            }
        }

    }

    @Test
    public void filterDateDayFirstWithoutComma() throws Exception {

        // https://github.com/philterd/phileas/issues/462
        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        for (final String date : List.of("27 March 1965", "27 Mar 1965", "27-Mar-1965", "27th March 1965", "3 March 2020",
                "27/Mar/1965", "27.Mar.1965")) {
            assertWholeDate(filter, date);
        }

    }

    @Test
    public void filterDateDayFirstFormat() throws Exception {

        // A day-first date's span carries a day-first format, not a month-first one such as "MMMM yyyy".
        // https://github.com/philterd/phileas/issues/462
        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Map<String, String> formats = new LinkedHashMap<>();
        formats.put("27 March, 1965", "dd MMMM, yyyy");
        formats.put("3 Mar, 2020", "dd MMMM, yyyy");
        formats.put("27th March, 1965", "dd MMMM, yyyy");
        formats.put("27 March 1965", "dd MMMM yyyy");
        formats.put("27 Mar 1965", "dd MMMM yyyy");
        formats.put("27-Mar-1965", "dd-MMMM-yyyy");
        formats.put("27/Mar/1965", "dd/MMMM/yyyy");
        formats.put("27.Mar.1965", "dd.MMMM.yyyy");

        for (final Map.Entry<String, String> entry : formats.entrySet()) {
            final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "It was on " + entry.getKey() + " at the office.");
            Assertions.assertEquals(1, filtered.getSpans().size(), entry.getKey());
            Assertions.assertEquals(entry.getKey(), filtered.getSpans().get(0).getText(), entry.getKey());
            Assertions.assertEquals(entry.getValue(), filtered.getSpans().get(0).getPattern(), entry.getKey());
        }

    }

    @Test
    public void filterDateDayFirstMonthFirstUnchanged() throws Exception {

        // Month-first dates keep their month-first format.
        // https://github.com/philterd/phileas/issues/462
        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final Map<String, String> formats = new LinkedHashMap<>();
        formats.put("March 27, 1965", "MMMM dd, yyyy");
        formats.put("March 27 1965", "MMMM dd yyyy");
        formats.put("March 1965", "MMMM yyyy");

        for (final Map.Entry<String, String> entry : formats.entrySet()) {
            final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "It was on " + entry.getKey() + " at the office.");
            Assertions.assertEquals(1, filtered.getSpans().size(), entry.getKey());
            Assertions.assertEquals(entry.getKey(), filtered.getSpans().get(0).getText(), entry.getKey());
            Assertions.assertEquals(entry.getValue(), filtered.getSpans().get(0).getPattern(), entry.getKey());
        }

    }

}
