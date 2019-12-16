package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.CityFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.CreditCardFilterStrategy;
import com.mtnfog.phileas.services.anonymization.CreditCardAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.CreditCardFilter;
import com.mtnfog.test.phileas.services.filters.AbstractFilterTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CreditCardFilterTest extends AbstractFilterTest {

    @Test
    public void filterCreditCard() throws Exception {

        final List<CreditCardFilterStrategy> strategies = Arrays.asList(new CreditCardFilterStrategy());
        CreditCardFilter filter = new CreditCardFilter(strategies, new CreditCardAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet());

        // VISA

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the payment method is 4532613702852251 visa.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 22, 38, FilterType.CREDIT_CARD));

        spans  = filter.filter(getFilterProfile(), "context", "documentid", "the payment method is 4556662764258031");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 22, 38, FilterType.CREDIT_CARD));

        spans  = filter.filter(getFilterProfile(), "context", "documentid", "the payment method is 4556662764258000");
        Assert.assertEquals(0, spans.size());

        // AMEX

        spans = filter.filter(getFilterProfile(), "context", "documentid", "the payment method is 376454057275914");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 22, 37, FilterType.CREDIT_CARD));

        spans = filter.filter(getFilterProfile(), "context", "documentid", "the payment method is 346009657106278.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 22, 37, FilterType.CREDIT_CARD));

        // MASTERCARD

        spans = filter.filter(getFilterProfile(), "context", "documentid", "the payment method is 5567408136464012");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 22, 38, FilterType.CREDIT_CARD));

        spans = filter.filter(getFilterProfile(), "context", "documentid", "the payment method is 5100170632668801.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 22, 38, FilterType.CREDIT_CARD));

        // DISCOVER

        spans = filter.filter(getFilterProfile(), "context", "documentid", "the payment method is 6011485579364263");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 22, 38, FilterType.CREDIT_CARD));

        spans = filter.filter(getFilterProfile(), "context", "documentid", "the payment method is 6011792597726344.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 22, 38, FilterType.CREDIT_CARD));

    }

}
