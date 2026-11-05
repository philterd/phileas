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
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.mockito.Mockito.mock;

/**
 * When every strategy has a condition and none is satisfied, the value is left unchanged.
 */
class UnmatchedStrategyConditionTest {

    private static final String INPUT = "Contact jane.doe@example.com today.";

    private static final String CONDITIONAL_STRATEGIES =
            "{ \"condition\": \"token == \\\"other@example.com\\\"\", \"strategy\": \"REDACT\", \"redactionFormat\": \"{{{REDACTED-%t}}}\" }, "
            + "{ \"condition\": \"context == \\\"another-context\\\"\", \"strategy\": \"REDACT\", \"redactionFormat\": \"{{{REDACTED-%t}}}\" }";

    private final VectorService vectorService = mock(VectorService.class);

    @Test
    void noSatisfiedConditionLeavesTheValueUnchanged() throws Exception {

        final TextFilterResult result = filter(CONDITIONAL_STRATEGIES);

        Assertions.assertEquals(INPUT, result.getFilteredText());
        Assertions.assertTrue(result.getExplanation().appliedSpans().isEmpty());
        Assertions.assertEquals(1, result.getExplanation().identifiedSpans().size());

    }

    @Test
    void theSameStrategiesApplyWhenAConditionIsSatisfied() throws Exception {

        final TextFilterResult result = filter(CONDITIONAL_STRATEGIES, "another-context");

        Assertions.assertEquals("Contact {{{REDACTED-email-address}}} today.", result.getFilteredText());
        Assertions.assertEquals(1, result.getExplanation().appliedSpans().size());

    }

    @Test
    void aFinalUnconditionalStrategyAppliesWhenNoConditionIsSatisfied() throws Exception {

        final TextFilterResult result = filter(CONDITIONAL_STRATEGIES
                + ", { \"strategy\": \"REDACT\", \"redactionFormat\": \"{{{FALLBACK-%t}}}\" }");

        Assertions.assertEquals("Contact {{{FALLBACK-email-address}}} today.", result.getFilteredText());
        Assertions.assertEquals(1, result.getExplanation().appliedSpans().size());

    }

    @Test
    void noStrategiesDefaultsToRedact() throws Exception {

        final TextFilterResult result = filter("");

        Assertions.assertEquals("Contact {{{REDACTED-email-address}}} today.", result.getFilteredText());

    }

    private TextFilterResult filter(final String strategies) throws Exception {
        return filter(strategies, "context");
    }

    private TextFilterResult filter(final String strategies, final String context) throws Exception {

        final String json = "{ \"identifiers\": { \"emailAddress\": { \"emailAddressFilterStrategies\": [ "
                + strategies + " ] } } }";

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), new DefaultContextService(), vectorService, null);

        return service.filter(new Gson().fromJson(json, Policy.class), context, INPUT);

    }

}
