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
package ai.philterd.phileas.services;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.rules.dictionary.SetDictionaryFilter;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Position;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Ignored;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

/**
 * Dictionary filters match a term next to a line break or tab as they do next to a space, and match
 * a multi-word term whose words are separated by any run of whitespace.
 */
class DictionaryWhitespaceTest {

    private static final String CUSTOM_TERM = "Zephyrous";

    private final VectorService vectorService = mock(VectorService.class);

    // Each filter, with a term from its bundled word list (or the policy, for a custom dictionary).
    static Stream<Arguments> filtersAndSeparators() {

        final Map<String, String> filters = Map.of(
                "firstName", "John",
                "surname", "Jones",
                "city", "Boston",
                "state", "California",
                "county", "Fayette",
                "hospital", "UCLA Medical Center",
                "dictionaries", CUSTOM_TERM);

        return filters.entrySet().stream().flatMap(filter ->
                Stream.of("\n", "\r\n", "\r", "\t", " ").flatMap(separator ->
                        Stream.of(false, true).map(fuzzy ->
                                Arguments.of(filter.getKey(), filter.getValue(), separator, fuzzy))));

    }

    @ParameterizedTest(name = "{0} {1} separator={2} fuzzy={3}")
    @MethodSource("filtersAndSeparators")
    void aTermNextToASeparatorIsRedacted(final String type, final String term, final String separator,
                                         final boolean fuzzy) throws Exception {

        // Numbers are in no word list and too far from any term to match fuzzily.
        final String input = "12" + separator + term + separator + "34";

        final TextFilterResult result = filter(policy(type, fuzzy, null), input);

        assertSpanCovers(result, input, term);

        // The separators are left in place.
        Assertions.assertTrue(result.getFilteredText().startsWith("12" + separator), result.getFilteredText());
        Assertions.assertTrue(result.getFilteredText().endsWith(separator + "34"), result.getFilteredText());

    }

    @Test
    void bothWordsOfANameSplitByALineBreakAreRedacted() throws Exception {

        final String input = "Hello team,\nGeorge\nWashington called.";

        final TextFilterResult result = filter(policy("firstName", false, null), input);

        assertSpanCovers(result, input, "George");
        assertSpanCovers(result, input, "Washington");
        Assertions.assertEquals("Hello team,\n{{{REDACTED-first-name}}}\n{{{REDACTED-first-name}}} called.",
                result.getFilteredText());

    }

    static Stream<Arguments> multiWordSeparators() {
        return Stream.of("\n", "\r\n", "\t", "   ", " \r\n ").flatMap(separator ->
                Stream.of(false, true).map(fuzzy -> Arguments.of(separator, fuzzy)));
    }

    @ParameterizedTest(name = "separator={0} fuzzy={1}")
    @MethodSource("multiWordSeparators")
    void aMultiWordTermSplitByWhitespaceIsOneSpan(final String separator, final boolean fuzzy) throws Exception {

        final String term = "UCLA Medical" + separator + "Center";
        final String input = "Admitted to " + term + " today.";

        final TextFilterResult result = filter(policy("hospital", fuzzy, null), input);

        assertSpanCovers(result, input, term);
        Assertions.assertFalse(result.getFilteredText().contains("Medical"), result.getFilteredText());

    }

    @ParameterizedTest(name = "fuzzy={0}")
    @ValueSource(booleans = {false, true})
    void anIgnoredTermWrittenWithSpacesIgnoresAMatchSplitByALineBreak(final boolean fuzzy) throws Exception {

        final String term = "UCLA Medical\r\nCenter";
        final String input = "Admitted to " + term + " today.";

        final TextFilterResult result = filter(policy("hospital", fuzzy, "\"ignored\": [\"UCLA Medical Center\"]"), input);

        Assertions.assertTrue(result.getFilteredText().contains(term), result.getFilteredText());

        final Span span = findSpan(result, input.indexOf(term), input.indexOf(term) + term.length());
        Assertions.assertTrue(span.isIgnored());

    }

    @ParameterizedTest(name = "fuzzy={0}")
    @ValueSource(booleans = {false, true})
    void aPolicyIgnoredTermWrittenWithSpacesIgnoresAMatchSplitByALineBreak(final boolean fuzzy) throws Exception {

        final String term = "UCLA Medical\r\nCenter";
        final String input = "Admitted to " + term + " today.";

        final Policy policy = policy("hospital", fuzzy, null);
        policy.setIgnored(List.of(new Ignored("hospitals", List.of("UCLA Medical Center"), List.of(), false)));

        Assertions.assertEquals(input, filter(policy, input).getFilteredText());

    }

    @Test
    void ngramPositionsAreThoseOfTheirOwnWords() {

        final SetDictionaryFilter filter = new SetDictionaryFilter(FilterType.SURNAME,
                new FilterConfiguration.FilterConfigurationBuilder().build(), Set.of("Smith"), null);

        Assertions.assertEquals(Map.of(0, "Smithson", 9, "Smith"), starts(filter.getNgramsOfLength("Smithson Smith", 1)));
        Assertions.assertEquals(Map.of(0, "Smith", 6, "Smith", 12, "Smith"), starts(filter.getNgramsOfLength("Smith Smith Smith", 1)));
        Assertions.assertEquals(Map.of(0, "a\r\nb", 3, "b\t\tc"), starts(filter.getNgramsOfLength("a\r\nb\t\tc ", 2)));
        Assertions.assertTrue(filter.getNgramsOfLength("  \n ", 1).isEmpty());

    }

    private static Map<Integer, String> starts(final Map<Position, String> ngrams) {

        final Map<Integer, String> starts = new TreeMap<>();
        ngrams.forEach((position, ngram) -> {
            Assertions.assertEquals(ngram.length(), position.getEnd() - position.getStart());
            starts.put(position.getStart(), ngram);
        });
        return starts;

    }

    private static void assertSpanCovers(final TextFilterResult result, final String input, final String term) {

        final int start = input.indexOf(term);
        final Span span = findSpan(result, start, start + term.length());

        Assertions.assertEquals(term, span.getText());
        Assertions.assertFalse(span.isIgnored());
        Assertions.assertFalse(result.getFilteredText().contains(term), result.getFilteredText());

    }

    private static Span findSpan(final TextFilterResult result, final int start, final int end) {

        return result.getExplanation().identifiedSpans().stream()
                .filter(s -> s.getCharacterStart() == start && s.getCharacterEnd() == end)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no span at [" + start + ", " + end + "): "
                        + result.getExplanation().identifiedSpans()));

    }

    private TextFilterResult filter(final Policy policy, final String input) throws Exception {

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), new DefaultContextService(), vectorService, null);

        return service.filter(policy, "context", input);

    }

    private Policy policy(final String type, final boolean fuzzy, final String ignore) {

        final String extra = type.equals("dictionaries")
                ? ", \"terms\": [\"" + CUSTOM_TERM + "\"], \"classification\": \"codenames\""
                : "";

        final String filter = "{ \"fuzzy\": " + fuzzy + extra + (ignore == null ? "" : ", " + ignore) + " }";
        final String identifier = type.equals("dictionaries") ? "[" + filter + "]" : filter;

        return new Gson().fromJson("{ \"identifiers\": { \"" + type + "\": " + identifier + " } }", Policy.class);

    }

}
