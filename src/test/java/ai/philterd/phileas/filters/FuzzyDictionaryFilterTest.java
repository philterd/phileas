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

import ai.philterd.phileas.filters.rules.dictionary.FuzzyDictionaryFilter;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.SensitivityLevel;
import ai.philterd.phileas.services.anonymization.AlphanumericAnonymizationService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.strategies.custom.CustomDictionaryFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FuzzyDictionaryFilterTest extends AbstractFilterTest {

    @Test
    public void filterExactMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> terms = new HashSet<>(Arrays.asList("George", "Ted", "Bill", "John"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.OFF, terms, false);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "He lived with Bill in California.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 14, 18, FilterType.CUSTOM_DICTIONARY));
        Assertions.assertEquals("Bill", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterFuzzyHigh() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        // SensitivityLevel.HIGH: distance < 1 (distance 0 - exact match only in fuzzy logic, but exact match handled first)
        // Actually distance < 1 means 0.
        final Set<String> terms = new HashSet<>(Arrays.asList("George", "Ted", "Bill", "John"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.HIGH, terms, false);

        // This is an exact match, should be found.
        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "He lived with Bill in California.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("Bill", filtered.getSpans().get(0).getText());

        // This is NOT an exact match (Billi vs Bill). distance = 1.
        // SensitivityLevel.HIGH requires distance < 1. So it should NOT match.
        final Filtered filtered2 = filter.filter(getPolicy(), "context", PIECE, "He lived with Billi in California.");
        Assertions.assertEquals(0, filtered2.getSpans().size());

    }

    @Test
    public void filterExactMatchWithCapitalization() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> terms = new HashSet<>(Arrays.asList("George", "Ted", "Bill", "John"));

        // requireCapitalization = true
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.OFF, terms, true);

        // "Bill" is capitalized. Should be found.
        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "He lived with Bill in California.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("Bill", filtered.getSpans().get(0).getText());

        // "bill" is NOT capitalized. Should NOT be found.
        // Currently this fails because exact matches ignore requireCapitalization.
        final Filtered filtered2 = filter.filter(getPolicy(), "context", PIECE, "He lived with bill in California.");
        Assertions.assertEquals(0, filtered2.getSpans().size());

    }

    @Test
    public void filterFuzzyMedium() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        // SensitivityLevel.MEDIUM: distance <= 2
        final Set<String> terms = new HashSet<>(Arrays.asList("George", "Ted", "Bill", "John"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.MEDIUM, terms, true);

        // "Billi" vs "Bill" is distance 1.
        // ngrams.get(0) contains "Billi" at some position.
        // entry "Bill" has 0 spaces, so spacesInEntry = 0.
        // loop over ngrams.get(0).
        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "He lived with Billi in California.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("Bill", filtered.getSpans().get(0).getText());
        Assertions.assertEquals(0.7, filtered.getSpans().get(0).getConfidence());

    }

    @Test
    public void filterFuzzyLow() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        // SensitivityLevel.LOW: distance < 3
        final Set<String> terms = new HashSet<>(Arrays.asList("George", "Ted", "Bill", "John"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.LOW, terms, false);

        // "Billie" vs "Bill" is distance 2.
        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "He lived with Billie in California.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("Billie", filtered.getSpans().get(0).getText());
        Assertions.assertEquals(0.5, filtered.getSpans().get(0).getConfidence());

    }

    @Test
    public void filterRequireCapitalizationTrue() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> terms = new HashSet<>(Arrays.asList("George", "Ted", "Bill", "John"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.MEDIUM, terms, true);

        // "billi" vs "Bill" distance is 1 (if case insensitive), but it's not capitalized.
        // Wait, the dictionary pattern is CASE_INSENSITIVE.
        // Exact match is checked first using matcher.find() on the Pattern.
        // Fuzzy match checks Character.isUpperCase(ngram.charAt(0)) if requireCapitalization is true.

        // Not capitalized, should NOT match fuzzy.
        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "He lived with billi in California.");
        Assertions.assertEquals(0, filtered.getSpans().size());

        // Capitalized, SHOULD match fuzzy.
        final Filtered filtered2 = filter.filter(getPolicy(), "context", PIECE, "He lived with Billi in California.");
        Assertions.assertEquals(1, filtered2.getSpans().size());

    }

    @Test
    public void filterRequireCapitalizationFalse() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> terms = new HashSet<>(Arrays.asList("George", "Ted", "Bill", "John"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.MEDIUM, terms, false);

        // Not capitalized, SHOULD match fuzzy because requireCapitalization is false.
        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "He lived with billi in California.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("billi", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterPhraseExactMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> terms = new HashSet<>(Arrays.asList("George Jones", "Bill Smith"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.OFF, terms, false);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "He lived with Bill Smith in California.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("Bill Smith", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterPhraseFuzzyMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> terms = new HashSet<>(Arrays.asList("George Jones", "Bill Smith"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.MEDIUM, terms, false);

        // "Bill Smeth" vs "Bill Smith" distance is 1.
        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "He lived with Bill Smeth in California.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("Bill Smeth", filtered.getSpans().get(0).getText());

    }

}
