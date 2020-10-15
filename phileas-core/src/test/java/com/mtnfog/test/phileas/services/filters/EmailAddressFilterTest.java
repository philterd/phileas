package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.EmailAddressFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.EmailAddressAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.EmailAddressFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EmailAddressFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterEmail() throws Exception {

        final List<EmailAddressFilterStrategy> strategies = Arrays.asList(new EmailAddressFilterStrategy());
        EmailAddressFilter filter = new EmailAddressFilter(strategies, new EmailAddressAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "my email is none@none.com.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 12, 25, FilterType.EMAIL_ADDRESS));
        Assertions.assertEquals("none@none.com", filterResult.getSpans().get(0).getText());

    }

}
