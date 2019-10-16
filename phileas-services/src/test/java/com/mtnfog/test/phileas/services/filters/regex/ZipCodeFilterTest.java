package com.mtnfog.test.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.ZipCodeFilterStrategy;
import com.mtnfog.phileas.services.anonymization.ZipCodeAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.ZipCodeFilter;
import com.mtnfog.test.phileas.services.filters.AbstractFilterTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ZipCodeFilterTest extends AbstractFilterTest {

    @Test
    public void filterZipCode1() throws Exception {

        final List<ZipCodeFilterStrategy> strategies = Arrays.asList(new ZipCodeFilterStrategy());
        ZipCodeFilter filter = new ZipCodeFilter(strategies, new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the zip is 90210.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 16, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode2() throws Exception {

        final List<ZipCodeFilterStrategy> strategies = Arrays.asList(new ZipCodeFilterStrategy());
        ZipCodeFilter filter = new ZipCodeFilter(strategies, new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the zip is 90210abd.");
        Assert.assertEquals(0, spans.size());

    }

    @Test
    public void filterZipCode3() throws Exception {

        final List<ZipCodeFilterStrategy> strategies = Arrays.asList(new ZipCodeFilterStrategy());
        ZipCodeFilter filter = new ZipCodeFilter(strategies, new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the zip is 90210 in california.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 16, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode4() throws Exception {

        final List<ZipCodeFilterStrategy> strategies = Arrays.asList(new ZipCodeFilterStrategy());
        ZipCodeFilter filter = new ZipCodeFilter(strategies, new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the zip is 85055 in california.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 16, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode5() throws Exception {

        final List<ZipCodeFilterStrategy> strategies = Arrays.asList(new ZipCodeFilterStrategy());
        ZipCodeFilter filter = new ZipCodeFilter(strategies, new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the zip is 90213-1544 in california.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 21, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode6() throws Exception {

        final List<ZipCodeFilterStrategy> strategies = Arrays.asList(new ZipCodeFilterStrategy());
        ZipCodeFilter filter = new ZipCodeFilter(strategies, new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived in 90210.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 76, 81, FilterType.ZIP_CODE));

    }

}
