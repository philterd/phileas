package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.Identifier;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.IdentifierFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.AlphanumericAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.IdentifierFilter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IdentifierFilterTest extends AbstractFilterTest {

    private final AnonymizationService anonymizationService = new AlphanumericAnonymizationService(new LocalAnonymizationCacheService());

    @Test
    public void filterId1() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is AB4736021 in california.");

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,19, FilterType.IDENTIFIER));
        Assert.assertEquals("AB4736021", spans.get(0).getText());

    }

    @Test
    public void filterId2() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is AB473-6021 in california.");

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId3() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is 473-6AB021 in california.");

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId4() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is AB473-6021 in california.");

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId5() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is 473-6AB021 in california.");

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId6() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is 123-45-6789 in california.");

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,21, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId7() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived at 90210. Patient id 00076A and 93821A. He is on biotin. Diagnosed with A0100.");

        Assert.assertEquals(5, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 48, 59, FilterType.IDENTIFIER));
        Assert.assertTrue(checkSpan(spans.get(1), 76, 81, FilterType.IDENTIFIER));
        Assert.assertTrue(checkSpan(spans.get(2), 94, 100, FilterType.IDENTIFIER));
        Assert.assertTrue(checkSpan(spans.get(3), 105, 111, FilterType.IDENTIFIER));
        Assert.assertTrue(checkSpan(spans.get(4), 145, 150, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId8() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is 000-00-00-00 ABC123 in california.");

        Assert.assertEquals(2, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10, 22, FilterType.IDENTIFIER));
        Assert.assertTrue(checkSpan(spans.get(1), 23, 29, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId9() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is AZ12 ABC123/123ABC in california.");

        Assert.assertEquals(3, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,14, FilterType.IDENTIFIER));
        Assert.assertTrue(checkSpan(spans.get(1), 15, 21, FilterType.IDENTIFIER));
        Assert.assertTrue(checkSpan(spans.get(2), 22, 28, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId10() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is H3SNPUHYEE7JD3H in california.");

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,25, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId11() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is 86637729 in california.");

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,18, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId12() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is 33778376 in california.");

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,18, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId13() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", "\\b[A-Z]{4,}\\b", true, strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is ABCD.");

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,14, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId14() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the id is 1234.");

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 10,14, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId15() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, Collections.emptySet(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "John Smith, patient ID A203493, was seen on February 18.");

        showSpans(spans);
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 23,30, FilterType.IDENTIFIER));

    }

}
