package io.philterd.test.phileas.services.postfilters;

import io.philterd.phileas.model.enums.FilterType;
import io.philterd.phileas.model.objects.Span;
import io.philterd.phileas.services.postfilters.TrailingSpacePostFilter;
import io.philterd.test.phileas.services.filters.AbstractFilterTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

public class TrailingSpacePostFilterTest extends AbstractFilterTest {

    @Test
    public void test1() {

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.URL, "context", "docid", 0.80, "Washington ", "*****",  "", false, new String[0]));

        final TrailingSpacePostFilter postFilter = TrailingSpacePostFilter.getInstance();
        final List<Span> filteredSpans = postFilter.filter("doesn't matter", spans);
        showSpans(filteredSpans);

        Assertions.assertEquals(1, filteredSpans.size());
        Assertions.assertEquals("Washington", filteredSpans.get(0).getText());
        Assertions.assertEquals(21, filteredSpans.get(0).getCharacterEnd());

    }

    @Test
    public void test2() {

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.URL, "context", "docid", 0.80, "Washington   ", "*****",  "", false, new String[0]));

        final TrailingSpacePostFilter postFilter = TrailingSpacePostFilter.getInstance();
        final List<Span> filteredSpans = postFilter.filter("doesn't matter", spans);
        showSpans(filteredSpans);

        Assertions.assertEquals(1, filteredSpans.size());
        Assertions.assertEquals("Washington", filteredSpans.get(0).getText());
        Assertions.assertEquals(19, filteredSpans.get(0).getCharacterEnd());

    }

}
