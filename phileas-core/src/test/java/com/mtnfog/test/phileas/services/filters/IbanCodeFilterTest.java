package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.AgeFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.AgeAnonymizationService;
import com.mtnfog.phileas.services.anonymization.IbanCodeAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.AgeFilter;
import com.mtnfog.phileas.services.filters.regex.IbanCodeFilter;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IbanCodeFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    private Filter getFilter(boolean validate) {
        return new IbanCodeFilter(null, new IbanCodeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), validate, windowSize);
    }

    @Test
    public void filter1() throws Exception {

        final Filter filter = getFilter(true);

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "bank code of GB33BUKB20201555555555 ok?");

        showSpans(spans);

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 13, 35, FilterType.IBAN_CODE));
        Assert.assertEquals("{{{REDACTED-iban-code}}}", spans.get(0).getReplacement());
        Assert.assertEquals("GB33BUKB20201555555555", spans.get(0).getText());

    }

    @Test
    public void filter2() throws Exception {

        final Filter filter = getFilter(false);

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "bank code of GB15MIDL40051512345678 ok?");

        showSpans(spans);

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 13, 35, FilterType.IBAN_CODE));
        Assert.assertEquals("{{{REDACTED-iban-code}}}", spans.get(0).getReplacement());
        Assert.assertEquals("GB15MIDL40051512345678", spans.get(0).getText());

    }

}