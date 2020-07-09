package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.AgeFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.AgeAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.AgeFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AgeFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filter0() throws Exception {

        // This tests PHL-68. When there are no filter strategies just redact.
        final AgeFilter filter = new AgeFilter(null, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", 0, "the patient is 3.5years old.");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 15, 27, FilterType.AGE));
        Assertions.assertEquals("{{{REDACTED-age}}}", spans.get(0).getReplacement());
        Assertions.assertEquals("3.5years old", spans.get(0).getText());

    }

    @Test
    public void filter1() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", 0, "the patient is 3.5years old.");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 15, 27, FilterType.AGE));

    }

    @Test
    public void filter2() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", 0, "the patient is 3.yrs.");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 15, 20, FilterType.AGE));

    }

    @Test
    public void filter3() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", 0, "the patient is 3yrs.");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 15, 19, FilterType.AGE));

    }

    @Test
    public void filter4() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", 0, "the patient is 3.5yrs.");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 15, 21, FilterType.AGE));

    }

    @Test
    public void filter5() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", 0, "the patient is 39yrs.");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 15, 20, FilterType.AGE));

    }

    @Test
    public void filter6() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", 0, "she is aged 39");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 7, 14, FilterType.AGE));

    }

    @Test
    public void filter7() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", 0, "she is age 39");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 7, 13, FilterType.AGE));

    }

    @Test
    public void filter8() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", 0, "she is age 39.5");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 7, 15, FilterType.AGE));

    }

}
