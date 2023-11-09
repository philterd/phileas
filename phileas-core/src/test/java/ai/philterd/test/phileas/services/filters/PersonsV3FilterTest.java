/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
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
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.policy.filters.strategies.ai.PersonsFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.model.services.MetricsService;
import ai.philterd.phileas.services.anonymization.PersonsAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import ai.philterd.phileas.services.filters.ai.opennlp.PersonsV3Filter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PersonsV3FilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(PersonsV3FilterTest.class);

    private final AlertService alertService = Mockito.mock(AlertService.class);
    private final MetricsService metricsService = Mockito.mock(MetricsService.class);

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PersonsFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final File model = new File(getClass().getClassLoader().getResource("models/en-ner-person.bin").toURI());

        final Map<String, DescriptiveStatistics> stats = new LinkedHashMap<>();
        final Map<String, Double> thresholds = new LinkedHashMap<>();

        final PersonsV3Filter filter = new PersonsV3Filter(
                filterConfiguration,
                model.getAbsolutePath(),
                stats,
                metricsService,
                thresholds);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "George Washington and Abraham Lincoln were presidents.", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 37, FilterType.PERSON));
        Assertions.assertEquals("Abraham Lincoln", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("{{{REDACTED-person}}}", filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PersonsFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final File model = new File(getClass().getClassLoader().getResource("models/en-ner-person.bin").toURI());

        final Map<String, DescriptiveStatistics> stats = new LinkedHashMap<>();
        final Map<String, Double> thresholds = new LinkedHashMap<>();

        final PersonsV3Filter filter = new PersonsV3Filter(
                filterConfiguration,
                model.getAbsolutePath(),
                stats,
                metricsService,
                thresholds);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "George    Washington      and Abraham    Lincoln were presidents.", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 37, FilterType.PERSON));
        Assertions.assertEquals("Abraham Lincoln", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("{{{REDACTED-person}}}", filterResult.getSpans().get(0).getReplacement());

    }

}
