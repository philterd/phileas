package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.MacAddressFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.MacAddressAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.MacAddressFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MacAddressFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filter1() throws Exception {

        final List<MacAddressFilterStrategy> strategies = Arrays.asList(new MacAddressFilterStrategy());

        Filter filter = new MacAddressFilter(strategies, new MacAddressAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);
        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the mac is 00-14-22-04-25-37.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 28, FilterType.MAC_ADDRESS));
        Assertions.assertEquals("00-14-22-04-25-37", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter2() throws Exception {

        final List<MacAddressFilterStrategy> strategies = Arrays.asList(new MacAddressFilterStrategy());

        Filter filter = new MacAddressFilter(strategies, new MacAddressAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);
        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the mac is 00:14:22:04:25:37.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 28, FilterType.MAC_ADDRESS));

    }

}
