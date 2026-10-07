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
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Properties;

import static org.mockito.Mockito.mock;

/**
 * The tracking number filter's allowSpaces option decides whether a number written in
 * space-separated groups is detected.
 */
class TrackingNumberAllowSpacesTest {

    private static final String REDACTED = "Ship {{{REDACTED-tracking-number}}} today.";

    private final VectorService vectorService = mock(VectorService.class);

    // Each row is the only carrier enabled, a number written without spaces, and the same number
    // written in groups.
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', value = {
            "ups   | 1Z999AA10123456784     | 1Z 999 AA1 01 2345 6784",
            "ups   | T1234567890            | T 123 456 7890",
            "fedex | 123456789012           | 1234 5678 9012",
            "fedex | 12345678901234567890   | 1234 5678 9012 3456 7890",
            "usps  | EA123456789US          | EA 123 456 789 US",
            "usps  | 7012345678901234       | 7012 3456 7890 1234",
            "usps  | 9400100000000000000000 | 9400 1000 0000 0000 0000 00",
    })
    void withSpacesAllowedBothFormsAreRedacted(final String carrier, final String compact, final String spaced) throws Exception {

        Assertions.assertEquals(REDACTED, filter(carrier, true, compact));
        Assertions.assertEquals(REDACTED, filter(carrier, true, spaced));

    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', value = {
            "ups   | 1Z999AA10123456784     | 1Z 999 AA1 01 2345 6784",
            "ups   | T1234567890            | T 123 456 7890",
            "fedex | 123456789012           | 1234 5678 9012",
            "fedex | 12345678901234567890   | 1234 5678 9012 3456 7890",
            "usps  | EA123456789US          | EA 123 456 789 US",
            "usps  | 7012345678901234       | 7012 3456 7890 1234",
    })
    void withoutSpacesAllowedOnlyTheCompactFormIsRedacted(final String carrier, final String compact, final String spaced) throws Exception {

        Assertions.assertEquals(REDACTED, filter(carrier, false, compact));
        Assertions.assertEquals("Ship " + spaced + " today.", filter(carrier, false, spaced));

    }

    @ParameterizedTest(name = "allowSpaces={0}")
    @CsvSource({"false", "true"})
    void aDifferentCarrierDoesNotMatchTheSpacedForm(final boolean allowSpaces) throws Exception {

        // A UPS number is not matched when only USPS is enabled.
        Assertions.assertEquals("Ship 1Z 999 AA1 01 2345 6784 today.", filter("usps", allowSpaces, "1Z 999 AA1 01 2345 6784"));

    }

    private String filter(final String carrier, final boolean allowSpaces, final String number) throws Exception {

        final String json = "{ \"identifiers\": { \"trackingNumber\": { "
                + "\"ups\": " + carrier.equals("ups") + ", "
                + "\"fedex\": " + carrier.equals("fedex") + ", "
                + "\"usps\": " + carrier.equals("usps") + ", "
                + "\"allowSpaces\": " + allowSpaces + " } } }";

        final Policy policy = new Gson().fromJson(json, Policy.class);

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), new DefaultContextService(), vectorService, null);

        return service.filter(policy, "context", "Ship " + number + " today.").getFilteredText();

    }

}
