package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.AlphanumericAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.DriversLicenseFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

public class DriversLicenseFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    private Filter getFilter() {
        return new DriversLicenseFilter(null, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);
    }

    @Test
    public void filter1() throws Exception {

        final Filter filter = getFilter();

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the number is 123456789.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 23, FilterType.DRIVERS_LICENSE_NUMBER));
        Assertions.assertEquals("{{{REDACTED-drivers-license-number}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("123456789", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("SOUTH CAROLINA", filterResult.getSpans().get(0).getClassification());

    }

}