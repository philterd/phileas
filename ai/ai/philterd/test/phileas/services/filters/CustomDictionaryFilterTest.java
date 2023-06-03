package ai.philterd.test.phileas.services.filters;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.enums.SensitivityLevel;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.profile.filters.strategies.custom.CustomDictionaryFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.AlphanumericAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CustomDictionaryFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(CustomDictionaryFilterTest.class);

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterDictionaryExactMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new CustomDictionaryFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> names = new HashSet<>(Arrays.asList("george", "ted", "bill", "john"));
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.LOW, names, false, "names", 0);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", PIECE,"He lived with Bill in California.");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 18, FilterType.CUSTOM_DICTIONARY));
        Assertions.assertEquals("bill", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterDictionaryNoMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new CustomDictionaryFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> names = new HashSet<>(Arrays.asList("george", "ted", "bill", "john"));
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.LOW, names, false, "names", 0);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", PIECE,"He lived with Sam in California.");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

}
