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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

/**
 * Redaction of SSNs written with Unicode hyphens or wrapped across a line break, checked on the
 * filtered text rather than on the spans.
 */
public class SsnRedactionEndToEndTest {

    private PlainTextFilterService filterService;
    private Policy policy;

    @BeforeEach
    public void setup() throws Exception {

        filterService = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);

        final Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(List.of(new SsnFilterStrategy()));

        final Identifiers identifiers = new Identifiers();
        identifiers.setSsn(ssn);

        policy = new Policy();
        policy.setIdentifiers(identifiers);

    }

    @Test
    public void redactAscii() throws Exception {

        final TextFilterResult result = filterService.filter(policy, "context", "SSN: 078-05-1120 end");
        Assertions.assertEquals("SSN: {{{REDACTED-ssn}}} end", result.getFilteredText());

    }

    @Test
    public void redactNonBreakingHyphens() throws Exception {

        final TextFilterResult result = filterService.filter(policy, "context", "SSN: 078‑05‑1120 end");
        Assertions.assertEquals("SSN: {{{REDACTED-ssn}}} end", result.getFilteredText());

    }

    @Test
    public void redactLineWrapped() throws Exception {

        // The whole identifier goes, line break included, and the surrounding text is untouched.
        final TextFilterResult result = filterService.filter(policy, "context", "SSN: 078-05-\n1120 end");
        Assertions.assertEquals("SSN: {{{REDACTED-ssn}}} end", result.getFilteredText());

    }

    @Test
    public void redactRepeatedIdentifiers() throws Exception {

        final String input = "СНИЛС 078‑05‑1120 и 078-05-\n1120 конец";

        final TextFilterResult result = filterService.filter(policy, "context", input);
        Assertions.assertEquals("СНИЛС {{{REDACTED-ssn}}} и {{{REDACTED-ssn}}} конец",
                result.getFilteredText());

    }

    @Test
    public void leaveNumbersOnSeparateLines() throws Exception {

        final String input = "totals\n123\n45\n6789\n";

        final TextFilterResult result = filterService.filter(policy, "context", input);
        Assertions.assertEquals(input, result.getFilteredText());

    }

}
