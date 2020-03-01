package com.mtnfog.test.phileas.services.postfilters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.services.postfilters.TrailingSpacePostFilter;
import com.mtnfog.test.phileas.services.filters.FilterTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

public class TrailingSpacePostFilterTest extends FilterTest {

    @Test
    public void test1() {

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.URL, "context", "docid", 0.80, "Washington ", "*****", false, new String[0]));

        final TrailingSpacePostFilter postFilter = new TrailingSpacePostFilter();
        final List<Span> filteredSpans = postFilter.filter("doesn't matter", spans);

        showSpans(filteredSpans);
        Assert.assertEquals(1, filteredSpans.size());
        Assert.assertEquals("Washington", filteredSpans.get(0).getText());

    }

}
