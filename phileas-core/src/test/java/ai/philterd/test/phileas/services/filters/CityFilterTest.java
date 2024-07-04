/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License", attributes);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.test.phileas.services.filters;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.enums.SensitivityLevel;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.policy.filters.strategies.dynamic.CityFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.services.anonymization.CityAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class CityFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(CityFilterTest.class);

    private String INDEX_DIRECTORY = getIndexDirectory("cities");

    private static final AnonymizationService anonymizationService = new CityAnonymizationService(new LocalAnonymizationCacheService());
    private final AlertService alertService = Mockito.mock(AlertService.class);

    @BeforeEach
    public void before() {
        INDEX_DIRECTORY = System.getProperty( "os.name" ).contains( "indow" ) ? INDEX_DIRECTORY.substring(1) : INDEX_DIRECTORY;
        LOGGER.info("Using index directory {}", INDEX_DIRECTORY);
    }

    @Test
    public void filterCitiesClose() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CityFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CityAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.LOW, false);

        assertDoesNotThrow(filter::close);

    }

    @Test
    public void filterCitiesExactMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CityFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CityAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.MEDIUM, false);

        FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Lived in Washington.", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 19, FilterType.LOCATION_CITY));

    }

    @Test
    public void filterCitiesExactMatch2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CityFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CityAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.HIGH, false);

        FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Lived in New York.", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(2, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 17, FilterType.LOCATION_CITY));
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(1), 13, 17, FilterType.LOCATION_CITY));
        Assertions.assertEquals("new york", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterCitiesLow() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CityFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CityAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.LOW, false);

        FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE,"Lived in Wshington", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterCitiesMedium() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CityFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CityAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.MEDIUM, false);

        FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Lived in Wshington", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 18, FilterType.LOCATION_CITY));

    }

    @Test
    public void filterCitiesHigh() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CityFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CityAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.HIGH, false);

        FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE,"Lived in Wasinton", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 17, FilterType.LOCATION_CITY));

    }

}
