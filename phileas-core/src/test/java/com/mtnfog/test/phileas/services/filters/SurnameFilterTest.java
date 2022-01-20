package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.filter.FilterConfiguration;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.SurnameFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.SurnameAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class SurnameFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(SurnameFilterTest.class);

    private String INDEX_DIRECTORY = getIndexDirectory("surnames");

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @BeforeEach
    public void before() {
        INDEX_DIRECTORY = System.getProperty( "os.name" ).contains( "indow" ) ? INDEX_DIRECTORY.substring(1) : INDEX_DIRECTORY;
        LOGGER.info("Using index directory {}", INDEX_DIRECTORY);
    }

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new SurnameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new SurnameAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.LOW, false);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", "Lived in Wshington");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new SurnameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new SurnameAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.MEDIUM, false);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.MEDIUM), "context", "documentid", "Lived in Wshington");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(2, filterResult.getSpans().size());

    }

    @Test
    public void filter3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new SurnameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new SurnameAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.HIGH, false);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", "Jones");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filter4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new SurnameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new SurnameAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.LOW, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", "date");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filter5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new SurnameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new SurnameAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.LOW, false);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", "Jones");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filter6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new SurnameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new SurnameAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.LOW, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", "from");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

}
