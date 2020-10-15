package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.CreditCardFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.CreditCardAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.CreditCardFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CreditCardFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterCreditCardOnlyValid() throws Exception {

        final List<CreditCardFilterStrategy> strategies = Arrays.asList(new CreditCardFilterStrategy());
        CreditCardFilter filter = new CreditCardFilter(strategies, new CreditCardAnonymizationService(new LocalAnonymizationCacheService()), alertService,true, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        // VISA

        FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 4532613702852251 visa.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));
        Assertions.assertEquals("4532613702852251", filterResult.getSpans().get(0).getText());

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 4556662764258031");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 4929081870602661");
        Assertions.assertEquals(1, filterResult.getSpans().size());

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 4716-4366-8767-7438");
        Assertions.assertEquals(1, filterResult.getSpans().size());

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 4556 5849 8186 7933");
        Assertions.assertEquals(1, filterResult.getSpans().size());

        // AMEX

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 376454057275914");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 346009657106278.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        // MASTERCARD

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 5567408136464012");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 5100170632668801.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        // DISCOVER

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 6011485579364263");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 6011792597726344.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

    }

    @Test
    public void filterCreditCardValidAndInvalid() throws Exception {

        final List<CreditCardFilterStrategy> strategies = Arrays.asList(new CreditCardFilterStrategy());
        CreditCardFilter filter = new CreditCardFilter(strategies, new CreditCardAnonymizationService(new LocalAnonymizationCacheService()), alertService, false, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        // VISA

        FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 1234567812345678 visa.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 0000000000000000");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 9876543219876543");
        Assertions.assertEquals(1, filterResult.getSpans().size());

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 9876-5432-1987-6543");
        Assertions.assertEquals(1, filterResult.getSpans().size());

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 4556 6627 6425 8000");
        Assertions.assertEquals(1, filterResult.getSpans().size());

        // AMEX

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 376454057005914");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 346119657106278.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 37, FilterType.CREDIT_CARD));

        // MASTERCARD

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 5567408136464000");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 5100170632668000.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        // DISCOVER

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 6011485579364000");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

        filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the payment method is 6011792597726000.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 22, 38, FilterType.CREDIT_CARD));

    }

}
