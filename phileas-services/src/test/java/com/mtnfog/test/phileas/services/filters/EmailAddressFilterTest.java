package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.EmailAddressFilterStrategy;
import com.mtnfog.phileas.services.anonymization.EmailAddressAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.EmailAddressFilter;
import com.mtnfog.test.phileas.services.filters.AbstractFilterTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EmailAddressFilterTest extends AbstractFilterTest {

    @Test
    public void filterEmail() throws Exception {

        final List<EmailAddressFilterStrategy> strategies = Arrays.asList(new EmailAddressFilterStrategy());
        EmailAddressFilter filter = new EmailAddressFilter(strategies, new EmailAddressAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid","my email is none@none.com.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 12, 25, FilterType.EMAIL_ADDRESS));

    }

}
