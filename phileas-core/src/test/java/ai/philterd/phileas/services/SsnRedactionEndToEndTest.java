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
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.Ssn;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Redaction of SSNs written with Unicode hyphens or wrapped across a line break, checked on the
 * filtered text rather than on the spans.
 */
class SsnRedactionEndToEndTest {

    private PlainTextFilterService filterService;
    private Policy policy;

    @BeforeEach
    void setup() {

        filterService = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);

        final Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(List.of(new SsnFilterStrategy()));

        final Identifiers identifiers = new Identifiers();
        identifiers.setSsn(ssn);

        policy = new Policy();
        policy.setIdentifiers(identifiers);

    }

    private static Stream<Arguments> redactions() {
        return Stream.of(
                Arguments.of("ASCII control",
                        "SSN: 078-05-1120 end",
                        "SSN: {{{REDACTED-ssn}}} end"),
                Arguments.of("non-breaking hyphens",
                        "SSN: 078‑05‑1120 end",
                        "SSN: {{{REDACTED-ssn}}} end"),
                // The whole identifier goes, line break included, and the surrounding text is untouched.
                Arguments.of("wrapped across a line break",
                        "SSN: 078-05-\n1120 end",
                        "SSN: {{{REDACTED-ssn}}} end"),
                Arguments.of("repeated identifiers in different forms",
                        "СНИЛС 078‑05‑1120 и 078-05-\n1120 конец",
                        "СНИЛС {{{REDACTED-ssn}}} и {{{REDACTED-ssn}}} конец"),
                // No hyphen precedes the breaks, so the numbers are left alone.
                Arguments.of("numbers on separate lines",
                        "totals\n123\n45\n6789\n",
                        "totals\n123\n45\n6789\n")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("redactions")
    void redact(final String description, final String input, final String expected) throws Exception {

        final TextFilterResult result = filterService.filter(policy, "context", input);
        Assertions.assertEquals(expected, result.getFilteredText(), description);

    }

}
