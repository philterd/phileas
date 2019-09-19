package com.mtnfog.test.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.services.anonymization.ZipCodeAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.ZipCodeFilter;
import com.mtnfog.test.phileas.services.filters.AbstractFilterTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ZipCodeFilterTest extends AbstractFilterTest {

    @Test
    public void filterZipCode1() throws Exception {

        ZipCodeFilter filter = new ZipCodeFilter(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the zip is 90210.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 16, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode2() throws Exception {

        ZipCodeFilter filter = new ZipCodeFilter(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the zip is 90210abd.");
        Assert.assertEquals(0, spans.size());

    }

    @Test
    public void filterZipCode3() throws Exception {

        ZipCodeFilter filter = new ZipCodeFilter(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the zip is 90210 in california.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 16, FilterType.ZIP_CODE));

    }

}
