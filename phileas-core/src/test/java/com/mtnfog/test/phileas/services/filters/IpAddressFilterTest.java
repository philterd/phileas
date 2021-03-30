package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.FilterConfiguration;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.IpAddressFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.IpAddressAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.IpAddressFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class IpAddressFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterIpv41() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IpAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new IpAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IpAddressFilter filter = new IpAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the ip is 192.168.1.101.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10, 23, FilterType.IP_ADDRESS));
        Assertions.assertEquals("192.168.1.101", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterIpv61() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IpAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new IpAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IpAddressFilter filter = new IpAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the ip is 1::");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10, 13, FilterType.IP_ADDRESS));

    }

    @Test
    public void filterIpv62() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IpAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new IpAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IpAddressFilter filter = new IpAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the ip is 2001:0db8:85a3:0000:0000:8a2e:0370:7334");

        // Finds duplicate spans. Duplicates/overlapping will be removed by the service prior to returning.
        Assertions.assertEquals(2, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10, 49, FilterType.IP_ADDRESS));
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(1), 10, 40, FilterType.IP_ADDRESS));

    }

    @Test
    public void filterIpv63() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IpAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new IpAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IpAddressFilter filter = new IpAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the ip is fe80::0202:B3FF:FE1E:8329");

        // Finds duplicate spans. Duplicates/overlapping will be removed by the service prior to returning.
        Assertions.assertEquals(2, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10, 35, FilterType.IP_ADDRESS));
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(1), 10, 31, FilterType.IP_ADDRESS));

    }

}
