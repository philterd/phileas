package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.FilterConfiguration;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.BitcoinAddressFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.BitcoinAddressAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.BitcoinAddressFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class BitcoinAddressFilterTest extends AbstractFilterTest {

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new BitcoinAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new BitcoinAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final BitcoinAddressFilter filter = new BitcoinAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the address is 127NVqnjf8gB9BFAW2dnQeM6wqmy1gbGtv.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 15, 49, FilterType.BITCOIN_ADDRESS));
        Assertions.assertEquals("{{{REDACTED-bitcoin-address}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("127NVqnjf8gB9BFAW2dnQeM6wqmy1gbGtv", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new BitcoinAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new BitcoinAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final BitcoinAddressFilter filter = new BitcoinAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the address is 12qnjf8FAW2dnQeM6wqmy1gbGtv.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 15, 42, FilterType.BITCOIN_ADDRESS));
        Assertions.assertEquals("{{{REDACTED-bitcoin-address}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("12qnjf8FAW2dnQeM6wqmy1gbGtv", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new BitcoinAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new BitcoinAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final BitcoinAddressFilter filter = new BitcoinAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the address is 126wqmy1gbGtv.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

}