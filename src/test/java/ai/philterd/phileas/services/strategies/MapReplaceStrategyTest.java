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
import ai.philterd.phileas.services.anonymization.AlphanumericAnonymizationService;
import ai.philterd.phileas.services.anonymization.AnonymizationService;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.generators.ReplacementGenerator;
import ai.philterd.phileas.services.strategies.rules.IdentifierFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class MapReplaceStrategyTest {

    private static final String[] WINDOW = new String[3];

    private AbstractFilterStrategy strategy() {
        final AbstractFilterStrategy strategy = new IdentifierFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.MAP_REPLACE);
        return strategy;
    }

    private Replacement replace(final AbstractFilterStrategy strategy, final String token) throws Exception {
        final AnonymizationService anonymizationService = new AlphanumericAnonymizationService();
        final ContextService contextService = new DefaultContextService();
        return strategy.getReplacement(contextService, "name", "context", token, WINDOW,
                new Crypto(), new FPE(), anonymizationService, null);
    }

    @Test
    public void inlineMappingHit() throws Exception {

        final AbstractFilterStrategy strategy = strategy();
        strategy.setMappings(Map.of("Acme Corp", "Widget Co"));

        // Case-insensitive by default.
        Assertions.assertEquals("Widget Co", replace(strategy, "acme corp").getReplacement());
        Assertions.assertEquals("Widget Co", replace(strategy, "Acme Corp").getReplacement());

    }

    @Test
    public void caseSensitiveMapping() throws Exception {

        final AbstractFilterStrategy strategy = strategy();
        strategy.setCaseSensitive(true);
        strategy.setMappings(Map.of("Acme Corp", "Widget Co"));

        Assertions.assertEquals("Widget Co", replace(strategy, "Acme Corp").getReplacement());

        // A different case is now a miss and falls back (default REDACT).
        Assertions.assertEquals("{{{REDACTED-id}}}", replace(strategy, "acme corp").getReplacement());

    }

    @Test
    public void fallbackToRedactOnMiss() throws Exception {

        final AbstractFilterStrategy strategy = strategy();
        strategy.setMappings(Map.of("known", "replacement"));

        Assertions.assertEquals("{{{REDACTED-id}}}", replace(strategy, "unknown").getReplacement());

    }

    @Test
    public void fallbackToStaticReplaceOnMiss() throws Exception {

        final AbstractFilterStrategy strategy = strategy();
        strategy.setMappings(Map.of("known", "replacement"));
        strategy.setFallbackStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("REDACTED");

        Assertions.assertEquals("REDACTED", replace(strategy, "unknown").getReplacement());

    }

    @Test
    public void generatorUsedOnMiss() throws Exception {

        final AbstractFilterStrategy strategy = strategy();
        strategy.setMappings(Map.of("known", "replacement"));
        strategy.setReplacementGenerator((token, label) -> "generated-for-" + token);

        Assertions.assertEquals("generated-for-unknown", replace(strategy, "unknown").getReplacement());

    }

    @Test
    public void generatorOutputEqualToInputFallsBack() throws Exception {

        final AbstractFilterStrategy strategy = strategy();
        strategy.setFallbackStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("FALLBACK");
        // The generator echoes the input back (here in a different case); it must be rejected.
        strategy.setReplacementGenerator((token, label) -> token.toUpperCase());

        Assertions.assertEquals("FALLBACK", replace(strategy, "acme corp").getReplacement());

    }

    @Test
    public void generatorOutputWithReintroducedPiiFallsBack() throws Exception {

        final AbstractFilterStrategy strategy = strategy();
        strategy.setFallbackStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("FALLBACK");
        strategy.setReplacementGenerator((token, label) -> "call 555-867-5309");
        // A validator that flags the generated value as containing PII forces a fallback.
        strategy.setReplacementValidator(candidate -> true);

        Assertions.assertEquals("FALLBACK", replace(strategy, "unknown").getReplacement());

    }

    @Test
    public void generatorOutputPassingValidationIsUsed() throws Exception {

        final AbstractFilterStrategy strategy = strategy();
        strategy.setReplacementGenerator((token, label) -> "Safe Value");
        strategy.setReplacementValidator(candidate -> false);

        Assertions.assertEquals("Safe Value", replace(strategy, "unknown").getReplacement());

    }

    @Test
    public void generatorInvokedOncePerTokenInContextScope() throws Exception {

        final AbstractFilterStrategy strategy = strategy();
        strategy.setReplacementScope(AbstractFilterStrategy.REPLACEMENT_SCOPE_CONTEXT);

        final int[] calls = {0};
        strategy.setReplacementGenerator((token, label) -> {
            calls[0]++;
            return "generated-" + calls[0];
        });

        final ContextService contextService = new DefaultContextService();
        final AnonymizationService anonymizationService = new AlphanumericAnonymizationService();

        final String first = strategy.getReplacement(contextService, "name", "context", "unknown", WINDOW,
                new Crypto(), new FPE(), anonymizationService, null).getReplacement();
        final String second = strategy.getReplacement(contextService, "name", "context", "unknown", WINDOW,
                new Crypto(), new FPE(), anonymizationService, null).getReplacement();

        // The same token in the same context yields the same output, and the generator ran only once.
        Assertions.assertEquals(first, second);
        Assertions.assertEquals(1, calls[0]);

    }

    @Test
    public void failingGeneratorNotRetriedForRepeatedTokenInContextScope() throws Exception {

        final AbstractFilterStrategy strategy = strategy();
        strategy.setReplacementScope(AbstractFilterStrategy.REPLACEMENT_SCOPE_CONTEXT);
        strategy.setFallbackStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("FALLBACK");

        final int[] calls = {0};
        strategy.setReplacementGenerator((token, label) -> {
            calls[0]++;
            throw new RuntimeException("generator unavailable");
        });

        final ContextService contextService = new DefaultContextService();
        final AnonymizationService anonymizationService = new AlphanumericAnonymizationService();

        final String first = strategy.getReplacement(contextService, "name", "context", "unknown", WINDOW,
                new Crypto(), new FPE(), anonymizationService, null).getReplacement();
        final String second = strategy.getReplacement(contextService, "name", "context", "unknown", WINDOW,
                new Crypto(), new FPE(), anonymizationService, null).getReplacement();

        // The fallback is cached in context, so the failing generator is not retried for the same token.
        Assertions.assertEquals("FALLBACK", first);
        Assertions.assertEquals("FALLBACK", second);
        Assertions.assertEquals(1, calls[0]);

    }

    @Test
    public void documentScopeDoesNotCacheAcrossCalls() throws Exception {

        final AbstractFilterStrategy strategy = strategy();
        // DOCUMENT scope is the default; each call resolves independently.
        final int[] calls = {0};
        strategy.setReplacementGenerator((token, label) -> "generated-" + (++calls[0]));

        replace(strategy, "unknown");
        replace(strategy, "unknown");

        Assertions.assertEquals(2, calls[0]);

    }

    @Test
    public void generatorFailureFallsBack() throws Exception {

        final AbstractFilterStrategy strategy = strategy();
        strategy.setMappings(Map.of("known", "replacement"));
        strategy.setFallbackStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("FALLBACK");

        final ReplacementGenerator failing = (token, label) -> {
            throw new RuntimeException("generator unavailable");
        };
        strategy.setReplacementGenerator(failing);

        Assertions.assertEquals("FALLBACK", replace(strategy, "unknown").getReplacement());

    }

    @Test
    public void blankGeneratorOutputFallsBack() throws Exception {

        final AbstractFilterStrategy strategy = strategy();
        strategy.setFallbackStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("FALLBACK");
        strategy.setReplacementGenerator((token, label) -> "   ");

        Assertions.assertEquals("FALLBACK", replace(strategy, "unknown").getReplacement());

    }

    @Test
    public void inlineMappingsOverrideFileMappings() throws Exception {

        final AbstractFilterStrategy strategy = strategy();
        strategy.setMappings(Map.of("shared", "inline-wins"));
        // Simulate the file-loaded map that Filter builds from mappingFiles.
        strategy.initializeMappings(Map.of("shared", "file-value", "fileonly", "file-only-value"));

        Assertions.assertEquals("inline-wins", replace(strategy, "shared").getReplacement());
        Assertions.assertEquals("file-only-value", replace(strategy, "fileonly").getReplacement());

    }

    @Test
    public void mappingFilesGetterDefaultsEmpty() {
        Assertions.assertEquals(List.of(), strategy().getMappingFiles());
    }

}
