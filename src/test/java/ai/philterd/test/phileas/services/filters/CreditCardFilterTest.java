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

import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.services.DefaultContextService;
import ai.philterd.phileas.services.anonymization.CreditCardAnonymizationService;
import ai.philterd.phileas.services.filters.regex.CreditCardFilter;
import ai.philterd.phileas.services.strategies.rules.CreditCardFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CreditCardFilterTest extends AbstractFilterTest {
    
    @Test
    public void filterCreditCardOnlyValid() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CreditCardFilterStrategy()))
                .withAnonymizationService(new CreditCardAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final CreditCardFilter filter = new CreditCardFilter(filterConfiguration, true, false, true);

        // VISA

        FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 4532613702852251 visa.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));
        Assertions.assertEquals("4532613702852251", filterResult.getSpans().get(0).getText());

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 4556662764258031", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 4929081870602661", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 4716-4366-8767-7438", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 4556 5849 8186 7933", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

        // AMEX

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 376454057275914", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 346009657106278.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        // MASTERCARD

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 5567408136464012", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 5100170632668801.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        // DISCOVER

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 6011485579364263", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 6011792597726344.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

    }


    @Test
    public void filterCreditCardValidAndInvalid() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CreditCardFilterStrategy()))
                .withAnonymizationService(new CreditCardAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final CreditCardFilter filter = new CreditCardFilter(filterConfiguration, false, false, true);

        // VISA

        FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 1234567812345678 visa.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 0000000000000000", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 9876543219876543", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 9876-5432-1987-6543", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 4556 6627 6425 8000", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

        // AMEX

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 376454057005914", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 346119657106278.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        // MASTERCARD

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 5567408136464000", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 5100170632668000.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        // DISCOVER

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 6011485579364000", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 6011792597726000.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

    }

    @Test
    public void filterCreditCardBorderedByDashes() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CreditCardFilterStrategy()))
                .withAnonymizationService(new CreditCardAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final CreditCardFilter filter = new CreditCardFilter(filterConfiguration, false, false, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 1234567812345678- visa.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(0.6, filterResult.getSpans().get(0).getConfidence());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        final FilterResult filterResult2 = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is -1234567812345678 visa.", attributes);
        Assertions.assertEquals(1, filterResult2.getSpans().size());
        Assertions.assertEquals(0.6, filterResult2.getSpans().get(0).getConfidence());
        Assertions.assertTrue(checkSpan(filterResult2.getSpans().get(0), 23, 39, FilterType.CREDIT_CARD));

        final FilterResult filterResult3 = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is -1234567812345678- visa.", attributes);
        Assertions.assertEquals(1, filterResult3.getSpans().size());
        Assertions.assertEquals(0.5, filterResult3.getSpans().get(0).getConfidence());
        Assertions.assertTrue(checkSpan(filterResult3.getSpans().get(0), 23, 39, FilterType.CREDIT_CARD));

        final FilterResult filterResult4 = filter.filter(getPolicy(), "context",  "documentid", PIECE, "1234567812345678- visa.", attributes);
        Assertions.assertEquals(1, filterResult4.getSpans().size());
        Assertions.assertEquals(0.6, filterResult4.getSpans().get(0).getConfidence());
        Assertions.assertTrue(checkSpan(filterResult4.getSpans().get(0), 0, 16, FilterType.CREDIT_CARD));

        final FilterResult filterResult5 = filter.filter(getPolicy(), "context",  "documentid", PIECE, "-1234567812345678", attributes);
        Assertions.assertEquals(1, filterResult5.getSpans().size());
        Assertions.assertEquals(0.6, filterResult5.getSpans().get(0).getConfidence());
        Assertions.assertTrue(checkSpan(filterResult5.getSpans().get(0), 1, 17, FilterType.CREDIT_CARD));

        final FilterResult filterResult6 = filter.filter(getPolicy(), "context",  "documentid", PIECE, "-1234567812345678-", attributes);
        Assertions.assertEquals(1, filterResult6.getSpans().size());
        Assertions.assertEquals(0.5, filterResult6.getSpans().get(0).getConfidence());
        Assertions.assertTrue(checkSpan(filterResult6.getSpans().get(0), 1, 17, FilterType.CREDIT_CARD));
    }

    @Test
    public void filterCreditCardPrecedingDigits() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CreditCardFilterStrategy()))
                .withAnonymizationService(new CreditCardAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final CreditCardFilter filter = new CreditCardFilter(filterConfiguration, true, false, false);

        FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "00752457000200000007230041111111111111116", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(0.7, filterResult.getSpans().get(0).getConfidence());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 24, 40, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "0075245700020000000133004111-1111-1111-11116", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(0.7, filterResult.getSpans().get(0).getConfidence(), 0.01);
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 24, 43, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 4111111111111111 visa.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(0.9, filterResult.getSpans().get(0).getConfidence(), 0.01);
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));
    }

    @Test
    public void filterCreditCardMultipleOnlyValid() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CreditCardFilterStrategy()))
                .withAnonymizationService(new CreditCardAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final CreditCardFilter filter = new CreditCardFilter(filterConfiguration, true, false, true);

        FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the payment method is 4532613702852251 visa and 1532000000852251.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));
        Assertions.assertEquals("4532613702852251", filterResult.getSpans().get(0).getText());

    }

}
