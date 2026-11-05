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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.filters.rules.dictionary.SetDictionaryFilter;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.services.strategies.custom.CustomDictionaryFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class SetDictionaryFilterTest extends AbstractFilterTest {

    @Test
    public void filterDictionaryExactMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> names = new HashSet<>(Arrays.asList("george", "ted", "Bill", "john"));
        final SetDictionaryFilter filter = new SetDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, names, "none");

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "He lived with Bill in California.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 14, 18, FilterType.CUSTOM_DICTIONARY));
        Assertions.assertEquals("Bill", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDictionaryCaseInsensitiveMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> names = new HashSet<>(Arrays.asList("george", "ted", "bill", "john"));
        final SetDictionaryFilter filter = new SetDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, names, "none");

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "He lived with Bill in California.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 14, 18, FilterType.CUSTOM_DICTIONARY));
        Assertions.assertEquals("Bill", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDictionaryNoMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> names = new HashSet<>(Arrays.asList("george", "ted", "bill", "john"));
        final SetDictionaryFilter filter = new SetDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, names, "none");

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "He lived with Sam in California.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterDictionaryPhraseMatch1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> names = new HashSet<>(Arrays.asList("george jones", "ted", "bill", "john"));
        final SetDictionaryFilter filter = new SetDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, names, "none");

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "He lived with george jones in California.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 14, 26, FilterType.CUSTOM_DICTIONARY));
        Assertions.assertEquals("george jones", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDictionaryPhraseMatch2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> names = new HashSet<>(Arrays.asList("george jones jr", "ted", "bill smith", "john"));
        final SetDictionaryFilter filter = new SetDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, names, "none");

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Bill Smith lived with george jones jr in California.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(2, filtered.getSpans().size());

        for(final Span span : filtered.getSpans()) {
            Assertions.assertTrue(span.getText().equals("george jones jr") || span.getText().equals("Bill Smith"));
            Assertions.assertTrue(span.getCharacterStart() == 0 || span.getCharacterStart() == 22);
            Assertions.assertTrue(span.getCharacterEnd() == 10 || span.getCharacterEnd() == 37);
        }

    }

    @Test
    public void filterDictionaryPhraseMatchMultipleMatches() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> names = new HashSet<>(Arrays.asList("george jones", "ted", "bill", "john"));
        final SetDictionaryFilter filter = new SetDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, names, "none");

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "He lived with george jones and george jones in California.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(2, filtered.getSpans().size());

        for(final Span span : filtered.getSpans()) {
            Assertions.assertEquals("george jones", span.getText());
            Assertions.assertTrue(span.getCharacterStart() == 31 || span.getCharacterStart() == 14);
            Assertions.assertTrue(span.getCharacterEnd() == 43 || span.getCharacterEnd() == 26);
        }

    }

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("candidate1", "candidate2");

        final CustomDictionaryFilterStrategy customDictionaryFilterStrategy = new CustomDictionaryFilterStrategy();
        customDictionaryFilterStrategy.setStrategy(RANDOM_REPLACE);
        customDictionaryFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(customDictionaryFilterStrategy))
                .withWindowSize(windowSize)
                .build();

        final Set<String> names = new HashSet<>(Arrays.asList("george", "ted", "Bill", "john"));
        final SetDictionaryFilter filter = new SetDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, names, "none");

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "He lived with Bill in California.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

    @Test
    public void filterFromBundledDictionaryFile() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        // Uses the file-loading constructor, which reads terms from the bundled cities.txt dictionary.
        final SetDictionaryFilter filter = new SetDictionaryFilter(FilterType.LOCATION_CITY, filterConfiguration);

        // The term is adjacent to a trailing period; the whitespace tokenizer produces the token
        // "Boston.", which must still match the city "Boston" (and the span must cover only "Boston").
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "He visited Boston.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 17, FilterType.LOCATION_CITY));
        Assertions.assertEquals("Boston", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterDictionaryWithSurroundingPunctuation() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> names = new HashSet<>(Arrays.asList("george", "ted", "bill", "john"));
        final SetDictionaryFilter filter = new SetDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, names, "none");

        // Terms appear next to a comma and a period; both should still match, and each span should
        // cover only the term itself, not the punctuation.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "He knew Bill, Ted, and John.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(3, filtered.getSpans().size());

        for(final Span span : filtered.getSpans()) {
            Assertions.assertTrue(span.getText().equals("Bill") || span.getText().equals("Ted") || span.getText().equals("John"));
        }

    }

}
