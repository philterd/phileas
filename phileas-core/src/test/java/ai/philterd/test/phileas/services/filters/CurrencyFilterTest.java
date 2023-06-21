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
import ai.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.CurrencyFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.CurrencyAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import ai.philterd.phileas.services.filters.regex.CurrencyFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class CurrencyFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new CurrencyFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CurrencyAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the drug cost is $35.53 .");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 17, 23, FilterType.CURRENCY));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new CurrencyFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CurrencyAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the drug cost is $35.53.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 17, 23, FilterType.CURRENCY));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new CurrencyFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CurrencyAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the drug cost is $35.00.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 17, 23, FilterType.CURRENCY));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new CurrencyFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CurrencyAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the drug cost is $3.00.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 17, 22, FilterType.CURRENCY));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new CurrencyFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CurrencyAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the drug cost is $.50.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 17, 21, FilterType.CURRENCY));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter6() throws Exception {

        final CurrencyFilterStrategy currencyFilterStrategy = new CurrencyFilterStrategy();
        currencyFilterStrategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(currencyFilterStrategy))
                .withAlertService(alertService)
                .withAnonymizationService(new CurrencyAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the drug cost is $50.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 17, 20, FilterType.CURRENCY));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter7() throws Exception {

        final CurrencyFilterStrategy currencyFilterStrategy = new CurrencyFilterStrategy();
        currencyFilterStrategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(currencyFilterStrategy))
                .withAlertService(alertService)
                .withAnonymizationService(new CurrencyAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the drug cost is $.50.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 17, 21, FilterType.CURRENCY));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

}
