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
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.policy.filters.strategies.dynamic.SurnameFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.SurnameAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class SurnameFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(SurnameFilterTest.class);

    private final String INDEX_DIRECTORY = getIndexDirectory("surnames");

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @BeforeEach
    public void before() {
        LOGGER.info("Using index directory {}", INDEX_DIRECTORY);
    }

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SurnameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new SurnameAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.LOW, false);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Lived in Wshington", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SurnameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new SurnameAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.MEDIUM, false);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Lived in Wshington", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(2, filterResult.getSpans().size());

    }

    @Test
    public void filter3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SurnameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new SurnameAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.HIGH, false);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Jones", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filter4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SurnameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new SurnameAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.LOW, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "date", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filter5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SurnameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new SurnameAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.LOW, false);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Jones", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filter6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SurnameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new SurnameAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.LOW, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "from", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

}
