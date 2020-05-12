package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.VinFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.VinAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.VinFilter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VinFilterTest extends AbstractFilterTest {

    private final AnonymizationService anonymizationService = new VinAnonymizationService(new LocalAnonymizationCacheService());

    @Test
    public void filterVin1() throws Exception {

        final List<VinFilterStrategy> strategies = Arrays.asList(new VinFilterStrategy());
        VinFilter filter = new VinFilter(strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the vin is JB3BA36KXHU036784.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 28, FilterType.VIN));
        Assert.assertEquals("JB3BA36KXHU036784", spans.get(0).getText());

    }

    @Test
    public void filterVin2() throws Exception {

        final List<VinFilterStrategy> strategies = Arrays.asList(new VinFilterStrategy());
        VinFilter filter = new VinFilter(strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the vin is 2T2HK31U38C057399.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 28, FilterType.VIN));

    }

    @Test
    public void filterVin3() throws Exception {

        final List<VinFilterStrategy> strategies = Arrays.asList(new VinFilterStrategy());
        VinFilter filter = new VinFilter(strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the vin is 11131517191011111.");
        Assert.assertEquals(0, spans.size());

    }

    @Test
    public void filterVin4() throws Exception {

        final List<VinFilterStrategy> strategies = Arrays.asList(new VinFilterStrategy());
        VinFilter filter = new VinFilter(strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the vin is 11131517191X11111.");
        Assert.assertEquals(0, spans.size());

    }

    @Test
    public void filterVin5() throws Exception {

        final List<VinFilterStrategy> strategies = Arrays.asList(new VinFilterStrategy());
        VinFilter filter = new VinFilter(strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the vin is 2t2hk31u38c057399.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 28, FilterType.VIN));

    }

}
