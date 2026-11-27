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
package ai.philterd.phileas.services.filters.filtering;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.RedactionWarning;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

public class CsvColumnRuleWarningsTest {

    private static final String CELL = "Roster cell text";
    private static final byte[] CSV = ("Name,Email\n" + CELL + ",pat@example.com\n").getBytes(StandardCharsets.UTF_8);

    private static BinaryDocumentFilterResult filter(final String columns, final FilterOptions options) throws Exception {
        final Policy policy = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{}},\"tabular\":{\"sheets\":"
                + "[{\"unlistedColumns\":\"ignore\",\"columns\":" + columns + "}]}}", Policy.class);
        return new CsvFilterService(new PhileasConfiguration(new Properties()), new DefaultContextService(),
                new InMemoryVectorService(), null).filter(policy, new DefaultContextService(), new InMemoryVectorService(),
                "ctx", CSV, MimeType.TEXT_CSV, options);
    }

    @Test
    public void aHeaderThatMatchesNoColumnIsAWarningOnTheResult() throws Exception {

        final BinaryDocumentFilterResult result = filter("[{\"header\":\"Social Security\",\"action\":\"redact\","
                + "\"strategies\":[{}]}]", FilterOptions.defaults());

        Assertions.assertEquals(List.of(RedactionWarning.unmatchedHeader("Social Security", null)), result.getWarnings());
        final String json = new Gson().toJson(result.getWarnings());
        Assertions.assertFalse(json.contains(CELL) || json.contains("Name"), json);

        // Text output has it too.
        Assertions.assertEquals(result.getWarnings(), filter("[{\"header\":\"Social Security\",\"action\":\"redact\","
                + "\"strategies\":[{}]}]", FilterOptions.defaults().withOutput(FilterOptions.Output.TEXT)).getWarnings());

    }

    @Test
    public void aMatchedHeaderAndAnIndexRuleGiveNoWarningAndNoneIsSerialized() throws Exception {

        final BinaryDocumentFilterResult result = filter("[{\"header\":\"Email\",\"action\":\"redact\",\"strategies\":[{}]},"
                + "{\"index\":9,\"action\":\"ignore\"}]", FilterOptions.defaults());

        Assertions.assertTrue(result.getWarnings().isEmpty(), result.getWarnings().toString());
        final JsonObject json = new Gson().toJsonTree(result).getAsJsonObject();
        Assertions.assertFalse(json.has("warnings"), json.toString());

    }

}
