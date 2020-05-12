package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.PhoneNumberExtensionFilterStrategy;
import com.mtnfog.phileas.services.anonymization.AlphanumericAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.PhoneNumberExtensionFilter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PhoneNumberExtensionFilterTest extends AbstractFilterTest {

    @Test
    public void filter1() throws Exception {

        final List<PhoneNumberExtensionFilterStrategy> strategies = Arrays.asList(new PhoneNumberExtensionFilterStrategy());
        PhoneNumberExtensionFilter filter = new PhoneNumberExtensionFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","he is at x123");

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 9, 13, FilterType.PHONE_NUMBER_EXTENSION));
        Assert.assertEquals("x123", spans.get(0).getText());

    }

    @Test
    public void filter2() throws Exception {

        final List<PhoneNumberExtensionFilterStrategy> strategies = Arrays.asList(new PhoneNumberExtensionFilterStrategy());
        PhoneNumberExtensionFilter filter = new PhoneNumberExtensionFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","his phone number was +1 151-841-2829 x416.");

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 37, 41, FilterType.PHONE_NUMBER_EXTENSION));
        Assert.assertEquals("x416", spans.get(0).getText());

    }

}
