package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.VinFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.VinAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.VinFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VinFilterTest extends AbstractFilterTest {

    private final AnonymizationService anonymizationService = new VinAnonymizationService(new LocalAnonymizationCacheService());

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterVin1() throws Exception {

        final List<VinFilterStrategy> strategies = Arrays.asList(new VinFilterStrategy());
        VinFilter filter = new VinFilter(strategies, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the vin is JB3BA36KXHU036784.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 28, FilterType.VIN));
        Assertions.assertEquals("JB3BA36KXHU036784", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterVin2() throws Exception {

        final List<VinFilterStrategy> strategies = Arrays.asList(new VinFilterStrategy());
        VinFilter filter = new VinFilter(strategies, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the vin is 2T2HK31U38C057399.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 28, FilterType.VIN));

    }

    @Test
    public void filterVin3() throws Exception {

        final List<VinFilterStrategy> strategies = Arrays.asList(new VinFilterStrategy());
        VinFilter filter = new VinFilter(strategies, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the vin is 11131517191011111.");
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterVin4() throws Exception {

        final List<VinFilterStrategy> strategies = Arrays.asList(new VinFilterStrategy());
        VinFilter filter = new VinFilter(strategies, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the vin is 11131517191X11111.");
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterVin5() throws Exception {

        final List<VinFilterStrategy> strategies = Arrays.asList(new VinFilterStrategy());
        VinFilter filter = new VinFilter(strategies, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the vin is 2t2hk31u38c057399.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 28, FilterType.VIN));

    }

}
