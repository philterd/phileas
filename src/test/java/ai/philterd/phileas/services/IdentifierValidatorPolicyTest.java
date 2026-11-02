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
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Properties;

/**
 * The aba, verhoeff, and damm validators named in a policy run on the custom identifier filter's
 * matches while filtering: a value that passes the check is redacted and a value of the same
 * shape that fails it is left in the text.
 */
class IdentifierValidatorPolicyTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "aba, \\\\b\\\\d{9}\\\\b, 021000021, 021000022",
            "verhoeff, \\\\b\\\\d{6}\\\\b, 758722, 758723",
            "damm, \\\\b\\\\d{6}\\\\b, 112946, 112949"
    })
    void theValidatorRunsDuringFiltering(final String validator, final String pattern, final String valid,
                                         final String invalid) throws Exception {

        final String json = "{ \"identifiers\": { \"identifiers\": [ { \"pattern\": \"" + pattern + "\", "
                + "\"classification\": \"" + validator + "-number\", \"validator\": \"" + validator + "\", "
                + "\"identifierFilterStrategies\": [ { \"strategy\": \"REDACT\", \"redactionFormat\": \"{{{REDACTED-%t}}}\" } ] } ] } }";

        final Policy policy = new Gson().fromJson(json, Policy.class);

        final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);

        final TextFilterResult result = service.filter(policy, "context", "Values " + valid + " and " + invalid + " on file.");

        Assertions.assertEquals("Values {{{REDACTED-id}}} and " + invalid + " on file.", result.getFilteredText());

    }

}
