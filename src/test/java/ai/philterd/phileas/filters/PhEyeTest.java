/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.services.anonymization.AgeAnonymizationService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeConfiguration;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

@Disabled("Disabled until this is an integration tests and there can be a ph-eye service running to test against.")
public class PhEyeTest extends AbstractFilterTest {

    @Test
    public void filter1() throws Exception {

        final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration("http://localhost:18080");
        final boolean removePunctuation = false;
        final Map<String, Double> thresholds = new HashMap<>();

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withAnonymizationService(new AgeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final PhEyeFilter filter = new PhEyeFilter(filterConfiguration, phEyeConfiguration, removePunctuation, thresholds);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "George Washington was the first president.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("George Washington", filtered.getSpans().iterator().next().getText());

    }

    @Test
    public void filter2() throws Exception {

        final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration("http://localhost:18080");
        final boolean removePunctuation = false;
        final Map<String, Double> thresholds = new HashMap<>();

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withAnonymizationService(new AgeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final PhEyeFilter filter = new PhEyeFilter(filterConfiguration, phEyeConfiguration, removePunctuation, thresholds);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "No name here was the first president.");

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void multipleFilterCalls() throws Exception {

        final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration("http://localhost:18080");
        final boolean removePunctuation = false;
        final Map<String, Double> thresholds = new HashMap<>();

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withAnonymizationService(new AgeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final PhEyeFilter filter = new PhEyeFilter(filterConfiguration, phEyeConfiguration, removePunctuation, thresholds);

        // This is to test the http connection pooling for connections to ph-eye.
        for(int x = 0; x < 10; x++) {

            final Filtered filtered1 = filter.filter(getPolicy(), "context", PIECE, "George Washington was the first president.");
            Assertions.assertEquals(1, filtered1.getSpans().size());
            Assertions.assertEquals("George Washington", filtered1.getSpans().iterator().next().getText());

            final Filtered filtered2 = filter.filter(getPolicy(), "context", PIECE, "No name here was the first president.");
            Assertions.assertEquals(0, filtered2.getSpans().size());

        }

    }

}
