package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.FilterConfiguration;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.StreetAddressFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.StreetAddressAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.StreetAddressFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class StreetAddressFilterTest extends AbstractFilterTest {

    final AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StreetAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "lived at 100 Main St");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 20, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StreetAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "lived at 100 S Main St");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 22, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StreetAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "lived at 100 South Main St");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 26, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StreetAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "lived at 1000 Main Street");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 25, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StreetAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "North 2800 Clay Edwards Drive");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 6, 29, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StreetAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "14 Southampton St.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 18, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StreetAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "22 Newport Drive");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 16, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter8() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StreetAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "78 Glendale Street");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 18, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter9() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StreetAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "6 Berkshire Court");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 17, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter10() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StreetAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "291 North Pawnee Ave.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 21, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter11() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StreetAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "468 William Street");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 18, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter12() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StreetAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "291 6th Dr.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 11, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter13() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StreetAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "9444 Heritage St.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 17, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter14() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StreetAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "70 Birchpond Street");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 19, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter15() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StreetAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "656 S. Inverness St.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 20, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter16() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StreetAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "9142 Arlington Court");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 20, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter17() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StreetAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "4 Devonshire Ct.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 16, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter18() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StreetAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StreetAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "4 Devonshire Ct");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 15, FilterType.STREET_ADDRESS));

    }

}
