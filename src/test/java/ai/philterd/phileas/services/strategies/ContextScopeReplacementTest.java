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
package ai.philterd.phileas.services.strategies;

import ai.philterd.phileas.model.filtering.Replacement;
import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.services.anonymization.CityAnonymizationService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.strategies.dynamic.CityFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests CONTEXT-scope referential integrity in {@link AbstractFilterStrategy#getAnonymizedToken}.
 * In particular it guards the previously-broken case where a detected token happened to equal a
 * replacement value already stored for a different token: the strategy used to return a null
 * replacement, leaving the token unredacted (it surfaced downstream as the literal text "null").
 */
public class ContextScopeReplacementTest {

    private static final String[] WINDOW = new String[3];

    private CityFilterStrategy contextStrategy() {
        final CityFilterStrategy strategy = new CityFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);
        strategy.setReplacementScope(AbstractFilterStrategy.REPLACEMENT_SCOPE_CONTEXT);
        return strategy;
    }

    @Test
    public void sameTokenGetsSameReplacementWithinContext() throws Exception {

        final DefaultContextService contextService = new DefaultContextService();
        final CityAnonymizationService anonymizationService = new CityAnonymizationService(contextService);
        final CityFilterStrategy strategy = contextStrategy();

        final Replacement first = strategy.getReplacement("name", "ctx", "Springfield", WINDOW,
                new Crypto(), new FPE(), anonymizationService, null);
        final Replacement second = strategy.getReplacement("name", "ctx", "Springfield", WINDOW,
                new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertNotNull(first.getReplacement());
        Assertions.assertEquals(first.getReplacement(), second.getReplacement(),
                "the same token must map to the same replacement within a context");
    }

    @Test
    public void tokenEqualToAnExistingReplacementIsStillRedacted() throws Exception {

        final DefaultContextService contextService = new DefaultContextService();
        final CityAnonymizationService anonymizationService = new CityAnonymizationService(contextService);
        final CityFilterStrategy strategy = contextStrategy();

        // Simulate that an earlier token was anonymized to "Metropolis": that value now exists in
        // the context as a replacement (for some other, different token).
        contextService.putReplacement("some-other-token", "Metropolis", "LOCATION_CITY");

        // Now a real token whose text happens to equal that existing replacement value is detected.
        final Replacement replacement = strategy.getReplacement("name", "ctx", "Metropolis", WINDOW,
                new Crypto(), new FPE(), anonymizationService, null);

        // It must still be redacted (non-null) rather than left as a null/"null" replacement.
        Assertions.assertNotNull(replacement.getReplacement(),
                "a token equal to an existing replacement value must still be redacted, not left null");

        // And it must now be tracked under its own token so it is consistent on the next occurrence.
        Assertions.assertTrue(contextService.containsToken("Metropolis"),
                "the token should be stored in the context under its own key");

        final Replacement again = strategy.getReplacement("name", "ctx", "Metropolis", WINDOW,
                new Crypto(), new FPE(), anonymizationService, null);
        Assertions.assertEquals(replacement.getReplacement(), again.getReplacement(),
                "the token must map consistently on subsequent occurrences");
    }

}
