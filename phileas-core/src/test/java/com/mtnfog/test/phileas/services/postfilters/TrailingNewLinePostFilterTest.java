package com.mtnfog.test.phileas.services.postfilters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.services.postfilters.TrailingNewLinePostFilter;
import com.mtnfog.test.phileas.services.filters.AbstractFilterTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

public class TrailingNewLinePostFilterTest extends AbstractFilterTest {

    @Test
    public void test1() {

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.URL, "context", "docid", 0.80, "ends with\n", "*****",  "", false, new String[0]));

        final TrailingNewLinePostFilter postFilter = TrailingNewLinePostFilter.getInstance();
        final List<Span> filteredSpans = postFilter.filter("doesn't matter", spans);

        showSpans(filteredSpans);
        Assertions.assertEquals(1, filteredSpans.size());
        Assertions.assertEquals("ends with", filteredSpans.get(0).getText());
        Assertions.assertEquals(21, filteredSpans.get(0).getCharacterEnd());

    }

    @Test
    public void test2() {

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.URL, "context", "docid", 0.80, "ends with", "*****",  "", false, new String[0]));

        final TrailingNewLinePostFilter postFilter = TrailingNewLinePostFilter.getInstance();
        final List<Span> filteredSpans = postFilter.filter("doesn't matter", spans);

        showSpans(filteredSpans);
        Assertions.assertEquals(1, filteredSpans.size());
        Assertions.assertEquals("ends with", filteredSpans.get(0).getText());
        Assertions.assertEquals(22, filteredSpans.get(0).getCharacterEnd());

    }

    @Test
    public void test3() {

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.URL, "context", "docid", 0.80, "ends with\n\n", "*****",  "", false, new String[0]));

        final TrailingNewLinePostFilter postFilter = TrailingNewLinePostFilter.getInstance();
        final List<Span> filteredSpans = postFilter.filter("doesn't matter", spans);

        showSpans(filteredSpans);
        Assertions.assertEquals(1, filteredSpans.size());
        Assertions.assertEquals("ends with", filteredSpans.get(0).getText());
        Assertions.assertEquals(20, filteredSpans.get(0).getCharacterEnd());

    }

}
