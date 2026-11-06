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
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Properties;

/**
 * Guards against filtering time growing quadratically with the number of spans (issue #496). Before
 * the fix, 60,000 currency spans took minutes; they now take about a second, so the timeouts leave a
 * wide margin for slow machines while still failing if the growth returns.
 */
public class ManySpansFilteringTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private String filter(final String policyJson, final String input) throws Exception {

        final Policy policy = new Gson().fromJson(policyJson, Policy.class);
        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), new DefaultContextService(), new InMemoryVectorService(), null);

        return service.filter(policy, "context", input).getFilteredText();

    }

    @Test
    public void filterManySpans() {

        final String input = "1.2 USD ".repeat(60_000);

        final String filtered = Assertions.assertTimeoutPreemptively(TIMEOUT,
                () -> filter("{\"identifiers\": {\"currency\": {}}}", input));

        Assertions.assertEquals("{{{REDACTED-currency}}} ".repeat(60_000), filtered);

    }

    @Test
    public void filterManySpansThatOverlapAcrossFilters() {

        // The currency span 20 CAD overlaps the date 1/2/20 in each repetition.
        final String input = "DOB 1/2/20 CAD stable. ".repeat(30_000);

        final String filtered = Assertions.assertTimeoutPreemptively(TIMEOUT,
                () -> filter("{\"identifiers\": {\"date\": {}, \"currency\": {}}}", input));

        Assertions.assertEquals("DOB {{{REDACTED-date}}} {{{REDACTED-currency}}} stable. ".repeat(30_000), filtered);

    }

}
