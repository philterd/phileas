package com.mtnfog.test.phileas.services.postfilters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.services.postfilters.TrailingPeriodPostFilter;
import com.mtnfog.test.phileas.services.filters.FilterTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

public class TrailingPeriodPostFilterTest extends FilterTest {

    @Test
    public void test1() {

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.URL, "context", "docid", 0.80, "link.test.", "*****", false, new String[0]));

        final TrailingPeriodPostFilter postFilter = new TrailingPeriodPostFilter();
        final List<Span> filteredSpans = postFilter.filter("doesn't matter", spans);

        showSpans(filteredSpans);
        Assert.assertEquals(1, filteredSpans.size());
        Assert.assertEquals("link.test", filteredSpans.get(0).getText());

    }

}
