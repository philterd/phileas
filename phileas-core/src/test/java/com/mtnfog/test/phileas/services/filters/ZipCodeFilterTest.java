package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.ZipCodeFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.ZipCodeAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.ZipCodeFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ZipCodeFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterZipCode1() throws Exception {

        final List<ZipCodeFilterStrategy> strategies = Arrays.asList(new ZipCodeFilterStrategy());
        ZipCodeFilter filter = new ZipCodeFilter(strategies, new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the zip is 90210.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 16, FilterType.ZIP_CODE));
        Assertions.assertEquals("90210", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterZipCode2() throws Exception {

        final List<ZipCodeFilterStrategy> strategies = Arrays.asList(new ZipCodeFilterStrategy());
        ZipCodeFilter filter = new ZipCodeFilter(strategies, new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the zip is 90210abd.");
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterZipCode3() throws Exception {

        final List<ZipCodeFilterStrategy> strategies = Arrays.asList(new ZipCodeFilterStrategy());
        ZipCodeFilter filter = new ZipCodeFilter(strategies, new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the zip is 90210 in california.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 16, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode4() throws Exception {

        final List<ZipCodeFilterStrategy> strategies = Arrays.asList(new ZipCodeFilterStrategy());
        ZipCodeFilter filter = new ZipCodeFilter(strategies, new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the zip is 85055 in california.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 16, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode5() throws Exception {

        final List<ZipCodeFilterStrategy> strategies = Arrays.asList(new ZipCodeFilterStrategy());
        ZipCodeFilter filter = new ZipCodeFilter(strategies, new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the zip is 90213-1544 in california.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 21, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode6() throws Exception {

        final List<ZipCodeFilterStrategy> strategies = Arrays.asList(new ZipCodeFilterStrategy());
        ZipCodeFilter filter = new ZipCodeFilter(strategies, new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "George Washington was president and his ssn was 123-45-6789 and he lived in 90210.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 76, 81, FilterType.ZIP_CODE));

    }

}
