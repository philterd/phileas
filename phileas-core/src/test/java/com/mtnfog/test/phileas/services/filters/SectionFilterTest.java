package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.SectionFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.AlphanumericAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.SectionFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SectionFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterSection1() throws Exception {

        final String startPattern = "BEGIN-REDACT";
        final String endPattern = "END-REDACT";

        final List<SectionFilterStrategy> strategies = Arrays.asList(new SectionFilterStrategy());
        final SectionFilter filter = new SectionFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, startPattern, endPattern, Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "This is some test. BEGIN-REDACT This text should be redacted. END-REDACT This is outside the text.");

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 19, 72, FilterType.SECTION));
        Assertions.assertEquals("BEGIN-REDACT This text should be redacted. END-REDACT", spans.get(0).getText());

    }

    @Test
    public void filterSection2() throws Exception {

        final String startPattern = "BEGIN-REDACT";
        final String endPattern = "END-REDACT";

        final List<SectionFilterStrategy> strategies = Arrays.asList(new SectionFilterStrategy());
        final SectionFilter filter = new SectionFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, startPattern, endPattern, Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "This is some test. BEGIN-REDACT This text should be redacted. This is outside the text.");

        Assertions.assertEquals(0, spans.size());

    }

    @Test
    public void filterSection3() throws Exception {

        final String startPattern = "BEGIN-REDACT";
        final String endPattern = "END-REDACT";

        final List<SectionFilterStrategy> strategies = Arrays.asList(new SectionFilterStrategy());
        final SectionFilter filter = new SectionFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, startPattern, endPattern, Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "BEGIN-REDACT This text should be redacted. END-REDACT This is outside the text.");

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 0, 53, FilterType.SECTION));
        Assertions.assertEquals("BEGIN-REDACT This text should be redacted. END-REDACT", spans.get(0).getText());

    }

}
