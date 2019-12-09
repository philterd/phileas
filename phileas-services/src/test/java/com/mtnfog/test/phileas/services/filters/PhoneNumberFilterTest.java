package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.PhoneNumberFilterStrategy;
import com.mtnfog.phileas.services.anonymization.AlphanumericAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.custom.PhoneNumberRulesFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class PhoneNumberFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(PhoneNumberFilterTest.class);

    @Test
    public void filterPhone1() throws Exception {

        final List<PhoneNumberFilterStrategy> strategies = Arrays.asList(new PhoneNumberFilterStrategy());
        PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the number is (123) 456-7890.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 14, 28, FilterType.PHONE_NUMBER));

    }

    @Test
    public void filterPhone2() throws Exception {

        final List<PhoneNumberFilterStrategy> strategies = Arrays.asList(new PhoneNumberFilterStrategy());
        PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the number is (123) 456-7890 and (123) 456-7890.");
        Assert.assertEquals(2, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 14, 28, FilterType.PHONE_NUMBER));
        Assert.assertTrue(checkSpan(spans.get(1), 33, 47, FilterType.PHONE_NUMBER));

    }

    @Test
    public void filterPhone3() throws Exception {

        final List<PhoneNumberFilterStrategy> strategies = Arrays.asList(new PhoneNumberFilterStrategy());
        PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the number is 123-456-7890.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 14, 26, FilterType.PHONE_NUMBER));

    }

    @Test
    public void filterPhone4() throws Exception {

        final List<PhoneNumberFilterStrategy> strategies = Arrays.asList(new PhoneNumberFilterStrategy());
        PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the number is 123-456-7890 and he was ok.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 14, 26, FilterType.PHONE_NUMBER));

    }

    @Test
    public void filterPhone5() throws Exception {

        final List<PhoneNumberFilterStrategy> strategies = Arrays.asList(new PhoneNumberFilterStrategy());
        PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the number is ( 800 ) 123-4567 and he was ok.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 14, 30, FilterType.PHONE_NUMBER));

    }

    @Test
    public void filterPhone6() throws Exception {

        final List<PhoneNumberFilterStrategy> strategies = Arrays.asList(new PhoneNumberFilterStrategy());
        PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the number is (800) 123-4567 x532 and he was ok.");

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 14, 33, FilterType.PHONE_NUMBER));

    }

    @Test
    public void filterPhone7() throws Exception {

        final List<PhoneNumberFilterStrategy> strategies = Arrays.asList(new PhoneNumberFilterStrategy());
        PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the number is (800) 123-4567x532 and he was ok.");

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 14, 32, FilterType.PHONE_NUMBER));

    }

}