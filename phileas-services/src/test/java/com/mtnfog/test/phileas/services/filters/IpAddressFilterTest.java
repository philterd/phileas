package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.IpAddressFilterStrategy;
import com.mtnfog.phileas.services.anonymization.IpAddressAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.IpAddressFilter;
import com.mtnfog.test.phileas.services.filters.AbstractFilterTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IpAddressFilterTest extends AbstractFilterTest {

    @Test
    public void filterIpv41() throws Exception {

        final List<IpAddressFilterStrategy> strategies = Arrays.asList(new IpAddressFilterStrategy());
        IpAddressFilter filter = new IpAddressFilter(strategies, new IpAddressAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto());
        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the ip is 192.168.1.101.");

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10, 23, FilterType.IP_ADDRESS));

    }

    @Test
    public void filterIpv61() throws Exception {

        final List<IpAddressFilterStrategy> strategies = Arrays.asList(new IpAddressFilterStrategy());
        IpAddressFilter filter = new IpAddressFilter(strategies, new IpAddressAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto());
        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the ip is 1::");

        // Finds duplicate spans. Duplicates/overlapping will be removed by the service prior to returning.
        Assert.assertEquals(2, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10, 13, FilterType.IP_ADDRESS));
        Assert.assertTrue(checkSpan(spans.get(1), 10, 13, FilterType.IP_ADDRESS));

    }

    @Test
    public void filterIpv62() throws Exception {

        final List<IpAddressFilterStrategy> strategies = Arrays.asList(new IpAddressFilterStrategy());
        IpAddressFilter filter = new IpAddressFilter(strategies, new IpAddressAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto());
        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the ip is 2001:0db8:85a3:0000:0000:8a2e:0370:7334");

        // Finds duplicate spans. Duplicates/overlapping will be removed by the service prior to returning.
        Assert.assertEquals(2, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10, 49, FilterType.IP_ADDRESS));
        Assert.assertTrue(checkSpan(spans.get(1), 10, 40, FilterType.IP_ADDRESS));

    }

    @Test
    public void filterIpv63() throws Exception {

        final List<IpAddressFilterStrategy> strategies = Arrays.asList(new IpAddressFilterStrategy());
        IpAddressFilter filter = new IpAddressFilter(strategies, new IpAddressAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto());
        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the ip is fe80::0202:B3FF:FE1E:8329");

        // Finds duplicate spans. Duplicates/overlapping will be removed by the service prior to returning.
        Assert.assertEquals(2, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10, 35, FilterType.IP_ADDRESS));
        Assert.assertTrue(checkSpan(spans.get(1), 10, 31, FilterType.IP_ADDRESS));

    }

}
