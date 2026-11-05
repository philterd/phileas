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
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

/**
 * A dictionary-backed filter's own ignored terms and ignored patterns apply to the matched value,
 * whatever its case, with fuzzy matching both off and on.
 */
class DictionaryIgnoredTermsTest {

    private static final String TERM = "Quorlan";
    private static final String OTHER = "Thrandia";
    private static final String INPUT = "Notes from " + TERM + " and " + OTHER + " today";

    private final VectorService vectorService = mock(VectorService.class);

    static Stream<Arguments> filters() {
        return Stream.of("surname", "firstName", "city", "county", "state", "hospital", "dictionaries")
                .flatMap(type -> Stream.of(Arguments.of(type, false), Arguments.of(type, true)));
    }

    @ParameterizedTest(name = "{0} fuzzy={1}")
    @MethodSource("filters")
    void anIgnoredTermIsNotRedacted(final String type, final boolean fuzzy) throws Exception {
        assertOnlyOtherRedacted(type, fuzzy, "\"ignored\": [\"" + TERM + "\"]");
    }

    @ParameterizedTest(name = "{0} fuzzy={1}")
    @MethodSource("filters")
    void anIgnoredTermInLowercaseIsNotRedacted(final String type, final boolean fuzzy) throws Exception {
        assertOnlyOtherRedacted(type, fuzzy, "\"ignored\": [\"" + TERM.toLowerCase() + "\"]");
    }

    @ParameterizedTest(name = "{0} fuzzy={1}")
    @MethodSource("filters")
    void anIgnoredTermInUppercaseIsNotRedacted(final String type, final boolean fuzzy) throws Exception {
        assertOnlyOtherRedacted(type, fuzzy, "\"ignored\": [\"" + TERM.toUpperCase() + "\"]");
    }

    @ParameterizedTest(name = "{0} fuzzy={1}")
    @MethodSource("filters")
    void anIgnoredPatternMatchingTheValueIsNotRedacted(final String type, final boolean fuzzy) throws Exception {
        assertOnlyOtherRedacted(type, fuzzy, "\"ignoredPatterns\": [{\"pattern\": \"Q.*\"}]");
    }

    @ParameterizedTest(name = "{0} fuzzy={1}")
    @MethodSource("filters")
    void aPatternMatchingTheWholeInputButNotTheValueIgnoresNothing(final String type, final boolean fuzzy) throws Exception {

        final TextFilterResult result = filter(policy(type, fuzzy, "\"ignoredPatterns\": [{\"pattern\": \"Notes.*today\"}]"));

        Assertions.assertFalse(result.getFilteredText().contains(TERM), result.getFilteredText());
        Assertions.assertFalse(result.getFilteredText().contains(OTHER), result.getFilteredText());

    }

    @ParameterizedTest(name = "{0} fuzzy={1}")
    @MethodSource("filters")
    void withoutAnIgnoreListBothTermsAreRedacted(final String type, final boolean fuzzy) throws Exception {

        final TextFilterResult result = filter(policy(type, fuzzy, null));

        Assertions.assertFalse(result.getFilteredText().contains(TERM), result.getFilteredText());
        Assertions.assertFalse(result.getFilteredText().contains(OTHER), result.getFilteredText());

    }

    private void assertOnlyOtherRedacted(final String type, final boolean fuzzy, final String ignore) throws Exception {

        final TextFilterResult result = filter(policy(type, fuzzy, ignore));
        final String filtered = result.getFilteredText();

        Assertions.assertTrue(filtered.contains(TERM), filtered);
        Assertions.assertFalse(filtered.contains(OTHER), filtered);

        final List<Span> termSpans = result.getExplanation().identifiedSpans().stream()
                .filter(span -> TERM.equals(span.getText()))
                .toList();

        Assertions.assertFalse(termSpans.isEmpty(), "the term should still be identified");
        Assertions.assertTrue(termSpans.stream().allMatch(Span::isIgnored), "the term's spans should be ignored");

    }

    private TextFilterResult filter(final Policy policy) throws Exception {

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), new DefaultContextService(), vectorService, null);

        return service.filter(policy, "context", INPUT);

    }

    private Policy policy(final String type, final boolean fuzzy, final String ignore) {

        final String filter = "{ \"terms\": [\"" + TERM + "\", \"" + OTHER + "\"], \"fuzzy\": " + fuzzy
                + (type.equals("dictionaries") ? ", \"classification\": \"names\"" : "")
                + (ignore == null ? "" : ", " + ignore)
                + " }";

        final String identifier = type.equals("dictionaries") ? "[" + filter + "]" : filter;

        return new Gson().fromJson("{ \"identifiers\": { \"" + type + "\": " + identifier + " } }", Policy.class);

    }

}
