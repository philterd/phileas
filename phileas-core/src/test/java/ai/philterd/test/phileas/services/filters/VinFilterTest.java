/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
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
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.policy.filters.strategies.rules.VinFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.services.anonymization.AlphanumericAnonymizationService;
import ai.philterd.phileas.services.anonymization.VinAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import ai.philterd.phileas.services.filters.regex.VinFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class VinFilterTest extends AbstractFilterTest {

    private final AnonymizationService anonymizationService = new VinAnonymizationService(new LocalAnonymizationCacheService());

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterVin1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new VinFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final VinFilter filter = new VinFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the vin is JB3BA36KXHU036784.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 28, FilterType.VIN));
        Assertions.assertEquals("JB3BA36KXHU036784", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterVin2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new VinFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final VinFilter filter = new VinFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the vin is 2T2HK31U38C057399.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 28, FilterType.VIN));

    }

    @Test
    public void filterVin3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new VinFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final VinFilter filter = new VinFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the vin is 11131517191011111.");
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterVin4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new VinFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final VinFilter filter = new VinFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the vin is 11131517191X11111.");
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterVin5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new VinFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final VinFilter filter = new VinFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the vin is 2t2hk31u38c057399.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 28, FilterType.VIN));

    }

}
