package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.AgeAnonymizationService;
import com.mtnfog.phileas.services.anonymization.StreetAddressAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.StreetAddressFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

public class StreetAddressFilterTest extends AbstractFilterTest {

    @Test
    public void filter1() throws Exception {

        final AlertService alertService = Mockito.mock(AlertService.class);

        final StreetAddressFilter filter = new StreetAddressFilter(null, new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "lived at 100 Main St");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 20, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter2() throws Exception {

        final AlertService alertService = Mockito.mock(AlertService.class);

        final StreetAddressFilter filter = new StreetAddressFilter(null, new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "lived at 100 S Main St");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 22, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter3() throws Exception {

        final AlertService alertService = Mockito.mock(AlertService.class);

        final StreetAddressFilter filter = new StreetAddressFilter(null, new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "lived at 100 South Main St");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 26, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter4() throws Exception {

        final AlertService alertService = Mockito.mock(AlertService.class);

        final StreetAddressFilter filter = new StreetAddressFilter(null, new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "lived at 1000 Main Street");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 25, FilterType.STREET_ADDRESS));

    }

}
