package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.MacAddressFilterStrategy;
import com.mtnfog.phileas.services.anonymization.MacAddressAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.MacAddressFilter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MacAddressFilterTest extends AbstractFilterTest {

    @Test
    public void filter1() throws Exception {

        final List<MacAddressFilterStrategy> strategies = Arrays.asList(new MacAddressFilterStrategy());
        Filter filter = new MacAddressFilter(strategies, new MacAddressAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto());
        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the mac is 00-14-22-04-25-37.");

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 28, FilterType.MAC_ADDRESS));

    }

    @Test
    public void filter2() throws Exception {

        final List<MacAddressFilterStrategy> strategies = Arrays.asList(new MacAddressFilterStrategy());
        Filter filter = new MacAddressFilter(strategies, new MacAddressAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto());
        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the mac is 00:14:22:04:25:37.");

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 11, 28, FilterType.MAC_ADDRESS));

    }

}
