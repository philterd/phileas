/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
import ai.philterd.phileas.model.profile.filters.strategies.dynamic.StateFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.StateAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class StateFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(StateFilterTest.class);

    private String INDEX_DIRECTORY = getIndexDirectory("states");

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @BeforeEach
    public void before() {
        INDEX_DIRECTORY = System.getProperty( "os.name" ).contains( "indow" ) ? INDEX_DIRECTORY.substring(1) : INDEX_DIRECTORY;
        LOGGER.info("Using index directory {}", INDEX_DIRECTORY);
    }

    @Test
    public void filterStatesLow() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StateFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StateAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_STATE, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.LOW, false);

        FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", PIECE,"Lived in Washington");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals("washington", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterStatesMedium() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StateFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StateAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_STATE, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.MEDIUM, false);

        FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.MEDIUM), "context", "documentid", PIECE, "Lived in Wshington");
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterStatesHigh() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StateFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StateAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_STATE, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.HIGH, false);

        FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.HIGH), "context", "documentid", PIECE, "Lived in Wasinton");
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

}
