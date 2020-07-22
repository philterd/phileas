package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.PassportNumberAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.PassportNumberFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

public class PassportNumberFilterTest extends AbstractFilterTest {

    final AlertService alertService = Mockito.mock(AlertService.class);

    private Filter getFilter() {
        return new PassportNumberFilter(null, new PassportNumberAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);
    }

    @Test
    public void filter1() throws Exception {

        final Filter filter = getFilter();

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the passport number is 036001231.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 23, 32, FilterType.PASSPORT_NUMBER));
        Assertions.assertEquals("{{{REDACTED-passport-number}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("036001231", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("US", filterResult.getSpans().get(0).getClassification());

    }

}