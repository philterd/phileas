/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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
import ai.philterd.phileas.model.filter.rules.dictionary.FuzzyDictionaryFilter;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.policy.filters.strategies.dynamic.HospitalAbbreviationFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.HospitalAbbreviationAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class HospitalAbbreviationFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(HospitalAbbreviationFilterTest.class);

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new HospitalAbbreviationFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new HospitalAbbreviationAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.HOSPITAL_ABBREVIATION, filterConfiguration, SensitivityLevel.HIGH, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Went to WMC", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

}
