package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.AgeFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.AgeAnonymizationService;
import com.mtnfog.phileas.services.anonymization.AlphanumericAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.AgeFilter;
import com.mtnfog.phileas.services.filters.regex.TrackingNumberFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TrackingNumberFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filter0() throws Exception {

        final Filter filter = new TrackingNumberFilter(null, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the tracking number is 1Z9YF1280343418566");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 23, 41, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", spans.get(0).getReplacement());
        Assertions.assertEquals("1Z9YF1280343418566", spans.get(0).getText());
        Assertions.assertEquals("ups", spans.get(0).getClassification());

    }

    @Test
    public void filter1() throws Exception {

        final Filter filter = new TrackingNumberFilter(null, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the tracking number is 9400100000000000000000");

        showSpans(spans);

        Assertions.assertEquals(2, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(1), 23, 45, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", spans.get(1).getReplacement());
        Assertions.assertEquals("9400100000000000000000", spans.get(1).getText());
        Assertions.assertEquals("usps", spans.get(1).getClassification());

    }

    @Test
    @Disabled
    public void filter2() throws Exception {

        final Filter filter = new TrackingNumberFilter(null, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the tracking number is 9400 1000 0000 0000 0000");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 23, 47, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", spans.get(0).getReplacement());
        Assertions.assertEquals("9400 1000 0000 0000 0000", spans.get(0).getText());
        Assertions.assertEquals("usps", spans.get(0).getClassification());

    }

    @Test
    public void filter3() throws Exception {

        final Filter filter = new TrackingNumberFilter(null, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the tracking number is 4204319935009201990138501144099814");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 23, 57, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", spans.get(0).getReplacement());
        Assertions.assertEquals("4204319935009201990138501144099814", spans.get(0).getText());
        Assertions.assertEquals("usps", spans.get(0).getClassification());

    }

    @Test
    public void filter4() throws Exception {

        final Filter filter = new TrackingNumberFilter(null, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the tracking number is 420431993500920199013850114409");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 23, 53, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", spans.get(0).getReplacement());
        Assertions.assertEquals("420431993500920199013850114409", spans.get(0).getText());
        Assertions.assertEquals("usps", spans.get(0).getClassification());

    }

    @Test
    public void filter5() throws Exception {

        final Filter filter = new TrackingNumberFilter(null, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the tracking number is 4204319935009201990138501144");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 23, 51, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", spans.get(0).getReplacement());
        Assertions.assertEquals("4204319935009201990138501144", spans.get(0).getText());
        Assertions.assertEquals("usps", spans.get(0).getClassification());

    }

    @Test
    public void filter6() throws Exception {

        final Filter filter = new TrackingNumberFilter(null, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the tracking number is 42043199350092019901385011");

        showSpans(spans);

        Assertions.assertEquals(2, spans.size());

        Assertions.assertTrue(checkSpan(spans.get(0), 23, 49, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", spans.get(0).getReplacement());
        Assertions.assertEquals("42043199350092019901385011", spans.get(0).getText());
        Assertions.assertEquals("ups", spans.get(0).getClassification());

        Assertions.assertTrue(checkSpan(spans.get(1), 23, 49, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", spans.get(1).getReplacement());
        Assertions.assertEquals("42043199350092019901385011", spans.get(1).getText());
        Assertions.assertEquals("usps", spans.get(1).getClassification());

    }

}
