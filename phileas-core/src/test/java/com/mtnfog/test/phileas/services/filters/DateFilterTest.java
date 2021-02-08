package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.DateFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.DateAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.DateFilter;
import com.mtnfog.phileas.services.validators.DateSpanValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DateFilterTest extends AbstractFilterTest {
    
    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterDate1() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "May 22, 1999");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 12, FilterType.DATE));
        Assertions.assertEquals("May 22, 1999", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterDate2() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "13-06-31");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate3() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "2205-02-31");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 10, FilterType.DATE));

    }

    @Test
    public void filterDate4() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "02-31-2019");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 10, FilterType.DATE));

    }

    @Test
    public void filterDate5() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "02-31-19");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate6() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "2-8-2019");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate7() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "2-15-2019");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 9, FilterType.DATE));

    }

    @Test
    public void filterDate8() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "January 2012");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 12, FilterType.DATE));

    }

    @Test
    public void filterDate9() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "December 2015");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 13, FilterType.DATE));

    }

    @Test
    public void filterDate10() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "November 1999");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 13, FilterType.DATE));

    }

    @Test
    public void filterDate11() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "april 1999");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 10, FilterType.DATE));

    }

    @Test
    public void filterDate12() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "12-05-2014");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 10, FilterType.DATE));

    }

    @Test
    public void filterDate13() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "November 22, 1999");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 17, FilterType.DATE));

    }

    @Test
    public void filterDate14() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "November 22nd, 1999");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 19, FilterType.DATE));

    }

    @Test
    public void filterDate15() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "November 22 nd, 1999");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 20, FilterType.DATE));

    }

    @Test
    public void filterDate16() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "November 22nd");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 13, FilterType.DATE));

    }

    @Test
    public void filterDate17() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "May 1 st");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate18() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "June 13th");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 9, FilterType.DATE));

    }

    @Test
    public void filterDate19() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "November 2, 1999");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 16, FilterType.DATE));

    }

    @Test
    public void filterDate20() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "May 1st");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 7, FilterType.DATE));

    }

    @Test
    public void filterDate21() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "December 4th");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 12, FilterType.DATE));

    }

    @Test
    public void filterDate22() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "02-31-19@12:00");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate23() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, true, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "02-31-19@12:00");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterDate24() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, true, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "02-35-19@12:00");
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterDate25() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, true, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "02-15-19");
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterDate26() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "The good news is everywhere we go it is that way but this may be on top of that.");
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterDate27() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "The good news is everywhere we go it is that way but this may 15 be on top of that.");
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterDate28() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "The good news is everywhere we go it is that way but this may 15, 2020 be on top of that.");
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterDate29() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "The good news is everywhere we go it is that way but this may 15 2020 be on top of that.");
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterDate30() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "The good news is everywhere we go it is that way but this may 15 19 be on top of that.");
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterDate31() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "The good news is everywhere we go it is that way but this may 5 19 be on top of that.");
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterDate32() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "The good news is everywhere we go it is that way but this June 21, 2020 be on top of that.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals("June 21, 2020", filterResult.getSpans().get(0).getText());

        showSpans(filterResult.getSpans());

    }

    @Test
    public void filterDate33() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "The good news is everywhere we go it is that way but this 09-2021 be on top of that.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals("09-2021", filterResult.getSpans().get(0).getText());

        showSpans(filterResult.getSpans());

    }

    @Test
    public void filterDate34() throws Exception {

        final DateFilterStrategy dateFilterStrategy = new DateFilterStrategy();
        dateFilterStrategy.setStrategy(AbstractFilterStrategy.RELATIVE);
        dateFilterStrategy.setShiftYears(3);

        final List<DateFilterStrategy> strategies = Arrays.asList(dateFilterStrategy);
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "Owns and drives his own vehicle but states he has not driven his car since last October 2009.");

        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals("11 years 4 months ago", filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filterDate35() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), alertService, true, DateSpanValidator.getInstance(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "Observation: 91-100% strong nuclear staining");
        Assertions.assertEquals(0, filterResult.getSpans().size());

        showSpans(filterResult.getSpans());

    }

}
