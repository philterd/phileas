package com.mtnfog.test.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.AlphanumericAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.IdentifierFilter;
import com.mtnfog.test.phileas.services.filters.AbstractFilterTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class IdentifierFilterTest extends AbstractFilterTest {

    private final AnonymizationService anonymizationService = new AlphanumericAnonymizationService(new LocalAnonymizationCacheService());

    @Test
    public void filterId1() throws Exception {

        IdentifierFilter filter = new IdentifierFilter(anonymizationService);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is AB4736021 in california.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,19, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId2() throws Exception {

        IdentifierFilter filter = new IdentifierFilter(anonymizationService);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is AB473-6021 in california.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId3() throws Exception {

        IdentifierFilter filter = new IdentifierFilter(anonymizationService);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is 473-6AB021 in california.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId4() throws Exception {

        IdentifierFilter filter = new IdentifierFilter(anonymizationService);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is AB473-6021 in california.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId5() throws Exception {

        IdentifierFilter filter = new IdentifierFilter(anonymizationService);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is 473-6AB021 in california.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId6() throws Exception {

        IdentifierFilter filter = new IdentifierFilter(anonymizationService);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is 123-45-6789abc in california.");
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,24, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId7() throws Exception {

        IdentifierFilter filter = new IdentifierFilter(anonymizationService);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived at 90210. Patient id 00076a and 93821a. He is on biotin. Diagnosed with A0100.");
        Assert.assertEquals(4, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 48, 59, FilterType.IDENTIFIER));
        Assert.assertTrue(checkSpan(spans.get(1), 94, 100, FilterType.IDENTIFIER));
        Assert.assertTrue(checkSpan(spans.get(2), 105, 111, FilterType.IDENTIFIER));
        Assert.assertTrue(checkSpan(spans.get(3), 145, 150, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId8() throws Exception {

        IdentifierFilter filter = new IdentifierFilter(anonymizationService);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is 000-00-00-00 Abc123 in california.");
        Assert.assertEquals(2, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10, 22, FilterType.IDENTIFIER));
        Assert.assertTrue(checkSpan(spans.get(1), 23, 29, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId9() throws Exception {

        IdentifierFilter filter = new IdentifierFilter(anonymizationService);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is AZ12 ABC123/123Abc in california.");
        Assert.assertEquals(3, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,14, FilterType.IDENTIFIER));
        Assert.assertTrue(checkSpan(spans.get(1), 15, 21, FilterType.IDENTIFIER));
        Assert.assertTrue(checkSpan(spans.get(2), 22, 28, FilterType.IDENTIFIER));

    }

}
