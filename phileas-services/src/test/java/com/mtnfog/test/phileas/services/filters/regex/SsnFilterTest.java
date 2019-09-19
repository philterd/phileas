package com.mtnfog.test.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.services.anonymization.AlphanumericAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.SsnFilter;
import com.mtnfog.test.phileas.services.filters.AbstractFilterTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class SsnFilterTest extends AbstractFilterTest {

    @Test
    public void filterSsn1() throws Exception {

        SsnFilter filter = new SsnFilter(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the ssn is 123-45-6789.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 22, FilterType.SSN));

    }

    @Test
    public void filterSsn2() throws Exception {

        SsnFilter filter = new SsnFilter(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the ssn is 123456789.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 20, FilterType.SSN));

    }

    @Test
    public void filterSsn3() throws Exception {

        SsnFilter filter = new SsnFilter(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the ssn is 123 45 6789.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 22, FilterType.SSN));

    }

    @Test
    public void filterSsn4() throws Exception {

        SsnFilter filter = new SsnFilter(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the ssn is 123 45 6789.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 22, FilterType.SSN));

    }

    @Test
    public void filterSsn5() throws Exception {

        SsnFilter filter = new SsnFilter(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the ssn is 123 454 6789.");
        Assert.assertEquals(0, spans.size());

    }

    @Test
    public void filterSsn6() throws Exception {

        SsnFilter filter = new SsnFilter(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the ssn is 123 4f 6789.");
        Assert.assertEquals(0, spans.size());

    }

}
