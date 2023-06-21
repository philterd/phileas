/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.test.phileas.services.postfilters;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.profile.FilterProfile;
import ai.philterd.phileas.model.profile.IgnoredPattern;
import ai.philterd.phileas.services.postfilters.IgnoredPatternsFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class IgnoredPatternsFilterTest {

    @Test
    public void ignored1() {

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

    @Test
    public void notIgnored1() {

        final IgnoredPattern ignoredPattern = new IgnoredPattern();
        ignoredPattern.setPattern("[A-Z0-9]{4}");
        ignoredPattern.setName("example-id");

        final FilterProfile filterProfile = new FilterProfile();
        filterProfile.setIgnoredPatterns(Arrays.asList(ignoredPattern));

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(6, 10, FilterType.IDENTIFIER, "context", "docid", 0.80, "Ab01", "*****",  "", false, new String[0]));

        final IgnoredPatternsFilter ignoredPatternsFilter = new IgnoredPatternsFilter(Arrays.asList(ignoredPattern));
        final List<Span> filteredSpans = ignoredPatternsFilter.filter("ID is Ab01.", spans);

        Assertions.assertEquals(1, filteredSpans.size());

    }

}
