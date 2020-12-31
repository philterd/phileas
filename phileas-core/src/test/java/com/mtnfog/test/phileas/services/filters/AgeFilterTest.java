package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.IgnoredPattern;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.AgeFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.AgeAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.AgeFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

public class AgeFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterIgnoredPattern1() throws Exception {

        final AlertService alertService = Mockito.mock(AlertService.class);

        final IgnoredPattern ignoredPattern = new IgnoredPattern("[0-9]+(years old)");
        final List<IgnoredPattern> ignoredPatterns = Arrays.asList(ignoredPattern);

        final AgeFilter filter = new AgeFilter(null, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), ignoredPatterns, new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the patient is 35years old.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(filterResult.getSpans().get(0).isIgnored());

    }

    @Test
    public void filterIgnoredCaseSensitive() throws Exception {

        final AlertService alertService = Mockito.mock(AlertService.class);

        final Set<String> ignore = new LinkedHashSet<>();
        ignore.add("35yEaRs old");

        final AgeFilter filter = new AgeFilter(null, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, ignore, Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the patient is 35yEaRs old.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(filterResult.getSpans().get(0).isIgnored());

    }

    @Test
    public void filter0() throws Exception {

        // This tests PHL-68. When there are no filter strategies just redact.
        final AgeFilter filter = new AgeFilter(null, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the patient is 3.5years old.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 15, 27, FilterType.AGE));
        Assertions.assertEquals("{{{REDACTED-age}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("3.5years old", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter1() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the patient is 3.5years old.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 15, 27, FilterType.AGE));

    }

    @Test
    public void filter2() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the patient age is 3.yrs.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 19, 24, FilterType.AGE));

    }

    @Test
    public void filter3() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the patient age is 3yrs.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 19, 23, FilterType.AGE));

    }

    @Test
    public void filter4() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the patient is 3.5yrs old.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 15, 25, FilterType.AGE));

    }

    @Test
    public void filter5() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the patient is 39yrs. old");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 15, 25, FilterType.AGE));

    }

    @Test
    public void filter6() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "she is aged 39");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 7, 14, FilterType.AGE));

    }

    @Test
    public void filter7() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "she is age 39");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 7, 13, FilterType.AGE));

    }

    @Test
    public void filter8() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "she is age 39.5");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 7, 15, FilterType.AGE));

    }

    @Test
    public void filter9() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "Patient Timothy Hook is 72 Yr. old male lives alone.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 24, 34, FilterType.AGE));

        LOGGER.info("Span text: " + filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter10() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "Cari Morris is 75 yo female alert and oriented x’s3 with some mild memory loss.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 15, 21, FilterType.AGE));

        LOGGER.info("Span text: " + filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter11() throws Exception {

        final List<AgeFilterStrategy> strategies = Arrays.asList(new AgeFilterStrategy());
        AgeFilter filter = new AgeFilter(strategies, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "Had symptoms for the past 10 years");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

}
