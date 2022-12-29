package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.filter.FilterConfiguration;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.CityFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.CityAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class CityFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(CityFilterTest.class);

    private String INDEX_DIRECTORY = getIndexDirectory("cities");

    private static final AnonymizationService anonymizationService = new CityAnonymizationService(new LocalAnonymizationCacheService());
    private AlertService alertService = Mockito.mock(AlertService.class);

    @BeforeEach
    public void before() {
        INDEX_DIRECTORY = System.getProperty( "os.name" ).contains( "indow" ) ? INDEX_DIRECTORY.substring(1) : INDEX_DIRECTORY;
        LOGGER.info("Using index directory {}", INDEX_DIRECTORY);
    }

    @Test
    public void filterCitiesClose() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new CityFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CityAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.LOW, false);

        assertDoesNotThrow(() -> filter.close());

    }

    @Test
    public void filterCitiesExactMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new CityFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CityAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.MEDIUM, false);

        FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.MEDIUM), "context", "documentid", PIECE, "Lived in Washington.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 19, FilterType.LOCATION_CITY));

    }

    @Test
    public void filterCitiesExactMatch2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new CityFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CityAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.HIGH, false);

        FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.HIGH), "context", "documentid", PIECE, "Lived in New York.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(2, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 17, FilterType.LOCATION_CITY));
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(1), 13, 17, FilterType.LOCATION_CITY));
        Assertions.assertEquals("new york", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterCitiesLow() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new CityFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CityAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.LOW, false);

        FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", PIECE,"Lived in Wshington");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterCitiesMedium() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new CityFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CityAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.MEDIUM, false);

        FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.MEDIUM), "context", "documentid", PIECE, "Lived in Wshington");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 18, FilterType.LOCATION_CITY));

    }

    @Test
    public void filterCitiesHigh() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new CityFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CityAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.HIGH, false);

        FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", PIECE,"Lived in Wasinton");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 17, FilterType.LOCATION_CITY));

    }

}
