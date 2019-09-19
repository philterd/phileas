package com.mtnfog.test.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.services.anonymization.IpAddressAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.IpAddressFilter;
import com.mtnfog.test.phileas.services.filters.AbstractFilterTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class IpAddressFilterTest extends AbstractFilterTest {

    @Test
    public void filterIp() throws Exception {

        IpAddressFilter filter = new IpAddressFilter(new IpAddressAnonymizationService(new LocalAnonymizationCacheService()));
        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the ip is 192.168.1.101.");

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10, 23, FilterType.IP_ADDRESS));

    }

}
