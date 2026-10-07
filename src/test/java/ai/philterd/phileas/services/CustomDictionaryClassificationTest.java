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
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Properties;

import static org.mockito.Mockito.mock;

/**
 * A custom dictionary's classification reaches its spans and the %l redaction placeholder, whether
 * or not the dictionary is fuzzy.
 */
class CustomDictionaryClassificationTest {

    private final VectorService vectorService = mock(VectorService.class);

    @ParameterizedTest(name = "fuzzy={0}")
    @ValueSource(booleans = {false, true})
    void theClassificationIsOnTheSpanAndInTheReplacement(final boolean fuzzy) throws Exception {

        final String json = "{ \"identifiers\": { \"dictionaries\": [ { \"terms\": [\"Zephyrous\"], \"fuzzy\": " + fuzzy + ", "
                + "\"classification\": \"codename\", "
                + "\"customFilterStrategies\": [ { \"strategy\": \"REDACT\", \"redactionFormat\": \"{{{%t:%l}}}\" } ] } ] } }";

        final TextFilterResult result = filter(new Gson().fromJson(json, Policy.class), "Codename Zephyrous here.");

        Assertions.assertEquals("Codename {{{custom-dictionary:codename}}} here.", result.getFilteredText());

        for (final List<Span> spans : List.of(result.getExplanation().identifiedSpans(), result.getExplanation().appliedSpans())) {
            Assertions.assertEquals(1, spans.size());
            Assertions.assertEquals("codename", spans.get(0).getClassification());
        }

    }

    @Test
    void aBundledDictionaryHasNoClassification() throws Exception {

        final Policy policy = new Gson().fromJson("{ \"identifiers\": { \"city\": {} } }", Policy.class);

        final TextFilterResult result = filter(policy, "Moved to Boston today.");

        for (final List<Span> spans : List.of(result.getExplanation().identifiedSpans(), result.getExplanation().appliedSpans())) {
            Assertions.assertEquals(1, spans.size());
            Assertions.assertNull(spans.get(0).getClassification());
        }

    }

    private TextFilterResult filter(final Policy policy, final String input) throws Exception {

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), new DefaultContextService(), vectorService, null);

        return service.filter(policy, "context", input);

    }

}
