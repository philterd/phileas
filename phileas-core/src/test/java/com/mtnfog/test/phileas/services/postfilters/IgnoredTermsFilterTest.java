package com.mtnfog.test.phileas.services.postfilters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.Ignored;
import com.mtnfog.phileas.services.postfilters.IgnoredTermsFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class IgnoredTermsFilterTest {

    @Test
    public void ignored() {

        final Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("Washington", "California", "Virginia"));

        final FilterProfile filterProfile = new FilterProfile();
        filterProfile.setIgnored(Arrays.asList(ignored));

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.LOCATION_STATE, "context", "docid", 0.80, "test", "*****", false, new String[0]));

        final IgnoredTermsFilter ignoredTermsFilter = new IgnoredTermsFilter(ignored);
        final List<Span> filteredSpans = ignoredTermsFilter.filter("He lived in Washington.", spans);

        Assertions.assertEquals(0, filteredSpans.size());

    }

    @Test
    public void notIgnored() {

        final Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("Seattle", "California", "Virginia"));

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.LOCATION_STATE, "context", "docid", 0.80, "test", "*****", false, new String[0]));

        final IgnoredTermsFilter ignoredTermsFilter = new IgnoredTermsFilter(ignored);
        final List<Span> filteredSpans = ignoredTermsFilter.filter("He lived in Washington.", spans);

        Assertions.assertEquals(1, filteredSpans.size());

    }

    @Test
    public void caseSensitive1Test() {

        final Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("washington", "California", "Virginia"));

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.LOCATION_STATE, "context", "docid", 0.80, "test", "*****", false, new String[0]));

        final IgnoredTermsFilter ignoredTermsFilter = new IgnoredTermsFilter(ignored);
        final List<Span> filteredSpans = ignoredTermsFilter.filter("He lived in Washington.", spans);

        Assertions.assertEquals(0, filteredSpans.size());

    }

    @Test
    public void caseSensitive2Test() {

        final Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("Washington", "California", "Virginia"));

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.LOCATION_STATE, "context", "docid", 0.80, "test", "*****", false, new String[0]));

        final IgnoredTermsFilter ignoredTermsFilter = new IgnoredTermsFilter(ignored);
        final List<Span> filteredSpans = ignoredTermsFilter.filter("He lived in Washington.", spans);

        Assertions.assertEquals(0, filteredSpans.size());

    }

}
