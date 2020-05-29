package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
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
import java.util.Collections;
import java.util.List;

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

        final List<CityFilterStrategy> strategies = Arrays.asList(new CityFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, strategies, INDEX_DIRECTORY, SensitivityLevel.LOW, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        assertDoesNotThrow(() -> filter.close());

    }

    @Test
    public void filterCitiesExactMatch() throws Exception {

        final List<CityFilterStrategy> strategies = Arrays.asList(new CityFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, strategies, INDEX_DIRECTORY, SensitivityLevel.MEDIUM, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.MEDIUM), "context", "documentid","Lived in Washington.");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 9, 19, FilterType.LOCATION_CITY));

    }

    @Test
    public void filterCitiesExactMatch2() throws Exception {

        final List<CityFilterStrategy> strategies = Arrays.asList(new CityFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, strategies, INDEX_DIRECTORY, SensitivityLevel.HIGH, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.HIGH), "context", "documentid","Lived in New York.");

        showSpans(spans);

        Assertions.assertEquals(2, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 9, 17, FilterType.LOCATION_CITY));
        Assertions.assertTrue(checkSpan(spans.get(1), 13, 17, FilterType.LOCATION_CITY));
        Assertions.assertEquals("new york", spans.get(0).getText());

    }

    @Test
    public void filterCitiesLow() throws Exception {

        final List<CityFilterStrategy> strategies = Arrays.asList(new CityFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, strategies, INDEX_DIRECTORY, SensitivityLevel.LOW, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid","Lived in Wshington");

        showSpans(spans);

        Assertions.assertEquals(0, spans.size());

    }

    @Test
    public void filterCitiesMedium() throws Exception {

        final List<CityFilterStrategy> strategies = Arrays.asList(new CityFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, strategies, INDEX_DIRECTORY, SensitivityLevel.MEDIUM, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.MEDIUM), "context", "documentid","Lived in Wshington");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 9, 18, FilterType.LOCATION_CITY));

    }

    @Test
    public void filterCitiesHigh() throws Exception {

        final List<CityFilterStrategy> strategies = Arrays.asList(new CityFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, strategies, INDEX_DIRECTORY, SensitivityLevel.HIGH, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid","Lived in Wasinton");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 9, 17, FilterType.LOCATION_CITY));

    }

}
