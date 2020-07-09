package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.AgeAnonymizationService;
import com.mtnfog.phileas.services.anonymization.BitcoinAddressAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.AgeFilter;
import com.mtnfog.phileas.services.filters.regex.BitcoinAddressFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

public class BitcoinAddressFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    private Filter getFilter() {
        return new BitcoinAddressFilter(null, new BitcoinAddressAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), new Crypto(), windowSize);
    }

    @Test
    public void filter1() throws Exception {

        final Filter filter = getFilter();

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", 0, "the address is 127NVqnjf8gB9BFAW2dnQeM6wqmy1gbGtv.");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 15, 49, FilterType.BITCOIN_ADDRESS));
        Assertions.assertEquals("{{{REDACTED-bitcoin-address}}}", spans.get(0).getReplacement());
        Assertions.assertEquals("127NVqnjf8gB9BFAW2dnQeM6wqmy1gbGtv", spans.get(0).getText());

    }

    @Test
    public void filter2() throws Exception {

        final Filter filter = getFilter();

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", 0, "the address is 12qnjf8FAW2dnQeM6wqmy1gbGtv.");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 15, 42, FilterType.BITCOIN_ADDRESS));
        Assertions.assertEquals("{{{REDACTED-bitcoin-address}}}", spans.get(0).getReplacement());
        Assertions.assertEquals("12qnjf8FAW2dnQeM6wqmy1gbGtv", spans.get(0).getText());

    }

    @Test
    public void filter3() throws Exception {

        final Filter filter = getFilter();

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", 0, "the address is 126wqmy1gbGtv.");

        showSpans(spans);

        Assertions.assertEquals(0, spans.size());

    }

}