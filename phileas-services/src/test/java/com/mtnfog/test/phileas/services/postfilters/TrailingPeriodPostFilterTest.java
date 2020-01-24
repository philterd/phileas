package com.mtnfog.test.phileas.services.postfilters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.services.postfilters.TrailingPeriodPostFilter;
import com.mtnfog.test.phileas.services.filters.AbstractFilterTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

public class TrailingPeriodPostFilterTest extends AbstractFilterTest {

    @Test
    public void ignored() {

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.URL, "context", "docid", 0.80, "link.test.", "*****", false));

        final TrailingPeriodPostFilter postFilter = new TrailingPeriodPostFilter();
        final List<Span> filteredSpans = postFilter.filter("He lived in Washington.", spans);

        showSpans(filteredSpans);
        Assert.assertEquals(1, filteredSpans.size());
        Assert.assertEquals(filteredSpans.get(0).getText(), "link.test");

    }

}
