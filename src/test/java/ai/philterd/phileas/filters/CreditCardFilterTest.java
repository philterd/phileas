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
import ai.philterd.phileas.services.anonymization.CreditCardAnonymizationService;
import ai.philterd.phileas.services.context.DefaultContextService;
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

        Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 4532613702852251 visa.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));
        Assertions.assertEquals("4532613702852251", filtered.getSpans().get(0).getText());

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 4556662764258031");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 4929081870602661");
        Assertions.assertEquals(1, filtered.getSpans().size());

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 4716-4366-8767-7438");
        Assertions.assertEquals(1, filtered.getSpans().size());

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 4556 5849 8186 7933");
        Assertions.assertEquals(1, filtered.getSpans().size());

        // AMEX

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 376454057275914");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 346009657106278.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        // MASTERCARD

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 5567408136464012");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 5100170632668801.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        // DISCOVER

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 6011485579364263");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 6011792597726344.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

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

        Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 1234567812345678 visa.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 0000000000000000");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 9876543219876543");
        Assertions.assertEquals(1, filtered.getSpans().size());

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 9876-5432-1987-6543");
        Assertions.assertEquals(1, filtered.getSpans().size());

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 4556 6627 6425 8000");
        Assertions.assertEquals(1, filtered.getSpans().size());

        // AMEX

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 376454057005914");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 346119657106278.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        // MASTERCARD

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 5567408136464000");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 5100170632668000.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        // DISCOVER

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 6011485579364000");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 6011792597726000.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

    }

    @Test
    public void filterCreditCardBorderedByDashes() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CreditCardFilterStrategy()))
                .withAnonymizationService(new CreditCardAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final CreditCardFilter filter = new CreditCardFilter(filterConfiguration, false, false, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 1234567812345678- visa.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(0.6, filtered.getSpans().get(0).getConfidence());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        final Filtered filtered2 = filter.filter(getPolicy(), "context", PIECE, "the payment method is -1234567812345678 visa.");
        Assertions.assertEquals(1, filtered2.getSpans().size());
        Assertions.assertEquals(0.6, filtered2.getSpans().get(0).getConfidence());
        Assertions.assertTrue(checkSpan(filtered2.getSpans().get(0), 23, 39, FilterType.CREDIT_CARD));

        final Filtered filtered3 = filter.filter(getPolicy(), "context", PIECE, "the payment method is -1234567812345678- visa.");
        Assertions.assertEquals(1, filtered3.getSpans().size());
        Assertions.assertEquals(0.5, filtered3.getSpans().get(0).getConfidence());
        Assertions.assertTrue(checkSpan(filtered3.getSpans().get(0), 23, 39, FilterType.CREDIT_CARD));

        final Filtered filtered4 = filter.filter(getPolicy(), "context", PIECE, "1234567812345678- visa.");
        Assertions.assertEquals(1, filtered4.getSpans().size());
        Assertions.assertEquals(0.6, filtered4.getSpans().get(0).getConfidence());
        Assertions.assertTrue(checkSpan(filtered4.getSpans().get(0), 0, 16, FilterType.CREDIT_CARD));

        final Filtered filtered5 = filter.filter(getPolicy(), "context", PIECE, "-1234567812345678");
        Assertions.assertEquals(1, filtered5.getSpans().size());
        Assertions.assertEquals(0.6, filtered5.getSpans().get(0).getConfidence());
        Assertions.assertTrue(checkSpan(filtered5.getSpans().get(0), 1, 17, FilterType.CREDIT_CARD));

        final Filtered filtered6 = filter.filter(getPolicy(), "context", PIECE, "-1234567812345678-");
        Assertions.assertEquals(1, filtered6.getSpans().size());
        Assertions.assertEquals(0.5, filtered6.getSpans().get(0).getConfidence());
        Assertions.assertTrue(checkSpan(filtered6.getSpans().get(0), 1, 17, FilterType.CREDIT_CARD));
    }

    @Test
    public void filterCreditCardPrecedingDigits() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CreditCardFilterStrategy()))
                .withAnonymizationService(new CreditCardAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final CreditCardFilter filter = new CreditCardFilter(filterConfiguration, true, false, false);

        Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "00752457000200000007230041111111111111116");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(0.7, filtered.getSpans().get(0).getConfidence());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 24, 40, FilterType.CREDIT_CARD));

        filtered = filter.filter(getPolicy(), "context", PIECE, "0075245700020000000133004111-1111-1111-11116");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(0.7, filtered.getSpans().get(0).getConfidence(), 0.01);
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 24, 43, FilterType.CREDIT_CARD));

        filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 4111111111111111 visa.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(0.9, filtered.getSpans().get(0).getConfidence(), 0.01);
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));
    }

    @Test
    public void filterCreditCardMultipleOnlyValid() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CreditCardFilterStrategy()))
                .withAnonymizationService(new CreditCardAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final CreditCardFilter filter = new CreditCardFilter(filterConfiguration, true, false, true);

        Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the payment method is 4532613702852251 visa and 1532000000852251.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));
        Assertions.assertEquals("4532613702852251", filtered.getSpans().get(0).getText());

    }

}
