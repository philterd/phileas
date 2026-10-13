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
import ai.philterd.phileas.services.strategies.custom.CustomDictionaryFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FuzzyDictionaryFilterTest extends AbstractFilterTest {

    @Test
    void filterExactMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> terms = new HashSet<>(Arrays.asList("George", "Ted", "Bill", "John"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.OFF, terms, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "He lived with Bill in California.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 14, 18, FilterType.CUSTOM_DICTIONARY));
        Assertions.assertEquals("Bill", filtered.getSpans().get(0).getText());

    }

    @Test
    void filterExactMatchLowercaseInput() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> terms = new HashSet<>(List.of("Thrackmoor"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.OFF, terms, false);

        final String input = "thrackmoor is lowercase";
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 10, FilterType.CUSTOM_DICTIONARY));

        // Text is the match, not the term's casing.
        Assertions.assertEquals("thrackmoor", filtered.getSpans().get(0).getText());

    }

    @Test
    void filterExactMatchUppercaseInput() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> terms = new HashSet<>(List.of("Bluebird"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.OFF, terms, true);

        final String input = "The BLUEBIRD sings.";
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 4, 12, FilterType.CUSTOM_DICTIONARY));
        Assertions.assertEquals("BLUEBIRD", filtered.getSpans().get(0).getText());

    }

    @Test
    void filterExactMatchTextLongerThanTerm() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        // "Jo Jones" is eight characters but matches the ten-character "Jo\r\n\tJones".
        final Set<String> terms = new HashSet<>(List.of("Jo Jones"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.OFF, terms, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Jo\r\n\tJones");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 10, FilterType.CUSTOM_DICTIONARY));
        Assertions.assertEquals("Jo\r\n\tJones", filtered.getSpans().get(0).getText());

    }

    @Test
    void filterExactMatchTermIsLiteral() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        // The period in the term matches only a period, not any character.
        final Set<String> terms = new HashSet<>(List.of("St. Lucie"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.OFF, terms, false);

        Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "StX Lucie").getSpans().size());
        Assertions.assertEquals(1, filter.filter(contextService, getPolicy(), "context", PIECE, "St. Lucie").getSpans().size());

    }

    @Test
    void filterFuzzyHigh() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        // SensitivityLevel.HIGH: distance < 1 (distance 0 - exact match only in fuzzy logic, but exact match handled first)
        // Actually distance < 1 means 0.
        final Set<String> terms = new HashSet<>(Arrays.asList("George", "Ted", "Bill", "John"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.HIGH, terms, false);

        // This is an exact match, should be found.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "He lived with Bill in California.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("Bill", filtered.getSpans().get(0).getText());

        // This is NOT an exact match (Billi vs Bill). distance = 1.
        // SensitivityLevel.HIGH requires distance < 1. So it should NOT match.
        final Filtered filtered2 = filter.filter(contextService, getPolicy(), "context", PIECE, "He lived with Billi in California.");
        Assertions.assertEquals(0, filtered2.getSpans().size());

    }

    @Test
    void filterExactMatchWithCapitalization() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> terms = new HashSet<>(Arrays.asList("George", "Ted", "Bill", "John"));

        // requireCapitalization = true
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.OFF, terms, true);

        // "Bill" is capitalized. Should be found.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "He lived with Bill in California.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("Bill", filtered.getSpans().get(0).getText());

        // "bill" is NOT capitalized. Should NOT be found.
        // Currently this fails because exact matches ignore requireCapitalization.
        final Filtered filtered2 = filter.filter(contextService, getPolicy(), "context", PIECE, "He lived with bill in California.");
        Assertions.assertEquals(0, filtered2.getSpans().size());

    }

    @Test
    void filterFuzzyMedium() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        // SensitivityLevel.MEDIUM: distance <= 2
        final Set<String> terms = new HashSet<>(Arrays.asList("George", "Ted", "Bill", "John"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.MEDIUM, terms, true);

        // "Billi" vs "Bill" is distance 1.
        // ngrams.get(0) contains "Billi" at some position.
        // entry "Bill" has 0 spaces, so spacesInEntry = 0.
        // loop over ngrams.get(0).
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "He lived with Billi in California.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("Billi", filtered.getSpans().get(0).getText());
        Assertions.assertEquals(0.7, filtered.getSpans().get(0).getConfidence());

    }

    @Test
    void filterFuzzyLow() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        // SensitivityLevel.LOW: distance < 3
        final Set<String> terms = new HashSet<>(Arrays.asList("George", "Ted", "Bill", "John"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.LOW, terms, false);

        // "Billie" vs "Bill" is distance 2.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "He lived with Billie in California.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("Billie", filtered.getSpans().get(0).getText());
        Assertions.assertEquals(0.5, filtered.getSpans().get(0).getConfidence());

    }

    @Test
    void filterRequireCapitalizationTrue() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> terms = new HashSet<>(Arrays.asList("George", "Ted", "Bill", "John"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.MEDIUM, terms, true);

        // "billi" vs "Bill" distance is 1 (if case insensitive), but it's not capitalized.
        // Wait, the dictionary pattern is CASE_INSENSITIVE.
        // Exact match is checked first using matcher.find() on the Pattern.
        // Fuzzy match checks Character.isUpperCase(ngram.charAt(0)) if requireCapitalization is true.

        // Not capitalized, should NOT match fuzzy.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "He lived with billi in California.");
        Assertions.assertEquals(0, filtered.getSpans().size());

        // Capitalized, SHOULD match fuzzy.
        final Filtered filtered2 = filter.filter(contextService, getPolicy(), "context", PIECE, "He lived with Billi in California.");
        Assertions.assertEquals(1, filtered2.getSpans().size());

    }

    @Test
    void filterRequireCapitalizationFalse() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> terms = new HashSet<>(Arrays.asList("George", "Ted", "Bill", "John"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.MEDIUM, terms, false);

        // Not capitalized, SHOULD match fuzzy because requireCapitalization is false.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "He lived with billi in California.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("billi", filtered.getSpans().get(0).getText());

    }

    @Test
    void filterPhraseExactMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> terms = new HashSet<>(Arrays.asList("George Jones", "Bill Smith"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.OFF, terms, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "He lived with Bill Smith in California.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("Bill Smith", filtered.getSpans().get(0).getText());

    }

    @Test
    void filterPhraseFuzzyMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> terms = new HashSet<>(Arrays.asList("George Jones", "Bill Smith"));
        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, SensitivityLevel.MEDIUM, terms, false);

        // "Bill Smeth" vs "Bill Smith" distance is 1.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "He lived with Bill Smeth in California.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("Bill Smeth", filtered.getSpans().get(0).getText());

    }

    private FuzzyDictionaryFilter springfield(final SensitivityLevel sensitivityLevel, final boolean requireCapitalization) {
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();
        return new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, sensitivityLevel,
                Set.of("Springfield"), requireCapitalization);
    }

    /** Each span's text and confidence, in the order of the text. */
    private List<String> found(final Filtered filtered) {
        return filtered.getSpans().stream()
                .sorted(Comparator.comparingInt(span -> span.getCharacterStart()))
                .map(span -> span.getText() + "@" + span.getConfidence())
                .toList();
    }

    @Test
    void everyExactOccurrenceIsReportedOnceAtEachLevel() throws Exception {
        for (final SensitivityLevel level : List.of(SensitivityLevel.OFF, SensitivityLevel.HIGH, SensitivityLevel.MEDIUM, SensitivityLevel.LOW)) {
            final Filtered filtered = springfield(level, false).filter(contextService, getPolicy(), "context", PIECE,
                    "Springfield and Springfield and Springfield today");
            Assertions.assertEquals(List.of("Springfield@1.0", "Springfield@1.0", "Springfield@1.0"), found(filtered),
                    "every occurrence, each once, at " + level);
        }
    }

    @Test
    void everyMisspellingIsReportedWithinTheLevelsDistance() throws Exception {
        final String text = "Springfeld and Sprngfeld today";
        Assertions.assertEquals(List.of(), found(springfield(SensitivityLevel.OFF, false).filter(contextService, getPolicy(), "context", PIECE, text)));
        Assertions.assertEquals(List.of(), found(springfield(SensitivityLevel.HIGH, false).filter(contextService, getPolicy(), "context", PIECE, text)));
        Assertions.assertEquals(List.of("Springfeld@0.7"),
                found(springfield(SensitivityLevel.MEDIUM, false).filter(contextService, getPolicy(), "context", PIECE, text)),
                "one edit at medium; two edits is too far");
        Assertions.assertEquals(List.of("Springfeld@0.5", "Sprngfeld@0.5"),
                found(springfield(SensitivityLevel.LOW, false).filter(contextService, getPolicy(), "context", PIECE, text)));
        Assertions.assertEquals(List.of("Springfeld@0.7", "Springfeld@0.7"),
                found(springfield(SensitivityLevel.MEDIUM, false).filter(contextService, getPolicy(), "context", PIECE, "Springfeld and Springfeld today")),
                "a repeated misspelling is reported each time");
    }

    @Test
    void aMisspellingIsReportedAlongsideAnExactOccurrence() throws Exception {
        final String text = "Springfield and Springfeld today";
        Assertions.assertEquals(List.of("Springfield@1.0", "Springfeld@0.7"),
                found(springfield(SensitivityLevel.MEDIUM, false).filter(contextService, getPolicy(), "context", PIECE, text)));
        Assertions.assertEquals(List.of("Springfield@1.0", "Springfeld@0.5"),
                found(springfield(SensitivityLevel.LOW, false).filter(contextService, getPolicy(), "context", PIECE, text)));
        Assertions.assertEquals(List.of("Springfield@1.0"),
                found(springfield(SensitivityLevel.HIGH, false).filter(contextService, getPolicy(), "context", PIECE, text)));
        Assertions.assertEquals(List.of("Springfield@1.0"),
                found(springfield(SensitivityLevel.OFF, false).filter(contextService, getPolicy(), "context", PIECE, text)));
    }

    @Test
    void capitalizationIsCheckedForEachExactOccurrence() throws Exception {
        final String text = "Springfield and springfield and Springfield today";
        for (final SensitivityLevel level : List.of(SensitivityLevel.OFF, SensitivityLevel.MEDIUM)) {
            Assertions.assertEquals(List.of("Springfield@1.0", "Springfield@1.0"),
                    found(springfield(level, true).filter(contextService, getPolicy(), "context", PIECE, text)),
                    "the lowercase occurrence is not reported, exactly or as a fuzzy match, at " + level);
        }
        Assertions.assertEquals(List.of("Springfield@1.0", "springfield@1.0", "Springfield@1.0"),
                found(springfield(SensitivityLevel.MEDIUM, false).filter(contextService, getPolicy(), "context", PIECE, text)));
    }

    private FuzzyDictionaryFilter dictionary(final SensitivityLevel sensitivityLevel, final String... terms) {
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withWindowSize(windowSize)
                .build();
        return new FuzzyDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, sensitivityLevel, Set.of(terms), false);
    }

    /** Each span's position and text, in the order of the text. */
    private List<String> located(final Filtered filtered) {
        return filtered.getSpans().stream()
                .sorted(Comparator.comparingInt(span -> span.getCharacterStart()))
                .map(span -> span.getCharacterStart() + "-" + span.getCharacterEnd() + " " + span.getText())
                .toList();
    }

    @Test
    void aMisspellingNextToPunctuationIsReportedWithoutIt() throws Exception {
        final FuzzyDictionaryFilter filter = springfield(SensitivityLevel.MEDIUM, false);
        Assertions.assertEquals(List.of("5-15 Springfeld"),
                located(filter.filter(contextService, getPolicy(), "context", PIECE, "From Springfeld.")));
        Assertions.assertEquals(List.of("3-13 Springfeld"),
                located(filter.filter(contextService, getPolicy(), "context", PIECE, "In Springfeld, she")));
        Assertions.assertEquals(List.of("1-11 Springfeld"),
                located(filter.filter(contextService, getPolicy(), "context", PIECE, "(Springfeld) today")));
    }

    @Test
    void aMultiWordMisspellingNextToPunctuationIsReportedWithoutIt() throws Exception {
        Assertions.assertEquals(List.of("9-20 Port Angels"),
                located(dictionary(SensitivityLevel.MEDIUM, "Port Angeles").filter(contextService, getPolicy(), "context", PIECE,
                        "She saw (Port Angels) today")));
    }

    @Test
    void aTermEndingInPunctuationKeepsItInTheComparison() throws Exception {
        Assertions.assertEquals(List.of("8-22 Washingtn D.C."),
                located(dictionary(SensitivityLevel.MEDIUM, "Washington D.C.").filter(contextService, getPolicy(), "context", PIECE,
                        "Born in Washingtn D.C. in May")));
    }

    @Test
    void punctuationAndShortWordsAreNotCompared() throws Exception {
        // "(Te)" is two edits from "Ted", within low's distance, but trimmed it is too short to compare.
        Assertions.assertEquals(List.of(),
                located(dictionary(SensitivityLevel.LOW, "Ted").filter(contextService, getPolicy(), "context", PIECE,
                        "Ask (Te) or --- now ...")));
    }

    @Test
    void autoAllowsMoreEditsForLongerWords() {
        Assertions.assertEquals(0, FuzzyDictionaryFilter.autoEditsAllowed(3));
        Assertions.assertEquals(0, FuzzyDictionaryFilter.autoEditsAllowed(4));
        Assertions.assertEquals(1, FuzzyDictionaryFilter.autoEditsAllowed(5));
        Assertions.assertEquals(1, FuzzyDictionaryFilter.autoEditsAllowed(8));
        Assertions.assertEquals(2, FuzzyDictionaryFilter.autoEditsAllowed(9));
        Assertions.assertEquals(2, FuzzyDictionaryFilter.autoEditsAllowed(20));
    }

    @Test
    void aSwapOfNeighboringLettersIsOneEdit() {
        Assertions.assertEquals(1, FuzzyDictionaryFilter.optimalStringAlignmentDistance("smith", "smiht"));
        Assertions.assertEquals(0, FuzzyDictionaryFilter.optimalStringAlignmentDistance("smith", "smith"));
        Assertions.assertEquals(1, FuzzyDictionaryFilter.optimalStringAlignmentDistance("springfield", "springfeld"));
        Assertions.assertEquals(2, FuzzyDictionaryFilter.optimalStringAlignmentDistance("springfield", "sprngfeld"));
        Assertions.assertEquals(3, FuzzyDictionaryFilter.optimalStringAlignmentDistance("", "abc"));
    }

    @Test
    void autoMatchesByTheComparedWordsLength() throws Exception {
        // Under five letters, only an exact match: "Jonn" is one edit from "John".
        Assertions.assertEquals(List.of(), located(dictionary(SensitivityLevel.AUTO, "John").filter(contextService, getPolicy(), "context", PIECE,
                "Ask Jonn today")));
        // Five letters, one edit, and a swap counts as one: "Smiht" for "Smith".
        Assertions.assertEquals(List.of("4-9 Smiht"), located(dictionary(SensitivityLevel.AUTO, "Smith").filter(contextService, getPolicy(), "context", PIECE,
                "Ask Smiht today")));
        // Eight letters allow one edit, not two: "Kathrne" for "Katherine" is two edits at seven letters.
        Assertions.assertEquals(List.of(), located(dictionary(SensitivityLevel.AUTO, "Katherine").filter(contextService, getPolicy(), "context", PIECE,
                "Ask Kathrne today")));
        Assertions.assertEquals(List.of("4-12 Katherin"), located(dictionary(SensitivityLevel.AUTO, "Katherine").filter(contextService, getPolicy(), "context", PIECE,
                "Ask Katherin today")));
        // Nine letters or more allow two edits.
        Assertions.assertEquals(List.of("4-13 Sprngfeld"), located(springfield(SensitivityLevel.AUTO, false).filter(contextService, getPolicy(), "context", PIECE,
                "Ask Sprngfeld today")));
    }

    @Test
    void autoGivesAFuzzyMatchTheConfidenceOfItsDistance() throws Exception {
        // As the fixed levels do: 0.7 for one edit, 0.5 for two. An exact match is 1.0.
        Assertions.assertEquals(List.of("Springfield@1.0", "Springfeld@0.7", "Sprngfeld@0.5"),
                found(springfield(SensitivityLevel.AUTO, false).filter(contextService, getPolicy(), "context", PIECE,
                        "Springfield and Springfeld and Sprngfeld today")));
    }

    @Test
    void autoReportsAFuzzyMatchOnlyWhenItIsCapitalized() throws Exception {
        Assertions.assertEquals(List.of("Springfeld@0.7"),
                found(springfield(SensitivityLevel.AUTO, false).filter(contextService, getPolicy(), "context", PIECE, "From Springfeld and springfeld")));
        Assertions.assertEquals(List.of("springfield@1.0"),
                found(springfield(SensitivityLevel.AUTO, false).filter(contextService, getPolicy(), "context", PIECE, "From springfield today")),
                "an exact match is reported in any case unless capitalized is set");
    }

}
