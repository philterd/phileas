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
package ai.philterd.phileas.services.documentprocessors;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.IncrementalRedaction;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.Ssn;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

/**
 * Exercises the replacement loop in {@link UnstructuredDocumentProcessor}, which applies span
 * replacements with a running offset. These tests focus on replacements whose length differs from
 * the original span (the case that previously relied on rebuilding the span list via shiftSpans),
 * including a shrinking replacement where the cumulative offset is negative.
 */
public class UnstructuredDocumentProcessorReplacementTest {

    private PlainTextFilterService service(final Properties properties) {
        return new PlainTextFilterService(new PhileasConfiguration(properties),
                new DefaultContextService(), new InMemoryVectorService(), null);
    }

    /** SSN policy that statically replaces each SSN with a single short token. */
    private Policy shortReplacementSsnPolicy() {
        final SsnFilterStrategy strategy = new SsnFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("#");

        final Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(List.of(strategy));

        final Identifiers identifiers = new Identifiers();
        identifiers.setSsn(ssn);

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);
        return policy;
    }

    @Test
    public void multipleShrinkingReplacementsArePlacedCorrectly() throws Exception {

        // Each SSN (11 chars) becomes "#" (1 char), so the running offset goes increasingly
        // negative. A wrong offset would misplace the second and third replacements.
        final TextFilterResult result = service(new Properties())
                .filter(shortReplacementSsnPolicy(), "context",
                        "A 123-45-6789 B 234-56-7890 C 345-67-8901 D");

        Assertions.assertEquals("A # B # C # D", result.getFilteredText());

    }

    @Test
    public void incrementalRedactionHashesMatchAfterShrinkingReplacements() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("incremental.redactions.enabled", "true");

        final TextFilterResult result = service(properties)
                .filter(shortReplacementSsnPolicy(), "context",
                        "A 123-45-6789 B 234-56-7890 C");

        Assertions.assertEquals("A # B # C", result.getFilteredText());
        Assertions.assertFalse(result.getIncrementalRedactions().isEmpty());

        // Each incremental snapshot must hash to its recorded hash, and they progress left-to-right.
        for (final IncrementalRedaction redaction : result.getIncrementalRedactions()) {
            Assertions.assertEquals(DigestUtils.sha256Hex(redaction.getIncrementallyRedactedText()),
                    redaction.getHash());
        }

        // First increment redacts the first SSN only; the final increment equals the filtered text.
        Assertions.assertEquals("A # B 234-56-7890 C",
                result.getIncrementalRedactions().get(0).getIncrementallyRedactedText());
        Assertions.assertEquals("A # B # C",
                result.getIncrementalRedactions().get(result.getIncrementalRedactions().size() - 1)
                        .getIncrementallyRedactedText());

    }

}
