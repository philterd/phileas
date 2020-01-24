package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.UrlFilterStrategy;
import com.mtnfog.phileas.services.anonymization.UrlAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.UrlFilter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UrlFilterTest extends AbstractFilterTest {

    @Test
    public void filterUrl1() throws Exception {

        final List<UrlFilterStrategy> strategies = Arrays.asList(new UrlFilterStrategy());
        UrlFilter filter = new UrlFilter(strategies, new UrlAnonymizationService(new LocalAnonymizationCacheService()), true, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the page is http://page.com.");
        showSpans(spans);
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 12, 27, FilterType.URL));

    }

    @Test
    public void filterUrl2() throws Exception {

        final List<UrlFilterStrategy> strategies = Arrays.asList(new UrlFilterStrategy());
        UrlFilter filter = new UrlFilter(strategies, new UrlAnonymizationService(new LocalAnonymizationCacheService()), true, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the page is myhomepage.com.");
        showSpans(spans);
        Assert.assertEquals(0, spans.size());

    }

    @Test
    public void filterUrl3() throws Exception {

        final List<UrlFilterStrategy> strategies = Arrays.asList(new UrlFilterStrategy());
        UrlFilter filter = new UrlFilter(strategies, new UrlAnonymizationService(new LocalAnonymizationCacheService()), true, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the page is http://myhomepage.com/folder/page.html.");
        showSpans(spans);
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 12, 51, FilterType.URL));

    }

    @Test
    public void filterUrl4() throws Exception {

        final List<UrlFilterStrategy> strategies = Arrays.asList(new UrlFilterStrategy());
        UrlFilter filter = new UrlFilter(strategies, new UrlAnonymizationService(new LocalAnonymizationCacheService()), true, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the page is http://www.myhomepage.com/folder/page.html");
        showSpans(spans);
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 12, 54, FilterType.URL));

    }

    @Test
    public void filterUrl5() throws Exception {

        final List<UrlFilterStrategy> strategies = Arrays.asList(new UrlFilterStrategy());
        UrlFilter filter = new UrlFilter(strategies, new UrlAnonymizationService(new LocalAnonymizationCacheService()), true, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the page is www.myhomepage.com/folder/page.html.");
        showSpans(spans);
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 12, 48, FilterType.URL));

    }

    @Test
    public void filterUrl6() throws Exception {

        final List<UrlFilterStrategy> strategies = Arrays.asList(new UrlFilterStrategy());
        UrlFilter filter = new UrlFilter(strategies, new UrlAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the page is myhomepage.com.");
        showSpans(spans);
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 12, 26, FilterType.URL));

    }

    @Test
    public void filterUrl7() throws Exception {

        final List<UrlFilterStrategy> strategies = Arrays.asList(new UrlFilterStrategy());
        UrlFilter filter = new UrlFilter(strategies, new UrlAnonymizationService(new LocalAnonymizationCacheService()), true, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the page is www.myhomepage.com:80/folder/page.html.");
        showSpans(spans);
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 12, 51, FilterType.URL));

    }

    @Test
    public void filterUrl8() throws Exception {

        final List<UrlFilterStrategy> strategies = Arrays.asList(new UrlFilterStrategy());
        UrlFilter filter = new UrlFilter(strategies, new UrlAnonymizationService(new LocalAnonymizationCacheService()), true, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the page is http://192.168.1.1:80/folder/page.html.");
        showSpans(spans);
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 12, 51, FilterType.URL));

    }

    @Test
    public void filterUrl9() throws Exception {

        final List<UrlFilterStrategy> strategies = Arrays.asList(new UrlFilterStrategy());
        UrlFilter filter = new UrlFilter(strategies, new UrlAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the page is 192.168.1.1:80/folder/page.html.");
        showSpans(spans);
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 12, 44, FilterType.URL));

    }

    @Test
    public void filterUrl10() throws Exception {

        final List<UrlFilterStrategy> strategies = Arrays.asList(new UrlFilterStrategy());
        UrlFilter filter = new UrlFilter(strategies, new UrlAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the page is http://192.168.1.1:80/folder/page.html.");
        showSpans(spans);
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 12, 51, FilterType.URL));

    }

    @Test
    public void filterUrl11() throws Exception {

        final List<UrlFilterStrategy> strategies = Arrays.asList(new UrlFilterStrategy());
        UrlFilter filter = new UrlFilter(strategies, new UrlAnonymizationService(new LocalAnonymizationCacheService()), false, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the page is https://192.168.1.1:80/folder/page.html.");
        showSpans(spans);
        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 12, 52, FilterType.URL));

    }

    @Test
    public void filterUrl12() throws Exception {

        final List<UrlFilterStrategy> strategies = Arrays.asList(new UrlFilterStrategy());
        UrlFilter filter = new UrlFilter(strategies, new UrlAnonymizationService(new LocalAnonymizationCacheService()), true, Collections.emptySet());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "the page is test.ok new sentence");
        showSpans(spans);
        Assert.assertEquals(0, spans.size());

    }

}
