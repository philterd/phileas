package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.DateFilterStrategy;
import com.mtnfog.phileas.services.anonymization.DateAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.DateFilter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DateFilterTest extends AbstractFilterTest {

    @Test
    public void filterDate1() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","May 22, 1999");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 12, FilterType.DATE));

    }

    @Test
    public void filterDate2() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","13-06-31");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate3() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","2205-02-31");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 10, FilterType.DATE));

    }

    @Test
    public void filterDate4() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","02-31-2019");

        // This is matching two regexes and since spans is a list both get added.
        // The duplicate span will be dropped later.
        Assert.assertEquals(2, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 10, FilterType.DATE));
        Assert.assertTrue(checkSpan(spans.get(1), 0, 10, FilterType.DATE));

    }

    @Test
    public void filterDate5() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","02-31-19");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate6() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","2-8-2019");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate7() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","2-15-2019");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 9, FilterType.DATE));

    }

    @Test
    public void filterDate8() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","January 2012");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 12, FilterType.DATE));

    }

    @Test
    public void filterDate9() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","December 2015");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 13, FilterType.DATE));

    }

    @Test
    public void filterDate10() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","November 1999");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 13, FilterType.DATE));

    }

    @Test
    public void filterDate11() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","april 1999");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 10, FilterType.DATE));

    }

    @Test
    public void filterDate12() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        // This is matching two regexes and since spans is a list both get added.
        // The duplicate span will be dropped later.
        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","12-05-2014");
        Assert.assertEquals(2, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 10, FilterType.DATE));
        Assert.assertTrue(checkSpan(spans.get(1), 0, 10, FilterType.DATE));

    }

    @Test
    public void filterDate13() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","November 22, 1999");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 17, FilterType.DATE));

    }

    @Test
    public void filterDate14() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","November 22nd, 1999");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 19, FilterType.DATE));

    }

    @Test
    public void filterDate15() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","November 22 nd, 1999");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 20, FilterType.DATE));

    }

    @Test
    public void filterDate16() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","November 22nd");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 13, FilterType.DATE));

    }

    @Test
    public void filterDate17() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","May 1 st");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate18() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","June 13th");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 9, FilterType.DATE));

    }

    @Test
    public void filterDate19() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","November 2, 1999");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 16, FilterType.DATE));

    }

    @Test
    public void filterDate20() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","May 1st");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 7, FilterType.DATE));

    }

    @Test
    public void filterDate21() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","December 4th");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 12, FilterType.DATE));

    }

    @Test
    public void filterDate22() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","02-31-19@12:00");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 8, FilterType.DATE));

    }

    @Test
    public void filterDate23() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), true, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","02-31-19@12:00");
        Assert.assertEquals(0, spans.size());

    }

    @Test
    public void filterDate24() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), true, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","02-35-19@12:00");
        Assert.assertEquals(0, spans.size());

    }

    @Test
    public void filterDate25() throws Exception {

        final List<DateFilterStrategy> strategies = Arrays.asList(new DateFilterStrategy());
        DateFilter filter = new DateFilter(strategies, new DateAnonymizationService(new LocalAnonymizationCacheService()), true, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","02-15-19");
        Assert.assertEquals(1, spans.size());

    }

}
