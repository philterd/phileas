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
package ai.philterd.phileas.filters.rules;

import ai.philterd.phileas.filters.rules.RegexProbe.Application;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The probe runs a pattern the way it is applied while filtering: searched for at every position
 * of a document, or anchored to the whole of a span's text.
 */
class RegexProbeTest {

    // https://github.com/philterd/phileas/issues/480: ignores a span that names a court. Anchored to a
    // span the lookahead scans the text once, but searched for it scans once per start position.
    private static final String NAMES_A_COURT = "(?i)(?=.*?\\b(?:court|tribunal|chamber)\\b).*";

    // Exponential however it is applied (issue #356).
    private static final String EXPONENTIAL = "(.*a){16}b";

    // A non-atomic repeated group recurses per character (issue #357).
    private static final String RECURSIVE = "(?:[a-z]|\\d)*";

    // A budget far below what either probe needs for a pattern that is fast, so a rejection is the
    // pattern's cost and not the machine's. The accepting cases use the default budget.
    private static final long SHORT_BUDGET_MS = 50;

    @ParameterizedTest(name = "anchored: {0}")
    @ValueSource(strings = {
            NAMES_A_COURT,
            "(?i).*\\b(?:court|tribunal|chamber)\\b.*",
            ".*(court|tribunal).*(district|county).*",
            "(?=.*court)(?=.*tribunal).*",
            "\\d{3}-\\d{2}-\\d{4}"
    })
    void anchoredAcceptsAPatternThatIsLinearOnASpan(final String regex) {
        Assertions.assertEquals(Optional.empty(),
                RegexProbe.probe(Pattern.compile(regex), 1000, Application.MATCHES));
    }

    @Test
    void searchedRejectsThePatternTheAnchoredProbeAccepts() {

        // The same pattern costs quadratic time when it is tried at every position of the canary.
        final Pattern pattern = Pattern.compile(".*(court|tribunal).*(district|county).*");

        Assertions.assertTrue(RegexProbe.probe(pattern, SHORT_BUDGET_MS, Application.MATCHES).isEmpty());

        final Optional<String> failure = RegexProbe.probe(pattern, SHORT_BUDGET_MS, Application.FIND);
        Assertions.assertTrue(failure.isPresent());
        Assertions.assertTrue(failure.get().startsWith("exceeded the 50 ms budget"), failure.get());

    }

    @Test
    void anchoredRejectsACatastrophicPattern() {

        final Optional<String> failure = RegexProbe.probe(Pattern.compile(EXPONENTIAL), SHORT_BUDGET_MS,
                Application.MATCHES);

        Assertions.assertTrue(failure.isPresent());
        Assertions.assertTrue(failure.get().startsWith("exceeded the 50 ms budget"), failure.get());

    }

    @Test
    void anchoredRejectsAPatternThatOverflowsTheStack() {

        final Optional<String> failure = RegexProbe.probe(Pattern.compile(RECURSIVE), 1000, Application.MATCHES);

        Assertions.assertTrue(failure.isPresent());
        Assertions.assertTrue(failure.get().startsWith("overflowed the stack"), failure.get());

    }

    @Test
    void searchedIsTheDefault() {

        Assertions.assertEquals(RegexProbe.probe(Pattern.compile(EXPONENTIAL), SHORT_BUDGET_MS, Application.FIND),
                RegexProbe.probe(Pattern.compile(EXPONENTIAL), SHORT_BUDGET_MS));

        Assertions.assertEquals(RegexProbe.probe(Pattern.compile(RECURSIVE), 1000, Application.FIND),
                RegexProbe.probe(Pattern.compile(RECURSIVE), 1000));

        Assertions.assertTrue(RegexProbe.probe(Pattern.compile("[A-Z]{2}[0-9]{4}"), 1000).isEmpty());

    }

}
