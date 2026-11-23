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
import ai.philterd.phileas.filters.Filter;
import ai.philterd.phileas.policy.Policy;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Every filter is given the settings a policy can set on any filter: ignored terms, ignored files, ignored
 * patterns, priority, id, and window size. The filters are read from the policy model, as in {@link FpeWiringTest},
 * so a filter added later that drops one of these settings fails here.
 */
public class FilterSettingsWiringTest {

    @TempDir
    static Path temp;

    @ParameterizedTest(name = "{0}")
    @MethodSource("ai.philterd.phileas.services.FpeWiringTest#filters")
    public void everyFilterIsGivenItsSettings(final String key, final boolean list, final String strategiesKey) throws Exception {

        final Path ignoredFile = temp.resolve(key + "-ignored.txt");
        Files.writeString(ignoredFile, "termfromfile\n");

        final String settings = "\"ignored\": [ \"ignoredterm\" ], "
                + "\"ignoredFiles\": [ \"" + ignoredFile.toString().replace("\\", "\\\\") + "\" ], "
                + "\"ignoredPatterns\": [ { \"name\": \"codes\", \"pattern\": \"qq[0-9]+\" } ], "
                + "\"priority\": 7, \"id\": \"filter-under-test\", \"windowSize\": 9, "
                + "\"" + strategiesKey + "\": [ { \"strategy\": \"REDACT\" } ]";

        final String body = "{ " + FpeWiringTest.required(key) + settings + " }";
        final String json = "{ \"identifiers\": { \"" + key + "\": " + (list ? "[ " + body + " ]" : body) + " } }";

        final List<Filter> filters = new FilterPolicyLoader(new PhileasConfiguration(new Properties()), new SecureRandom(), null)
                .getFiltersForPolicy(new Gson().fromJson(json, Policy.class), new HashMap<>());

        Assertions.assertFalse(filters.isEmpty(), key);

        for (final Filter filter : filters) {
            Assertions.assertEquals(7, filter.getPriority(), key + " priority");
            Assertions.assertEquals("filter-under-test", filter.getId(), key + " id");
            Assertions.assertEquals(9, filter.getWindowSize(), key + " windowSize");
            Assertions.assertTrue(filter.isIgnored("ignoredterm"), key + " ignored");
            Assertions.assertTrue(filter.isIgnored("termfromfile"), key + " ignoredFiles");
            Assertions.assertTrue(filter.isIgnored("qq123"), key + " ignoredPatterns");
            Assertions.assertFalse(filter.isIgnored("anything else"), key);
        }

    }

}
