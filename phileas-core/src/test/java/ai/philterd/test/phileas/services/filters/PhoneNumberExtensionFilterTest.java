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
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.profile.filters.strategies.rules.PhoneNumberExtensionFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.MacAddressAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import ai.philterd.phileas.services.filters.regex.PhoneNumberExtensionFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class PhoneNumberExtensionFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhoneNumberExtensionFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new MacAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberExtensionFilter filter = new PhoneNumberExtensionFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "he is at x123");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 13, FilterType.PHONE_NUMBER_EXTENSION));
        Assertions.assertEquals("x123", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhoneNumberExtensionFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new MacAddressAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberExtensionFilter filter = new PhoneNumberExtensionFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "his phone number was +1 151-841-2829 x416.");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 37, 41, FilterType.PHONE_NUMBER_EXTENSION));
        Assertions.assertEquals("x416", filterResult.getSpans().get(0).getText());

    }

}
