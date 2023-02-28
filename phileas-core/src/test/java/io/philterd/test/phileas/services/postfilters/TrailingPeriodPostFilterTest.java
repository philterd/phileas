package io.philterd.test.phileas.services.postfilters;

import io.philterd.phileas.model.enums.FilterType;
import io.philterd.phileas.model.objects.Span;
import io.philterd.phileas.services.postfilters.TrailingPeriodPostFilter;
import io.philterd.test.phileas.services.filters.AbstractFilterTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

public class TrailingPeriodPostFilterTest extends AbstractFilterTest {

    @Test
    public void test1() {

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.URL, "context", "docid", 0.80, "link.test.", "*****",  "", false, new String[0]));

        final TrailingPeriodPostFilter postFilter = TrailingPeriodPostFilter.getInstance();
        final List<Span> filteredSpans = postFilter.filter("doesn't matter", spans);

        showSpans(filteredSpans);
        Assertions.assertEquals(1, filteredSpans.size());
        Assertions.assertEquals("link.test", filteredSpans.get(0).getText());
        Assertions.assertEquals(21, filteredSpans.get(0).getCharacterEnd());

    }

    @Test
    public void test2() {

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.URL, "context", "docid", 0.80, "link.test....", "*****",  "", false, new String[0]));

        final TrailingPeriodPostFilter postFilter = TrailingPeriodPostFilter.getInstance();
        final List<Span> filteredSpans = postFilter.filter("doesn't matter", spans);

        showSpans(filteredSpans);
        Assertions.assertEquals(1, filteredSpans.size());
        Assertions.assertEquals("link.test", filteredSpans.get(0).getText());
        Assertions.assertEquals(18, filteredSpans.get(0).getCharacterEnd());

    }

    @Test
    public void test3() {

        // A street address can end with a period.

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.STREET_ADDRESS, "context", "docid", 0.80, "4 Devonshire Ct.", "*****",  "", false, new String[0]));

        final TrailingPeriodPostFilter postFilter = TrailingPeriodPostFilter.getInstance();
        final List<Span> filteredSpans = postFilter.filter("doesn't matter", spans);

        showSpans(filteredSpans);
        Assertions.assertEquals(1, filteredSpans.size());
        Assertions.assertEquals("4 Devonshire Ct.", filteredSpans.get(0).getText());
        Assertions.assertEquals(22, filteredSpans.get(0).getCharacterEnd());

    }

}
