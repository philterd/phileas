package ai.philterd.test.phileas.services.filters;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.enums.SensitivityLevel;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.profile.filters.strategies.dynamic.CountyFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.services.anonymization.CountyAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class CountyFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(CountyFilterTest.class);

    private String INDEX_DIRECTORY = getIndexDirectory("counties");

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @BeforeEach
    public void before() {
        INDEX_DIRECTORY = System.getProperty( "os.name" ).contains( "indow" ) ? INDEX_DIRECTORY.substring(1) : INDEX_DIRECTORY;
        LOGGER.info("Using index directory {}", INDEX_DIRECTORY);
    }

    @Test
    public void filterCountiesLow() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new CountyFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CountyAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_COUNTY, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.LOW, false);

        FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", PIECE,"Lived in Fyette");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterCountiesMedium() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new CountyFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CountyAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_COUNTY, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.MEDIUM, false);

        FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.MEDIUM), "context", "documentid", PIECE, "Lived in Fyette");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals("fyette", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterCountiesHigh() throws Exception {

        AnonymizationService anonymizationService = new CountyAnonymizationService(new LocalAnonymizationCacheService());

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new CountyFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CountyAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_COUNTY, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.HIGH, false);

        FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.HIGH), "context", "documentid", PIECE, "Lived in Fyette");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(3, filterResult.getSpans().size());

    }

}
