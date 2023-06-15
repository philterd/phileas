package ai.philterd.test.phileas.services.filters;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.profile.filters.strategies.rules.MacAddressFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.MacAddressAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import ai.philterd.phileas.services.filters.regex.MacAddressFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class MacAddressFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new MacAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new MacAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final MacAddressFilter filter = new MacAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the mac is 00-14-22-04-25-37.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 28, FilterType.MAC_ADDRESS));
        Assertions.assertEquals("00-14-22-04-25-37", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new MacAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new MacAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final MacAddressFilter filter = new MacAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the mac is 00:14:22:04:25:37.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 28, FilterType.MAC_ADDRESS));

    }

}