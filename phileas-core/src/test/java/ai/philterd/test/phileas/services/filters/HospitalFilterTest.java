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
import ai.philterd.phileas.model.policy.filters.strategies.dynamic.HospitalFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.HospitalAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class HospitalFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(HospitalFilterTest.class);

    private final String INDEX_DIRECTORY = getIndexDirectory("hospitals");

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @BeforeEach
    public void before() {
        LOGGER.info("Using index directory {}", INDEX_DIRECTORY);
    }

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new HospitalFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new HospitalAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.HOSPITAL, filterConfiguration, INDEX_DIRECTORY, SensitivityLevel.LOW, false);

        FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE,"Wyoming Medical Center", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals("wyoming medical center", filterResult.getSpans().get(0).getText());

    }

}
