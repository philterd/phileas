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
import ai.philterd.phileas.services.anonymization.DeaNumberAnonymizationService;
import ai.philterd.phileas.services.filters.regex.DeaNumberFilter;
import ai.philterd.phileas.services.strategies.rules.DeaNumberFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

// AB1234563 is the conventional example of a DEA number. Other numbers are derived from it.
public class DeaNumberFilterTest extends AbstractFilterTest {

    private DeaNumberFilter filter() {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new DeaNumberFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        return new DeaNumberFilter(filterConfiguration);

    }

    @Test
    public void filterDeaNumber() throws Exception {

        final Filtered filtered = filter().filter(contextService, getPolicy(), "context", PIECE, "the dea number is AB1234563.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 18, 27, FilterType.DEA_NUMBER));
        Assertions.assertEquals("AB1234563", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDeaNumberOfABusiness() throws Exception {

        // A registrant identified by a business name has 9 as the second character.
        final Filtered filtered = filter().filter(contextService, getPolicy(), "context", PIECE, "registration A91234563 on file");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("A91234563", filtered.getSpans().get(0).getText());

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "the dea number is AB1234564.",   // Wrong check digit.
            "the dea number is ab1234563.",   // Lower case.
            "the dea number is A11234563.",   // A digit other than 9 in the second position.
            "the dea number is AB123456.",    // Too few digits.
            "the dea number is XAB1234563."   // Part of a longer token.
    })
    public void lookAlikesAreNotMatched(final String text) throws Exception {

        final Filtered filtered = filter().filter(contextService, getPolicy(), "context", PIECE, text);
        Assertions.assertEquals(0, filtered.getSpans().size(), text);

    }

    @Test
    public void randomReplacementIsADeaNumber() {

        final DeaNumberAnonymizationService service = new DeaNumberAnonymizationService(random);

        for(int i = 0; i < 100; i++) {
            final String replacement = service.anonymize("AB1234563");
            Assertions.assertTrue(DeaNumberFilter.isValid(replacement), replacement);
        }

    }

}
