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
import ai.philterd.phileas.policy.IgnoredPattern;
import ai.philterd.phileas.policy.InvalidPolicyPatternException;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.Identifier;
import ai.philterd.phileas.policy.filters.Section;
import ai.philterd.phileas.policy.filters.Ssn;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.rules.IdentifierFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.SectionFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Properties;

/**
 * A policy-supplied regular expression is validated when the policy is loaded, so a bad pattern
 * fails the policy rather than whichever document first produces a token that reaches it.
 */
class PolicyPatternValidationTest {

    // Polynomial rather than exponential on this engine, but far past the budget at canary length.
    private static final String CATASTROPHIC = "(x+x+)+y";

    private static final String MALFORMED = "[";

    private static PlainTextFilterService filterService() throws Exception {
        return new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);
    }

    private static Policy withIdentifierPattern(final String pattern) {

        final Identifier identifier = new Identifier();
        identifier.setClassification("custom");
        identifier.setPattern(pattern);
        identifier.setIdentifierFilterStrategies(List.of(new IdentifierFilterStrategy()));

        final Identifiers identifiers = new Identifiers();
        identifiers.setIdentifiers(List.of(identifier));

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        return policy;

    }

    private static Policy withIgnoredPattern(final String pattern) {

        final Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(List.of(new SsnFilterStrategy()));
        ssn.setIgnoredPatterns(List.of(new IgnoredPattern("test-ignored", pattern)));

        final Identifiers identifiers = new Identifiers();
        identifiers.setSsn(ssn);

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        return policy;

    }

    @ParameterizedTest(name = "identifier: {0}")
    @CsvSource({
            "'[', 'not a valid regular expression'",
            "'(x+x+)+y', 'budget'",
            "'(?:[a-z]|\\d)*', 'overflowed the stack'"
    })
    void rejectIdentifierPattern(final String pattern, final String expected) throws Exception {

        final InvalidPolicyPatternException e = Assertions.assertThrows(InvalidPolicyPatternException.class,
                () -> filterService().filter(withIdentifierPattern(pattern), "context", "nothing here"));

        Assertions.assertTrue(e.getMessage().contains(expected), e.getMessage());

        // The message names the filter and the pattern, not just the failure.
        Assertions.assertTrue(e.getMessage().contains("identifier 'custom'"), e.getMessage());
        Assertions.assertTrue(e.getMessage().contains(pattern), e.getMessage());

    }

    @ParameterizedTest(name = "ignored: {0}")
    @CsvSource({
            "'[', 'not a valid regular expression'",
            "'(x+x+)+y', 'budget'",
            "'(?:[a-z]|\\d)*', 'overflowed the stack'"
    })
    void rejectIgnoredPattern(final String pattern, final String expected) throws Exception {

        final InvalidPolicyPatternException e = Assertions.assertThrows(InvalidPolicyPatternException.class,
                () -> filterService().filter(withIgnoredPattern(pattern), "context", "nothing here"));

        Assertions.assertTrue(e.getMessage().contains(expected), e.getMessage());
        Assertions.assertTrue(e.getMessage().contains("ignored pattern 'test-ignored' on the Ssn filter"), e.getMessage());
        Assertions.assertTrue(e.getMessage().contains(pattern), e.getMessage());

    }

    @Test
    void rejectBeforeAnyDocumentReachesThePattern() throws Exception {

        // The failure used to depend on the document: one with no matching token filtered fine.
        final Policy policy = withIgnoredPattern(MALFORMED);

        Assertions.assertThrows(InvalidPolicyPatternException.class,
                () -> filterService().filter(policy, "context", "no identifiers in this text at all"));

    }

    @Test
    void rejectTopLevelIgnoredPattern() throws Exception {

        final Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(List.of(new SsnFilterStrategy()));

        final Identifiers identifiers = new Identifiers();
        identifiers.setSsn(ssn);

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);
        policy.setIgnoredPatterns(List.of(new IgnoredPattern("top", MALFORMED)));

        final InvalidPolicyPatternException e = Assertions.assertThrows(InvalidPolicyPatternException.class,
                () -> filterService().filter(policy, "context", "the ssn is 123-45-6789."));

        Assertions.assertTrue(e.getMessage().contains("ignored pattern 'top' on the policy"), e.getMessage());

    }

    @Test
    void rejectSectionPatternNamingTheOffendingHalf() throws Exception {

        final Section section = new Section();
        section.setStartPattern("<start>");
        section.setEndPattern(MALFORMED);
        section.setSectionFilterStrategies(List.of(new SectionFilterStrategy()));

        final Identifiers identifiers = new Identifiers();
        identifiers.setSections(List.of(section));

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        final InvalidPolicyPatternException e = Assertions.assertThrows(InvalidPolicyPatternException.class,
                () -> filterService().filter(policy, "context", "nothing here"));

        Assertions.assertTrue(e.getMessage().contains("section end pattern"), e.getMessage());

    }

    @Test
    void aDisabledBudgetStillCompilesButDoesNotProbe() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("regex.timeout.ms", "0");

        final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(properties),
                new DefaultContextService(), new InMemoryVectorService(), null);

        // Nothing would stop a probe that does not finish, so the slow pattern is left alone.
        Assertions.assertDoesNotThrow(
                () -> service.filter(withIdentifierPattern(CATASTROPHIC), "context", "nothing here"));

        // A pattern that does not compile is still rejected.
        Assertions.assertThrows(InvalidPolicyPatternException.class,
                () -> service.filter(withIdentifierPattern(MALFORMED), "context", "nothing here"));

    }

    @Test
    void validPatternsLoadAndFilter() throws Exception {

        final Policy policy = withIgnoredPattern("\\d{3}-\\d{2}-\\d{4}");

        final TextFilterResult result = filterService().filter(policy, "context", "the ssn is 123-45-6789.");

        // The ignored pattern matches the SSN, so it is left in place.
        Assertions.assertEquals("the ssn is 123-45-6789.", result.getFilteredText());

    }

    @Test
    void validIdentifierPatternLoadsAndFilters() throws Exception {

        final Policy policy = withIdentifierPattern("[A-Z]{2}\\d{4}");

        final TextFilterResult result = filterService().filter(policy, "context", "the id is AB1234.");

        Assertions.assertEquals("the id is {{{REDACTED-id}}}.", result.getFilteredText());

    }

}
