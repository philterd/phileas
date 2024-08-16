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
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.policy.filters.strategies.rules.CreditCardFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.CreditCardAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import ai.philterd.phileas.services.filters.regex.CreditCardFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class CreditCardFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterCreditCardOnlyValid() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CreditCardFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CreditCardAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final CreditCardFilter filter = new CreditCardFilter(filterConfiguration, true);

        // VISA

        FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 4532613702852251 visa.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));
        Assertions.assertEquals("4532613702852251", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals(0.9, filterResult.getSpans().get(0).getConfidence());

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 4556662764258031", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 4929081870602661", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 4716-4366-8767-7438", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 4556 5849 8186 7933", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

        // AMEX

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 376454057275914", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));
        Assertions.assertEquals(0.9, filterResult.getSpans().get(0).getConfidence());

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 346009657106278.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        // MASTERCARD

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 5567408136464012", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));
        Assertions.assertEquals(0.9, filterResult.getSpans().get(0).getConfidence());

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 5100170632668801.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        // DISCOVER

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 6011485579364263", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));
        Assertions.assertEquals(0.9, filterResult.getSpans().get(0).getConfidence());

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 6011792597726344.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

    }

    @Test
    public void filterCreditCardValidAndInvalid() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CreditCardFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new CreditCardAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final CreditCardFilter filter = new CreditCardFilter(filterConfiguration, false);

        // VISA

        FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 1234567812345678 visa.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 0000000000000000", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 9876543219876543", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 9876-5432-1987-6543", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 4556 6627 6425 8000", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

        // AMEX

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 376454057005914", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 346119657106278.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        // MASTERCARD

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 5567408136464000", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 5100170632668000.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        // DISCOVER

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 6011485579364000", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the payment method is 6011792597726000.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

    }

    @Test
    public void filterCreditCardsWithinUUIDs() throws Exception {

        final CreditCardFilterStrategy strategy = new CreditCardFilterStrategy();

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(strategy))
                .withAlertService(alertService)
                .withAnonymizationService(new CreditCardAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final CreditCardFilter filter = new CreditCardFilter(filterConfiguration, true);
        final String validCard = "47223179-9330-4259";

        // match with preceding dash
        FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the UUID is b66c-" + validCard, attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(FilterType.CREDIT_CARD, filterResult.getSpans().get(0).getFilterType());
        Assertions.assertEquals(validCard, filterResult.getSpans().get(0).getText());
        Assertions.assertEquals(0.6, filterResult.getSpans().get(0).getConfidence());  // reduced because of dash
        Assertions.assertTrue(filterResult.getSpans().get(0).isApplied());

        // match with trailing dash
        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, validCard + "-b66c is a UUID", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(FilterType.CREDIT_CARD, filterResult.getSpans().get(0).getFilterType());
        Assertions.assertEquals(validCard, filterResult.getSpans().get(0).getText());
        Assertions.assertEquals(0.6, filterResult.getSpans().get(0).getConfidence());  // reduced because of dash
        Assertions.assertTrue(filterResult.getSpans().get(0).isApplied());

        // match with preceding and trailing dash
        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "{ id: \"b66c-" + validCard + "-ab12\" }", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(FilterType.CREDIT_CARD, filterResult.getSpans().get(0).getFilterType());
        Assertions.assertEquals(validCard, filterResult.getSpans().get(0).getText());
        Assertions.assertEquals(0.5, filterResult.getSpans().get(0).getConfidence());  // reduced because of dashes
        Assertions.assertTrue(filterResult.getSpans().get(0).isApplied());

        // skip applying low-confidence spans
        strategy.setConditions("confidence > 0.7");
        filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "{ id: \"b66c-" + validCard + "-ab12\" }", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(FilterType.CREDIT_CARD, filterResult.getSpans().get(0).getFilterType());
        Assertions.assertEquals(validCard, filterResult.getSpans().get(0).getText());
        Assertions.assertEquals(0.5, filterResult.getSpans().get(0).getConfidence());  // reduced because of dashes
        Assertions.assertFalse(filterResult.getSpans().get(0).isApplied());            // not applied because of condition

    }

}
