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
package ai.philterd.phileas.services;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.Age;
import ai.philterd.phileas.policy.filters.PhoneNumber;
import ai.philterd.phileas.policy.filters.Ssn;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.rules.AgeFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.PhoneNumberFilterStrategy;
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
 * A phone number match must not run across the end of a sentence, where it would take the SSN before
 * the period and the number that starts the next sentence.
 */
class PhoneNumberSentenceBoundaryEndToEndTest {

    private PlainTextFilterService filterService;
    private Policy policy;

    @BeforeEach
    void setup() {

        filterService = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);

        final Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(List.of(new SsnFilterStrategy()));

        final PhoneNumber phoneNumber = new PhoneNumber();
        phoneNumber.setPhoneNumberFilterStrategies(List.of(new PhoneNumberFilterStrategy()));

        final Age age = new Age();
        age.setAgeFilterStrategies(List.of(new AgeFilterStrategy()));

        final Identifiers identifiers = new Identifiers();
        identifiers.setSsn(ssn);
        identifiers.setPhoneNumber(phoneNumber);
        identifiers.setAge(age);

        policy = new Policy();
        policy.setIdentifiers(identifiers);

    }

    private static Stream<Arguments> redactions() {
        return Stream.of(
                Arguments.of("SSN at the end of a sentence",
                        "SSN 123-45-6789.",
                        "SSN {{{REDACTED-ssn}}}."),
                Arguments.of("SSN, then a sentence with an age",
                        "SSN 123-45-6789. The patient is a 57-year-old male.",
                        "SSN {{{REDACTED-ssn}}}. The patient is a {{{REDACTED-age}}} male."),
                Arguments.of("SSN, then a sentence that starts with an age",
                        "SSN 123-45-6789. 57-year-old male.",
                        "SSN {{{REDACTED-ssn}}}. {{{REDACTED-age}}} male."),
                Arguments.of("SSN, then a non-breaking space and a sentence that starts with an age",
                        "SSN 123-45-6789.\u00A057-year-old male.",
                        "SSN {{{REDACTED-ssn}}}.\u00A0{{{REDACTED-age}}} male."),
                Arguments.of("phone number, then a sentence that starts with a number",
                        "Call 412-555-0187. 57 people called.",
                        "Call {{{REDACTED-phone-number}}}. 57 people called."),
                Arguments.of("phone number with dots",
                        "Call 412.555.0187 today.",
                        "Call {{{REDACTED-phone-number}}} today."),
                Arguments.of("phone number with a dot and a space between each group",
                        "Call 412. 555. 0187 today.",
                        "Call {{{REDACTED-phone-number}}} today."),
                Arguments.of("phone number with a dot and a space after the area code",
                        "Call 412. 555-0187 today.",
                        "Call {{{REDACTED-phone-number}}} today."),
                Arguments.of("phone number with a dot and a space before the last group",
                        "Call (412) 555. 0187 today.",
                        "Call {{{REDACTED-phone-number}}} today."),
                Arguments.of("phone number with a country code and spaced dots",
                        "Call +1 412. 555. 0187 today.",
                        "Call {{{REDACTED-phone-number}}} today."),
                Arguments.of("international phone number with a spaced dot",
                        "Call +44 20 7946. 0958 today.",
                        "Call {{{REDACTED-phone-number}}} today."),
                Arguments.of("phone number with spaced dots at the end of a sentence",
                        "Call 412. 555. 0187. Thanks.",
                        "Call {{{REDACTED-phone-number}}}. Thanks."),
                Arguments.of("phone number with spaced dots, then a sentence that starts with a number",
                        "Call 412. 555. 0187. 57 people called.",
                        "Call {{{REDACTED-phone-number}}}. 57 people called."),
                Arguments.of("phone number with a spaced dot after the area code, then a sentence that starts with a number",
                        "Call 412. 555-0187. 57 people called.",
                        "Call {{{REDACTED-phone-number}}}. 57 people called."),
                Arguments.of("phone number with a country code and spaced dots, then a sentence that starts with a number",
                        "Call +1 412. 555. 0187. 57 people called.",
                        "Call {{{REDACTED-phone-number}}}. 57 people called."),
                Arguments.of("international phone number with a spaced dot, then a sentence that starts with a number",
                        "Call +44 20 7946. 0958. 57 people called.",
                        "Call {{{REDACTED-phone-number}}}. 57 people called.")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("redactions")
    void redact(final String description, final String input, final String expected) throws Exception {

        final TextFilterResult result = filterService.filter(policy, "context", input);
        Assertions.assertEquals(expected, result.getFilteredText(), description);

    }

}
