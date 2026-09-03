/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.services.filters.regex.SsnFilter;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/** The filter id is a log label: it must reach the filter and change no filtering. */
public class FilterIdTest extends AbstractFilterTest {

    private static final String TEXT = "the ssn is 123-45-6789.";

    private SsnFilter filterWithId(final String id) {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withWindowSize(windowSize)
                .withId(id)
                .build();

        return new SsnFilter(filterConfiguration);

    }

    @Test
    public void idIsAvailableOnTheFilter() {
        Assertions.assertEquals("intake-ssn", filterWithId("intake-ssn").getId());
    }

    @Test
    public void idIsNullWhenNotSet() {
        Assertions.assertNull(filterWithId(null).getId());
    }

    @Test
    public void idNamesTheFilterInDiagnostics() {

        // describe() is what the filter's log messages use.
        Assertions.assertEquals("ssn (id: intake-ssn)", filterWithId("intake-ssn").describe());
        Assertions.assertEquals("ssn", filterWithId(null).describe());
        Assertions.assertEquals("ssn", filterWithId("   ").describe());

    }

    @Test
    public void idDoesNotChangeFiltering() throws Exception {

        final Filtered withId = filterWithId("intake-ssn")
                .filter(contextService, getPolicy(), "context", PIECE, TEXT);

        final Filtered withoutId = filterWithId(null)
                .filter(contextService, getPolicy(), "context", PIECE, TEXT);

        Assertions.assertEquals(withoutId.getSpans().size(), withId.getSpans().size());
        Assertions.assertEquals(withoutId.getSpans().get(0).getCharacterStart(), withId.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(withoutId.getSpans().get(0).getCharacterEnd(), withId.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals(withoutId.getSpans().get(0).getFilterType(), withId.getSpans().get(0).getFilterType());

    }

}
