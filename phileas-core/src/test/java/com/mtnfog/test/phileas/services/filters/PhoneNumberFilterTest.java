package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.FilterConfiguration;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.PhoneNumberFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.MacAddressAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.custom.PhoneNumberRulesFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class PhoneNumberFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(PhoneNumberFilterTest.class);

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterPhone1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhoneNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new MacAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", "the number is (123) 456-7890.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 28, FilterType.PHONE_NUMBER));
        Assertions.assertEquals("(123) 456-7890", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals(0.95, filterResult.getSpans().get(0).getConfidence());

    }

    @Test
    public void filterPhone2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhoneNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new MacAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", "the number is (123) 456-7890 and (123) 456-7890.");
        Assertions.assertEquals(2, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 28, FilterType.PHONE_NUMBER));
        Assertions.assertEquals(0.95, filterResult.getSpans().get(0).getConfidence());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(1), 33, 47, FilterType.PHONE_NUMBER));
        Assertions.assertEquals(0.95, filterResult.getSpans().get(1).getConfidence());

    }

    @Test
    public void filterPhone3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhoneNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new MacAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", "the number is 123-456-7890.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 26, FilterType.PHONE_NUMBER));
        Assertions.assertEquals(0.95, filterResult.getSpans().get(0).getConfidence());

    }

    @Test
    public void filterPhone4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhoneNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new MacAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", "the number is 123-456-7890 and he was ok.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 26, FilterType.PHONE_NUMBER));

    }

    @Test
    public void filterPhone5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhoneNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new MacAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", "the number is ( 800 ) 123-4567 and he was ok.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 30, FilterType.PHONE_NUMBER));
        Assertions.assertEquals(0.75, filterResult.getSpans().get(0).getConfidence());

    }

    @Test
    public void filterPhone6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhoneNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new MacAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", "the number is (800) 123-4567 x532 and he was ok.");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 33, FilterType.PHONE_NUMBER));
        Assertions.assertEquals(0.75, filterResult.getSpans().get(0).getConfidence());

    }

    @Test
    public void filterPhone7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhoneNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new MacAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", "the number is (800) 123-4567x532 and he was ok.");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 32, FilterType.PHONE_NUMBER));
        Assertions.assertEquals(0.75, filterResult.getSpans().get(0).getConfidence());

    }

    @Test
    public void filterPhone8() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhoneNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new MacAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", "7 64116-3220");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(0.60, filterResult.getSpans().get(0).getConfidence());

    }

}
