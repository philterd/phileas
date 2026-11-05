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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Properties;

/**
 * When spans from different filters overlap in part, none of the text they cover stays in the output.
 */
class OverlappingSpansEndToEndTest {

    private static final String POLICY = "{\"identifiers\": {\"ssn\": {}, \"phoneNumber\": {}, \"date\": {}, "
            + "\"zipCode\": {}, \"age\": {}, \"currency\": {}}}";

    private static String filter(final String policy, final String input) throws Exception {

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), new DefaultContextService(), new InMemoryVectorService(), null);

        return service.filter(new Gson().fromJson(policy, Policy.class), "context", input).getFilteredText();

    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "DOB 1/2/20 CAD stable. | DOB {{{REDACTED-date}}} {{{REDACTED-currency}}} stable.",
            "DOB 1/2/20 USD today. | DOB {{{REDACTED-date}}} {{{REDACTED-currency}}} today.",
            "DOB 1/2/20 SEK paid. | DOB {{{REDACTED-date}}} {{{REDACTED-currency}}} paid.",
            "Call 555-0187 USD today. | Call {{{REDACTED-phone-number}}} {{{REDACTED-currency}}} today.",
            "Call 555-0187 CAD stable. | Call {{{REDACTED-phone-number}}} {{{REDACTED-currency}}} stable.",
            "On March 5 dollars were paid. | On {{{REDACTED-date}}} {{{REDACTED-currency}}} were paid.",
            "SSN 123-45-67.89 CHF stable. | SSN {{{REDACTED-phone-number}}}{{{REDACTED-currency}}} stable.",
            "Age of 1/2/20 noted. | {{{REDACTED-age}}}{{{REDACTED-date}}} noted."
    })
    void textCoveredByAPartlyOverlappingSpanIsNotLeftInTheOutput(final String input, final String expected) throws Exception {
        Assertions.assertEquals(expected, filter(POLICY, input));
    }

    @Test
    void anIgnoredSpanDoesNotExposeTheSpanItOverlaps() throws Exception {

        final String policy = "{\"identifiers\": {\"date\": {}, \"currency\": {\"ignored\": [\"20 CAD\"]}}}";

        Assertions.assertEquals("DOB {{{REDACTED-date}}} CAD stable.", filter(policy, "DOB 1/2/20 CAD stable."));

    }

    @Test
    void aSpanWhoseConditionIsNotMetDoesNotExposeTheSpanItOverlaps() throws Exception {

        final String policy = "{\"identifiers\": {\"date\": {}, \"currency\": {\"currencyFilterStrategies\": "
                + "[{\"strategy\": \"REDACT\", \"condition\": \"context == \\\"other\\\"\"}]}}}";

        Assertions.assertEquals("DOB {{{REDACTED-date}}} CAD stable.", filter(policy, "DOB 1/2/20 CAD stable."));

    }

    @Test
    void anEarlierIgnoredSpanDoesNotExposeTheSpanItOverlaps() throws Exception {

        final String policy = "{\"identifiers\": {\"date\": {\"ignored\": [\"01/02/2020\"]}, \"currency\": {}}}";

        Assertions.assertEquals("DOB 01/02/{{{REDACTED-currency}}} paid.", filter(policy, "DOB 01/02/2020 USD paid."));

    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "phoneNumber | ignored | 123-45-6789 57 | ssn | SSN 123-45-6789 57 ok. | SSN {{{REDACTED-ssn}}} 57 ok.",
            "currency | ignored | 90210 USD | zipCode | Zip 90210 USD paid. | Zip {{{REDACTED-zip-code}}} USD paid."
    })
    void anIgnoredSpanDoesNotHideAValueOfAnotherFilterInsideIt(final String filter, final String property, final String term,
                                                               final String other, final String input, final String expected) throws Exception {

        final String policy = "{\"identifiers\": {\"" + other + "\": {}, \"" + filter + "\": {\"" + property + "\": [\"" + term + "\"]}}}";

        Assertions.assertEquals(expected, filter(policy, input));

    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "phoneNumber | phoneNumberFilterStrategies | ssn | SSN 123-45-6789 57 ok. | SSN {{{REDACTED-ssn}}} 57 ok.",
            "currency | currencyFilterStrategies | zipCode | Zip 90210 USD paid. | Zip {{{REDACTED-zip-code}}} USD paid."
    })
    void aSpanWhoseConditionIsNotMetDoesNotHideAValueOfAnotherFilterInsideIt(final String filter, final String strategies, final String other,
                                                                             final String input, final String expected) throws Exception {

        final String policy = "{\"identifiers\": {\"" + other + "\": {}, \"" + filter + "\": {\"" + strategies + "\": "
                + "[{\"strategy\": \"REDACT\", \"condition\": \"context == \\\"other\\\"\"}]}}}";

        Assertions.assertEquals(expected, filter(policy, input));

    }

    @Test
    void anIgnoredTermOnTheShorterSpansOwnFilterStillApplies() throws Exception {

        final String policy = "{\"identifiers\": {\"zipCode\": {\"ignored\": [\"90210\"]}, \"currency\": {}}}";

        Assertions.assertEquals("Zip {{{REDACTED-currency}}} paid.", filter(policy, "Zip 90210 USD paid."));

    }

    @Test
    void aPolicyLevelIgnoredTermLeavesItsTextAlone() throws Exception {

        final String policy = "{\"ignored\": [{\"terms\": [\"90210 USD\"]}], \"identifiers\": {\"zipCode\": {}, \"currency\": {}}}";

        Assertions.assertEquals("Zip 90210 USD paid.", filter(policy, "Zip 90210 USD paid."));

    }

    @Test
    void aSpanInsideAnotherSpanIsStillDropped() throws Exception {
        Assertions.assertEquals("Zip {{{REDACTED-currency}}} paid.", filter(POLICY, "Zip 90210 USD paid."));
    }

    @Test
    void spansFromTheSameFilterStillResolveToOne() throws Exception {
        Assertions.assertEquals("Age: {{{REDACTED-age}}}", filter(POLICY, "Age: 69 years"));
    }

}
