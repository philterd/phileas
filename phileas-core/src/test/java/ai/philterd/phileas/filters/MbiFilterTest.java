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

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.services.anonymization.MbiAnonymizationService;
import ai.philterd.phileas.services.filters.regex.MbiFilter;
import ai.philterd.phileas.services.strategies.rules.MbiFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

// 1EG4-TE5-MK73 is the example MBI CMS publishes. Other values are derived from it.
public class MbiFilterTest extends AbstractFilterTest {

    private MbiFilter filter() {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new MbiFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        return new MbiFilter(filterConfiguration);

    }

    @Test
    public void filterMbiUnformatted() throws Exception {

        final Filtered filtered = filter().filter(contextService, getPolicy(), "context", PIECE, "the mbi is 1EG4TE5MK73.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 22, FilterType.MBI));

    }

    @Test
    public void filterMbiInTheCardGrouping() throws Exception {

        final Filtered filtered = filter().filter(contextService, getPolicy(), "context", PIECE, "the mbi is 1EG4-TE5-MK73.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 24, FilterType.MBI));
        Assertions.assertEquals("1EG4-TE5-MK73", filtered.getSpans().get(0).getText());

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "the mbi is 0EG4TE5MK73.",     // Begins with 0.
            "the mbi is 1SG4TE5MK73.",     // S is never used.
            "the mbi is 1EG4TE5MK7Z.",     // A letter where a digit belongs.
            "the mbi is 14G4TE5MK73.",     // A digit in the 2nd position, which is always a letter.
            "the mbi is 1EG4-TE5MK73.",    // Only one of the two hyphens.
            "the mbi is 1eg4te5mk73.",     // Lower case.
            "the mbi is 1EG4TE5MK734."     // Part of a longer token.
    })
    public void lookAlikesAreNotMatched(final String text) throws Exception {

        final Filtered filtered = filter().filter(contextService, getPolicy(), "context", PIECE, text);
        Assertions.assertEquals(0, filtered.getSpans().size(), text);

    }

    @Test
    public void randomReplacementIsAnMbiInTheTokensGrouping() {

        final MbiAnonymizationService service = new MbiAnonymizationService(random);

        for(int i = 0; i < 100; i++) {
            final String unformatted = service.anonymize("1EG4TE5MK73");
            Assertions.assertTrue(unformatted.matches("[0-9A-Z]{11}") && MbiFilter.isValid(unformatted), unformatted);
            final String grouped = service.anonymize("1EG4-TE5-MK73");
            Assertions.assertTrue(grouped.matches(".{4}-.{3}-.{4}") && MbiFilter.isValid(grouped), grouped);
        }

    }

}
