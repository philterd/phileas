package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.FilterConfiguration;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.ZipCodeFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.ZipCodeAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.ZipCodeFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class ZipCodeFilterTest extends AbstractFilterTest {

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterZipCode1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", "the zip is 90210.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 16, FilterType.ZIP_CODE));
        Assertions.assertEquals("90210", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterZipCode2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", "the zip is 90210abd.");
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterZipCode3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", "the zip is 90210 in california.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 16, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", "the zip is 85055 in california.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 16, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", "the zip is 90213-1544 in california.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 21, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived in 90210.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 76, 81, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true);

        // Tests whole word only.
        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived in 9021032.");
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterZipCode8() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true);

        // Tests whole word only.
        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived in 90210-1234.");
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterZipCode9() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, false);

        // Tests without delimiter.
        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived in 902101234.");
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterZipCode10() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true);

        // Tests without delimiter.
        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived in 902101234.");
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

}
