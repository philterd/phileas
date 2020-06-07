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

}
