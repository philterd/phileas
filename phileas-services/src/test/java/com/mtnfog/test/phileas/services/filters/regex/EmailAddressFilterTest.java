package com.mtnfog.test.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.services.anonymization.EmailAddressAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.EmailAddressFilter;
import com.mtnfog.test.phileas.services.filters.AbstractFilterTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class EmailAddressFilterTest extends AbstractFilterTest {

    @Test
    public void filterEmail() throws Exception {

        EmailAddressFilter filter = new EmailAddressFilter(new EmailAddressAnonymizationService(new LocalAnonymizationCacheService()));

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","my email is none@none.com.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 12, 25, FilterType.EMAIL_ADDRESS));

    }

}
