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
import ai.philterd.phileas.policy.filters.Itin;
import ai.philterd.phileas.policy.filters.Ssn;
import ai.philterd.phileas.policy.filters.ZipCode;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.rules.ItinFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.ZipCodeFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * A ZIP+4 code is redacted as a ZIP code, not as an SSN or ITIN, when the SSN, ITIN, and ZIP code
 * filters are all enabled. See issue #460.
 */
class ZipPlus4RedactionEndToEndTest {

    private PlainTextFilterService filterService;
    private Policy policy;

    @BeforeEach
    void setup() throws Exception {

        filterService = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);

        final Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(List.of(new SsnFilterStrategy()));

        final Itin itin = new Itin();
        itin.setItinFilterStrategies(List.of(new ItinFilterStrategy()));

        final ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(List.of(new ZipCodeFilterStrategy()));

        final Identifiers identifiers = new Identifiers();
        identifiers.setSsn(ssn);
        identifiers.setItin(itin);
        identifiers.setZipCode(zipCode);

        policy = new Policy();
        policy.setIdentifiers(identifiers);

    }

    private static Stream<Arguments> redactions() {
        return Stream.of(
                Arguments.of("ZIP+4",
                        "ships to 52168-1208 today",
                        "ships to {{{REDACTED-zip-code}}} today"),
                Arguments.of("ZIP+4 with a leading zero",
                        "ships to 02965-1837 today",
                        "ships to {{{REDACTED-zip-code}}} today"),
                Arguments.of("ZIP+4 beginning with 9",
                        "ships to 90210-1234 today",
                        "ships to {{{REDACTED-zip-code}}} today"),
                Arguments.of("SSN",
                        "SSN: 078-05-1120 end",
                        "SSN: {{{REDACTED-ssn}}} end"),
                Arguments.of("ITIN",
                        "ITIN: 912-70-1234 end",
                        "ITIN: {{{REDACTED-itin}}} end")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("redactions")
    void redact(final String description, final String input, final String expected) throws Exception {

        final TextFilterResult result = filterService.filter(policy, "context", input);
        Assertions.assertEquals(expected, result.getFilteredText(), description);

    }

}
