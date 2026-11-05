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
 * The UPS patterns do not match ordinary words or a bare 9-digit number.
 */
class TrackingNumberUpsFalsePositiveTest {

    private final VectorService vectorService = mock(VectorService.class);

    @ParameterizedTest(name = "allowSpaces={1} {0}")
    @CsvSource(delimiter = '|', textBlock = """
            The temperature was transported to the treatment room.     | false
            The temperature was transported to the treatment room.     | true
            THE TEMPERATURE WAS TRANSPORTED TO THE TREATMENT ROOM.     | false
            THE TEMPERATURE WAS TRANSPORTED TO THE TREATMENT ROOM.     | true
            Temperature and Transported were noted.                    | false
            Temperature and Transported were noted.                    | true
            Order 123456789 shipped today.                             | false
            Order 123456789 shipped today.                             | true
            Order 123 456 789 shipped today.                           | true
            Tracking T12345ABCDE shipped today.                        | false
            """)
    void notDetected(final String input, final boolean allowSpaces) throws Exception {
        Assertions.assertEquals(input, filter(input, allowSpaces));
    }

    @ParameterizedTest(name = "allowSpaces={1} {0}")
    @CsvSource(delimiter = '|', textBlock = """
            T1234567890    | false
            T1234567890    | true
            t1234567890    | false
            T 123 456 7890 | true
            """)
    void tPrefixedNumberIsDetected(final String number, final boolean allowSpaces) throws Exception {
        Assertions.assertEquals("Ship {{{REDACTED-tracking-number}}} today.",
                filter("Ship " + number + " today.", allowSpaces));
    }

    private String filter(final String input, final boolean allowSpaces) throws Exception {

        final String json = "{ \"identifiers\": { \"trackingNumber\": { \"allowSpaces\": " + allowSpaces + " } } }";

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), new DefaultContextService(), vectorService, null);

        return service.filter(new Gson().fromJson(json, Policy.class), "context", input).getFilteredText();

    }

}
