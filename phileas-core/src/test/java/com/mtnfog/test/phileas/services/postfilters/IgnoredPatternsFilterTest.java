package com.mtnfog.test.phileas.services.postfilters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.IgnoredPattern;
import com.mtnfog.phileas.services.postfilters.IgnoredPatternsFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class IgnoredPatternsFilterTest {

    @Test
    public void ignored() {

        final IgnoredPattern ignoredPattern = new IgnoredPattern();
        ignoredPattern.setPattern("[A-Z0-9]{4}");
        ignoredPattern.setName("example-id");

        final FilterProfile filterProfile = new FilterProfile();
        filterProfile.setIgnoredPatterns(Arrays.asList(ignoredPattern));

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(6, 10, FilterType.IDENTIFIER, "context", "docid", 0.80, "AB01", "*****",  "", false, new String[0]));

        final IgnoredPatternsFilter ignoredPatternsFilter = new IgnoredPatternsFilter(Arrays.asList(ignoredPattern));
        final List<Span> filteredSpans = ignoredPatternsFilter.filter("ID is AB01.", spans);

        Assertions.assertEquals(0, filteredSpans.size());

    }

}
