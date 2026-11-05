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
package ai.philterd.phileas.services.split;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.Ssn;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Properties;

class SplitFactoryTest {

    @ParameterizedTest
    @CsvSource({
            "newline, NewLineSplitService",
            "NEWLINE, NewLineSplitService",
            "width, LineWidthSplitService",
            "Width, LineWidthSplitService",
            "characters, CharacterCountSplitService",
            "CHARACTERS, CharacterCountSplitService",
            "character, CharacterCountSplitService",
            "Character, CharacterCountSplitService"
    })
    void eachAcceptedMethodResolvesToItsSplitService(final String method, final String expected) {

        Assertions.assertEquals(expected, SplitFactory.getSplitService(method, 100).getClass().getSimpleName());

    }

    @ParameterizedTest
    @ValueSource(strings = {"sentence", "chars", "new line", ""})
    void anUnknownMethodRaisesAndNamesTheAcceptedMethods(final String method) {

        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> SplitFactory.getSplitService(method, 100));

        Assertions.assertTrue(ex.getMessage().contains("'" + method + "'"), ex.getMessage());
        Assertions.assertTrue(ex.getMessage().contains("newline, width, characters, character"), ex.getMessage());

    }

    @Test
    void aNullMethodRaises() {

        Assertions.assertThrows(IllegalArgumentException.class, () -> SplitFactory.getSplitService(null, 100));

    }

    @Test
    void theSpecificationExampleSplitsByCharacterCount() {

        // spec/v1.2.0/examples/splitting-overlap.json names the method in the singular.
        final Policy policy = new Gson().fromJson("""
                { "config": { "splitting": { "enabled": true, "threshold": 10000, "method": "character", "overlap": 200 } } }""",
                Policy.class);

        Assertions.assertInstanceOf(CharacterCountSplitService.class, SplitFactory.getSplitService(
                policy.getConfig().getSplitting().getMethod(), policy.getConfig().getSplitting().getThreshold()));

    }

    @Test
    void anUnknownMethodRaisesForADocumentBelowTheThreshold() {

        // The document is far below the threshold, so it would not be split, but the method is still
        // resolved and the policy error surfaces on the first document.
        final Policy policy = policy(true, "sentence");

        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> filter(policy, "ssn 123-45-6789"));

        Assertions.assertTrue(ex.getMessage().contains("'sentence'"), ex.getMessage());

    }

    @Test
    void anUnknownMethodRaisesForADocumentOverTheThreshold() {

        final Policy policy = policy(true, "sentence");
        policy.getConfig().getSplitting().setThreshold(10);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> filter(policy, "ssn 123-45-6789\nssn 234-56-7891"));

    }

    @Test
    void anUnknownMethodIsIgnoredWhenSplittingIsDisabled() throws Exception {

        final Policy policy = policy(false, "sentence");
        policy.getConfig().getSplitting().setThreshold(10);

        Assertions.assertEquals("ssn {{{REDACTED-ssn}}}", filter(policy, "ssn 123-45-6789").getFilteredText());

    }

    @Test
    void theCharacterAliasFiltersADocumentOverTheThreshold() throws Exception {

        final Policy policy = policy(true, "character");
        policy.getConfig().getSplitting().setThreshold(30);

        Assertions.assertEquals("The ssn is {{{REDACTED-ssn}}}. The other ssn is {{{REDACTED-ssn}}}.",
                filter(policy, "The ssn is 123-45-6789. The other ssn is 234-56-7891.").getFilteredText());

    }

    private Policy policy(final boolean enabled, final String method) {

        final Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(List.of(new SsnFilterStrategy()));

        final Identifiers identifiers = new Identifiers();
        identifiers.setSsn(ssn);

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);
        policy.getConfig().getSplitting().setEnabled(enabled);
        policy.getConfig().getSplitting().setMethod(method);

        return policy;

    }

    private TextFilterResult filter(final Policy policy, final String input) throws Exception {
        return new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null)
                .filter(policy, "context", input);
    }

}
