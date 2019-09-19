package com.mtnfog.test.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.VinAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.VinFilter;
import com.mtnfog.test.phileas.services.filters.AbstractFilterTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class VinFilterTest extends AbstractFilterTest {

    private final AnonymizationService anonymizationService = new VinAnonymizationService(new LocalAnonymizationCacheService());

    @Test
    public void filterVin1() throws Exception {

        VinFilter filter = new VinFilter(anonymizationService);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the vin is JB3BA36KXHU036784.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 28, FilterType.VIN));

    }

    @Test
    public void filterVin2() throws Exception {

        VinFilter filter = new VinFilter(anonymizationService);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the vin is 2T2HK31U38C057399.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 28, FilterType.VIN));

    }

    @Test
    public void filterVin3() throws Exception {

        VinFilter filter = new VinFilter(anonymizationService);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the vin is 11131517191011111.");
        Assert.assertEquals(0, spans.size());

    }

    @Test
    public void filterVin4() throws Exception {

        VinFilter filter = new VinFilter(anonymizationService);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the vin is 11131517191X11111.");
        Assert.assertEquals(0, spans.size());

    }

    @Test
    public void filterVin5() throws Exception {

        VinFilter filter = new VinFilter(anonymizationService);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the vin is 2t2hk31u38c057399.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 28, FilterType.VIN));

    }

}
