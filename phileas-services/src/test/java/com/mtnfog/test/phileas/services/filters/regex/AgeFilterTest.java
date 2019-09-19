package com.mtnfog.test.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.services.anonymization.AgeAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.AgeFilter;
import com.mtnfog.test.phileas.services.filters.AbstractFilterTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class AgeFilterTest extends AbstractFilterTest {

    @Test
    public void filter1() throws Exception {

        AgeFilter filter = new AgeFilter(new AgeAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the patient is 3.5years old.");

        showSpans(spans);

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 15, 27, FilterType.AGE));

    }

    @Test
    public void filter2() throws Exception {

        AgeFilter filter = new AgeFilter(new AgeAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the patient is 3.yrs.");

        showSpans(spans);

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 15, 20, FilterType.AGE));

    }

    @Test
    public void filter3() throws Exception {

        AgeFilter filter = new AgeFilter(new AgeAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the patient is 3yrs.");

        showSpans(spans);

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 15, 19, FilterType.AGE));

    }

    @Test
    public void filter4() throws Exception {

        AgeFilter filter = new AgeFilter(new AgeAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the patient is 3.5yrs.");

        showSpans(spans);

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 15, 21, FilterType.AGE));

    }

    @Test
    public void filter5() throws Exception {

        AgeFilter filter = new AgeFilter(new AgeAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the patient is 39yrs.");

        showSpans(spans);

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 15, 20, FilterType.AGE));

    }

    @Test
    public void filter6() throws Exception {

        AgeFilter filter = new AgeFilter(new AgeAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "she is aged 39");

        showSpans(spans);

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 7, 14, FilterType.AGE));

    }

    @Test
    public void filter7() throws Exception {

        AgeFilter filter = new AgeFilter(new AgeAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "she is age 39");

        showSpans(spans);

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 7, 13, FilterType.AGE));

    }

    @Test
    public void filter8() throws Exception {

        AgeFilter filter = new AgeFilter(new AgeAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "she is age 39.5");

        showSpans(spans);

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 7, 15, FilterType.AGE));

    }

}
