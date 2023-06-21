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
import ai.philterd.phileas.model.profile.filters.strategies.rules.CreditCardFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.CreditCardAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import ai.philterd.phileas.services.filters.regex.CreditCardFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class CreditCardFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterCreditCardOnlyValid() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new CreditCardFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CreditCardAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final CreditCardFilter filter = new CreditCardFilter(filterConfiguration, true);

        // VISA

        FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 4532613702852251 visa.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));
        Assertions.assertEquals("4532613702852251", filterResult.getSpans().get(0).getText());

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 4556662764258031");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 4929081870602661");
        Assertions.assertEquals(1, filterResult.getSpans().size());

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 4716-4366-8767-7438");
        Assertions.assertEquals(1, filterResult.getSpans().size());

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 4556 5849 8186 7933");
        Assertions.assertEquals(1, filterResult.getSpans().size());

        // AMEX

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 376454057275914");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 346009657106278.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        // MASTERCARD

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 5567408136464012");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 5100170632668801.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        // DISCOVER

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 6011485579364263");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 6011792597726344.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

    }

    @Test
    public void filterCreditCardValidAndInvalid() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new CreditCardFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CreditCardAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final CreditCardFilter filter = new CreditCardFilter(filterConfiguration, false);

        // VISA

        FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 1234567812345678 visa.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 0000000000000000");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 9876543219876543");
        Assertions.assertEquals(1, filterResult.getSpans().size());

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 9876-5432-1987-6543");
        Assertions.assertEquals(1, filterResult.getSpans().size());

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 4556 6627 6425 8000");
        Assertions.assertEquals(1, filterResult.getSpans().size());

        // AMEX

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 376454057005914");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 346119657106278.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        // MASTERCARD

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 5567408136464000");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 5100170632668000.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        // DISCOVER

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 6011485579364000");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the payment method is 6011792597726000.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

    }

}
