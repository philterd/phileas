package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.DriversLicenseAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.DriversLicenseFilter;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

public class DriversLicenseFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    private Filter getFilter() {
        return new DriversLicenseFilter(null, new DriversLicenseAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);
    }

    @Test
    public void filter1() throws Exception {

        final Filter filter = getFilter();

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the number is 123456789.");

        showSpans(spans);

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 14, 23, FilterType.DRIVERS_LICENSE_NUMBER));
        Assert.assertEquals("{{{REDACTED-drivers-license-number}}}", spans.get(0).getReplacement());
        Assert.assertEquals("123456789", spans.get(0).getText());

    }

}