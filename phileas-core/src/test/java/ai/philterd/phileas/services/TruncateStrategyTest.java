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
import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.ai.PhEyeFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.DateFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.ZipCodeFilterStrategy;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Properties;

import static org.mockito.Mockito.mock;

/**
 * TRUNCATE keeps at most length - 1 characters, so no value is returned whole and no length throws.
 */
class TruncateStrategyTest {

    private static final String CASES = """
            # value,    settings,                                                        expected
            AB,         '',                                                              A*
            ABCD,       '',                                                              ABC*
            ABCD1234,   '',                                                              ABCD****
            AB,         ', "truncateDirection": "TRAILING"',                             *B
            ABCD,       ', "truncateDirection": "TRAILING"',                             *BCD
            ABCD1234,   ', "truncateDirection": "TRAILING"',                             ****1234
            A,          ', "truncateLeaveCharacters": 2',                                *
            AB,         ', "truncateLeaveCharacters": 2',                                A*
            ABC,        ', "truncateLeaveCharacters": 2',                                AB*
            ABCD1234,   ', "truncateLeaveCharacters": 2',                                AB******
            A,          ', "truncateLeaveCharacters": 2, "truncateDirection": "TRAILING"', *
            AB,         ', "truncateLeaveCharacters": 2, "truncateDirection": "TRAILING"', *B
            ABC,        ', "truncateLeaveCharacters": 2, "truncateDirection": "TRAILING"', *BC
            ABCD1234,   ', "truncateLeaveCharacters": 2, "truncateDirection": "TRAILING"', ******34
            ABCD,       ', "truncateLeaveCharacters": 0',                                A***
            """;

    // A zip code keeps 1 to 4 of its five digits, whatever truncateLeaveCharacters the policy gives.
    private static final String ZIP_CODE_CASES = """
            # value,     settings,                                                          expected
            90210,       '',                                                                9021*
            90210,       ', "truncateLeaveCharacters": 0',                                  9****
            90210,       ', "truncateLeaveCharacters": 1',                                  9****
            90210,       ', "truncateLeaveCharacters": 4',                                  9021*
            90210,       ', "truncateLeaveCharacters": 5',                                  9021*
            90210,       ', "truncateLeaveCharacters": 6',                                  9021*
            90210-0110,  ', "truncateLeaveCharacters": 5',                                  9021*
            90210,       ', "truncateDirection": "TRAILING"',                               *0210
            90210,       ', "truncateLeaveCharacters": 0, "truncateDirection": "TRAILING"', ****0
            90210,       ', "truncateLeaveCharacters": 1, "truncateDirection": "TRAILING"', ****0
            90210,       ', "truncateLeaveCharacters": 4, "truncateDirection": "TRAILING"', *0210
            90210,       ', "truncateLeaveCharacters": 5, "truncateDirection": "TRAILING"', *0210
            90210,       ', "truncateLeaveCharacters": 6, "truncateDirection": "TRAILING"', *0210
            90210-0110,  ', "truncateLeaveCharacters": 5, "truncateDirection": "TRAILING"', *0210
            """;

    private final VectorService vectorService = mock(VectorService.class);

    @ParameterizedTest
    @CsvSource(textBlock = CASES)
    void standardStrategy(final String value, final String settings, final String expected) throws Exception {

        final String json = "{ \"identifiers\": { \"identifiers\": [ { \"pattern\": \"" + value + "\", "
                + "\"identifierFilterStrategies\": [ { \"strategy\": \"TRUNCATE\"" + settings + " } ] } ] } }";

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), new DefaultContextService(), vectorService, null);

        final String input = "Value " + value + " here.";

        Assertions.assertEquals("Value " + expected + " here.",
                service.filter(new Gson().fromJson(json, Policy.class), "context", input).getFilteredText());

    }

    @ParameterizedTest
    @CsvSource(textBlock = CASES)
    void dateStrategy(final String value, final String settings, final String expected) throws Exception {
        Assertions.assertEquals(expected, replace(DateFilterStrategy.class, value, settings));
    }

    @ParameterizedTest
    @CsvSource(textBlock = CASES)
    void phEyeStrategy(final String value, final String settings, final String expected) throws Exception {
        Assertions.assertEquals(expected, replace(PhEyeFilterStrategy.class, value, settings));
    }

    @ParameterizedTest
    @CsvSource(textBlock = ZIP_CODE_CASES)
    void zipCodePolicy(final String value, final String settings, final String expected) throws Exception {

        final String json = "{ \"identifiers\": { \"zipCode\": { "
                + "\"zipCodeFilterStrategies\": [ { \"strategy\": \"TRUNCATE\"" + settings + " } ] } } }";

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), new DefaultContextService(), vectorService, null);

        final String input = "Zip " + value + " here.";

        Assertions.assertEquals("Zip " + expected + " here.",
                service.filter(new Gson().fromJson(json, Policy.class), "context", input).getFilteredText());

    }

    @ParameterizedTest
    @CsvSource(textBlock = ZIP_CODE_CASES)
    void zipCodeStrategy(final String value, final String settings, final String expected) throws Exception {
        Assertions.assertEquals(expected, replace(ZipCodeFilterStrategy.class, value, settings));
    }

    private String replace(final Class<? extends AbstractFilterStrategy> type, final String value,
                           final String settings) throws Exception {

        final AbstractFilterStrategy strategy = new Gson().fromJson("{ \"strategy\": \"TRUNCATE\"" + settings + " }", type);

        return strategy.getReplacement(new DefaultContextService(), "label", "context", value,
                new String[]{value}, new Crypto(), new FPE(), null, null).getReplacement();

    }

}
