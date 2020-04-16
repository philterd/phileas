package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.SectionFilterStrategy;
import com.mtnfog.phileas.services.anonymization.AlphanumericAnonymizationService;
import com.mtnfog.phileas.services.cache.anonymization.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.SectionFilter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SectionFilterTest extends AbstractFilterTest {

    @Test
    public void filterSection1() throws Exception {

        final String startPattern = "BEGIN-REDACT";
        final String endPattern = "END-REDACT";

        final List<SectionFilterStrategy> strategies = Arrays.asList(new SectionFilterStrategy());
        final SectionFilter filter = new SectionFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), startPattern, endPattern, Collections.emptySet(), new Crypto());

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "This is some test. BEGIN-REDACT This text should be redacted. END-REDACT This is outside the text.");

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 19, 72, FilterType.SECTION));
        Assert.assertEquals("BEGIN-REDACT This text should be redacted. END-REDACT", spans.get(0).getText());

    }

    @Test
    public void filterSection2() throws Exception {

        final String startPattern = "BEGIN-REDACT";
        final String endPattern = "END-REDACT";

        final List<SectionFilterStrategy> strategies = Arrays.asList(new SectionFilterStrategy());
        final SectionFilter filter = new SectionFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), startPattern, endPattern, Collections.emptySet(), new Crypto());

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "This is some test. BEGIN-REDACT This text should be redacted. This is outside the text.");

        Assert.assertEquals(0, spans.size());

    }

    @Test
    public void filterSection3() throws Exception {

        final String startPattern = "BEGIN-REDACT";
        final String endPattern = "END-REDACT";

        final List<SectionFilterStrategy> strategies = Arrays.asList(new SectionFilterStrategy());
        final SectionFilter filter = new SectionFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), startPattern, endPattern, Collections.emptySet(), new Crypto());

        final List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "BEGIN-REDACT This text should be redacted. END-REDACT This is outside the text.");

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 0, 53, FilterType.SECTION));
        Assert.assertEquals("BEGIN-REDACT This text should be redacted. END-REDACT", spans.get(0).getText());

    }

}
