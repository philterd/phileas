package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.Filter;
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
        return new PassportNumberFilter(null, new PassportNumberAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);
    }

    @Test
    public void filter1() throws Exception {

        final Filter filter = getFilter();

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", 0, "the passport number is 036001231.");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 23, 32, FilterType.PASSPORT_NUMBER));
        Assertions.assertEquals("{{{REDACTED-passport-number}}}", spans.get(0).getReplacement());
        Assertions.assertEquals("036001231", spans.get(0).getText());
        Assertions.assertEquals("US", spans.get(0).getClassification());

    }

}