/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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

import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.services.MetricsService;
import ai.philterd.phileas.services.anonymization.AgeAnonymizationService;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeConfiguration;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Disabled("Disabled until this is an integration tests and there can be a ph-eye service running to test against.")
public class PhEyeTest extends AbstractFilterTest {

    @Test
    public void filter1() throws Exception {

        final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration("http://localhost:18080");
        final MetricsService metricsService = Mockito.mock(MetricsService.class);
        final boolean removePunctuation = false;
        final Map<String, Double> thresholds = new HashMap<>();

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withAnonymizationService(new AgeAnonymizationService())
                .withWindowSize(windowSize)
                .build();

        final PhEyeFilter filter = new PhEyeFilter(filterConfiguration, phEyeConfiguration, metricsService, removePunctuation, thresholds);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", Collections.emptyMap(), "documentid", PIECE, "George Washington was the first president.", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals("George Washington", filterResult.getSpans().iterator().next().getText());

    }

    @Test
    public void filter2() throws Exception {

        final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration("http://localhost:18080");
        final MetricsService metricsService = Mockito.mock(MetricsService.class);
        final boolean removePunctuation = false;
        final Map<String, Double> thresholds = new HashMap<>();

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withAnonymizationService(new AgeAnonymizationService())
                .withWindowSize(windowSize)
                .build();

        final PhEyeFilter filter = new PhEyeFilter(filterConfiguration, phEyeConfiguration, metricsService, removePunctuation, thresholds);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", Collections.emptyMap(), "documentid", PIECE, "No name here was the first president.", attributes);

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void multipleFilterCalls() throws Exception {

        final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration("http://localhost:18080");
        final MetricsService metricsService = Mockito.mock(MetricsService.class);
        final boolean removePunctuation = false;
        final Map<String, Double> thresholds = new HashMap<>();

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withAnonymizationService(new AgeAnonymizationService())
                .withWindowSize(windowSize)
                .build();

        final PhEyeFilter filter = new PhEyeFilter(filterConfiguration, phEyeConfiguration, metricsService, removePunctuation, thresholds);

        // This is to test the http connection pooling for connections to ph-eye.
        for(int x = 0; x < 10; x++) {

            final FilterResult filterResult1 = filter.filter(getPolicy(), "context", Collections.emptyMap(), "documentid", PIECE, "George Washington was the first president.", attributes);
            Assertions.assertEquals(1, filterResult1.getSpans().size());
            Assertions.assertEquals("George Washington", filterResult1.getSpans().iterator().next().getText());

            final FilterResult filterResult2 = filter.filter(getPolicy(), "context", Collections.emptyMap(), "documentid", PIECE, "No name here was the first president.", attributes);
            Assertions.assertEquals(0, filterResult2.getSpans().size());

        }

    }

}
