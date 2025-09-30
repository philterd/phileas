/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services.postfilters;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.filters.AbstractFilterTest;
import ai.philterd.phileas.services.filters.postfilters.TrailingNewLinePostFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

public class TrailingNewLinePostFilterTest extends AbstractFilterTest {

    @Test
    public void test1() {

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.URL, "context", "docid", 0.80, "ends with" + System.lineSeparator(), "*****",  "", false, true, new String[0], 0));

        final TrailingNewLinePostFilter postFilter = TrailingNewLinePostFilter.getInstance();
        final List<Span> filteredSpans = postFilter.filter("doesn't matter", spans);

        showSpans(filteredSpans);
        Assertions.assertEquals(1, filteredSpans.size());
        Assertions.assertEquals("ends with", filteredSpans.get(0).getText());
        Assertions.assertEquals(22 - System.lineSeparator().length(), filteredSpans.get(0).getCharacterEnd());

    }

    @Test
    public void test2() {

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(12, 22, FilterType.URL, "context", "docid", 0.80, "ends with", "*****",  "", false, true, new String[0], 0));

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
        spans.add(Span.make(12, 22, FilterType.URL, "context", "docid", 0.80, "ends with" + System.lineSeparator() + System.lineSeparator(), "*****",  "", false, true, new String[0], 0));

        final TrailingNewLinePostFilter postFilter = TrailingNewLinePostFilter.getInstance();
        final List<Span> filteredSpans = postFilter.filter("doesn't matter", spans);

        showSpans(filteredSpans);
        Assertions.assertEquals(1, filteredSpans.size());
        Assertions.assertEquals("ends with", filteredSpans.get(0).getText());
        Assertions.assertEquals(22 - System.lineSeparator().length() * 2, filteredSpans.get(0).getCharacterEnd());

    }

}
