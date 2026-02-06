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

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.services.anonymization.AlphanumericAnonymizationService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.filters.regex.DriversLicenseFilter;
import ai.philterd.phileas.services.strategies.rules.DriversLicenseFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class DriversLicenseFilterTest extends AbstractFilterTest {

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("candidate1", "candidate2");
        final AlphanumericAnonymizationService alphanumericAnonymizationService = new AlphanumericAnonymizationService(new DefaultContextService(), new SecureRandom(), candidates);

        final DriversLicenseFilterStrategy driversLicenseFilterStrategy = new DriversLicenseFilterStrategy();
        driversLicenseFilterStrategy.setStrategy(RANDOM_REPLACE);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(driversLicenseFilterStrategy))
                .withAnonymizationService(alphanumericAnonymizationService)
                .withWindowSize(windowSize)
                .build();

        final DriversLicenseFilter filter = new DriversLicenseFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the number is 123456789.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

}