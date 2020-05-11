package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.SsnFilterStrategy;
import com.mtnfog.phileas.services.anonymization.AlphanumericAnonymizationService;
import com.mtnfog.phileas.services.cache.anonymization.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.SsnFilter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SsnFilterTest extends AbstractFilterTest {

    @Test
    public void filterSsn1() throws Exception {

        final List<SsnFilterStrategy> strategies = Arrays.asList(new SsnFilterStrategy());
        SsnFilter filter = new SsnFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the ssn is 123-45-6789.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 22, FilterType.SSN));
        Assert.assertEquals("123-45-6789", spans.get(0).getText());

    }

    @Test
    public void filterSsn2() throws Exception {

        final List<SsnFilterStrategy> strategies = Arrays.asList(new SsnFilterStrategy());
        SsnFilter filter = new SsnFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the ssn is 123456789.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 20, FilterType.SSN));

    }

    @Test
    public void filterSsn3() throws Exception {

        final List<SsnFilterStrategy> strategies = Arrays.asList(new SsnFilterStrategy());
        SsnFilter filter = new SsnFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the ssn is 123 45 6789.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 22, FilterType.SSN));

    }

    @Test
    public void filterSsn4() throws Exception {

        final List<SsnFilterStrategy> strategies = Arrays.asList(new SsnFilterStrategy());
        SsnFilter filter = new SsnFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the ssn is 123 45 6789.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 22, FilterType.SSN));

    }

    @Test
    public void filterSsn5() throws Exception {

        final List<SsnFilterStrategy> strategies = Arrays.asList(new SsnFilterStrategy());
        SsnFilter filter = new SsnFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the ssn is 123 454 6789.");
        Assert.assertEquals(0, spans.size());

    }

    @Test
    public void filterSsn6() throws Exception {

        final List<SsnFilterStrategy> strategies = Arrays.asList(new SsnFilterStrategy());
        SsnFilter filter = new SsnFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the ssn is 123 4f 6789.");
        Assert.assertEquals(0, spans.size());

    }

    @Test
    public void filterSsn7() throws Exception {

        final List<SsnFilterStrategy> strategies = Arrays.asList(new SsnFilterStrategy());
        SsnFilter filter = new SsnFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the ssn is 11-1234567.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 21, FilterType.SSN));

    }

}
