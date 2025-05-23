/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License", attributes);
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
package ai.philterd.test.phileas.services.filters;

import ai.philterd.phileas.model.cache.InMemoryCache;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.policy.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.DateFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.DateAnonymizationService;
import ai.philterd.phileas.services.filters.regex.DateFilter;
import ai.philterd.phileas.services.validators.DateSpanValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

public class DateFilterTest extends AbstractFilterTest {
    
    private final AlertService alertService = Mockito.mock(AlertService.class);

    private FilterConfiguration buildFilterConfiguration() {

        return new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new DateFilterStrategy()))
                .withAnonymizationService(new DateAnonymizationService(new InMemoryCache()))
                .withAlertService(alertService)
                .build();

    }

    @Test
    public void filterDate1() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "May 22, 1999", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 12, FilterType.DATE));
        Assertions.assertEquals("May 22, 1999", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterDate2() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "13-06-31", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate3() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "2205-02-31", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 10, FilterType.DATE));

    }

    @Test
    public void filterDate4() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());
        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "02-31-2019", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 10, FilterType.DATE));

    }

    @Test
    public void filterDate5() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "02-31-19", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate6() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "2-8-2019", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate7() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "2-15-2019", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 9, FilterType.DATE));

    }

    @Test
    public void filterDate8() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "January 2012", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 12, FilterType.DATE));

    }

    @Test
    public void filterDate9() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "December 2015", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 13, FilterType.DATE));

    }

    @Test
    public void filterDate10() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "November 1999", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 13, FilterType.DATE));

    }

    @Test
    public void filterDate11() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "april 1999", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 10, FilterType.DATE));

    }

    @Test
    public void filterDate12() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "12-05-2014", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 10, FilterType.DATE));

    }

    @Test
    public void filterDate13() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "November 22, 1999", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 17, FilterType.DATE));

    }

    @Test
    public void filterDate14() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "November 22nd, 1999", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 19, FilterType.DATE));

    }

    @Test
    public void filterDate15() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "November 22 nd, 1999", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 20, FilterType.DATE));

    }

    @Test
    public void filterDate16() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "November 22nd", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 13, FilterType.DATE));

    }

    @Test
    public void filterDate17() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "May 1 st", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate18() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "June 13th", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 9, FilterType.DATE));

    }

    @Test
    public void filterDate19() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "November 2, 1999", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 16, FilterType.DATE));

    }

    @Test
    public void filterDate20() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "May 1st", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 7, FilterType.DATE));

    }

    @Test
    public void filterDate21() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "December 4th", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 12, FilterType.DATE));

    }

    @Test
    public void filterDate22() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "02-31-19@12:00", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate23() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "02-31-19@12:00", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterDate24() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "02-35-19@12:00", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterDate25() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "02-15-19", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterDate26() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "The good news is everywhere we go it is that way but this may be on top of that.", attributes);
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterDate27() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "The good news is everywhere we go it is that way but this may 15 be on top of that.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterDate28() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "The good news is everywhere we go it is that way but this may 15, 2020 be on top of that.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterDate29() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "The good news is everywhere we go it is that way but this may 15 2020 be on top of that.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterDate30() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "The good news is everywhere we go it is that way but this may 15 19 be on top of that.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterDate31() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "The good news is everywhere we go it is that way but this may 5 19 be on top of that.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterDate32() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "The good news is everywhere we go it is that way but this June 21, 2020 be on top of that.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals("June 21, 2020", filterResult.getSpans().get(0).getText());

        showSpans(filterResult.getSpans());

    }

    @Test
    public void filterDate33() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "The good news is everywhere we go it is that way but this 09-2021 be on top of that.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals("09-2021", filterResult.getSpans().get(0).getText());

        showSpans(filterResult.getSpans());

    }

    @Test
    public void filterDate34() throws Exception {

        final DateFilterStrategy dateFilterStrategy = new DateFilterStrategy();
        dateFilterStrategy.setStrategy(AbstractFilterStrategy.RELATIVE);
        dateFilterStrategy.setShiftYears(3);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(dateFilterStrategy))
                .withAnonymizationService(new DateAnonymizationService(new InMemoryCache()))
                .withAlertService(alertService)
                .build();

        final DateFilter filter = new DateFilter(filterConfiguration, false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Owns and drives his own vehicle but states he has not driven his car since last October 2009.", attributes);

        LOGGER.info(filterResult.getSpans().get(0).getReplacement());

        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());
        // This value can differ depending on when the test is run.
        Assertions.assertTrue(filterResult.getSpans().get(0).getReplacement().startsWith("15 years") || filterResult.getSpans().get(0).getReplacement().startsWith("14 years"));

    }

    @Test
    public void filterDate35() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Observation: 91-100% strong nuclear staining", attributes);
        Assertions.assertEquals(0, filterResult.getSpans().size());

        showSpans(filterResult.getSpans());

    }

    @Test
    public void filterDate36() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "That on July 3, 2012 an involuntary petition on behalf of FKAAHS, Inc. fka Aire", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(8, filterResult.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(20, filterResult.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("July 3, 2012", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterDate37() throws Exception {

        // See PHL-204: The date is not being found when onlyValidDates=true.

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "That on July 3, 2012 an involuntary petition on behalf of FKAAHS, Inc. fka Aire", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(8, filterResult.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(20, filterResult.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("July 3, 2012", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterDate38() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), false, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Case No. 12-12110 K", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(9, filterResult.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(16, filterResult.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("12-1211", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterDate39() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Case No. 12-12110 K", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterDate40() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Case 1-20-01023-MJK", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterDate41() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "That on July 3, 2012 an involuntary", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(8, filterResult.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(20, filterResult.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("July 3, 2012", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterDate42() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Entered 06/16/20 11:55:37,", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(8, filterResult.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(16, filterResult.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("06/16/20", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterDate43() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Case 1-20-01023-MJK,    Doc 1,    Filed 06/16/20,    Entered 06/16/20 11:55:37,", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(2, filterResult.getSpans().size());
        Assertions.assertEquals(61, filterResult.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(69, filterResult.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("06/16/20", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals(40, filterResult.getSpans().get(1).getCharacterStart());
        Assertions.assertEquals(48, filterResult.getSpans().get(1).getCharacterEnd());
        Assertions.assertEquals("06/16/20", filterResult.getSpans().get(1).getText());

    }

    @Test
    public void filterDate44() throws Exception {

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "On August 22, 2012, Plaintiff, Wendy J. Christophersen, was appointed as interim", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(3, filterResult.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(18, filterResult.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("August 22, 2012", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterDate45() throws Exception {

        // PHL-239: Support dates like Aug. 31, 2020

        final DateFilter filter = new DateFilter(buildFilterConfiguration(), true, DateSpanValidator.getInstance());

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "On Aug. 31, 2020, Plaintiff, Wendy J. Christophersen, was appointed as interim", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(3, filterResult.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(16, filterResult.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals("Aug. 31, 2020", filterResult.getSpans().get(0).getText());

    }

}
